"""
ConversationHistoryService — хранение истории диалога в Redis.

Ключ истории:   chat:history:{session_id}
Ключ контекста: chat:context:{session_id}
Формат истории: JSON-список {"role": "user" | "assistant", "content": "..."}
Формат контекста: {"title": "...", "author": "...", "book_ids": [...]}
TTL:   CONVERSATION_HISTORY_TTL секунд (по умолчанию 3600 = 1 час)
Max:   CONVERSATION_MAX_MESSAGES сообщений (по умолчанию 20 = 10 обменов)
"""

from __future__ import annotations

import json
from typing import TYPE_CHECKING, List, Optional

from loguru import logger

from app.config import Settings

if TYPE_CHECKING:
    from app.services.shared.cache_service import CacheService


class ConversationHistoryService:
    """
    Хранит и извлекает историю диалога и активный контекст книги из Redis.

    Разработан для использования совместно с CacheService: принимает его
    как зависимость, чтобы не создавать отдельное подключение к Redis.
    """

    def __init__(self, cache: "CacheService", settings: Settings):
        self._cache = cache
        self._ttl = settings.CONVERSATION_HISTORY_TTL
        self._max_messages = settings.CONVERSATION_MAX_MESSAGES

    # ─── Вспомогательные методы ───────────────────────────────────────────────

    @property
    def _redis(self):
        """Прямой доступ к aioredis.Redis через CacheService."""
        return self._cache.redis

    def _key(self, session_id: str) -> str:
        return f"chat:history:{session_id}"

    def _context_key(self, session_id: str) -> str:
        return f"chat:context:{session_id}"

    # ─── История диалога ──────────────────────────────────────────────────────

    async def get_history(self, session_id: str) -> List[dict]:
        """
        Возвращает историю диалога в формате для DeepSeek messages.

        Returns:
            Список [{"role": "user"|"assistant", "content": "..."}, ...].
            Пустой список, если история отсутствует или Redis недоступен.
        """
        try:
            raw = await self._redis.get(self._key(session_id))
            if raw:
                return json.loads(raw)
        except Exception as exc:
            logger.warning(f"ConversationHistory.get failed [{session_id!r}]: {exc}")
        return []

    async def add_turn(
        self,
        session_id: str,
        user_message: str,
        assistant_message: str,
    ) -> None:
        """
        Добавляет одну пару user/assistant в историю и обновляет TTL.

        Если история превышает max_messages, отрезаются самые старые сообщения.
        Ошибки Redis логируются, но не пробрасываются (не критичны для ответа).
        """
        try:
            history = await self.get_history(session_id)
            history.append({"role": "user",      "content": user_message})
            history.append({"role": "assistant",  "content": assistant_message})

            if len(history) > self._max_messages:
                history = history[-self._max_messages:]

            await self._redis.setex(
                self._key(session_id),
                self._ttl,
                json.dumps(history, ensure_ascii=False),
            )
            logger.debug(
                f"ConversationHistory saved: session={session_id!r}, "
                f"msgs={len(history)}, ttl={self._ttl}s"
            )
        except Exception as exc:
            logger.warning(f"ConversationHistory.add_turn failed [{session_id!r}]: {exc}")

    async def clear(self, session_id: str) -> None:
        """Удаляет всю историю и активный контекст сессии."""
        try:
            await self._redis.delete(self._key(session_id))
            await self._redis.delete(self._context_key(session_id))
            logger.debug(f"ConversationHistory cleared: session={session_id!r}")
        except Exception as exc:
            logger.warning(f"ConversationHistory.clear failed [{session_id!r}]: {exc}")

    # ─── Активный контекст книги ──────────────────────────────────────────────

    async def get_active_context(self, session_id: str) -> Optional[dict]:
        """
        Возвращает активный книжный контекст сессии.

        Returns:
            {"title": "...", "author": "...", "book_ids": [...]}
            или None, если контекст не установлен.
        """
        try:
            raw = await self._redis.get(self._context_key(session_id))
            if raw:
                return json.loads(raw)
        except Exception as exc:
            logger.warning(
                f"ConversationHistory.get_active_context failed [{session_id!r}]: {exc}"
            )
        return None

    async def set_active_context(self, session_id: str, context: dict) -> None:
        """
        Сохраняет активный книжный контекст.

        Args:
            context: {"title": "...", "author": "...", "book_ids": [...]}
        """
        try:
            await self._redis.setex(
                self._context_key(session_id),
                self._ttl,
                json.dumps(context, ensure_ascii=False),
            )
            logger.debug(
                f"ActiveContext set: session={session_id!r}, "
                f"book={context.get('title')!r}, author={context.get('author')!r}"
            )
        except Exception as exc:
            logger.warning(
                f"ConversationHistory.set_active_context failed [{session_id!r}]: {exc}"
            )

    async def clear_active_context(self, session_id: str) -> None:
        """Сбрасывает активный контекст книги (не трогает историю)."""
        try:
            await self._redis.delete(self._context_key(session_id))
            logger.debug(f"ActiveContext cleared: session={session_id!r}")
        except Exception as exc:
            logger.warning(
                f"ConversationHistory.clear_active_context failed [{session_id!r}]: {exc}"
            )

    # ─── Вспомогательные ──────────────────────────────────────────────────────

    @property
    def _redis(self):
        """Возвращает актуальный Redis-клиент из CacheService."""
        return self._cache.redis

    @staticmethod
    def _key(session_id: str) -> str:
        return f"chat:history:{session_id}"
