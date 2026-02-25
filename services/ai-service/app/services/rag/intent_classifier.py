"""
Intent Classifier Service.

Определяет намерение пользователя за один вызов LLM и извлекает
структурированные данные из запроса.

Типы интентов:
  * book_question   — вопрос по конкретной книге/автору
  * recommendation  — просьба посоветовать книги
  * general         — общий поиск по всей библиотеке
"""

from __future__ import annotations

import json
import re
from typing import Optional

from loguru import logger

from app.models.chat import (
    BookEntity,
    ClassifiedIntent,
    IntentType,
    RecommendationFilters,
)
from app.services.rag.deepseek_client import DeepSeekClient


# ─── Системный промпт классификатора ──────────────────────────────────────────

_SYSTEM_PROMPT = """\
Ты — классификатор запросов для цифровой библиотеки. Твоя задача — \
проанализировать запрос пользователя и вернуть JSON-объект.

Определи тип запроса (поле "intent"):
  "book_question"   — пользователь спрашивает о конкретной книге, авторе или персонажах
                      (примеры: «кто такой Раскольников?», «как зовут героев у Ремарка в Трёх товарищах», «что случилось с Фаустом в конце»)
  "recommendation"  — пользователь просит порекомендовать книги
                      (примеры: «посоветуй фантастику», «что почитать на вечер», «хочу что-то лёгкое»)
  "general"         — всё остальное: поиск цитат, вопросы без привязки к книге, сравнения

Правила для каждого типа:

Если intent = "book_question":
  - Извлеки "title": название книги (null — если не упомянуто)
  - Извлеки "author": имя автора в ИМЕНИТЕЛЬНОМ падеже, каноническая форма
    (например «Ремарка» → «Эрих Мария Ремарк», «Гете» / «Гёте» → «Иоганн Вольфганг фон Гёте»)
    null — если автор не упомянут
  - Извлеки "clean_query": вопрос БЕЗ упоминания книги и автора
    (пример: «как зовут главных героев в произведении Ремарка Три товарища» →
             «как зовут главных героев»)

Если intent = "recommendation":
  - Извлеки "genres": список жанров ([], если не упомянуты)
    Используй ТОЧНЫЕ названия из каталога:
      «фантастика» / «sci-fi» / «научная фантастика» → «Научная фантастика»
      «фэнтези»                                       → «Фэнтези»
      «детектив»                                      → «Детектив»
      «триллер»                                       → «Триллер»
      «классика» / «классическая»                     → «Классическая литература»
      «ужасы» / «хоррор»                              → «Ужасы»
      «мистика»                                       → «Мистика»
      «драма»                                         → «Драма»
      «психологический»                               → «Психологическая проза»
      «исторический» / «история»                      → «Исторический роман»
      «приключения»                                   → «Приключения»
      «философия» / «философский»                     → «Философия»
      «научпоп»                                       → «Научно-популярная литература»
      «поэзия»                                        → «Поэзия»
      «биография»                                     → «Биография»
      «комедия» / «юмор»                              → «Комедия»
      «современная проза»                             → «Современная проза»
      «романтика» / «любовный роман»                  → «Современная проза»
  - Извлеки "min_word_count" и "max_word_count": границы объёма в тыс. слов (null — не указано)
    «на вечер» / «небольшое произведение» → min_word_count: 20,  max_word_count: 80
    «короткая» / «на ночь» «маленькая»           → min_word_count: null, max_word_count: 40
    «на выходные» / «средняя»                → min_word_count: null, max_word_count: 200
    «рассказ» / «рассказы» / «новелла»        → min_word_count: null, max_word_count: 15
    «длинная» / «эпос»                     → min_word_count: null, max_word_count: null
  - Извлеки "mood": настроение (null — не указано)
    Например: «лёгкое», «напряжённое», «душевное», «грустное», «весёлое»
  - Извлеки "language": язык (null — не указан)
    «на русском» / «русскоязычная» → «ru»; «на английском» → «en»
  - Извлеки "min_rating": минимальный рейтинг float (null — не указан)
    «хорошая» / «качественная» → 4.0; не упомянуто → null
  - Извлеки "era": эпоха/период (null — не указан)
    «советская» → «советская», «классика» → «классика», «современная» → «современная»
  - Извлеки "author_filter": имя автора в именительном падеже (null — не указан)
    «от Lондона» / «жанр Лондона» / «произведения Лондона» → "Джек Лондон"
    «у Достоевского» / «по Достоевскому»                → "Фёдор Достоевский"
    Обязательно приводить к именительному падежу: «Достоевского» → "Достоевский"
  - Извлеки "keywords": ключевые слова / теги для семантического поиска ([], если нет явной темы/настроения)
    «мотивирующая» / «вдохновляющая»  → ["мотивация", "вдохновение", "саморазвитие", "победа"]
    «грустная» / «печальная»           → ["драма", "печаль", "трагедия", "потеря"]
    «лёгкая» / «весёлая»               → ["юмор", "оптимистичная", "лёгкое чтение"]
    «о любви»                          → ["любовь", "романтика"]
    «напряжённая» / «захватывающая»    → ["напряжение", "тайна", "интрига"]
    «сюжетные повороты» / «твист» / «неожиданный финал» → ["твист", "неожиданный финал", "интрига", "тайна", "непредсказуемый"]
    «для подростков» / «молодёжная»    → ["взросление", "поиск себя", "для подростков"]
    «душевная» / «тёплая»              → ["душевность", "тепло", "уют", "жизнь"]
    «о войне» / «военная»                → ["война", "подвиг", "мужество", "фронт"]
    «антиутопия» / «дистопия»          → ["антиутопия", "тоталитаризм", "будущее", "бунт"]
    «философская» / «глубокая» / «задумчивая» → ["философия", "смысл жизни", "экзистенция"]
    «о дружбе» / «о предательстве»       → ["дружба", "предательство", "лояльность"]
    Для чисто жанровых запросов (фантастика, детектив) — keywords = []
  - Извлеки "similar_to_books": список названий книг-эталонов ([], если не указаны)
    Заполняй ТОЛЬКО когда пользователь явно называет конкретные книги как образец:
    «что-то похожее на Гарри Поттера» → ["Гарри Поттер"]
    «посоветуй как 1984, но с хорошим концом» → ["1984"]
    «ищу книги в стиле Властелина Колец и Хоббита» → ["Властелин колец", "Хоббит"]
    «что-нибудь похожее на Мастера и Маргариту или Воланда» → ["Мастер и Маргарита"]
    «в стиле Стругацких» — НЕ книга, а автор → similar_to_books: [], author_filter: "Аркадий и Борис Стругацкие"
    Пустой список [], если конкретные книги НЕ названы (жанр/настроение ≠ книга-эталон)
  - Поставь "clean_query" = исходный запрос (для дополнительного RAG поиска)

Если intent = "general":
  - Поставь "clean_query" = исходный запрос

Отвечай ТОЛЬКО валидным JSON, без пояснений, без markdown-блоков.
"""


# ─── Примеры для few-shot (вставляются в user-сообщение) ──────────────────────

_FEW_SHOT = """\
Примеры:

Запрос: «как зовут главных героев в произведении Ремарка Три товарища»
Ответ: {"intent":"book_question","title":"Три товарища","author":"Эрих Мария Ремарк","clean_query":"как зовут главных героев"}

Запрос: «посоветуй что-нибудь из фантастики на вечер»
Ответ: {"intent":"recommendation","genres":["Научная фантастика"],"keywords":[],"author_filter":null,"min_word_count":20,"max_word_count":80,"mood":null,"language":null,"min_rating":null,"era":null,"clean_query":"посоветуй что-нибудь из фантастики на вечер"}

Запрос: «что мне почитать — хочу что-то душевное на русском, не очень длинное»
Ответ: {"intent":"recommendation","genres":[],"keywords":["душевность","тепло","уют","жизнь"],"author_filter":null,"min_word_count":20,"max_word_count":200,"mood":"душевное","language":"ru","min_rating":null,"era":null,"clean_query":"что мне почитать — хочу что-то душевное на русском, не очень длинное"}

Запрос: «посоветуй что-нибудь мотивирующее»
Ответ: {"intent":"recommendation","genres":[],"keywords":["мотивация","вдохновение","саморазвитие","победа"],"author_filter":null,"min_word_count":null,"max_word_count":null,"mood":"мотивирующее","language":null,"min_rating":null,"era":null,"clean_query":"посоветуй что-нибудь мотивирующее"}

Запрос: «найди цитаты про любовь»
Ответ: {"intent":"general","clean_query":"найди цитаты про любовь"}

Запрос: «что происходит в конце Мастера и Маргариты»
Ответ: {"intent":"book_question","title":"Мастер и Маргарита","author":"Михаил Булгаков","clean_query":"что происходит в конце"}

Запрос: «посоветуй советскую классику с высоким рейтингом»
Ответ: {"intent":"recommendation","genres":["Классическая литература"],"keywords":[],"author_filter":null,"min_word_count":null,"max_word_count":null,"mood":null,"language":"ru","min_rating":4.0,"era":"советская","clean_query":"посоветуй советскую классику с высоким рейтингом"}

Запрос: «что почитать из фантастики с элементами драмы?»
Ответ: {"intent":"recommendation","genres":["Научная фантастика","Драма"],"keywords":[],"author_filter":null,"min_word_count":null,"max_word_count":null,"mood":null,"language":null,"min_rating":null,"era":null,"clean_query":"что почитать из фантастики с элементами драмы?"}

Запрос: «посоветуй книги с неожиданными сюжетными поворотами»
Ответ: {"intent":"recommendation","genres":[],"keywords":["твист","неожиданный финал","интрига","тайна","непредсказуемый"],"author_filter":null,"min_word_count":null,"max_word_count":null,"mood":"напряжённое","language":null,"min_rating":null,"era":null,"clean_query":"посоветуй книги с неожиданными сюжетными поворотами"}

Запрос: «посоветуй книги про поиск себя, взросление, любовь, развитие персонажа от Джека Лондона»
Ответ: {"intent":"recommendation","genres":[],"keywords":["взросление","поиск себя","любовь","развитие персонажа"],"author_filter":"Джек Лондон","min_word_count":null,"max_word_count":null,"mood":null,"language":null,"min_rating":null,"era":null,"similar_to_books":[],"clean_query":"посоветуй книги про поиск себя, взросление, любовь, развитие персонажа от Джека Лондона"}

Запрос: «что-нибудь похожее на Гарри Поттера?»
Ответ: {"intent":"recommendation","genres":[],"keywords":[],"author_filter":null,"min_word_count":null,"max_word_count":null,"mood":null,"language":null,"min_rating":null,"era":null,"similar_to_books":["Гарри Поттер"],"clean_query":"что-нибудь похожее на Гарри Поттера?"}

Запрос: «посоветуй книги похожие на Гарри Поттера и Властелин Колец»
Ответ: {"intent":"recommendation","genres":[],"keywords":[],"author_filter":null,"min_word_count":null,"max_word_count":null,"mood":null,"language":null,"min_rating":null,"era":null,"similar_to_books":["Гарри Поттер","Властелин колец"],"clean_query":"посоветуй книги похожие на Гарри Поттера и Властелин Колец"}

Запрос: «ищу что-то в стиле 1984, но с оптимистичным финалом»
Ответ: {"intent":"recommendation","genres":[],"keywords":["антиутопия","оптимистичный финал"],"author_filter":null,"min_word_count":null,"max_word_count":null,"mood":"оптимистичное","language":null,"min_rating":null,"era":null,"similar_to_books":["1984"],"clean_query":"ищу что-то в стиле 1984, но с оптимистичным финалом"}

Запрос: «что-то в духе Стругацких, но про современность»
Ответ: {"intent":"recommendation","genres":[],"keywords":[],"author_filter":"Аркадий и Борис Стругацкие","min_word_count":null,"max_word_count":null,"mood":null,"language":null,"min_rating":null,"era":"современная","similar_to_books":[],"clean_query":"что-то в духе Стругацких, но про современность"}

Теперь классифицируй:
"""


class IntentClassifierService:
    """
    Классифицирует запрос пользователя через LLM и возвращает
    структурированный ClassifiedIntent.

    Использует DeepSeekClient.generate_raw() — один вызов LLM ~200-300 мс.
    При недоступности LLM применяет быстрый эвристический fallback (~0 мс).
    """

    def __init__(self, llm: DeepSeekClient):
        self.llm = llm

    # ─── Публичный API ─────────────────────────────────────────────────────────

    async def classify(self, query: str) -> ClassifiedIntent:
        """
        Классифицирует запрос.

        Returns:
            ClassifiedIntent — тип + извлечённые данные.
        """
        if not self.llm.is_available():
            logger.warning("LLM недоступен — использую эвристический fallback")
            return self._heuristic_classify(query)

        try:
            raw = await self.llm.generate_raw(
                system_prompt=_SYSTEM_PROMPT,
                user_message=f"{_FEW_SHOT}Запрос: «{query}»",
                temperature=0.0,
                max_tokens=256,
            )
            result = self._parse_llm_response(raw, query)
            logger.info(f"Intent classified: {result.intent} | query={query[:60]!r}")
            return result

        except Exception as exc:
            logger.warning(f"IntentClassifier LLM error: {exc} — fallback к эвристикам")
            return self._heuristic_classify(query)

    # ─── Парсинг ответа LLM ────────────────────────────────────────────────────

    def _parse_llm_response(self, raw: str, original_query: str) -> ClassifiedIntent:
        """Парсит JSON-ответ LLM в ClassifiedIntent."""
        # Вырезаем JSON если обёрнут в ```...```
        raw = raw.strip()
        json_match = re.search(r'\{.*\}', raw, re.DOTALL)
        if not json_match:
            raise ValueError(f"No JSON found in LLM response: {raw[:200]!r}")

        data = json.loads(json_match.group())
        intent_str = data.get("intent", "general")

        try:
            intent = IntentType(intent_str)
        except ValueError:
            intent = IntentType.GENERAL

        clean_query = data.get("clean_query") or original_query

        if intent == IntentType.BOOK_QUESTION:
            book_entity = BookEntity(
                title=data.get("title") or None,
                author=data.get("author") or None,
                clean_query=clean_query,
            )
            return ClassifiedIntent(
                intent=intent,
                book_entity=book_entity,
                clean_query=clean_query,
            )

        if intent == IntentType.RECOMMENDATION:
            filters = RecommendationFilters(
                genres=data.get("genres") or [],
                keywords=data.get("keywords") or [],
                author_filter=data.get("author_filter") or None,
                min_word_count=data.get("min_word_count"),
                max_word_count=data.get("max_word_count"),
                mood=data.get("mood") or None,
                language=data.get("language") or None,
                min_rating=data.get("min_rating"),
                era=data.get("era") or None,
                similar_to_books=data.get("similar_to_books") or [],
            )
            return ClassifiedIntent(
                intent=intent,
                recommendation_filters=filters,
                clean_query=clean_query,
            )

        # GENERAL
        return ClassifiedIntent(intent=IntentType.GENERAL, clean_query=clean_query)

    # ─── Эвристический fallback (работает без LLM) ────────────────────────────

    def _heuristic_classify(self, query: str) -> ClassifiedIntent:
        """
        Быстрое определение интента по ключевым словам.
        Запускается если LLM недоступен.
        """
        q = query.lower()

        # Рекомендации
        rec_keywords = [
            "посове", "порекоменд", "что почитать", "что бы почитать",
            "хочу почитать", "какие книги", "подбери", "подскажи книг",
            "на вечер", "на выходные", "для души", "чтение на", "посоветуй",
            "рекоменд",
        ]
        if any(k in q for k in rec_keywords):
            return ClassifiedIntent(
                intent=IntentType.RECOMMENDATION,
                recommendation_filters=RecommendationFilters(),
                clean_query=query,
            )

        # Вопрос по книге — сигналы: «в произведении», «у автора», «в книге»
        book_keywords = [
            "в произведении", "в романе", "в книге", "в повести", "в рассказе",
            "главный герой", "главная героиня", "персонаж", "у ремарка",
            "у булгакова", "у толстого", "у достоевского",
        ]
        if any(k in q for k in book_keywords):
            return ClassifiedIntent(
                intent=IntentType.BOOK_QUESTION,
                book_entity=BookEntity(clean_query=query),
                clean_query=query,
            )

        return ClassifiedIntent(intent=IntentType.GENERAL, clean_query=query)
