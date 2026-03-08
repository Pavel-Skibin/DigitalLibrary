"""
RAG Retrieval Service.

Гибридный поиск по чанкам книг в Qdrant:
  * Dense vectors (USER-bge-m3, "text_dense")
  * Sparse vectors (BM25, "text_sparse")
  * RRF fusion для объединения результатов
  * Optional reranking (BAAI/bge-reranker-v2-m3)
  * get_context_window — соседние чанки для расширения контекста
"""

from __future__ import annotations

from dataclasses import dataclass, field
from typing import List, Optional, Tuple

from loguru import logger
from qdrant_client.models import (
    Filter,
    FieldCondition,
    MatchValue,
    MatchAny,
    Range,
)

from app.config import Settings
from app.services.shared.qdrant_service import QdrantService
from app.services.recommendations.embedding_service import EmbeddingService
from app.services.rag.reranker_service import RerankerService


# ─── Результат поиска ──────────────────────────────────────────────────────────

@dataclass
class RetrievedChunk:
    """Чанк, найденный при поиске."""
    text: str
    book_id: int
    chapter_title: str
    chapter_index: int
    chunk_index: int
    position_in_chapter: float
    token_count: int
    score: float
    # Метаданные книги (из payload)
    metadata: dict = field(default_factory=dict)


# ─── Сервис ────────────────────────────────────────────────────────────────────

class RAGRetrievalService:
    """
    Сервис для поиска релевантных чанков в RAG Qdrant-коллекции.

    Реализует:
      - Hybrid search (Dense + BM25 Sparse) с RRF-объединением
      - Фильтрацию по book_id
      - Context window expansion (соседние чанки)
    """

    # RRF константа (стандарт = 60)
    _RRF_K = 60

    def __init__(
        self,
        settings: Settings,
        qdrant: QdrantService,
        embedding_service: EmbeddingService,
        reranker: Optional[RerankerService] = None,
    ):
        self.settings = settings
        self.qdrant = qdrant
        self.embedding_service = embedding_service
        self.reranker = reranker  # Optional: для переранжирования результатов

    # ─── Публичный API ─────────────────────────────────────────────────────────

    def search_chunks(
        self,
        query: str,
        top_k: Optional[int] = None,
        book_ids: Optional[List[int]] = None,
        use_hybrid: bool = True,
        auto_filter_books: bool = False,
        max_books_for_auto_filter: int = 3,
    ) -> List[RetrievedChunk]:
        """
        Поиск чанков, релевантных запросу.

        Args:
            query:      Текст запроса пользователя.
            top_k:      Количество возвращаемых результатов (по умолчанию из settings).
            book_ids:   Ограничить поиск конкретными книгами (None = все книги).
            use_hybrid: True — Dense + BM25 + RRF; False — только Dense.
            auto_filter_books: True — автоматически найти релевантные книги перед поиском.
            max_books_for_auto_filter: Количество книг для авто-фильтрации (default: 3).

        Returns:
            Список RetrievedChunk, отсортированных по убыванию релевантности.
        """
        if top_k is None:
            top_k = self.settings.RAG_TOP_K

        # ── Smart Book Filtering (Two-Stage Retrieval) ────────────────────────
        # Если включена авто-фильтрация и book_ids не заданы явно → находим книги
        if auto_filter_books and not book_ids:
            book_ids = self._find_relevant_books(query, limit=max_books_for_auto_filter * 3)
            if book_ids:
                book_ids = book_ids[:max_books_for_auto_filter]
                logger.info(f"Auto-filter: found {len(book_ids)} relevant books: {book_ids}")
            else:
                logger.warning("Auto-filter: no books found, searching entire collection")

        logger.info(
            f"RAG search | query={query[:80]!r} | top_k={top_k} "
            f"| book_ids={book_ids} | hybrid={use_hybrid}"
        )

        # Генерируем dense embedding (USER-bge-m3 — без префиксов)
        query_dense = self.embedding_service.get_embedding(query)

        # Фильтр по книгам
        search_filter = self._build_filter(book_ids)

        # Overfetch для RRF слияния (если reranker включён — больше overfetch)
        use_reranker = self.settings.ENABLE_RERANKER and self.reranker and self.reranker.is_available()
        if use_reranker:
            fetch_limit = self.settings.RAG_RERANKER_OVERFETCH
        else:
            fetch_limit = max(top_k * 3, 20)

        # ── Dense search ──────────────────────────────────────────────────────
        dense_results = self.qdrant.client.query_points(
            collection_name=self.qdrant.collection_rag,
            query=query_dense.tolist(),
            using="text_dense",
            limit=fetch_limit,
            query_filter=search_filter,
            with_payload=True,
        ).points
        logger.debug(f"Dense search → {len(dense_results)} results")

        # ── Sparse (BM25) search ──────────────────────────────────────────────
        sparse_results = []
        if use_hybrid:
            query_sparse = self.qdrant._generate_bm25_sparse_vector(query)
            try:
                sparse_results = self.qdrant.client.query_points(
                    collection_name=self.qdrant.collection_rag,
                    query=query_sparse,
                    using="text_sparse",
                    limit=fetch_limit,
                    query_filter=search_filter,
                    with_payload=True,
                ).points
                logger.debug(f"BM25 search → {len(sparse_results)} results")
            except Exception as exc:
                logger.warning(f"BM25 search failed (dense-only fallback): {exc}")

        # ── RRF слияние ───────────────────────────────────────────────────────
        # Overfetch для reranking (или сразу top_k если reranker отключён)
        rrf_limit = fetch_limit if use_reranker else top_k
        merged = self._rrf_merge(dense_results, sparse_results, rrf_limit)

        chunks = [self._point_to_chunk(point, score) for point, score in merged]
        logger.debug(f"RRF merge → {len(chunks)} chunks before reranking")

        # ── Reranking (Phase 8: +60% Context Precision) ───────────────────────
        if use_reranker:
            texts = [c.text for c in chunks]
            reranked_indices = self.reranker.rerank(query, texts, top_k=top_k)
            chunks = [chunks[idx] for idx, score in reranked_indices]
            logger.info(f"Reranker: {len(texts)} → {len(chunks)} chunks (top_k={top_k})")
        else:
            chunks = chunks[:top_k]  # Без reranker — просто обрезаем до top_k

        logger.info(f"RAG search → {len(chunks)} chunks returned")
        return chunks

    def get_context_window(
        self,
        book_id: int,
        chunk_index: int,
        window_size: int = 2,
    ) -> List[RetrievedChunk]:
        """
        Возвращает соседние чанки вокруг заданного chunk_index.

        Полезно для расширения контекста перед отправкой в LLM:
        [chunk-2, chunk-1, chunk, chunk+1, chunk+2]

        Args:
            book_id:      ID книги.
            chunk_index:  Индекс центрального чанка.
            window_size:  Количество чанков с каждой стороны.

        Returns:
            Список чанков, отсортированных по chunk_index.
        """
        scroll_filter = Filter(
            must=[
                FieldCondition(key="book_id", match=MatchValue(value=book_id)),
                FieldCondition(
                    key="chunk_index",
                    range=Range(
                        gte=chunk_index - window_size,
                        lte=chunk_index + window_size,
                    ),
                ),
            ]
        )

        points, _ = self.qdrant.client.scroll(
            collection_name=self.qdrant.collection_rag,
            scroll_filter=scroll_filter,
            limit=window_size * 2 + 1,
            with_payload=True,
            with_vectors=False,
        )

        chunks = [self._point_to_chunk(p, 1.0) for p in points]
        chunks.sort(key=lambda c: c.chunk_index)
        logger.debug(
            f"Context window [{chunk_index-window_size}..{chunk_index+window_size}] "
            f"→ {len(chunks)} chunks for book_id={book_id}"
        )
        return chunks

    # ─── Вспомогательные методы ────────────────────────────────────────────────

    @staticmethod
    def _normalize_text_for_bm25(text: str) -> str:
        """
        Нормализация текста для улучшения BM25 поиска.
        
        Применяет:
        - Замена ё → е
        - Замена умлаутов (ä→а, ö→о, ü→у, ë→е и т.д.)
        - Приведение к нижнему регистру
        
        Пример: "Гёте" → "гете", чтобы совпадало с запросом "Гете"
        """
        # Замены для кириллицы
        replacements = {
            'ё': 'е', 'Ё': 'Е',
            # Умлауты и диакритики
            'ä': 'а', 'Ä': 'А',
            'ö': 'о', 'Ö': 'О', 
            'ü': 'у', 'Ü': 'У',
            'ë': 'е', 'Ë': 'Е',
            'ï': 'и', 'Ï': 'И',
        }
        
        normalized = text
        for old_char, new_char in replacements.items():
            normalized = normalized.replace(old_char, new_char)
        
        return normalized.lower()

    def _find_relevant_books(self, query: str, limit: int = 3) -> List[int]:
        """
        Two-Stage Retrieval: находит релевантные книги по запросу.
        
        Использует BM25 поиск по коллекции recommendations для точного поиска
        по ключевым словам (автор, название, жанры, описание).
        
        Пример: "Как зовут героев в Фауста Гете" → находит book_id книги "Фауст"
        
        Args:
            query: Запрос пользователя (может содержать автора, название, жанр)
            limit: Максимальное количество книг для поиска
            
        Returns:
            Список book_ids релевантных книг (пустой список если ничего не найдено)
        """
        try:
            # Normalize query for better BM25 matching (ё→е, умлауты и т.д.)
            normalized_query = self._normalize_text_for_bm25(query)
            
            # Add alternative spelling variants for common author names
            # Example: "Гете" → "Гете Гёте" to match "Гёте" in database
            query_variants = normalized_query
            if "гете" in normalized_query:
                query_variants += " гёте"
            
            if query_variants != normalized_query:
                logger.debug(f"Query variants: '{normalized_query}' → '{query_variants}'")
            
            # BM25-only search (точный поиск по ключевым словам)
            # Не используем Dense, т.к. коллекция recommendations использует RoSBERTa embeddings
            query_sparse = self.qdrant._generate_bm25_sparse_vector(query_variants)
            
            # Fetch more results for debugging
            fetch_limit = max(limit * 3, 10)
            sparse_results = self.qdrant.client.query_points(
                collection_name=self.qdrant.collection_recommendations,
                query=query_sparse,
                using="bm25",
                limit=fetch_limit,
                with_payload=True,  # Get all payload for debugging
            ).points
            
            if not sparse_results:
                logger.warning(f"Book filter (BM25): '{query[:60]}' → no books found in recommendations collection")
                return []
            
            # Log all found books for debugging
            logger.info(f"Book filter (BM25): query='{query[:60]}' → {len(sparse_results)} candidates")
            for i, point in enumerate(sparse_results[:10], 1):
                book_id = point.payload.get("book_id")
                title = point.payload.get("title", "Unknown")
                authors = point.payload.get("authors", [])
                authors_str = ", ".join(authors) if authors else "Unknown"
                score = point.score if hasattr(point, 'score') else 0.0
                logger.info(f"  [{i}] book_id={book_id}: {title} | {authors_str} | score={score:.4f}")
            
            # Extract top-N book_ids
            book_ids = [
                point.payload.get("book_id") 
                for point in sparse_results[:limit]
                if point.payload.get("book_id")
            ]
            
            return book_ids
            
        except Exception as e:
            logger.error(f"Failed to find relevant books: {e}")
            return []

    def _build_filter(self, book_ids: Optional[List[int]]) -> Optional[Filter]:
        """Создаёт фильтр по книгам (None если ограничений нет)."""
        if not book_ids:
            return None
        return Filter(
            must=[FieldCondition(key="book_id", match=MatchAny(any=book_ids))]
        )

    def _rrf_merge(
        self,
        dense: list,
        sparse: list,
        limit: int,
    ) -> List[Tuple[object, float]]:
        """
        Reciprocal Rank Fusion (RRF).

        score(doc) = Σ 1 / (k + rank_i)
        k = 60 (стандарт)
        """
        scores: dict[int, float] = {}
        points: dict[int, object] = {}

        for rank, p in enumerate(dense, start=1):
            scores[p.id] = scores.get(p.id, 0.0) + 1.0 / (self._RRF_K + rank)
            points[p.id] = p

        for rank, p in enumerate(sparse, start=1):
            scores[p.id] = scores.get(p.id, 0.0) + 1.0 / (self._RRF_K + rank)
            if p.id not in points:
                points[p.id] = p

        ranked = sorted(scores.items(), key=lambda x: x[1], reverse=True)[:limit]
        return [(points[pid], score) for pid, score in ranked]

    def _point_to_chunk(self, point, score: float) -> RetrievedChunk:
        """Конвертирует Qdrant-точку в RetrievedChunk."""
        p = point.payload or {}
        return RetrievedChunk(
            text=p.get("text", ""),
            book_id=p.get("book_id", 0),
            chapter_title=p.get("chapter_title", ""),
            chapter_index=p.get("chapter_index", 0),
            chunk_index=p.get("chunk_index", 0),
            position_in_chapter=p.get("position_in_chapter", 0.0),
            token_count=p.get("token_count", 0),
            score=score,
            metadata={
                "title": p.get("title", ""),
                "authors": p.get("authors", []),
                "genres": p.get("genres", []),
                "language": p.get("language", ""),
                "publication_year": p.get("publication_year"),
            },
        )
