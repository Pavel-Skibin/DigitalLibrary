"""
DeepSeek LLM Client.

Генерирует ответы на основе контекстных чанков через DeepSeek API.
"""

from __future__ import annotations

from dataclasses import dataclass, field
from typing import List, Optional

import httpx
from loguru import logger

from app.config import Settings


# ─── Ответ LLM ────────────────────────────────────────────────────────────────

@dataclass
class LLMResponse:
    """Ответ языковой модели."""
    answer: str
    usage: dict = field(default_factory=dict)   # input_tokens, output_tokens, total_tokens
    model: str = ""


# ─── Клиент ───────────────────────────────────────────────────────────────────

class DeepSeekClient:
    """
    Клиент DeepSeek API для генерации ответов.

    Требует: DEEPSEEK_API_KEY в .env.
    Системный промпт заточен под роль «помощника-библиотекаря».
    """

    DEEPSEEK_API_URL = "https://api.deepseek.com/v1/chat/completions"

    def __init__(self, settings: Settings):
        self.settings = settings
        self.api_key: Optional[str] = settings.DEEPSEEK_API_KEY
        self.model: str = settings.DEEPSEEK_MODEL

        if self.api_key:
            logger.info(f"LLM backend: DeepSeek API (model={self.model})")
        else:
            logger.warning("DEEPSEEK_API_KEY not set — LLM unavailable.")

    # ─── Публичный API ─────────────────────────────────────────────────────────

    async def generate_answer(
        self,
        query: str,
        context_chunks: List[str],
        system_prompt: Optional[str] = None,
    ) -> LLMResponse:
        """
        Генерирует ответ на вопрос с использованием контекстных фрагментов.

        Args:
            query:          Вопрос пользователя.
            context_chunks: Тексты найденных чанков (релевантный контекст).
            system_prompt:  Системный промпт (None → промпт по умолчанию).

        Returns:
            LLMResponse с ответом и статистиками.

        Raises:
            RuntimeError: Если нет доступного LLM-бэкенда.
            httpx.HTTPError: При ошибке сети / API.
        """
        if system_prompt is None:
            system_prompt = self._default_system_prompt()

        user_message = self._format_user_message(query, context_chunks)

        if not self.api_key:
            raise RuntimeError(
                "DEEPSEEK_API_KEY не задан. Укажите ключ в .env."
            )
        return await self._call_deepseek(system_prompt, user_message)

    def is_available(self) -> bool:
        """Проверяет, задан ли DEEPSEEK_API_KEY."""
        return bool(self.api_key)

    async def generate_raw(
        self,
        system_prompt: str,
        user_message: str,
        temperature: float = 0.0,
        max_tokens: int = 512,
    ) -> str:
        """
        Прямой вызов LLM с произвольными system/user промптами.

        В отличие от generate_answer(), не форматирует чанки — используется
        для задач классификации и структурированной генерации (JSON-ответы).

        Returns:
            Сырой текст ответа LLM.
        """
        if not self.api_key:
            raise RuntimeError("DEEPSEEK_API_KEY не задан. Укажите ключ в .env.")
        return await self._call_raw_deepseek(system_prompt, user_message, temperature, max_tokens)

    # ─── Приватные методы ──────────────────────────────────────────────────────

    async def _call_deepseek(self, system_prompt: str, user_message: str) -> LLMResponse:
        """Вызов DeepSeek REST API."""
        logger.debug(f"DeepSeek API call (model={self.model})")

        async with httpx.AsyncClient(timeout=60.0) as client:
            response = await client.post(
                self.DEEPSEEK_API_URL,
                headers={
                    "Authorization": f"Bearer {self.api_key}",
                    "Content-Type": "application/json",
                },
                json={
                    "model": self.model,
                    "messages": [
                        {"role": "system", "content": system_prompt},
                        {"role": "user",   "content": user_message},
                    ],
                    "temperature": 0.3,         # Низкая для фактичных ответов
                    "max_tokens": 1024,
                },
            )
            response.raise_for_status()
            data = response.json()

        answer = data["choices"][0]["message"]["content"]
        usage  = data.get("usage", {})
        logger.debug(f"DeepSeek usage: {usage}")
        return LLMResponse(answer=answer, usage=usage, model=self.model)

    # ─── Промпты ───────────────────────────────────────────────────────────────

    def _default_system_prompt(self) -> str:
        return (
            "Ты — помощник в цифровой библиотеке, который отвечает на вопросы "
            "исключительно на основе предоставленных фрагментов текста.\n\n"
            "СТРОГИЕ ПРАВИЛА (нарушение недопустимо):\n"
            "1. ТОЛЬКО КОНТЕКСТ: отвечай исключительно по предоставленным фрагментам. "
            "Не используй свои обучающие знания о книге, авторе или персонажах — "
            "даже если книга тебе известна. Твои знания могут расходиться с "
            "конкретным переводом или изданием; приоритет всегда у текста фрагментов.\n"
            "2. НЕТ ОТВЕТА: если в фрагментах нет информации для ответа — "
            "прямо скажи «В предоставленных фрагментах нет ответа на этот вопрос».\n"
            "3. НЕТ ДОМЫСЛОВ: не добавляй ничего, чего нет во фрагментах, "
            "не интерпретируй сверх написанного.\n"
            "4. ЦИТАТЫ: при необходимости цитируй текст дословно (в кавычках).\n"
            "5. КРАТКОСТЬ: 2–5 предложений, по существу вопроса.\n"
            "6. ЯЗЫК: пиши на том же языке, на котором задан вопрос."
        )

    def _format_user_message(self, query: str, context_chunks: List[str]) -> str:
        """Форматирует сообщение пользователя: нумерованный контекст + вопрос."""
        context = "\n\n".join(
            f"[Фрагмент {i + 1}]\n{chunk}"
            for i, chunk in enumerate(context_chunks)
        )
        return (
            f"Контекст из книг:\n"
            f"{context}\n\n"
            f"---\n\n"
            f"Вопрос: {query}\n\n"
            f"Ответ:"
        )

    async def _call_raw_deepseek(
        self,
        system_prompt: str,
        user_message: str,
        temperature: float,
        max_tokens: int,
    ) -> str:
        """Прямой вызов DeepSeek API без форматирования контекста."""
        async with httpx.AsyncClient(timeout=30.0) as client:
            response = await client.post(
                self.DEEPSEEK_API_URL,
                headers={
                    "Authorization": f"Bearer {self.api_key}",
                    "Content-Type": "application/json",
                },
                json={
                    "model": self.model,
                    "messages": [
                        {"role": "system", "content": system_prompt},
                        {"role": "user",   "content": user_message},
                    ],
                    "temperature":  temperature,
                    "max_tokens":   max_tokens,
                    "response_format": {"type": "json_object"},
                },
            )
            response.raise_for_status()
            data = response.json()
        return data["choices"][0]["message"]["content"]

