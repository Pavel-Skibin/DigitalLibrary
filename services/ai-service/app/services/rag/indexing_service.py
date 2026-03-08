"""
RAG Indexing Service.

Координирует полный пайплайн индексации одной книги:
  FB2 → Парсинг → Чанкинг → Embeddings → Qdrant
"""

from __future__ import annotations

import math
import time
from dataclasses import dataclass
from typing import List, Optional

try:
    from tqdm import tqdm as _tqdm
    _TQDM_AVAILABLE = True
except ImportError:
    _TQDM_AVAILABLE = False

from loguru import logger
from qdrant_client.models import PointStruct

from app.config import Settings
from app.services.rag.fb2_parser import FB2Parser
from app.services.rag.chunking_service import ChunkingService, TextChunk
from app.services.shared.qdrant_service import QdrantService
from app.services.shared.rest_client_service import RestClientService
from app.services.recommendations.embedding_service import EmbeddingService


# ─── Статус индексации ─────────────────────────────────────────────────────────

@dataclass
class IndexingResult:
    """Результат индексации книги."""
    book_id: int
    status: str                 # "success" | "error" | "skipped"
    total_chunks: int = 0
    chapters_found: int = 0
    message: str = ""

    @property
    def success(self) -> bool:
        return self.status == "success"


# ─── Сервис ────────────────────────────────────────────────────────────────────

class RAGIndexingService:
    """
    Полный пайплайн индексации одной книги в Qdrant RAG-коллекцию.

    Зависимости передаются через конструктор (Dependency Injection).
    """

    # ID-формула: book_id * CHUNK_ID_OFFSET + chunk_index
    # Даёт до 1 000 000 чанков на книгу — более чем достаточно.
    CHUNK_ID_OFFSET = 1_000_000

    def __init__(
        self,
        settings: Settings,
        qdrant: QdrantService,
        rest_client: RestClientService,
        embedding_service: EmbeddingService,
    ):
        self.settings = settings
        self.qdrant = qdrant
        self.rest_client = rest_client
        self.embedding_service = embedding_service

        self.fb2_parser = FB2Parser()
        self.chunking_service = ChunkingService(settings)

    # ─── Публичный API ─────────────────────────────────────────────────────────

    async def index_book(
        self,
        book_id: int,
        force_reindex: bool = False,
    ) -> IndexingResult:
        """
        Полная индексация одной книги.

        Args:
            book_id:       ID книги в book-catalog-service.
            force_reindex: Если True — удаляет существующие чанки перед
                           индексацией.

        Returns:
            IndexingResult с результатом операции.
        """
        logger.info(f"Starting RAG indexing for book_id={book_id} (force={force_reindex})")

        # 1. Проверяем, не проиндексирована ли уже книга
        existing_count = self.qdrant.count_book_chunks(book_id)
        if existing_count > 0 and not force_reindex:
            logger.info(f"book_id={book_id} already indexed ({existing_count} chunks). Skipping.")
            return IndexingResult(
                book_id=book_id,
                status="skipped",
                total_chunks=existing_count,
                message=f"Already indexed ({existing_count} chunks). Use force_reindex=True to re-index.",
            )

        if existing_count > 0 and force_reindex:
            logger.info(f"Re-indexing book_id={book_id}: removing {existing_count} old chunks …")
            self.qdrant.delete_book_chunks(book_id)

        # 2. Получаем FB2 XML из book-catalog-service
        fb2_xml = await self.rest_client.get_book_fb2(book_id)
        if fb2_xml is None:
            return IndexingResult(
                book_id=book_id,
                status="error",
                message="Failed to fetch FB2 content from book-catalog-service",
            )

        # 3. Парсим FB2
        try:
            content = self.fb2_parser.parse(fb2_xml)
        except Exception as exc:
            logger.error(f"FB2 parse error for book_id={book_id}: {exc}")
            return IndexingResult(
                book_id=book_id,
                status="error",
                message=f"FB2 parse error: {exc}",
            )

        if not content.chapters:
            return IndexingResult(
                book_id=book_id,
                status="error",
                message="No chapters found in FB2 content — nothing to index",
            )

        # 4. Получаем полные метаданные книги (PostgreSQL через book-catalog-service)
        book_metadata = await self._build_book_metadata(book_id, content)

        # 5. Чанкинг
        chunks = self.chunking_service.chunk_book(
            book_id=book_id,
            content=content,
            book_metadata=book_metadata,
        )

        if not chunks:
            return IndexingResult(
                book_id=book_id,
                status="error",
                message="Chunking produced 0 chunks",
            )

        logger.info(
            f"book_id={book_id} | chapters={len(content.chapters)} | chunks={len(chunks)}"
        )

        # 6. Генерация embeddings + upsert в Qdrant (батчами)
        await self._embed_and_upsert(chunks)

        logger.info(f"book_id={book_id} indexed: {len(chunks)} chunks stored in Qdrant")
        return IndexingResult(
            book_id=book_id,
            status="success",
            total_chunks=len(chunks),
            chapters_found=len(content.chapters),
            message=f"Successfully indexed {len(chunks)} chunks from {len(content.chapters)} chapters",
        )

    # ─── Внутренние методы ─────────────────────────────────────────────────────

    async def _build_book_metadata(self, book_id: int, content) -> dict:
        """
        Собирает метаданные книги: сначала из PostgreSQL (REST),
        затем добавляем что есть в FB2.
        """
        meta = dict(content.metadata)  # title, authors, genres, language, year, publisher

        # Пробуем обогатить из PostgreSQL
        try:
            book_info = await self.rest_client.get_book_metadata(book_id)
            if book_info:
                meta.update({
                    "title":            book_info.title or meta.get("title", ""),
                    "authors":          book_info.authors or meta.get("authors", []),
                    "genres":           book_info.genres or meta.get("genres", []),
                    "language":         book_info.language or meta.get("language", "ru"),
                    "publication_year": book_info.publication_year or meta.get("publication_year"),
                    "avg_rating":       book_info.average_rating or 0.0,
                    "popularity_score": float(book_info.ratings_count or 0),
                    "tags":             book_info.tags or [],
                    "description":      book_info.description or "",
                    "age_rating":       book_info.age_rating or "",
                })
        except Exception as exc:
            logger.warning(f"Could not enrich metadata from PostgreSQL for book_id={book_id}: {exc}")

        # Дефолты для полей LLM-рекомендаций (заполнятся позже в Phase 4)
        meta.setdefault("book_summary", "")
        meta.setdefault("themes", [])
        meta.setdefault("mood", "")
        meta.setdefault("writing_style", "")
        meta.setdefault("complexity_level", "средний")
        meta.setdefault("target_audience", "взрослые")
        meta.setdefault("era", "")
        meta.setdefault("avg_rating", 0.0)
        meta.setdefault("popularity_score", 0.0)

        return meta

    async def _embed_and_upsert(self, chunks: List[TextChunk]) -> None:
        """Генерит embeddings батчами и заливает в Qdrant."""
        batch_size = self.settings.RAG_BATCH_SIZE
        n_batches = math.ceil(len(chunks) / batch_size)
        total_chunks = len(chunks)

        logger.info(
            f"Starting vectorization: {total_chunks} chunks, "
            f"{n_batches} batches of {batch_size}"
        )

        batch_iter = range(n_batches)
        if _TQDM_AVAILABLE:
            batch_iter = _tqdm(
                batch_iter,
                total=n_batches,
                desc="Embedding",
                unit="batch",
                ncols=80,
            )

        t_start = time.time()
        chunks_done = 0

        for batch_num in batch_iter:
            batch = chunks[batch_num * batch_size : (batch_num + 1) * batch_size]
            texts = [c.text for c in batch]

            if not _TQDM_AVAILABLE:
                elapsed = time.time() - t_start
                speed = chunks_done / elapsed if elapsed > 0 else 0
                eta   = (total_chunks - chunks_done) / speed if speed > 0 else 0
                logger.info(
                    f"  Батч {batch_num + 1:>3}/{n_batches}  "
                    f"чанков {chunks_done:>4}/{total_chunks}  "
                    f"{speed:.1f} чанк/с  "
                    f"ETA {eta:.0f}с"
                )

            # Dense embeddings (USER-bge-m3, no prefix — RAG model)
            dense_embeddings = self.embedding_service.get_embeddings_batch(texts)

            # Собираем PointStruct
            points: List[PointStruct] = []
            for chunk, dense_vec in zip(batch, dense_embeddings):
                point_id = chunk.book_id * self.CHUNK_ID_OFFSET + chunk.chunk_index

                # BM25 sparse vector
                sparse_vec = self.qdrant._generate_bm25_sparse_vector(chunk.text)

                payload = {
                    # ── контент ──────────────────────────────────────────────
                    "text":                 chunk.text,
                    "token_count":          chunk.token_count,
                    # ── позиция ──────────────────────────────────────────────
                    "book_id":              chunk.book_id,
                    "chunk_index":          chunk.chunk_index,
                    "total_chunks":         chunk.total_chunks,
                    "chapter_title":        chunk.chapter_title,
                    "chapter_index":        chunk.chapter_index,
                    "position_in_chapter":  chunk.position_in_chapter,
                    # ── метаданные книги ──────────────────────────────────────
                    "title":                chunk.book_metadata.get("title", ""),
                    "authors":              chunk.book_metadata.get("authors", []),
                    "genres":               chunk.book_metadata.get("genres", []),
                    "tags":                 chunk.book_metadata.get("tags", []),
                    "language":             chunk.book_metadata.get("language", "ru"),
                    "publication_year":     chunk.book_metadata.get("publication_year"),
                    "publisher":            chunk.book_metadata.get("publisher", ""),
                    "description":          chunk.book_metadata.get("description", ""),
                    "age_rating":           chunk.book_metadata.get("age_rating", ""),
                    "word_count":           chunk.book_metadata.get("word_count"),
                    # LLM-метаданные (заполняются при индексации если RAG_ENABLE_LLM_METADATA=True)
                    "book_summary":         chunk.book_metadata.get("book_summary", ""),
                    "themes":               chunk.book_metadata.get("themes", []),
                    "mood":                 chunk.book_metadata.get("mood", ""),
                    "writing_style":        chunk.book_metadata.get("writing_style", ""),
                    "complexity_level":     chunk.book_metadata.get("complexity_level", "средний"),
                    "target_audience":      chunk.book_metadata.get("target_audience", "взрослые"),
                    "era":                  chunk.book_metadata.get("era", ""),
                    # ── аналитика ────────────────────────────────────────────
                    "avg_rating":           chunk.book_metadata.get("avg_rating", 0.0),
                    "popularity_score":     chunk.book_metadata.get("popularity_score", 0.0),
                }

                points.append(
                    PointStruct(
                        id=point_id,
                        vector={
                            "text_dense":  dense_vec.tolist(),
                            "text_sparse": sparse_vec,
                        },
                        payload=payload,
                    )
                )

            self.qdrant.upsert_rag_chunks(points)
            chunks_done += len(batch)

        elapsed_total = time.time() - t_start
        speed_total = total_chunks / elapsed_total if elapsed_total > 0 else 0
        logger.info(
            f"Vectorization complete: {total_chunks} chunks in {elapsed_total:.1f}s "
            f"({speed_total:.1f} chunks/s)"
        )
