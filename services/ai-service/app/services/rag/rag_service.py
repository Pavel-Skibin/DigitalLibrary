"""
RAG Service. Orchestration: Retrieval -> LLM -> Response.

Coordinates:
  * RAGRetrievalService — hybrid search chunks (USER-bge-m3 + BM25)
  * DeepSeekClient     — LLM answer generation
  * Response formatting with sources
"""

from __future__ import annotations

from loguru import logger
from typing import List

from app.config import Settings
from app.services.rag.retrieval_service import RAGRetrievalService, RetrievedChunk
from app.services.rag.deepseek_client import DeepSeekClient
from app.models.rag import (
    RAGQueryRequest,
    RAGQueryResponse,
    RAGSearchRequest,
    RAGSearchResponse,
    ChunkResult,
    ChunkSource,
)


class RAGService:
    """
    Главный сервис RAG-пайплайна.

    Алгоритм query():
      1. Retrieve — найти топ-K релевантных чанков (hybrid search)
      2. Generate — отправить контекст + вопрос в LLM
      3. Format   — вернуть ответ + источники

    Алгоритм search():
      1. Retrieve — только поиск, без LLM
    """

    _NO_CONTEXT_ANSWER = (
        "По данному запросу в библиотеке не найдено релевантных фрагментов текста. "
        "Попробуйте переформулировать вопрос или расширить область поиска."
    )

    def __init__(
        self,
        settings: Settings,
        retrieval: RAGRetrievalService,
        llm: DeepSeekClient,
    ):
        self.settings = settings
        self.retrieval = retrieval
        self.llm = llm

    # ─── Публичный API ─────────────────────────────────────────────────────────

    async def query(self, request: RAGQueryRequest) -> RAGQueryResponse:
        """
        Полный RAG pipeline: поиск чанков → LLM → ответ с источниками.

        Args:
            request: RAGQueryRequest с вопросом и параметрами.

        Returns:
            RAGQueryResponse — ответ + источники.
        """
        logger.info(f"RAG query: {request.query!r} (top_k={request.top_k})")

        # ── 1. Retrieval ──────────────────────────────────────────────────────
        chunks = self.retrieval.search_chunks(
            query=request.query,
            top_k=request.top_k,
            book_ids=request.book_ids,
            auto_filter_books=request.auto_filter_books,
        )

        if not chunks:
            logger.warning("RAG: no relevant chunks found")
            return RAGQueryResponse(
                answer=self._NO_CONTEXT_ANSWER,
                sources=[],
                query=request.query,
                context_chunks_count=0,
                usage={},
                llm_available=self.llm.is_available(),
            )

        # Context window: add neighbouring chunks for broader context
        window = self.settings.RAG_CONTEXT_WINDOW
        if window > 0:
            chunks = self._expand_context_window(chunks, window_size=window)
            logger.info(f"Context window [{window}] expanded to {len(chunks)} chunks")

        # ── 3. Проверяем доступность LLM ───────────────────────────────────
        if not self.llm.is_available():
            logger.warning("RAG: LLM not available, returning chunks summary")
            return RAGQueryResponse(
                answer=(
                    "LLM-бэкенд не настроен. Ниже приведены найденные фрагменты текста.\n\n"
                    + "\n\n".join(
                        f"[{c.chapter_title}] {c.text[:300]}..." for c in chunks
                    )
                ),
                sources=self._build_sources(chunks),
                query=request.query,
                context_chunks_count=len(chunks),
                usage={},
                llm_available=False,
            )

        # ── 4. Generate (LLM) ────────────────────────────────────────────
        context_texts = [chunk.text for chunk in chunks]
        try:
            llm_response = await self.llm.generate_answer(
                query=request.query,
                context_chunks=context_texts,
            )
        except Exception as exc:
            logger.error(f"LLM generation failed: {exc}")
            return RAGQueryResponse(
                answer=f"Ошибка LLM: {exc}",
                sources=self._build_sources(chunks),
                query=request.query,
                context_chunks_count=len(chunks),
                usage={},
                llm_available=False,
            )

        # ── 5. Format response ───────────────────────────────────────────
        return RAGQueryResponse(
            answer=llm_response.answer,
            sources=self._build_sources(chunks),
            query=request.query,
            context_chunks_count=len(chunks),
            usage=llm_response.usage,
            llm_available=True,
        )

    def search(self, request: RAGSearchRequest) -> RAGSearchResponse:
        """
        Только поиск чанков, без LLM.

        Возвращает полные тексты чанков с метаданными — полезно для отладки
        и для фронтенда, который самостоятельно обрабатывает результаты.
        """
        logger.info(f"RAG search: {request.query!r} (top_k={request.top_k})")

        chunks = self.retrieval.search_chunks(
            query=request.query,
            top_k=request.top_k,
            book_ids=request.book_ids,
            use_hybrid=request.use_hybrid,
            auto_filter_books=request.auto_filter_books,
        )

        results = [
            ChunkResult(
                book_id=c.book_id,
                chapter_title=c.chapter_title,
                chapter_index=c.chapter_index,
                chunk_index=c.chunk_index,
                position_in_chapter=c.position_in_chapter,
                token_count=c.token_count,
                score=round(c.score, 6),
                text=c.text,
                title=c.metadata.get("title", ""),
                authors=c.metadata.get("authors", []),
            )
            for c in chunks
        ]

        return RAGSearchResponse(
            query=request.query,
            results=results,
            total_found=len(results),
            book_ids_filter=request.book_ids,
        )

    # ─── Вспомогательные методы ────────────────────────────────────────────────

    def _expand_context_window(
        self,
        chunks: List[RetrievedChunk],
        window_size: int = 1,
    ) -> List[RetrievedChunk]:
        """
        Расширяет контекст: для каждого найденного чанка добавляет соседние чанки.

        Например, при window_size=1 и найденном chunk_index=10:
          добавляем чанки 9, 10, 11 → LLM видит более полный контекст.

        Дедуплицирует по (book_id, chunk_index) и сортирует по chunk_index.
        """
        seen: set = set()
        expanded: List[RetrievedChunk] = []

        for chunk in chunks:
            key = (chunk.book_id, chunk.chunk_index)
            if key not in seen:
                seen.add(key)
                expanded.append(chunk)

            # Запрашиваем соседей
            try:
                neighbors = self.retrieval.get_context_window(
                    book_id=chunk.book_id,
                    chunk_index=chunk.chunk_index,
                    window_size=window_size,
                )
                for neighbor in neighbors:
                    nkey = (neighbor.book_id, neighbor.chunk_index)
                    if nkey not in seen:
                        seen.add(nkey)
                        expanded.append(neighbor)
            except Exception as exc:
                logger.warning(f"Context window failed for chunk {chunk.chunk_index}: {exc}")

        # Сортируем по book_id, затем chunk_index (документный порядок → лучший контекст для LLM)
        expanded.sort(key=lambda c: (c.book_id, c.chunk_index))
        return expanded

    def _build_sources(self, chunks: List[RetrievedChunk]) -> List[ChunkSource]:
        """Конвертирует чанки в краткие источники для ответа."""
        return [
            ChunkSource(
                book_id=c.book_id,
                chapter_title=c.chapter_title,
                chunk_index=c.chunk_index,
                score=round(c.score, 6),
                text_preview=(c.text[:200] + "…") if len(c.text) > 200 else c.text,
            )
            for c in chunks
        ]
