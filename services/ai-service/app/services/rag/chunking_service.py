"""
Text Chunking Service для RAG.

Разбивает текст книги на чанки ~425 токенов с overlap ~60,
сохраняя границы абзацев и структуру глав.
"""

from __future__ import annotations

import re
from dataclasses import dataclass, field
from typing import List, Optional

from loguru import logger
from transformers import AutoTokenizer, PreTrainedTokenizerBase

from app.config import Settings
from app.services.rag.fb2_parser import FB2Content, BookChapter


# ─── Модель данных ─────────────────────────────────────────────────────────────

@dataclass
class TextChunk:
    """Чанк текста книги с полными метаданными."""

    # Контент
    text: str
    token_count: int

    # Позиция в книге
    book_id: int
    chunk_index: int            # Глобальный порядковый номер в книге (0-based)
    total_chunks: int           # Заполняется после разбивки всей книги (-1 до этого)

    # Позиция в главе
    chapter_title: str
    chapter_index: int
    position_in_chapter: int    # Порядковый номер чанка внутри главы (0-based)

    # Расширенные метаданные книги (из FB2 + PostgreSQL)
    book_metadata: dict = field(default_factory=dict)


# ─── Сервис ────────────────────────────────────────────────────────────────────

class ChunkingService:
    """
    Разбивает книгу на чанки с перекрытием.

    Алгоритм (paragraph-aware):
      1. Сплитит главу по двойным переносам строки (абзацы).
      2. Накапливает абзацы, пока сумма токенов <= target_chunk_size.
      3. При переполнении — сохраняет чанк. Overlap: последний абзац
         предыдущего чанка становится первым абзацем нового.
      4. Слишком длинные абзацы (> target_size) сплитятся по предложениям.
    """

    def __init__(self, settings: Settings):
        self.settings = settings
        self.target_size: int = settings.RAG_CHUNK_SIZE

        # Жёсткий лимит: USER-bge-m3 поддерживает до 8192 токенов.
        # Для retrieval используем 800 токенов, hard_max=1200 как потолок.
        self.hard_max_tokens: int = 1200
        self.overlap_tokens: int = settings.RAG_CHUNK_OVERLAP
        self.min_chunk_tokens: int = 50   # Минимальный размер чанка — мусор мельче этого отбрасывается

        # Загружаем только токенизатор (без весов модели — быстро и легко)
        logger.info(f"Loading tokenizer for chunking: {settings.EMBEDDING_MODEL_NAME_RAG}")
        self._tokenizer: PreTrainedTokenizerBase = AutoTokenizer.from_pretrained(
            settings.EMBEDDING_MODEL_NAME_RAG
        )
        # Отключаем предупреждение о длине последовательности — мы сами контролируем лимит
        if hasattr(self._tokenizer, 'model_max_length'):
            self._tokenizer.model_max_length = 1_000_000
        logger.info("Tokenizer ready")

    # ─── Публичный API ─────────────────────────────────────────────────────────

    def chunk_book(
        self,
        book_id: int,
        content: FB2Content,
        book_metadata: Optional[dict] = None,
    ) -> List[TextChunk]:
        """
        Разбивает всю книгу на чанки.

        Args:
            book_id:       ID книги.
            content:       Распарсенное содержимое (FB2Content).
            book_metadata: Дополнительные метаданные (PostgreSQL/FB2 описание).

        Returns:
            Список TextChunk (поле total_chunks заполнено у всех).
        """
        meta = book_metadata or {}
        # Докладываем метаданные из FB2, если не переданы явно
        for key, value in content.metadata.items():
            meta.setdefault(key, value)

        all_chunks: List[TextChunk] = []
        global_idx = 0

        for chapter in content.chapters:
            chapter_chunks = self._chunk_chapter(
                book_id=book_id,
                chapter=chapter,
                start_chunk_index=global_idx,
                book_metadata=meta,
            )
            all_chunks.extend(chapter_chunks)
            global_idx += len(chapter_chunks)

        if all_chunks:
            # Отбрасываем мусорные микрочанки
            before = len(all_chunks)
            all_chunks = [c for c in all_chunks if c.token_count >= self.min_chunk_tokens]
            dropped = before - len(all_chunks)
            if dropped:
                logger.debug(f"book_id={book_id}: dropped {dropped} micro-chunks (< {self.min_chunk_tokens} tok)")

            # Перенумеровать chunk_index после фильтрации
            for i, chunk in enumerate(all_chunks):
                chunk.chunk_index = i

            total = len(all_chunks)
            for chunk in all_chunks:
                chunk.total_chunks = total

            sizes = [c.token_count for c in all_chunks]
            logger.info(
                f"book_id={book_id} → {total} chunks | "
                f"avg={sum(sizes)//total} | min={min(sizes)} | max={max(sizes)} tokens"
            )
            # Жёсткая проверка: нет ли чанков сверх лимита модели
            over_limit = [c for c in all_chunks if c.token_count > self.hard_max_tokens]
            if over_limit:
                logger.warning(f"{len(over_limit)} chunks exceed hard_max={self.hard_max_tokens} tokens")

        return all_chunks

    # ─── Внутренние методы ─────────────────────────────────────────────────────

    def _chunk_chapter(
        self,
        book_id: int,
        chapter: BookChapter,
        start_chunk_index: int,
        book_metadata: dict,
    ) -> List[TextChunk]:
        """Разбивает одну главу на чанки."""
        paragraphs = [p.strip() for p in chapter.text.split("\n\n") if p.strip()]
        chunks: List[TextChunk] = []

        current_paras: List[str] = []
        current_tokens: int = 0
        pos_in_chapter: int = 0

        def flush(paras: List[str], tokens: int) -> Optional[str]:
            """
            Сохраняет накопленные параграфы как новый чанк.
            Возвращает текст чанка (для извлечения overlap-суффикса).
            """
            nonlocal pos_in_chapter
            if not paras:
                return None
            text = "\n\n".join(paras)
            actual_tokens = self._count_tokens(text)
            if actual_tokens > self.hard_max_tokens:
                text, actual_tokens = self._hard_truncate(text)
            chunks.append(self._make_chunk(
                book_id=book_id,
                chapter=chapter,
                text=text,
                chunk_index=start_chunk_index + len(chunks),
                position_in_chapter=pos_in_chapter,
                token_count=actual_tokens,
                book_metadata=book_metadata,
            ))
            pos_in_chapter += 1
            return text

        for para in paragraphs:
            para_tokens = self._count_tokens(para)

            if para_tokens > self.target_size:
                # Длинный абзац: сначала сбрасываем накопленное …
                prev_text = flush(current_paras, current_tokens)
                current_paras, current_tokens = [], 0

                # … потом разбиваем абзац по предложениям
                for split_chunk_text in self._split_by_sentences(para):
                    split_tokens = self._count_tokens(split_chunk_text)
                    chunks.append(self._make_chunk(
                        book_id=book_id,
                        chapter=chapter,
                        text=split_chunk_text,
                        chunk_index=start_chunk_index + len(chunks),
                        position_in_chapter=pos_in_chapter,
                        token_count=split_tokens,
                        book_metadata=book_metadata,
                    ))
                    pos_in_chapter += 1

            elif current_tokens + para_tokens <= self.target_size:
                current_paras.append(para)
                current_tokens += para_tokens

            else:
                # Не помещается — сохраняем текущий чанк
                prev_text = flush(current_paras, current_tokens)

                # Overlap: пытаемся взять последний абзац предыдущего чанка
                overlap_prefix = ""
                overlap_tok = 0
                if current_paras:
                    last_para = current_paras[-1]
                    last_para_tok = self._count_tokens(last_para)
                    if last_para_tok + para_tokens <= self.target_size:
                        # Абзац помещается целиком — идеальный overlap
                        overlap_prefix = last_para
                        overlap_tok = last_para_tok
                    else:
                        # Абзац слишком большой — берём токенный суффикс
                        # (последние overlap_tokens токенов предыдущего чанка)
                        overlap_prefix = self._token_suffix(prev_text, self.overlap_tokens)
                        overlap_tok = self._count_tokens(overlap_prefix)

                if overlap_prefix and overlap_tok + para_tokens <= self.target_size:
                    current_paras = [overlap_prefix, para]
                    current_tokens = overlap_tok + para_tokens
                else:
                    current_paras = [para]
                    current_tokens = para_tokens

        flush(current_paras, current_tokens)
        return chunks

    def _split_by_sentences(self, paragraph: str) -> List[str]:
        """Разбивает длинный абзац на чанки по предложениям."""
        sentences = re.split(r"(?<=[.!?…])\s+", paragraph)
        result: List[str] = []
        current: List[str] = []
        current_tokens = 0

        for sent in sentences:
            sent_tokens = self._count_tokens(sent)

            # Одно предложение само по себе длиннее лимита — жёстко усекаем
            if sent_tokens > self.hard_max_tokens:
                if current:
                    result.append(" ".join(current))
                    current, current_tokens = [], 0
                truncated, _ = self._hard_truncate(sent)
                result.append(truncated)
                continue

            if current_tokens + sent_tokens <= self.target_size:
                current.append(sent)
                current_tokens += sent_tokens
            else:
                if current:
                    result.append(" ".join(current))
                current = [sent]
                current_tokens = sent_tokens

        if current:
            result.append(" ".join(current))

        return result if result else [paragraph]

    def _hard_truncate(self, text: str) -> tuple[str, int]:
        """
        Жёстко усекает текст до hard_max_tokens.
        Возвращает (усечённый текст, количество токенов).
        """
        token_ids = self._tokenizer.encode(text, add_special_tokens=False)
        if len(token_ids) <= self.hard_max_tokens:
            return text, len(token_ids)
        truncated_ids = token_ids[:self.hard_max_tokens]
        truncated_text = self._tokenizer.decode(truncated_ids, skip_special_tokens=True)
        logger.debug(f"Hard-truncated chunk from {len(token_ids)} to {self.hard_max_tokens} tokens")
        return truncated_text, self.hard_max_tokens

    def _token_suffix(self, text: str, n_tokens: int) -> str:
        """
        Берёт последние n_tokens токенов из текста.
        Используется для overlap когда последний абзац слишком большой.
        """
        token_ids = self._tokenizer.encode(text, add_special_tokens=False)
        if len(token_ids) <= n_tokens:
            return text
        suffix_ids = token_ids[-n_tokens:]
        return self._tokenizer.decode(suffix_ids, skip_special_tokens=True)

    def _count_tokens(self, text: str) -> int:
        """Быстрый подсчёт токенов через токенизатор без специальных символов."""
        return len(self._tokenizer.encode(text, add_special_tokens=False))

    @staticmethod
    def _make_chunk(
        book_id: int,
        chapter: BookChapter,
        text: str,
        chunk_index: int,
        position_in_chapter: int,
        token_count: int,
        book_metadata: dict,
    ) -> TextChunk:
        return TextChunk(
            text=text,
            token_count=token_count,
            book_id=book_id,
            chunk_index=chunk_index,
            total_chunks=-1,                    # Заполняется в chunk_book()
            chapter_title=chapter.title,
            chapter_index=chapter.chapter_index,
            position_in_chapter=position_in_chapter,
            book_metadata=book_metadata,
        )
