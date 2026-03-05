"""
Unit tests for IntentClassifierService.
"""
import json
import pytest
from unittest.mock import AsyncMock, MagicMock

from app.models.chat import ClassifiedIntent, IntentType
from app.services.rag.intent_classifier import IntentClassifierService


# ─── Fixtures ─────────────────────────────────────────────────────────────────

@pytest.fixture
def mock_llm():
    llm = MagicMock()
    llm.is_available.return_value = True
    llm.generate_raw = AsyncMock()
    return llm


@pytest.fixture
def classifier(mock_llm):
    return IntentClassifierService(llm=mock_llm)


# ─── classify(): LLM unavailable → heuristic fallback ─────────────────────────

@pytest.mark.asyncio
async def test_classify_llm_unavailable_uses_heuristic(classifier, mock_llm):
    mock_llm.is_available.return_value = False
    result = await classifier.classify("что почитать на вечер")
    assert result.intent == IntentType.RECOMMENDATION
    mock_llm.generate_raw.assert_not_called()


@pytest.mark.asyncio
async def test_classify_llm_error_falls_back_to_heuristic(classifier, mock_llm):
    mock_llm.generate_raw.side_effect = RuntimeError("connection error")
    result = await classifier.classify("посоветуй что-нибудь")
    assert result.intent == IntentType.RECOMMENDATION
    assert result.clean_query == "посоветуй что-нибудь"


# ─── classify(): book_question ────────────────────────────────────────────────

@pytest.mark.asyncio
async def test_classify_book_question(classifier, mock_llm):
    mock_llm.generate_raw.return_value = json.dumps({
        "intent": "book_question",
        "title": "Мастер и Маргарита",
        "author": "Михаил Булгаков",
        "scope": "book",
        "clean_query": "что происходит в конце",
    })
    result = await classifier.classify("что происходит в конце Мастера и Маргариты")
    assert result.intent == IntentType.BOOK_QUESTION
    assert result.book_entity is not None
    assert result.book_entity.title == "Мастер и Маргарита"
    assert result.book_entity.author == "Михаил Булгаков"
    assert result.book_entity.scope == "book"
    assert result.clean_query == "что происходит в конце"


# ─── classify(): recommendation ───────────────────────────────────────────────

@pytest.mark.asyncio
async def test_classify_recommendation(classifier, mock_llm):
    mock_llm.generate_raw.return_value = json.dumps({
        "intent": "recommendation",
        "genres": ["Научная фантастика"],
        "keywords": [],
        "author_filter": None,
        "min_word_count": 20,
        "max_word_count": 80,
        "mood": None,
        "language": None,
        "min_rating": None,
        "era": None,
        "similar_to_books": [],
        "clean_query": "посоветуй что-нибудь из фантастики на вечер",
    })
    result = await classifier.classify("посоветуй что-нибудь из фантастики на вечер")
    assert result.intent == IntentType.RECOMMENDATION
    assert result.recommendation_filters is not None
    assert result.recommendation_filters.genres == ["Научная фантастика"]
    assert result.recommendation_filters.min_word_count == 20
    assert result.recommendation_filters.max_word_count == 80


# ─── classify(): quote_search ─────────────────────────────────────────────────

@pytest.mark.asyncio
async def test_classify_quote_search(classifier, mock_llm):
    mock_llm.generate_raw.return_value = json.dumps({
        "intent": "quote_search",
        "title": "Мастер и Маргарита",
        "author": "Михаил Булгаков",
        "clean_query": "фраза про смерть",
    })
    result = await classifier.classify("есть ли фраза про смерть в Мастере и Маргарите")
    assert result.intent == IntentType.QUOTE_SEARCH
    assert result.book_entity is not None
    assert result.book_entity.title == "Мастер и Маргарита"
    assert result.clean_query == "фраза про смерть"


# ─── classify(): general ──────────────────────────────────────────────────────

@pytest.mark.asyncio
async def test_classify_general(classifier, mock_llm):
    mock_llm.generate_raw.return_value = json.dumps({
        "intent": "general",
        "clean_query": "привет",
    })
    result = await classifier.classify("привет")
    assert result.intent == IntentType.GENERAL
    assert result.book_entity is None
    assert result.recommendation_filters is None


# ─── classify(): with history context ────────────────────────────────────────

@pytest.mark.asyncio
async def test_classify_with_history_passes_context(classifier, mock_llm):
    mock_llm.generate_raw.return_value = json.dumps({
        "intent": "book_question",
        "title": "Владычица озера",
        "author": "Анджей Сапковский",
        "scope": "book",
        "clean_query": "кем был Менно Коегорн",
    })
    history = [
        {"role": "user", "content": "Как погиб Менно Коегорн во Владычице озера?"},
        {"role": "assistant", "content": "Менно Коегорн погиб в осаде Нильфгаарда..."},
    ]
    result = await classifier.classify("кем он был?", history=history)
    assert result.intent == IntentType.BOOK_QUESTION
    # Verify context was included in the LLM call
    call_args = mock_llm.generate_raw.call_args
    user_message = call_args.kwargs.get("user_message", "")
    assert "Предыдущий контекст диалога:" in user_message


# ─── _parse_llm_response() ────────────────────────────────────────────────────

def test_parse_llm_response_book_question(classifier):
    raw = json.dumps({
        "intent": "book_question",
        "title": "1984",
        "author": "Джордж Оруэлл",
        "scope": "book",
        "clean_query": "о чём книга",
    })
    result = classifier._parse_llm_response(raw, "о чём книга 1984")
    assert result.intent == IntentType.BOOK_QUESTION
    assert result.book_entity.title == "1984"
    assert result.book_entity.author == "Джордж Оруэлл"


def test_parse_llm_response_recommendation_with_filters(classifier):
    raw = json.dumps({
        "intent": "recommendation",
        "genres": ["Детектив", "Триллер"],
        "keywords": ["напряжение", "интрига"],
        "author_filter": None,
        "min_word_count": None,
        "max_word_count": 200,
        "mood": "напряжённое",
        "language": "ru",
        "min_rating": 4.0,
        "era": None,
        "similar_to_books": [],
        "clean_query": "детектив с интригой",
    })
    result = classifier._parse_llm_response(raw, "хочу детектив с интригой")
    assert result.intent == IntentType.RECOMMENDATION
    f = result.recommendation_filters
    assert "Детектив" in f.genres
    assert f.mood == "напряжённое"
    assert f.language == "ru"
    assert f.min_rating == 4.0
    assert f.max_word_count == 200


def test_parse_llm_response_unknown_intent_falls_back_to_general(classifier):
    raw = json.dumps({
        "intent": "unknown_type",
        "clean_query": "something",
    })
    result = classifier._parse_llm_response(raw, "something")
    assert result.intent == IntentType.GENERAL


def test_parse_llm_response_json_wrapped_in_markdown(classifier):
    raw = '```json\n{"intent":"general","clean_query":"hello"}\n```'
    result = classifier._parse_llm_response(raw, "hello")
    assert result.intent == IntentType.GENERAL


def test_parse_llm_response_no_json_raises(classifier):
    with pytest.raises((ValueError, Exception)):
        classifier._parse_llm_response("нет json здесь", "query")


def test_parse_llm_response_missing_clean_query_uses_original(classifier):
    raw = json.dumps({"intent": "general"})
    result = classifier._parse_llm_response(raw, "original query")
    assert result.clean_query == "original query"


# ─── _heuristic_classify() ────────────────────────────────────────────────────

def test_heuristic_recommendation_keyword(classifier):
    result = classifier._heuristic_classify("посоветуй что-нибудь лёгкое")
    assert result.intent == IntentType.RECOMMENDATION
    assert result.recommendation_filters is not None


def test_heuristic_recommendation_chto_pochitat(classifier):
    result = classifier._heuristic_classify("что почитать на выходных?")
    assert result.intent == IntentType.RECOMMENDATION


def test_heuristic_quote_search(classifier):
    result = classifier._heuristic_classify("найди цитату про одиночество")
    assert result.intent == IntentType.QUOTE_SEARCH


def test_heuristic_book_question(classifier):
    result = classifier._heuristic_classify("расскажи про главного героя в книге")
    assert result.intent == IntentType.BOOK_QUESTION


def test_heuristic_general_fallback(classifier):
    result = classifier._heuristic_classify("привет, как дела?")
    assert result.intent == IntentType.GENERAL
    assert result.clean_query == "привет, как дела?"
