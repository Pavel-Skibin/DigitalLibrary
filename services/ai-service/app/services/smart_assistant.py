"""
Smart Assistant Service — оркестратор чата.

Алгоритм chat() = единая точка входа:
  1. IntentClassifier   — определяет тип запроса + извлекает данные (1 LLM-вызов)
  2. BookResolverService — для BOOK_QUESTION находит book_id(s) по title/author
  3. RAGService          — для BOOK_QUESTION и GENERAL: hybrid search + LLM ответ
     NLRecommendationService — для RECOMMENDATION: фильтры + поиск книг

Ответ всегда в едином формате ChatResponse.
"""

from __future__ import annotations

from typing import List, Optional

from loguru import logger

from app.config import Settings
from app.models.chat import (
    BookSource,
    ChatRequest,
    ChatResponse,
    ClassifiedIntent,
    IntentType,
    RecommendedBook,
)
from app.models.rag import RAGQueryRequest
from app.models.rag import ChunkSource as RAGChunkSource
from app.services.rag.book_resolver import BookResolverService
from app.services.rag.intent_classifier import IntentClassifierService
from app.services.rag.rag_service import RAGService
from app.services.recommendations.nl_recommendation_service import (
    NaturalLanguageRecommendationService,
)
from app.services.recommendations.recommendation_engine import RecommendationEngine
from app.models.book import BookRecommendation


class SmartAssistantService:
    """
    Главный оркестратор умного ассистента цифровой библиотеки.

    Принимает свободный текст от пользователя и возвращает:
      * Ответ на вопрос по книге  (BOOK_QUESTION)
      * Список рекомендаций       (RECOMMENDATION)
      * Ответ по всей библиотеке  (GENERAL)
    """

    # Шаблоны ответа для рекомендаций (если LLM не генерирует текст)
    _REC_HEADER = "Вот книги, которые могут вам понравиться:"
    _NO_REC     = "К сожалению, не нашёл книг по вашему запросу. Попробуйте изменить фильтры."
    _NO_BOOK    = (
        "Не смог найти указанную книгу в библиотеке. "
        "Попробуйте уточнить название или автора."
    )

    def __init__(
        self,
        settings:         Settings,
        intent_classifier: IntentClassifierService,
        book_resolver:    BookResolverService,
        rag_service:      RAGService,
        nl_rec_service:   NaturalLanguageRecommendationService,
        rec_engine:       RecommendationEngine,
    ):
        self.settings          = settings
        self.intent_classifier = intent_classifier
        self.book_resolver     = book_resolver
        self.rag               = rag_service
        self.nl_rec            = nl_rec_service
        self.rec_engine        = rec_engine

    # ─── Публичный API ─────────────────────────────────────────────────────────

    async def chat(self, request: ChatRequest) -> ChatResponse:
        """
        Обрабатывает сообщение пользователя.

        Возвращает ChatResponse с answer + sources (для RAG) или
        answer + recommendations (для рекомендаций).
        """
        logger.info(
            f"SmartAssistant.chat | user_id={request.user_id} | "
            f"message={request.message[:80]!r}"
        )

        # ── Шаг 1: Классификация интента ─────────────────────────────────────
        intent: ClassifiedIntent = await self.intent_classifier.classify(request.message)
        logger.info(f"Intent: {intent.intent} | clean_query={intent.clean_query!r}")

        debug = (
            {
                "intent":            intent.intent,
                "clean_query":       intent.clean_query,
                "book_entity":       intent.book_entity.dict() if intent.book_entity else None,
                "rec_filters":       intent.recommendation_filters.dict()
                                     if intent.recommendation_filters else None,
            }
            if self.settings.DEBUG
            else None
        )

        # ── Шаг 2: Диспетчеризация ────────────────────────────────────────────

        if intent.intent == IntentType.BOOK_QUESTION:
            return await self._handle_book_question(request, intent, debug)

        if intent.intent == IntentType.RECOMMENDATION:
            return await self._handle_recommendation(request, intent, debug)

        # GENERAL — поиск по всей библиотеке
        return await self._handle_general(request, intent, debug)

    # ─── BOOK_QUESTION ────────────────────────────────────────────────────────

    async def _handle_book_question(
        self,
        request: ChatRequest,
        intent:  ClassifiedIntent,
        debug:   Optional[dict],
    ) -> ChatResponse:
        """Отвечает на вопрос по конкретной книге через RAG."""
        entity = intent.book_entity

        # Резолвим книгу(и) по title + author
        book_ids: List[int] = []
        if entity and (entity.title or entity.author):
            book_ids = self.book_resolver.find_book_ids(
                title=entity.title,
                author=entity.author,
                limit=2,        # обычно одна книга; берём 2 на случай серий
            )
            if debug:
                debug["resolved_book_ids"] = book_ids

        clean_query = (entity.clean_query if entity else None) or request.message

        if not book_ids and (entity and entity.title):
            # Книга не найдена — сообщаем об этом
            logger.warning(
                f"Книга не найдена: title={entity.title!r}, "
                f"author={entity.author!r}"
            )
            return ChatResponse(
                intent=IntentType.BOOK_QUESTION,
                answer=self._NO_BOOK,
                debug=debug,
            )

        # Запрос к RAG
        rag_request = RAGQueryRequest(
            query=clean_query,
            top_k=request.top_k,
            book_ids=book_ids if book_ids else None,  # None = по всей библиотеке
            include_context=True,
            auto_filter_books=False,   # мы уже сами нашли книги
        )
        rag_response = await self.rag.query(rag_request)

        return ChatResponse(
            intent=IntentType.BOOK_QUESTION,
            answer=rag_response.answer,
            sources=self._rag_sources_to_chat_sources(rag_response.sources or []),
            debug=debug,
        )

    # ─── RECOMMENDATION ───────────────────────────────────────────────────────

    @staticmethod
    def _filters_are_empty(filters: "RecommendationFilters") -> bool:  # type: ignore[name-defined]
        """True — пользователь не указал никаких ограничений."""
        return (
            not filters.genres
            and not filters.keywords
            and filters.author_filter is None
            and filters.min_word_count is None
            and filters.max_word_count is None
            and filters.mood is None
            and filters.language is None
            and filters.min_rating is None
            and filters.era is None
            and not filters.similar_to_books
        )

    @staticmethod
    def _engine_recs_to_chat(recs: List[BookRecommendation]) -> List[RecommendedBook]:
        """Конвертирует BookRecommendation (движок) → RecommendedBook (чат)."""
        return [
            RecommendedBook(
                book_id=r.book_id,
                title=r.title,
                authors=r.authors,
                genres=r.genres,
                average_rating=r.average_rating,
                ratings_count=r.ratings_count or 0,
                cover_image_path=r.cover_image_path,
                reason=r.reason or "",
            )
            for r in recs
        ]

    async def _handle_recommendation(
        self,
        request: ChatRequest,
        intent:  ClassifiedIntent,
        debug:   Optional[dict],
    ) -> ChatResponse:
        """Возвращает рекомендации.

        Стратегия:
          * similar_to_books не пуст        → поиск похожих книг (мульти-якорь)
          * Нет фильтров + user_id         → RecommendationEngine (= /api/recommendations)
          * Иначе                          → NLRecommendationService (NL-фильтры)
        """
        filters = intent.recommendation_filters

        # Главный роут: похожие книги
        if filters.similar_to_books:
            return await self._handle_similar_books(request, intent, debug)

        if request.user_id and self._filters_are_empty(filters):
            logger.info(
                f"Recommendation: no filters, user_id={request.user_id} "
                "→ RecommendationEngine (same as /api/recommendations)"
            )
            engine_recs = await self.rec_engine.get_personalized_recommendations(
                user_id=request.user_id,
                limit=request.top_k,
            )
            books: List[RecommendedBook] = self._engine_recs_to_chat(engine_recs)
        else:
            books = await self.nl_rec.recommend(
                filters=filters,
                user_id=request.user_id,
                limit=request.top_k,
                clean_query=intent.clean_query,
            )

        if not books:
            return ChatResponse(
                intent=IntentType.RECOMMENDATION,
                answer=self._NO_REC,
                debug=debug,
            )

        return ChatResponse(
            intent=IntentType.RECOMMENDATION,
            answer=self._format_recommendation_answer(books, intent),
            recommendations=books,
            debug=debug,
        )
    async def _handle_similar_books(
        self,
        request: ChatRequest,
        intent:  ClassifiedIntent,
        debug:   Optional[dict],
    ) -> ChatResponse:
        """Подбирает книги похожие на 1..N указанных книг-эталонов.

        Стратегия:
          1. Резолвим book_id для каждого названия через BookResolverService.
          2. Вызываем RecommendationEngine.get_similar_to_books(мульти-якорь).
          3. Если книга не найдена — фоллбек на NLRecommendationService.
        """
        filters = intent.recommendation_filters
        book_titles = filters.similar_to_books

        # Резольвим book_id для каждого названия
        anchor_ids: List[int] = []
        resolved_titles: List[str] = []
        not_found_titles: List[str] = []

        for title in book_titles:
            ids = self.book_resolver.find_book_ids(title=title, author=None, limit=1)
            if ids:
                anchor_ids.append(ids[0])
                resolved_titles.append(title)
            else:
                not_found_titles.append(title)
                logger.warning(f"SimilarBooks: книга не найдена: {title!r}")

        if debug:
            debug["similar_anchors"] = {
                "resolved": dict(zip(resolved_titles, anchor_ids)),
                "not_found": not_found_titles,
            }

        if not anchor_ids:
            # Ни одна книга не нашлась — fallback: NL-поиск по названиям как keywords
            logger.info("SimilarBooks: нет якорей, фоллбек на NL search")
            fallback_filters = filters.copy(
                update={"keywords": list(filters.keywords) + book_titles,
                        "similar_to_books": []}
            )
            books = await self.nl_rec.recommend(
                filters=fallback_filters,
                user_id=request.user_id,
                limit=request.top_k,
                clean_query=intent.clean_query,
            )
        else:
            engine_recs = await self.rec_engine.get_similar_to_books(
                book_ids=anchor_ids,
                limit=request.top_k,
                exclude_ids=anchor_ids,
            )
            books = self._engine_recs_to_chat(engine_recs)

        if not books:
            return ChatResponse(
                intent=IntentType.RECOMMENDATION,
                answer=self._NO_REC,
                debug=debug,
            )

        # Формируем ответ
        if resolved_titles:
            titles_fmt = ", ".join(f"«{t}»" for t in resolved_titles)
            if not_found_titles:
                nf_fmt = ", ".join(f"«{t}»" for t in not_found_titles)
                answer_header = (
                    f"Подбрал похожее на {titles_fmt}. "
                    f"(Книги {nf_fmt} не найдены в библиотеке.)"
                )
            else:
                answer_header = f"Подбрал похожее на {titles_fmt}:"
        else:
            answer_header = "Вот похожие книги:"

        book_lines: List[str] = []
        for i, book in enumerate(books[:10], 1):
            authors_str = ", ".join(book.authors) if book.authors else ""
            rating_str  = f" {book.average_rating:.1f}" if book.average_rating else ""
            book_lines.append(
                f"{i}. **{book.title}**"
                + (f" — {authors_str}" if authors_str else "")
                + rating_str
            )

        return ChatResponse(
            intent=IntentType.RECOMMENDATION,
            answer=answer_header + "\n" + "\n".join(book_lines),
            recommendations=books,
            debug=debug,
        )
    # ─── GENERAL ──────────────────────────────────────────────────────────────

    async def _handle_general(
        self,
        request: ChatRequest,
        intent:  ClassifiedIntent,
        debug:   Optional[dict],
    ) -> ChatResponse:
        """Полнотекстовый RAG по всей библиотеке."""
        rag_request = RAGQueryRequest(
            query=intent.clean_query,
            top_k=request.top_k,
            include_context=True,
            auto_filter_books=False,
        )
        rag_response = await self.rag.query(rag_request)

        return ChatResponse(
            intent=IntentType.GENERAL,
            answer=rag_response.answer,
            sources=self._rag_sources_to_chat_sources(rag_response.sources or []),
            debug=debug,
        )

    # ─── Вспомогательные методы ───────────────────────────────────────────────

    @staticmethod
    def _rag_sources_to_chat_sources(sources: List[RAGChunkSource]) -> List[BookSource]:
        """Конвертирует RAGChunkSource → BookSource."""
        seen: set = set()
        result: List[BookSource] = []
        for src in sources:
            key = (src.book_id, src.chapter_title)
            if key in seen:
                continue
            seen.add(key)
            result.append(BookSource(
                book_id=src.book_id,
                title="",      # ChunkSource не хранит title — заполним при необходимости
                authors=[],
                chapter_title=src.chapter_title,
                chunk_index=src.chunk_index,
                score=src.score,
            ))
        return result

    @staticmethod
    def _format_recommendation_answer(
        books:  List[RecommendedBook],
        intent: ClassifiedIntent,
    ) -> str:
        """Формирует текстовый ответ со списком рекомендаций."""
        filters = intent.recommendation_filters
        parts: List[str] = []

        # Заголовок
        intro_parts: List[str] = []
        if filters and filters.similar_to_books:
            titles_fmt = ", ".join(f"«{t}»" for t in filters.similar_to_books[:3])
            intro_parts.append(f"похожее на {titles_fmt}")
        if filters and filters.author_filter:
            intro_parts.append(f"от автора «{filters.author_filter}»")
        if filters and filters.genres:
            intro_parts.append(", ".join(filters.genres).lower())
        if filters and filters.mood:
            intro_parts.append(f"с настроением «{filters.mood}»")
        if filters and filters.min_word_count and filters.max_word_count:
            intro_parts.append(f"{filters.min_word_count}к–{filters.max_word_count}к слов")
        elif filters and filters.max_word_count:
            intro_parts.append(f"до {filters.max_word_count}к слов")
        if filters and filters.era:
            intro_parts.append(filters.era)

        if intro_parts:
            header = f"Подобрал для вас {' '.join(intro_parts)}:"
        else:
            header = "Вот что могу порекомендовать:"

        parts.append(header)

        for i, book in enumerate(books[:10], 1):
            authors_str = ", ".join(book.authors) if book.authors else ""
            rating_str  = f" {book.average_rating:.1f}" if book.average_rating else ""
            words_str   = f" ({book.word_count}к слов)" if book.word_count else ""
            parts.append(
                f"{i}. **{book.title}**"
                + (f" — {authors_str}" if authors_str else "")
                + rating_str
                + words_str
            )

        return "\n".join(parts)
