"""
Book Resolver Service.

Находит book_id(s) по извлечённым из запроса title + author.

Стратегии (в порядке применения):
  1. BM25 search в коллекции books_recommendations по точному «title [author]»
     — работает отлично, когда DeepSeek нормализовал имя автора
  2. Scroll (точное совпадение title) — fallback при нулевых результатах BM25
"""

from __future__ import annotations

import re
from typing import List, Optional

from loguru import logger
from qdrant_client.models import FieldCondition, Filter, MatchValue

from app.services.shared.qdrant_service import QdrantService


class BookResolverService:
    """
    Преобразует {title, author} → List[book_id].

    Ключевое отличие от старого _find_relevant_books() в retrieval_service:
    — запрос строится из КАНОНИЧНОЙ формы автора (выдаёт IntentClassifier),
      что устраняет проблему умлаутов (Гете → Иоганн Вольфганг фон Гёте).
    — fallback через точный scroll по полю title.
    """

    # Максимальное кол-во кандидатов от BM25 перед дедупликацией
    _BM25_OVERFETCH = 15

    def __init__(self, qdrant: QdrantService):
        self.qdrant = qdrant

    # ─── Публичный API ─────────────────────────────────────────────────────────

    def find_book_ids(
        self,
        title:  Optional[str],
        author: Optional[str],
        limit:  int = 3,
    ) -> List[int]:
        """
        Находит book_id(s) для указанной книги.

        Args:
            title:  Название книги (из IntentClassifier).
            author: Автор в именительном падеже (из IntentClassifier).
            limit:  Максимальное количество возвращаемых книг.

        Returns:
            Список book_id (пустой — если книга не найдена).
        """
        if not title and not author:
            logger.warning("BookResolver: и title, и author пусты — нечего искать")
            return []

        # ── Стратегия 1: BM25 ─────────────────────────────────────────────────
        book_ids = self._bm25_search(title, author, limit)
        if book_ids:
            return book_ids

        # ── Стратегия 2: Точный scroll по title ───────────────────────────────
        if title:
            book_ids = self._exact_title_scroll(title, limit)
            if book_ids:
                return book_ids

        logger.warning(
            f"BookResolver: книга не найдена — title={title!r}, author={author!r}"
        )
        return []

    # ─── Стратегия 1: BM25 ────────────────────────────────────────────────────

    def _bm25_search(
        self,
        title:  Optional[str],
        author: Optional[str],
        limit:  int,
    ) -> List[int]:
        """
        BM25-поиск в коллекции books_recommendations.

        Строит запрос: «{title} {author}»  — так BM25 найдёт токены из
        обоих полей текстового профиля книги.
        """
        parts = []
        if title:
            parts.append(title.strip())
        if author:
            # Добавляем нормализованный вариант (замена ё→е, умлауты) для надёжности
            parts.append(author.strip())
            normalized = self._normalize(author)
            if normalized != author.lower():
                parts.append(normalized)

        query = " ".join(parts)
        if not query.strip():
            return []

        logger.debug(f"BookResolver BM25 query: {query!r}")

        try:
            sparse_vec = self.qdrant._generate_bm25_sparse_vector(query)
            results = self.qdrant.client.query_points(
                collection_name=self.qdrant.collection_recommendations,
                query=sparse_vec,
                using="bm25",
                limit=self._BM25_OVERFETCH,
                with_payload=True,
            ).points

            if not results:
                return []

            # Логируем кандидатов
            logger.debug(
                f"BookResolver BM25 → {len(results)} кандидатов для {title!r}:"
            )
            for r in results[:5]:
                logger.debug(
                    f"  book_id={r.payload.get('book_id')} "
                    f"title={r.payload.get('title')!r} "
                    f"authors={r.payload.get('authors')} "
                    f"score={r.score:.4f}"
                )

            # Если есть и title, и author — фильтруем по title-совпадению
            if title:
                title_lower = title.lower()
                filtered = [
                    r for r in results
                    if title_lower in (r.payload.get("title") or "").lower()
                ]
                if filtered:
                    results = filtered
                    logger.debug(
                        f"BookResolver: после title-фильтра осталось {len(filtered)} книг"
                    )

            book_ids = [
                r.payload["book_id"]
                for r in results[:limit]
                if r.payload.get("book_id")
            ]
            logger.info(
                f"BookResolver BM25 result: {book_ids} "
                f"(title={title!r}, author={author!r})"
            )
            return book_ids

        except Exception as exc:
            logger.error(f"BookResolver BM25 error: {exc}")
            return []

    # ─── Стратегия 2: Exact-scroll ────────────────────────────────────────────

    def _exact_title_scroll(self, title: str, limit: int) -> List[int]:
        """
        Scroll коллекции books_recommendations по точному совпадению title.
        Работает без текстового индекса — полный скан, но быстро (~1699 книг).
        """
        try:
            scroll_filter = Filter(
                must=[FieldCondition(key="title", match=MatchValue(value=title))]
            )
            points, _ = self.qdrant.client.scroll(
                collection_name=self.qdrant.collection_recommendations,
                scroll_filter=scroll_filter,
                limit=limit,
                with_payload=True,
            )

            if not points:
                # Попытка с нормализованным заголовком (ё→е)
                norm_title = self._normalize(title)
                if norm_title != title.lower():
                    scroll_filter2 = Filter(
                        must=[FieldCondition(key="title", match=MatchValue(value=norm_title))]
                    )
                    points, _ = self.qdrant.client.scroll(
                        collection_name=self.qdrant.collection_recommendations,
                        scroll_filter=scroll_filter2,
                        limit=limit,
                        with_payload=True,
                    )

            book_ids = [p.payload["book_id"] for p in points if p.payload.get("book_id")]
            if book_ids:
                logger.info(f"BookResolver exact-scroll result: {book_ids} for title={title!r}")
            return book_ids

        except Exception as exc:
            logger.error(f"BookResolver exact-scroll error: {exc}")
            return []

    # ─── Нормализация текста ──────────────────────────────────────────────────

    @staticmethod
    def _normalize(text: str) -> str:
        """Нормализация: строчные буквы + замена умлаутов и ё→е."""
        replacements = {
            'ё': 'е', 'Ё': 'е',
            'ё': 'е',  # на случай нестандартной кодировки
            'ä': 'а', 'ö': 'о', 'ü': 'у',
            'Ä': 'а', 'Ö': 'о', 'Ü': 'у',
        }
        result = text.lower()
        for old, new in replacements.items():
            result = result.replace(old, new)
        return result

    # ─── Методы для работы с сериями ─────────────────────────────────────────

    def get_series_for_book(self, book_id: int) -> Optional[str]:
        """
        Возвращает series_name для указанного book_id, или None.

        Используется для определения серии после резолва конкретной книги,
        чтобы потом найти все книги серии через find_series_ids().
        """
        try:
            points = self.qdrant.client.retrieve(
                collection_name=self.qdrant.collection_recommendations,
                ids=[book_id],
                with_payload=True,
            )
            if points:
                return points[0].payload.get("series_name") or None
        except Exception as exc:
            logger.error(f"BookResolver.get_series_for_book error: {exc}")
        return None

    def find_series_ids(self, series_name: str) -> List[int]:
        """
        Возвращает все book_ids книг из указанной серии.

        Использует scroll по payload.series_name — полный скан за ~1 мс
        при библиотеке до 10к книг.

        Returns:
            Список book_id всех книг серии (пустой при ошибке).
        """
        try:
            scroll_filter = Filter(
                must=[
                    FieldCondition(
                        key="series_name",
                        match=MatchValue(value=series_name),
                    )
                ]
            )
            all_ids: List[int] = []
            offset = None
            while True:
                points, next_offset = self.qdrant.client.scroll(
                    collection_name=self.qdrant.collection_recommendations,
                    scroll_filter=scroll_filter,
                    limit=100,
                    offset=offset,
                    with_payload=True,
                )
                for p in points:
                    bid = p.payload.get("book_id")
                    if bid:
                        all_ids.append(bid)
                if next_offset is None:
                    break
                offset = next_offset

            if all_ids:
                logger.info(
                    f"BookResolver: series={series_name!r} → {len(all_ids)} книг: {all_ids}"
                )
            return all_ids

        except Exception as exc:
            logger.error(f"BookResolver.find_series_ids error: {exc}")
            return []

    def find_books_by_author(self, author: str, limit: int = 20) -> List[int]:
        """
        Возвращает до `limit` book_id книг указанного автора через BM25.

        Используется как фоллбек для scope="series" когда у книги нет серии
        (например, у автора одиночные романы).
        """
        if not author:
            return []
        try:
            sparse_vec = self.qdrant._generate_bm25_sparse_vector(author)
            results = self.qdrant.client.query_points(
                collection_name=self.qdrant.collection_recommendations,
                query=sparse_vec,
                using="bm25",
                limit=limit,
                with_payload=True,
            ).points
            ids = [r.payload["book_id"] for r in results if r.payload.get("book_id")]
            if ids:
                logger.info(
                    f"BookResolver.find_books_by_author: author={author!r} → {len(ids)} книг"
                )
            return ids
        except Exception as exc:
            logger.error(f"BookResolver.find_books_by_author error: {exc}")
            return []
