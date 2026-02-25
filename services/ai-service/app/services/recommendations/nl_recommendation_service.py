"""
Natural Language Recommendation Service.

Преобразует фильтры, извлечённые LLM из текстового запроса, в конкретные
рекомендации книг из Qdrant.

Поддерживает:
  * Жанр, язык, рейтинг, эпоха     — через payload-фильтры
  * Максимальное кол-во слов        — через books_rag_chunks (word_count indexed)
  * Настроение (mood)               — через BM25 + payload
  * Персонализацию по user_id       — через вектор предпочтений пользователя
"""

from __future__ import annotations

from typing import Any, Dict, List, Optional

import numpy as np
from loguru import logger
from qdrant_client.models import (
    FieldCondition,
    Filter,
    MatchAny,
    MatchValue,
    Range,
)

from app.models.chat import RecommendationFilters, RecommendedBook
from app.services.recommendations.embedding_service import EmbeddingService
from app.services.shared.qdrant_service import QdrantService
from app.services.shared.rest_client_service import RestClientService


# Сопоставление эпох с годами публикации
_ERA_YEAR_RANGES: Dict[str, Dict[str, int]] = {
    "классика":      {"lte": 1910},
    "русская классика": {"lte": 1917},
    "советская":     {"gte": 1917, "lte": 1991},
    "современная":   {"gte": 2000},
    "зарубежная":    {},   # только язык фильтрует
    "20 век":        {"gte": 1900, "lte": 1999},
    "19 век":        {"gte": 1800, "lte": 1899},
}

# Расширение жанров: LLM может вернуть короткое название,
# нам нужно развернуть его в реальные жанры из каталога
_GENRE_EXPANSION: Dict[str, List[str]] = {
    # Фантастика
    "Фантастика":           ["Научная фантастика"],
    "Научная фантастика":   ["Научная фантастика"],
    "Фэнтези":              ["Фэнтези"],
    "Фантастика и фэнтези":  ["Научная фантастика", "Фэнтези"],
    # Классика
    "Классика":             ["Классическая литература"],
    "Классическая литература": ["Классическая литература"],
    "Русская классика":   ["Классическая литература"],
    # Детектив
    "Детектив":             ["Детектив", "Триллер"],
    "Триллер":              ["Триллер", "Детектив"],
    # Ужасы / Мистика
    "Ужасы":               ["Ужасы", "Готическая литература", "Мистика"],
    "Мистика":              ["Мистика", "Готическая литература"],
    "Готика":               ["Готическая литература", "Ужасы", "Мистика"],
    # Драма / Психология
    "Драма":               ["Драма", "Психологическая проза", "Социальная проза"],
    "Психология":          ["Психологическая проза", "Психологический роман"],
    "Психологическая проза": ["Психологическая проза", "Психологический роман"],
    # История
    "Историческая проза": ["Исторический роман", "Историческая проза", "История"],
    "Исторический роман": ["Исторический роман", "Историческая проза"],
    "История":              ["История", "Исторический роман"],
    # Приключения
    "Приключения":         ["Приключения"],
    # Философия
    "Философия":          ["Философия", "Философская проза", "Философский роман"],
    "Философская проза":  ["Философская проза", "Философский роман", "Философия"],
    # Научпоп
    "Научпоп":            ['Научно-популярная литература'],
    "Научно-популярная литература": ["Научно-популярная литература"],
    # Поэзия
    "Поэзия":              ["Поэзия", "Философская поэзия"],
    # Биография
    "Биография":           ["Биография", "Автобиография", "Мемуары"],
    # Современная проза
    "Современная проза":  ["Современная проза", "Социальная проза"],
    "Романтика":           ["Современная проза", "Беллетристика"],
    # Комедия
    "Комедия":             ["Комедия"],
    "Сатира":               ["Комедия", "Эссеистика"],
    # Духовное
    "Духовная литература": ["Духовная литература", "Религиозная литература"],
}


class NaturalLanguageRecommendationService:
    """
    Основной сервис NL-рекомендаций.

    Алгоритм recommend():
    1. Строим Qdrant payload-фильтр (жанр, язык, рейтинг, эпоха, word_count)
       word_count проиндексирован в books_recommendations как integer → Range(lte=N)
    2. Если есть user_id → персонализированный поиск (вектор предпочтений)
       Если нет          → популярные книги + BM25 по mood/genre
    3. Возвращаем RecommendedBook с причиной
    """

    def __init__(
        self,
        qdrant:       QdrantService,
        rest_client:  RestClientService,
        embedding:    EmbeddingService,   # RoSBERTa — для вектора предпочтений
    ):
        self.qdrant      = qdrant
        self.rest_client = rest_client
        self.embedding   = embedding

    # ─── Публичный API ─────────────────────────────────────────────────────────

    async def recommend(
        self,
        filters:     RecommendationFilters,
        user_id:     Optional[int] = None,
        limit:       int = 10,
        clean_query: Optional[str] = None,
    ) -> List[RecommendedBook]:
        """
        Возвращает рекомендации по NL-фильтрам.

        Args:
            filters:     Фильтры из IntentClassifier (жанры, word_count, mood, …)
            user_id:     ID пользователя для персонализации (None — общий поиск)
            limit:       Количество книг в ответе
            clean_query: Исходный текст запроса для семантического поиска
        """
        logger.info(
            f"NL Rec | user_id={user_id} | genres={filters.genres} "
            f"| author={filters.author_filter!r} | keywords={filters.keywords} "
            f"| words=[{filters.min_word_count},{filters.max_word_count}] "
            f"| mood={filters.mood} | lang={filters.language} | era={filters.era} "
            f"| clean_query={clean_query!r}"
        )

        # 1. Payload-фильтр для recommendations collection
        # word_count уже проиндексирован в books_recommendations → фильтруем напрямую
        qdrant_filter = self._build_qdrant_filter(filters)

        # Поиск с overfetch (потом Python-фильтры по word_count/rating)
        search_limit = limit * 3

        # 3. Поиск
        if user_id:
            raw = await self._personalized_search(user_id, filters, qdrant_filter, search_limit, clean_query=clean_query)
        else:
            raw = await self._general_search(filters, qdrant_filter, search_limit, clean_query=clean_query)

        # 4a. Python-фильтр по автору (substring, case-insensitive).
        # Qdrant уже фильтрует MatchValue --- здесь защита от неточного написания.
        if filters.author_filter:
            needle = filters.author_filter.lower()
            candidate_authors = [
                a
                for r in raw
                for a in (r.get("metadata") or {}).get("authors", [])
            ]
            logger.debug(f"Author filter check. needle={needle!r}; sample_authors={list(dict.fromkeys(candidate_authors))[:10]}")
            filtered = [
                r for r in raw
                if any(needle in a.lower() for a in (r.get("metadata") or {}).get("authors", []))
            ]
            if filtered:
                raw = filtered
                logger.info(f"Author filter '{filters.author_filter}': {len(raw)} книг после фильтра")
            else:
                logger.warning(
                    f"Author filter '{filters.author_filter}': 0 книг в топ-{len(raw)} результатах. "
                    f"Sample authors: {list(dict.fromkeys(candidate_authors))[:8]}"
                )
                logger.warning("Фильтр снят (Qdrant MatchValue мог не найти точного совпадения имени)")

        # 4b. Конвертация в RecommendedBook
        reason = self._build_reason(filters)
        books = [self._to_recommended_book(item, reason) for item in raw[:limit]]
        logger.info(f"NL Rec → {len(books)} книг")
        return books

    # ─── Фильтр Qdrant ────────────────────────────────────────────────────────

    def _expand_genres(self, genres: List[str]) -> List[str]:
        """Расширяет LLM-жанры в реальные названия из каталога.

        Пример: ["Фантастика"] → ["Научная фантастика"]
                ["Классика"]   → ["Классическая литература"]
        """
        expanded: List[str] = []
        seen: set = set()
        for genre in genres:
            targets = _GENRE_EXPANSION.get(genre)
            if targets:
                for t in targets:
                    if t not in seen:
                        expanded.append(t)
                        seen.add(t)
            else:
                # неизвестный жанр — оставляем как есть
                if genre not in seen:
                    expanded.append(genre)
                    seen.add(genre)
        return expanded

    def _build_qdrant_filter(
        self,
        filters: RecommendationFilters,
    ) -> Optional[Filter]:
        """Строит Qdrant-фильтр из NL-фильтров.

        word_count проиндексирован в books_recommendations как integer,
        поэтому фильтруем напрямую — без roundtrip в books_rag_chunks.
        """
        must: list = []

        # Автор — жёсткий MatchValue по keyword-массиву authors
        if filters.author_filter:
            must.append(FieldCondition(
                key="authors",
                match=MatchValue(value=filters.author_filter)
            ))

        # Жанр — MatchAny по полю genres (с расширением)
        if filters.genres:
            expanded = self._expand_genres(filters.genres)
            logger.debug(f"Genres expanded: {filters.genres} → {expanded}")
            must.append(FieldCondition(
                key="genres",
                match=MatchAny(any=expanded)
            ))

        # Язык
        if filters.language:
            must.append(FieldCondition(
                key="language",
                match=MatchValue(value=filters.language)
            ))

        # Минимальный рейтинг
        if filters.min_rating is not None:
            must.append(FieldCondition(
                key="average_rating",
                range=Range(gte=filters.min_rating)
            ))

        # Эпоха → годы
        if filters.era:
            year_range = _ERA_YEAR_RANGES.get(filters.era.lower(), {})
            if year_range:
                must.append(FieldCondition(
                    key="publication_year",
                    range=Range(
                        gte=year_range.get("gte"),
                        lte=year_range.get("lte"),
                    )
                ))

        # Объём книги (в тысячах слов, indexed integer в books_recommendations)
        # «на вечер» → min=20, max=80 → только книги в диапазоне 20к–​80к слов
        # Дефолтный floor: если пользователь не указал никакого объёма — исключаем мини-рассказы (< 10к слов)
        _no_length_constraint = (filters.min_word_count is None and filters.max_word_count is None)
        effective_min = filters.min_word_count if not _no_length_constraint else 10

        if effective_min is not None:
            must.append(FieldCondition(
                key="word_count",
                range=Range(gte=effective_min)
            ))
        if filters.max_word_count is not None:
            must.append(FieldCondition(
                key="word_count",
                range=Range(lte=filters.max_word_count)
            ))

        if not must:
            return None
        return Filter(must=must)

    # ─── Word-count whitelist ──────────────────────────────────────────────────

    # ─── Персонализированный поиск ────────────────────────────────────────────

    async def _personalized_search(
        self,
        user_id:     int,
        filters:     RecommendationFilters,
        qdrant_filter: Optional[Filter],
        limit:       int,
        clean_query: Optional[str] = None,
    ) -> List[Dict[str, Any]]:
        """
        Поиск с учётом вкусов пользователя.

        Строит взвешенный вектор из anchor-книг пользователя, затем
        ищет похожие с payload-фильтром.
        """
        # Получаем anchor-книги пользователя
        anchor_books = await self.rest_client.get_user_anchor_books(user_id, limit=10)

        if not anchor_books:
            logger.info(f"Нет anchor-книг для user_id={user_id} — общий поиск")
            return await self._general_search(filters, qdrant_filter, limit, clean_query=clean_query)

        # Собираем векторы
        embeddings, weights = [], []
        for item in anchor_books:
            vec = await self.qdrant.get_book_vector(item["book_id"])
            if vec is not None:
                embeddings.append(vec)
                weights.append(item["weight"])

        if not embeddings:
            return await self._general_search(filters, qdrant_filter, limit, clean_query=clean_query)

        # Взвешенный средний вектор
        arr = np.array(embeddings)
        w   = np.array(weights, dtype=float)
        w  /= w.sum()
        user_vec = np.average(arr, axis=0, weights=w)

        # Адаптивный блендинг: вес вектора пользователя зависит от специфики запроса.
        # Чем больше жёстких ограничений — тем меньше «тянуть» к личным предпочтениям,
        # тем важнее семантика самого запроса.
        #
        #  author_filter задан          → user_weight = 0.05  (автор уже ограничивает пул)
        #  genres + ещё что-то          → user_weight = 0.15
        #  keywords / mood              → user_weight = 0.30
        #  ничего конкретного           → user_weight = 0.70  (чистая персонализация)

        hard_constraints = sum([
            bool(filters.author_filter),
            bool(filters.genres),
            bool(filters.era),
            bool(filters.language),
        ])

        if filters.author_filter:
            user_weight = 0.05
        elif hard_constraints >= 2:
            user_weight = 0.15
        elif hard_constraints == 1 or filters.keywords or filters.mood:
            user_weight = 0.30
        else:
            user_weight = 0.70   # только личные предпочтения, никаких ограничений

        if user_weight < 1.0 and clean_query:
            try:
                query_encoded = self.embedding.get_embedding(f"clustering: {clean_query}")
                query_encoded = np.array(query_encoded, dtype=float)
                blended = user_weight * user_vec + (1.0 - user_weight) * query_encoded
                norm = np.linalg.norm(blended)
                if norm > 0:
                    user_vec = blended / norm
                logger.debug(
                    f"Personalized: user_weight={user_weight:.2f} "
                    f"(hard_constraints={hard_constraints}, "
                    f"author={bool(filters.author_filter)}, genres={bool(filters.genres)})"
                )
            except Exception as exc:
                logger.warning(f"Vector blend failed: {exc} — using pure user_vec")

        # BM25-текст из mood/genres/era/keywords/clean_query
        query_text = self._build_bm25_query(filters, clean_query)

        # Exclude already read
        exclude_ids = await self.rest_client.get_user_read_books(user_id)

        results = await self.qdrant.search_similar_books(
            query_vector=user_vec,
            query_text=query_text if query_text else None,
            limit=limit * 3,   # overfetch — потом фильтруем по word_count
            exclude_ids=list(exclude_ids),
            filters=self._to_legacy_filters(filters),
            use_hybrid=bool(query_text),
        )

        # Python-фильтры по word_count и average_rating
        # (search_similar_books принимает только dict-фильтры без Range)
        _no_lc = (filters.min_word_count is None and filters.max_word_count is None)
        _eff_min = filters.min_word_count if not _no_lc else 10

        def _passes_wc_filter(r: Dict[str, Any]) -> bool:
            wc = (r.get("metadata") or {}).get("word_count")
            if wc is None:
                return True  # NULL не фильтруем (cannot confirm violation)
            if _eff_min is not None and wc < _eff_min:
                return False
            if filters.max_word_count is not None and wc > filters.max_word_count:
                return False
            return True

        results = [r for r in results if _passes_wc_filter(r)]

        if filters.min_rating is not None:
            results = [
                r for r in results
                if (r.get("metadata") or {}).get("average_rating") is None
                or (r.get("metadata") or {}).get("average_rating") >= filters.min_rating
            ]

        return results[:limit]

    # ─── Общий поиск (без user_id) ────────────────────────────────────────────

    async def _general_search(
        self,
        filters:     RecommendationFilters,
        qdrant_filter: Optional[Filter],
        limit:       int,
        clean_query: Optional[str] = None,
    ) -> List[Dict[str, Any]]:
        """
        Поиск без персонализации.
        Если есть текст запроса (clean_query или mood/keywords/genres) — гибридный поиск.
        Fallback: scroll по фильтрам + сортировка по рейтингу.
        """
        try:
            query_text = self._build_bm25_query(filters, clean_query)

            if query_text:
                # Гибридный поиск: dense (RoSBERTa clustering) + BM25
                query_vector = self.embedding.get_embedding(
                    f"clustering: {clean_query or query_text}"
                )
                results = await self.qdrant.search_similar_books(
                    query_vector=query_vector,
                    query_text=query_text,
                    limit=limit * 3,       # overfetch — потом Python-фильтр
                    filters=self._to_legacy_filters(filters),
                    use_hybrid=True,
                )
                # Python-фильтры по min_rating и word_count
                _no_lc2 = (filters.min_word_count is None and filters.max_word_count is None)
                _eff_min2 = filters.min_word_count if not _no_lc2 else 10

                def _passes_wc(r: Dict[str, Any]) -> bool:
                    wc = (r.get("metadata") or {}).get("word_count")
                    if wc is None:
                        return True
                    if _eff_min2 is not None and wc < _eff_min2:
                        return False
                    if filters.max_word_count is not None and wc > filters.max_word_count:
                        return False
                    return True
                results = [r for r in results if _passes_wc(r)]
                if filters.min_rating is not None:
                    results = [
                        r for r in results
                        if (r.get("metadata") or {}).get("average_rating") is None
                        or (r.get("metadata") or {}).get("average_rating") >= filters.min_rating
                    ]
                return results[:limit]

            # Fallback: scroll + sort_by_popularity (использует Qdrant Range-фильтры)
            points, _ = self.qdrant.client.scroll(
                collection_name=self.qdrant.collection_recommendations,
                scroll_filter=qdrant_filter,
                limit=limit * 3,
                with_payload=True,
            )

            def popularity(p: Any) -> float:
                pay = p.payload or {}
                rating = pay.get("average_rating") or 0.0
                cnt    = pay.get("ratings_count") or 0
                import math
                return float(rating) * math.log(cnt + 1)

            points_sorted = sorted(points, key=popularity, reverse=True)[:limit]
            return [
                {"book_id": p.payload["book_id"], "score": 0.8, "metadata": p.payload}
                for p in points_sorted
                if p.payload.get("book_id")
            ]

        except Exception as exc:
            logger.error(f"NL general_search error: {exc}")
            return []

    # ─── Вспомогательные методы ───────────────────────────────────────────────

    def _build_bm25_query(self, filters: RecommendationFilters, clean_query: Optional[str] = None) -> str:
        """Формирует текстовую строку запроса из mood/era/genres/keywords/clean_query.

        clean_query включается в BM25 только если есть хотя бы один семантический
        фильтр (mood/era/genres/keywords) — иначе «посоветуй книги» без ограничений
        загрязняет поиск нерелевантными токенами и вносит недетерминированность.
        """
        parts: List[str] = []
        if filters.mood:
            parts.append(filters.mood)
        if filters.era:
            parts.append(filters.era)
        for genre in filters.genres:
            parts.append(genre)
        for kw in (filters.keywords or []):
            parts.append(kw)
        # Добавляем clean_query только если уже есть семантические части
        if clean_query and parts:
            parts.append(clean_query)
        return " ".join(parts)

    def _to_legacy_filters(self, filters: RecommendationFilters) -> Dict[str, Any]:
        """Конвертация в формат filters для qdrant.search_similar_books."""
        result: Dict[str, Any] = {}
        if filters.author_filter:
            result["author"] = filters.author_filter
        if filters.language:
            result["language"] = filters.language
        if filters.genres:
            result["genres"] = filters.genres
        if filters.era:
            year_range = _ERA_YEAR_RANGES.get(filters.era.lower(), {})
            if "gte" in year_range:
                result["min_year"] = year_range["gte"]
        return result

    def _build_reason(self, filters: RecommendationFilters) -> str:
        """Строит человекочитаемую причину рекомендации."""
        parts: List[str] = []
        if filters.author_filter:
            parts.append(f"автор: {filters.author_filter}")
        if filters.genres:
            parts.append(", ".join(filters.genres))
        if filters.mood:
            parts.append(f"настроение: {filters.mood}")
        if filters.min_word_count and filters.max_word_count:
            parts.append(f"{filters.min_word_count}к–{filters.max_word_count}к слов")
        elif filters.max_word_count:
            parts.append(f"≤{filters.max_word_count}к слов")
        elif filters.min_word_count:
            parts.append(f"≥{filters.min_word_count}к слов")
        if filters.era:
            parts.append(filters.era)
        if filters.min_rating:
            parts.append(f"рейтинг ≥ {filters.min_rating}")
        return "Подходит под запрос: " + "; ".join(parts) if parts else "Рекомендация для вас"

    @staticmethod
    def _to_recommended_book(item: Dict[str, Any], reason: str) -> RecommendedBook:
        meta = item.get("metadata") or {}
        return RecommendedBook(
            book_id=item.get("book_id") or meta.get("book_id") or 0,
            title=meta.get("title") or "",
            authors=meta.get("authors") or [],
            genres=meta.get("genres") or [],
            average_rating=meta.get("average_rating"),
            ratings_count=meta.get("ratings_count") or 0,
            word_count=meta.get("word_count"),
            cover_image_path=meta.get("cover_image_path"),
            reason=reason,
        )
