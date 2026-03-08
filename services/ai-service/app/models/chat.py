"""
Pydantic-модели для Smart Assistant Chat API.

POST /api/ai/chat — единая точка входа для:
  * Вопросов по конкретным книгам    (BOOK_QUESTION)
  * Запросов на рекомендации          (RECOMMENDATION)
  * Общих вопросов по всей библиотеке (GENERAL)
"""

from __future__ import annotations

from enum import Enum
from typing import Any, Dict, List, Optional

from pydantic import BaseModel, Field


# ─── Типы запроса ─────────────────────────────────────────────────────────────

class IntentType(str, Enum):
    BOOK_QUESTION  = "book_question"   # «Как зовут главного героя в Трёх товарищах?»
    RECOMMENDATION = "recommendation"  # «Посоветуй фантастику на вечер»
    GENERAL        = "general"         # «Найди цитаты про любовь»
    QUOTE_SEARCH   = "quote_search"    # «Найди цитату про одиночество из Ремарка»


# ─── Внутренние структуры классификатора ──────────────────────────────────────

class BookEntity(BaseModel):
    """Данные о книге, извлечённые из запроса (только для BOOK_QUESTION)."""
    title:       Optional[str] = None   # «Три товарища»
    author:      Optional[str] = None   # «Эрих Мария Ремарк»
    clean_query: str                    # вопрос без упоминания книги/автора
    # scope определяет область поиска:
    #   "book"   — искать только в конкретной книге (title явно назван)
    #   "series" — искать по всей серии/всем книгам автора (без конкретной книги)
    scope:       str = "book"


class RecommendationFilters(BaseModel):
    """Фильтры, извлечённые из запроса (только для RECOMMENDATION)."""
    genres:           List[str]       = Field(default_factory=list)  # ["Научная фантастика"]
    keywords:         List[str]       = Field(default_factory=list)  # ["мотивация", "саморазвитие"]
    author_filter:    Optional[str]   = None   # имя автора (именительный падеж, жёсткий фильтр)
    min_word_count:   Optional[int]   = None   # тыс. слов (нижняя граница, 20 = от 20к слов)
    max_word_count:   Optional[int]   = None   # тыс. слов (80 = до 80к слов)
    mood:             Optional[str]   = None   # «лёгкое», «напряжённое», …
    language:         Optional[str]   = None   # «ru», «en»
    min_rating:       Optional[float] = None   # >= 4.0
    era:              Optional[str]   = None   # «советская», «классика», «современная»
    similar_to_books: List[str]       = Field(default_factory=list)  # ["Гарри Поттер", "1984"]


class ClassifiedIntent(BaseModel):
    """Результат работы IntentClassifier."""
    intent:                  IntentType
    book_entity:             Optional[BookEntity]            = None
    recommendation_filters:  Optional[RecommendationFilters] = None
    clean_query:             str   # очищенный запрос без метаданных


# ─── Запрос пользователя ──────────────────────────────────────────────────────

class ChatRequest(BaseModel):
    """Входящий запрос к умному ассистенту."""
    message:    str            = Field(..., min_length=2, description="Сообщение пользователя")
    user_id:    Optional[int]  = Field(None, description="ID пользователя (для персонализации)")
    top_k:      int            = Field(5, ge=1, le=10, description="Макс. чанков для RAG")
    language:   Optional[str]  = Field(None, description="Предпочитаемый язык ответа")
    session_id: Optional[str]  = Field(None, description="ID сессии для хранения истории диалога")


# ─── Элементы ответа ──────────────────────────────────────────────────────────

class BookSource(BaseModel):
    """Источник — чанк из книги."""
    book_id:       int
    title:         str
    authors:       List[str] = []
    chapter_title: str       = ""
    chunk_index:   int       = 0
    score:         float


class RecommendedBook(BaseModel):
    """Рекомендованная книга."""
    book_id:        int
    title:          str
    authors:        List[str]       = []
    genres:         List[str]       = []
    average_rating: Optional[float] = None
    ratings_count:  int             = 0
    word_count:     Optional[int]   = None
    cover_image_path: Optional[str] = None
    reason:         str             = ""


# ─── Ответ ────────────────────────────────────────────────────────────────────

class ChatResponse(BaseModel):
    """Ответ умного ассистента."""
    intent:          IntentType
    answer:          str

    # Эхо session_id (если передан в запросе)
    session_id:      Optional[str]         = None

    # Для BOOK_QUESTION / GENERAL
    sources:         List[BookSource]     = Field(default_factory=list)

    # Для RECOMMENDATION
    recommendations: List[RecommendedBook] = Field(default_factory=list)

    # Отладочная информация (только при DEBUG=True)
    debug: Optional[Dict[str, Any]] = None
