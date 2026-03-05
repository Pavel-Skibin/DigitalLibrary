"""
Unit tests for RAGService.
"""
import pytest
from unittest.mock import AsyncMock, MagicMock, patch

from app.models.rag import (
    RAGQueryRequest,
    RAGSearchRequest,
    RAGQueryResponse,
    RAGSearchResponse,
)
from app.services.rag.deepseek_client import LLMResponse
from app.services.rag.retrieval_service import RetrievedChunk
from app.services.rag.rag_service import RAGService


# ─── Helpers ──────────────────────────────────────────────────────────────────

def _make_chunk(
    text: str = "Текст чанка",
    book_id: int = 1,
    chapter_title: str = "Глава 1",
    chapter_index: int = 0,
    chunk_index: int = 0,
    score: float = 0.9,
) -> RetrievedChunk:
    return RetrievedChunk(
        text=text,
        book_id=book_id,
        chapter_title=chapter_title,
        chapter_index=chapter_index,
        chunk_index=chunk_index,
        position_in_chapter=0.0,
        token_count=len(text.split()),
        score=score,
        metadata={"title": "Книга", "authors": ["Автор"]},
    )


# ─── Fixtures ─────────────────────────────────────────────────────────────────

@pytest.fixture
def mock_settings():
    s = MagicMock()
    s.RAG_CONTEXT_WINDOW = 0
    return s


@pytest.fixture
def mock_retrieval():
    r = MagicMock()
    r.search_chunks.return_value = []
    return r


@pytest.fixture
def mock_llm():
    llm = MagicMock()
    llm.is_available.return_value = True
    llm.generate_answer = AsyncMock(return_value=LLMResponse(answer="Ответ LLM", usage={}))
    return llm


@pytest.fixture
def rag_service(mock_settings, mock_retrieval, mock_llm):
    return RAGService(
        settings=mock_settings,
        retrieval=mock_retrieval,
        llm=mock_llm,
    )


# ─── query(): no chunks found ─────────────────────────────────────────────────

@pytest.mark.asyncio
async def test_query_no_chunks_returns_no_context_answer(rag_service, mock_retrieval):
    mock_retrieval.search_chunks.return_value = []
    request = RAGQueryRequest(query="кто такой Раскольников?", top_k=5)
    response = await rag_service.query(request)
    assert response.context_chunks_count == 0
    assert response.sources == []
    assert "не найдено" in response.answer.lower() or "попробуйте" in response.answer.lower()


# ─── query(): LLM not available ───────────────────────────────────────────────

@pytest.mark.asyncio
async def test_query_llm_unavailable_returns_chunks_summary(rag_service, mock_retrieval, mock_llm):
    mock_retrieval.search_chunks.return_value = [_make_chunk("Важный текст книги")]
    mock_llm.is_available.return_value = False
    request = RAGQueryRequest(query="что произошло?", top_k=5)
    response = await rag_service.query(request)
    assert response.llm_available is False
    assert response.context_chunks_count == 1
    assert len(response.sources) == 1
    mock_llm.generate_answer.assert_not_called()


# ─── query(): successful LLM answer ──────────────────────────────────────────

@pytest.mark.asyncio
async def test_query_llm_available_returns_full_response(rag_service, mock_retrieval, mock_llm):
    mock_retrieval.search_chunks.return_value = [
        _make_chunk("Фрагмент 1", chunk_index=0),
        _make_chunk("Фрагмент 2", chunk_index=1),
    ]
    mock_llm.generate_answer.return_value = LLMResponse(
        answer="Подробный ответ на вопрос", usage={"total_tokens": 100}
    )
    request = RAGQueryRequest(query="расскажи о сюжете", top_k=5)
    response = await rag_service.query(request)
    assert response.llm_available is True
    assert response.answer == "Подробный ответ на вопрос"
    assert response.context_chunks_count == 2
    assert response.usage == {"total_tokens": 100}
    assert len(response.sources) == 2


# ─── query(): LLM throws exception ────────────────────────────────────────────

@pytest.mark.asyncio
async def test_query_llm_exception_returns_error_message(rag_service, mock_retrieval, mock_llm):
    mock_retrieval.search_chunks.return_value = [_make_chunk()]
    mock_llm.generate_answer.side_effect = RuntimeError("API timeout")
    request = RAGQueryRequest(query="вопрос без ответа", top_k=5)
    response = await rag_service.query(request)
    assert response.llm_available is False
    assert "API timeout" in response.answer or "Ошибка" in response.answer


# ─── query(): history is passed to LLM ───────────────────────────────────────

@pytest.mark.asyncio
async def test_query_passes_history_to_llm(rag_service, mock_retrieval, mock_llm):
    mock_retrieval.search_chunks.return_value = [_make_chunk()]
    history = [{"role": "user", "content": "предыдущий вопрос"}]
    request = RAGQueryRequest(query="уточняющий вопрос", top_k=5, history=history)
    await rag_service.query(request)
    call_kwargs = mock_llm.generate_answer.call_args.kwargs
    assert call_kwargs.get("history") == history


# ─── search() ─────────────────────────────────────────────────────────────────

def test_search_returns_chunk_results(rag_service, mock_retrieval):
    mock_retrieval.search_chunks.return_value = [
        _make_chunk("Текст А", book_id=10, chapter_title="Пролог", score=0.95),
        _make_chunk("Текст Б", book_id=10, chapter_title="Глава 1", score=0.80),
    ]
    request = RAGSearchRequest(query="поиск", top_k=5)
    response = rag_service.search(request)
    assert isinstance(response, RAGSearchResponse)
    assert response.total_found == 2
    assert response.results[0].score == pytest.approx(0.95, abs=1e-4)
    assert response.results[0].chapter_title == "Пролог"


def test_search_empty_result(rag_service, mock_retrieval):
    mock_retrieval.search_chunks.return_value = []
    request = RAGSearchRequest(query="ничего нет", top_k=3)
    response = rag_service.search(request)
    assert response.total_found == 0
    assert response.results == []


def test_search_passes_book_ids_filter(rag_service, mock_retrieval):
    mock_retrieval.search_chunks.return_value = []
    request = RAGSearchRequest(query="фрагмент", top_k=5, book_ids=[1, 2, 3])
    response = rag_service.search(request)
    call_kwargs = mock_retrieval.search_chunks.call_args.kwargs
    assert call_kwargs.get("book_ids") == [1, 2, 3]
    assert response.book_ids_filter == [1, 2, 3]


# ─── _build_sources() ─────────────────────────────────────────────────────────

def test_build_sources_truncates_long_text(rag_service):
    long_text = "А" * 300
    chunk = _make_chunk(text=long_text)
    sources = rag_service._build_sources([chunk])
    assert len(sources) == 1
    assert sources[0].text_preview.endswith("…")
    assert len(sources[0].text_preview) <= 203  # 200 chars + "…"


def test_build_sources_short_text_not_truncated(rag_service):
    short_text = "Короткий текст"
    chunk = _make_chunk(text=short_text)
    sources = rag_service._build_sources([chunk])
    assert sources[0].text_preview == short_text
