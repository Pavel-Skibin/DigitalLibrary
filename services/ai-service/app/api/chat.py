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

from app.config import settings
from app.dependencies import get_smart_assistant_service, get_current_user, get_cache_service
from app.models.chat import ChatRequest, ChatResponse
from app.services.smart_assistant import SmartAssistantService
from app.services.shared.cache_service import CacheService

router = APIRouter(prefix="/chat", tags=["Smart Assistant"])


@router.get("/quota")
async def get_chat_quota(
    current_user: dict = Depends(get_current_user),
    cache: CacheService = Depends(get_cache_service),
) -> dict:
    """Вернуть информацию о квоте запросов к AI-ассистенту для текущего пользователя."""
    role = current_user.get("roleName", "USER")
    if role in ("ADMIN", "MODERATOR"):
        return {"role": role, "unlimited": True, "used": 0, "limit": None, "remaining": None}
    user_id = current_user.get("id")
    used = await cache.get_ai_quota_used(user_id)
    limit = settings.AI_DAILY_QUOTA_USER
    return {
        "role": role,
        "unlimited": False,
        "used": used,
        "limit": limit,
        "remaining": max(0, limit - used),
    }


@router.post("", response_model=ChatResponse)
@router.post("/", response_model=ChatResponse)
async def chat(
    request: ChatRequest,
    current_user: dict = Depends(get_current_user),
    assistant: SmartAssistantService = Depends(get_smart_assistant_service),
    cache: CacheService = Depends(get_cache_service),
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
    user_id = current_user.get("id")
    role = current_user.get("roleName", "USER")

    # Проверка квоты для роли USER
    if role == "USER":
        used = await cache.get_ai_quota_used(user_id)
        if used >= settings.AI_DAILY_QUOTA_USER:
            raise HTTPException(
                status_code=429,
                detail=f"Дневной лимит запросов исчерпан ({settings.AI_DAILY_QUOTA_USER}/день). Возвращайтесь завтра!",
            )

    request.user_id = user_id

    logger.info(
        f"POST /chat | user_id={user_id} | role={role} | "
        f"message={request.message[:80]!r}"
    )

    try:
        response = await assistant.chat(request)
        if role == "USER":
            await cache.increment_ai_quota(user_id)
        return response
    except HTTPException:
        raise
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
