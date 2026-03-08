"""
MetaEnrichmentService — автозаполнение метаданных книги через DeepSeek.

Принимает название + авторов, возвращает структурированные метаданные:
описание, жанры, теги, темы, год, язык, возрастной рейтинг, серию.
"""

from __future__ import annotations

import json
import re
from dataclasses import dataclass, field
from typing import List, Optional

from loguru import logger

from app.config import Settings
from app.services.rag.deepseek_client import DeepSeekClient


@dataclass
class BookMetadata:
    description:      str            = ""
    genres:           List[str]      = field(default_factory=list)
    tags:             List[str]      = field(default_factory=list)
    themes:           List[str]      = field(default_factory=list)
    publication_year: Optional[int]  = None
    language:         str            = "ru"
    age_rating:       str            = "0+"
    series_name:      Optional[str]  = None
    series_number:    Optional[int]  = None


_SYSTEM_PROMPT = """\
Ты — эксперт по мировой литературе. Тебе дадут название книги и имя автора.
Верни ТОЛЬКО корректный JSON без markdown-блоков и без пояснений.

Структура (все поля обязательны):
{
  "description": "<краткое описание 3-5 предложений>",
  "genres": ["<жанр1>", "<жанр2>"],
  "tags": ["<тег1>", "<тег2>", "<тег3>"],
  "publication_year": <целое число или null>,
  "language": "<код языка: ru / en / de / fr / ...>",
  "age_rating": "<одно из: 0+ / 6+ / 12+ / 16+ / 18+>",
  "series_name": "<название серии или null>",
  "series_number": <номер книги в серии или null>
}

Правила:
- genres: 1-4 точных литературных жанра на русском (Фэнтези, Детектив, Классика, Роман...)
- tags: 5-12 тегов на русском — сюда входят конкретные элементы сюжета (магия, война, школа, путешествия...)
  И центральные темы/идеи книги (взросление, дружба, любовь, добро и зло, предательство...).
  Не дублируй жанры в тегах.
- language: язык ОРИГИНАЛА книги
- Если информация неизвестна — null для скалярных полей, [] для списков
"""


class MetaEnrichmentService:
    """Сервис обогащения метаданных книги через DeepSeek LLM."""

    def __init__(self, settings: Settings) -> None:
        self._client = DeepSeekClient(settings)

    def is_available(self) -> bool:
        return self._client.is_available()

    async def enrich(self, title: str, authors: List[str]) -> BookMetadata:
        """
        Запрашивает у DeepSeek метаданные по названию и авторам книги.

        Returns:
            BookMetadata с заполненными полями.

        Raises:
            RuntimeError: Если DEEPSEEK_API_KEY не задан.
        """
        if not self._client.is_available():
            raise RuntimeError(
                "DEEPSEEK_API_KEY не задан — обогащение метаданных недоступно."
            )

        authors_str = ", ".join(authors) if authors else "неизвестен"
        user_message = f'Книга: "{title}"\nАвтор(ы): {authors_str}'

        logger.info(f"Meta enrichment: {title!r} by {authors_str}")

        raw = await self._client.generate_raw(
            system_prompt=_SYSTEM_PROMPT,
            user_message=user_message,
            temperature=0.2,
            max_tokens=800,
        )

        return self._parse(raw)

    # ──────────────────────────────────────────────────────────────────────────

    def _parse(self, raw: str) -> BookMetadata:
        cleaned = re.sub(r"```(?:json)?\s*|\s*```", "", raw).strip()

        try:
            data = json.loads(cleaned)
        except json.JSONDecodeError as exc:
            logger.warning(f"DeepSeek invalid JSON ({exc}): {raw[:200]}")
            return BookMetadata(description=cleaned[:1000])

        def _int(val) -> Optional[int]:
            try:
                return int(val) if val is not None else None
            except (ValueError, TypeError):
                return None

        def _str(val) -> Optional[str]:
            return str(val).strip() if val else None

        return BookMetadata(
            description      = str(data.get("description", "")).strip(),
            genres           = [str(g).strip() for g in data.get("genres", []) if g],
            tags             = self._merge_tags(data),
            themes           = [],  # упразднено — данные перенесены в tags
            publication_year = _int(data.get("publication_year")),
            language         = str(data.get("language", "ru")).strip().lower() or "ru",
            age_rating       = str(data.get("age_rating", "0+")).strip() or "0+",
            series_name      = _str(data.get("series_name")),
            series_number    = _int(data.get("series_number")),
        )

    @staticmethod
    def _merge_tags(data: dict) -> list:
        """Объединяет tags и themes (обратная совместимость) без дублей."""
        raw_tags   = [str(t).strip() for t in data.get("tags",   []) if t]
        raw_themes = [str(t).strip() for t in data.get("themes", []) if t]
        seen = {t.lower() for t in raw_tags}
        return raw_tags + [t for t in raw_themes if t.lower() not in seen]
