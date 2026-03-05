"""
Integration tests: IntentClassifierService → RAGService pipeline.

Tests the end-to-end flow where a classified query is routed into
the RAG pipeline, verifying that the two components work correctly together.
All external calls (LLM, Qdrant) are mocked at the boundary.
"""
import json
import pytest
from unittest.mock import AsyncMock, MagicMock

from app.models.chat import ClassifiedIntent, IntentType, BookEntity, RecommendationFilters
from app.models.rag import RAGQueryRequest, RAGQueryResponse
from app.services.rag.deepseek_client import LLMResponse
from app.services.rag.intent_classifier import IntentClassifierService
from app.services.rag.rag_service import RAGService
from app.services.rag.retrieval_service import RetrievedChunk


# ─── Shared helpers ───────────────────────────────────────────────────────────

def _make_chunk(text: str = "Фрагмент книги", book_id: int = 1, score: float = 0.9) -> RetrievedChunk:
    return RetrievedChunk(
        text=text,
        book_id=book_id,
        chapter_title="Глава 1",
        chapter_index=0,
        chunk_index=0,
        position_in_chapter=0.0,
        token_count=len(text.split()),
        score=score,
        metadata={"title": "Тестовая книга", "authors": ["Тестовый автор"]},
    )


# ─── Shared fixtures ──────────────────────────────────────────────────────────

@pytest.fixture
def mock_llm():
    llm = MagicMock()
    llm.is_available.return_value = True
    llm.generate_raw = AsyncMock()
    llm.generate_answer = AsyncMock()
    return llm


@pytest.fixture
def mock_retrieval():
    r = MagicMock()
    r.search_chunks.return_value = []
    return r


@pytest.fixture
def mock_settings():
    s = MagicMock()
    s.RAG_CONTEXT_WINDOW = 0
    return s


@pytest.fixture
def classifier(mock_llm):
    return IntentClassifierService(llm=mock_llm)


@pytest.fixture
def rag_service(mock_settings, mock_retrieval, mock_llm):
    return RAGService(settings=mock_settings, retrieval=mock_retrieval, llm=mock_llm)


# ─── Pipeline: classify then query RAG ────────────────────────────────────────

@pytest.mark.asyncio
async def test_book_question_flows_into_rag_query(classifier, rag_service, mock_llm, mock_retrieval):
    """
    Полный пайплайн: вопрос по книге → classify → RAG query → ответ.
    """
    # 1. Classify: mock LLM returns book_question
    mock_llm.generate_raw.return_value = json.dumps({
        "intent": "book_question",
        "title": "Мастер и Маргарита",
        "author": "Михаил Булгаков",
        "scope": "book",
        "clean_query": "кто такой Воланд",
    })

    # 2. RAG: mock retrieval and LLM answer
    mock_retrieval.search_chunks.return_value = [
        _make_chunk("Воланд — таинственный персонаж, дьявол в образе профессора"),
    ]
    mock_llm.generate_answer.return_value = LLMResponse(
        answer="Воланд — это персонаж, олицетворяющий дьявола.", usage={}
    )

    # ── Step 1: classify ──
    intent = await classifier.classify("кто такой Воланд в Мастере и Маргарите")
    assert intent.intent == IntentType.BOOK_QUESTION
    assert intent.book_entity is not None
    assert intent.book_entity.title == "Мастер и Маргарита"

    # ── Step 2: use clean_query in RAG ──
    rag_request = RAGQueryRequest(query=intent.clean_query, top_k=5)
    rag_response = await rag_service.query(rag_request)

    assert rag_response.llm_available is True
    assert "Воланд" in rag_response.answer
    assert rag_response.context_chunks_count == 1
    # Verify retrieval was called with the clean query (not the original)
    mock_retrieval.search_chunks.assert_called_once()
    call_kwargs = mock_retrieval.search_chunks.call_args.kwargs
    assert call_kwargs["query"] == "кто такой Воланд"


@pytest.mark.asyncio
async def test_recommendation_query_routed_without_rag(classifier, mock_llm):
    """
    Рекомендательные запросы классифицируются корректно и содержат фильтры.
    Проверяем, что классификатор + фильтры работают вместе как единый объект.
    """
    mock_llm.generate_raw.return_value = json.dumps({
        "intent": "recommendation",
        "genres": ["Детектив"],
        "keywords": ["интрига", "загадка"],
        "author_filter": None,
        "min_word_count": None,
        "max_word_count": 200,
        "mood": "захватывающее",
        "language": "ru",
        "min_rating": 4.0,
        "era": None,
        "similar_to_books": [],
        "clean_query": "посоветуй детектив с интригой",
    })

    intent = await classifier.classify("посоветуй хороший детектив с интригой на русском")
    assert intent.intent == IntentType.RECOMMENDATION
    f = intent.recommendation_filters
    assert "Детектив" in f.genres
    assert f.mood == "захватывающее"
    assert f.language == "ru"
    assert f.min_rating == 4.0
    assert f.max_word_count == 200
    # clean_query preserved for potential semantic search
    assert "детектив" in intent.clean_query.lower()


@pytest.mark.asyncio
async def test_rag_fallback_when_no_chunks_found(classifier, rag_service, mock_llm, mock_retrieval):
    """
    Если RAG не находит чанков — возвращает ответ «не найдено»,
    и LLM не вызывается.
    """
    mock_llm.generate_raw.return_value = json.dumps({
        "intent": "book_question",
        "title": "Неизвестная книга",
        "author": None,
        "scope": "book",
        "clean_query": "что происходит в конце",
    })
    mock_retrieval.search_chunks.return_value = []  # ничего не найдено

    intent = await classifier.classify("что происходит в конце неизвестной книги")
    assert intent.intent == IntentType.BOOK_QUESTION

    rag_request = RAGQueryRequest(query=intent.clean_query, top_k=3)
    rag_response = await rag_service.query(rag_request)

    assert rag_response.context_chunks_count == 0
    assert rag_response.sources == []
    # LLM generate_answer should NOT be called when no chunks found
    mock_llm.generate_answer.assert_not_called()


@pytest.mark.asyncio
async def test_heuristic_classify_feeds_into_rag(mock_settings, mock_retrieval):
    """
    Когда LLM классификатора недоступен — используется эвристика,
    и результат корректно передаётся в RAG (который работает со своим LLM).
    """
    # Отдельный LLM для классификатора (недоступен → эвристика)
    classifier_llm = MagicMock()
    classifier_llm.is_available.return_value = False

    # Отдельный LLM для RAG (доступен)
    rag_llm = MagicMock()
    rag_llm.is_available.return_value = True
    rag_llm.generate_answer = AsyncMock(
        return_value=LLMResponse(answer="Ответ по фрагменту", usage={})
    )

    classifier_svc = IntentClassifierService(llm=classifier_llm)
    rag_svc = RAGService(settings=mock_settings, retrieval=mock_retrieval, llm=rag_llm)

    mock_retrieval.search_chunks.return_value = [_make_chunk("Найденный фрагмент книги")]

    # Эвристика → GENERAL (нет явных ключевых слов)
    intent = await classifier_svc.classify("о чём эта история")
    rag_request = RAGQueryRequest(query=intent.clean_query, top_k=5)
    rag_response = await rag_svc.query(rag_request)

    assert rag_response.llm_available is True
    assert rag_response.context_chunks_count == 1
    assert rag_response.answer == "Ответ по фрагменту"


@pytest.mark.asyncio
async def test_quote_search_intent_classification(classifier, mock_llm):
    """
    Запрос на поиск цитаты классифицируется с извлечением книги/автора.
    """
    mock_llm.generate_raw.return_value = json.dumps({
        "intent": "quote_search",
        "title": None,
        "author": "Эрих Мария Ремарк",
        "clean_query": "цитата про одиночество",
    })

    intent = await classifier.classify("найди цитату про одиночество у Ремарка")
    assert intent.intent == IntentType.QUOTE_SEARCH
    assert intent.book_entity is not None
    assert intent.book_entity.author == "Эрих Мария Ремарк"
    assert intent.book_entity.title is None


@pytest.mark.asyncio
async def test_rag_search_uses_book_ids_filter(classifier, rag_service, mock_llm, mock_retrieval):
    """
    book_ids фильтр правильно пробрасывается через RAG pipeline.
    """
    mock_llm.generate_raw.return_value = json.dumps({
        "intent": "book_question",
        "title": "Зулали",
        "author": "Наринэ Абгарян",
        "scope": "book",
        "clean_query": "о чём книга",
    })
    mock_retrieval.search_chunks.return_value = [_make_chunk("Зулали — история о девочке", book_id=42)]
    mock_llm.generate_answer.return_value = LLMResponse(answer="Это история о девочке", usage={})

    intent = await classifier.classify("о чём книга Зулали Абгарян")

    rag_request = RAGQueryRequest(query=intent.clean_query, top_k=5, book_ids=[42])
    rag_response = await rag_service.query(rag_request)

    assert rag_response.context_chunks_count == 1
    call_kwargs = mock_retrieval.search_chunks.call_args.kwargs
    assert call_kwargs.get("book_ids") == [42]
