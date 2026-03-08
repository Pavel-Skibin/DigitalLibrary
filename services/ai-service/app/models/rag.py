"""
Pydantic-модели для RAG API.
"""

from __future__ import annotations

from typing import List, Optional

from pydantic import BaseModel, Field


# ─── Запрос ───────────────────────────────────────────────────────────────────

class RAGSearchRequest(BaseModel):
    """Поиск чанков без LLM-ответа (семантический поиск по тексту книг)."""
    query: str = Field(..., description="Поисковый запрос", min_length=2)
    top_k: int = Field(5, description="Количество результатов", ge=1, le=20)
    book_ids: Optional[List[int]] = Field(
        None, description="Ограничить поиск конкретными книгами"
    )
    use_hybrid: bool = Field(True, description="Hybrid search (Dense + BM25)")
    auto_filter_books: bool = Field(
        False, 
        description="Автоматически найти релевантные книги перед поиском (two-stage retrieval)"
    )


class RAGQueryRequest(BaseModel):
    """RAG-запрос: поиск + ответ через LLM."""
    query: str = Field(..., description="Вопрос пользователя", min_length=3)
    top_k: int = Field(5, description="Количество чанков для контекста", ge=1, le=10)
    book_ids: Optional[List[int]] = Field(
        None, description="Ограничить поиск конкретными книгами"
    )
    include_context: bool = Field(
        True, description="Включить найденные чанки в ответ"
    )
    auto_filter_books: bool = Field(
        False, 
        description="Автоматически найти релевантные книги перед поиском (two-stage retrieval)"
    )
    history: Optional[List[dict]] = Field(
        None, description="История диалога [{role, content}, ...] для контекста LLM"
    )


# ─── Элементы ответа ──────────────────────────────────────────────────────────

class ChunkResult(BaseModel):
    """Один найденный чанк."""
    book_id: int
    chapter_title: str
    chapter_index: int
    chunk_index: int
    position_in_chapter: float
    token_count: int
    score: float
    text: str
    # Метаданные книги
    title: str = ""
    authors: List[str] = []


class ChunkSource(BaseModel):
    """Источник (скрытая версия чанка для ответа LLM)."""
    book_id: int
    chapter_title: str
    chunk_index: int
    score: float
    text_preview: str = Field(..., description="Первые 200 символов текста")


# ─── Ответы ───────────────────────────────────────────────────────────────────

class RAGSearchResponse(BaseModel):
    """Ответ на поисковый запрос (без LLM)."""
    query: str
    results: List[ChunkResult]
    total_found: int
    book_ids_filter: Optional[List[int]] = None


class RAGQueryResponse(BaseModel):
    """Ответ RAG-системы с ответом LLM + источники."""
    answer: str = Field(..., description="Ответ языковой модели")
    sources: List[ChunkSource] = Field(
        ..., description="Источники (чанки), использованные для ответа"
    )
    query: str
    context_chunks_count: int
    usage: dict = Field(default_factory=dict, description="Статистика токенов LLM")
    llm_available: bool = True
