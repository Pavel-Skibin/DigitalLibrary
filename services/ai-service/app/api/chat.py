"""
Smart Assistant Chat API.

POST /api/ai/chat — единая точка входа для умного ассистента.

Автоматически определяет тип запроса и возвращает:
  * Ответ на вопрос по книге  (intent = book_question)
  * Список рекомендаций       (intent = recommendation)
  * Поиск по всей библиотеке  (intent = general)
"""

from fastapi import APIRouter, Depends, HTTPException
from loguru import logger

from app.dependencies import get_smart_assistant_service
from app.models.chat import ChatRequest, ChatResponse
from app.services.smart_assistant import SmartAssistantService

router = APIRouter(prefix="/chat", tags=["Smart Assistant"])


@router.post("", response_model=ChatResponse)
@router.post("/", response_model=ChatResponse)
async def chat(
    request: ChatRequest,
    assistant: SmartAssistantService = Depends(get_smart_assistant_service),
) -> ChatResponse:
    """
    Умный ассистент цифровой библиотеки.

    Автоматически определяет намерение запроса и обрабатывает его:

    **Примеры запросов:**

    Вопрос по книге:
    ```json
    {
      "message": "Как зовут главных героев в произведении Ремарка Три товарища?"
    }
    ```

    Запрос рекомендаций:
    ```json
    {
      "user_id": 42,
      "message": "Посоветуй что-нибудь из фантастики на вечер"
    }
    ```

    Общий поиск:
    ```json
    {
      "message": "Найди цитаты про любовь и потерю"
    }
    ```

    **Поля ответа:**
    - `intent` — тип запроса: `book_question`, `recommendation`, `general`
    - `answer` — текстовый ответ ассистента
    - `sources` — источники (чанки) для RAG-ответов
    - `recommendations` — список книг для рекомендаций
    - `debug` — отладочная информация (только при `DEBUG=True`)
    """
    logger.info(
        f"POST /chat | user_id={request.user_id} | "
        f"message={request.message[:80]!r}"
    )

    try:
        response = await assistant.chat(request)
        return response
    except Exception as exc:
        logger.error(f"SmartAssistant error: {exc}", exc_info=True)
        raise HTTPException(
            status_code=500,
            detail=f"Ошибка ассистента: {exc}",
        )


@router.get("/health")
async def chat_health(
    assistant: SmartAssistantService = Depends(get_smart_assistant_service),
) -> dict:
    """Проверка работоспособности Smart Assistant (LLM + Qdrant)."""
    llm_ok  = assistant.intent_classifier.llm.is_available()
    rag_ok  = assistant.rag.retrieval is not None

    return {
        "status":          "operational" if llm_ok else "degraded",
        "llm_available":   llm_ok,
        "rag_available":   rag_ok,
        "note": (
            "Работаю в режиме эвристик (без LLM)" if not llm_ok
            else "Полная функциональность"
        ),
    }
