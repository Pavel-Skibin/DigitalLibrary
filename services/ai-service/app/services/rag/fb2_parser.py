"""
FB2 (FictionBook 2.0) Parser.
Извлекает структуру глав и метаданные из XML-документа FB2.
"""

from __future__ import annotations

import xml.etree.ElementTree as ET
from dataclasses import dataclass, field
from typing import List, Optional
from loguru import logger


# ─── Пространство имён FB2 ───────────────────────────────────────────────────
_NS = "http://www.gribuser.ru/xml/fictionbook/2.0"
_NS_MAP = {"fb": _NS}


# ─── Модели данных ────────────────────────────────────────────────────────────

@dataclass
class BookChapter:
    """Глава книги с извлечённым текстом."""
    title: str
    text: str                   # Параграфы, разделённые \n\n
    chapter_index: int          # Порядковый номер главы в книге (0-based)
    depth: int                  # Уровень вложенности: 1 = глава, 2 = подглава


@dataclass
class FB2Content:
    """Полное содержимое книги после парсинга."""
    chapters: List[BookChapter]
    total_text_length: int
    metadata: dict              # title, authors, genres, language, publisher, year


# ─── Парсер ───────────────────────────────────────────────────────────────────

class FB2Parser:
    """
    Парсер FictionBook 2.0.

    Извлекает:
      * метаданные из <description/title-info>
      * текст и структуру глав из <body/section>

    Поддерживает вложенные <section> (подглавы).
    """

    def parse(self, fb2_xml: str) -> FB2Content:
        """
        Парсит FB2 XML-строку.

        Args:
            fb2_xml: Полный XML-документ в виде строки.

        Returns:
            FB2Content с главами и метаданными.

        Raises:
            ET.ParseError: Если XML невалидный.
        """
        try:
            root = ET.fromstring(fb2_xml)
        except ET.ParseError as exc:
            logger.error(f"FB2 XML parse error: {exc}")
            raise

        metadata = self._extract_metadata(root)
        chapters: List[BookChapter] = []

        body = root.find(f".//{{{_NS}}}body")
        if body is not None:
            # Эпиграфы на уровне <body> (ко всей книге) — добавляем как отдельную главу
            body_epigraphs = self._collect_epigraphs(body)
            if body_epigraphs:
                chapters.append(BookChapter(
                    title="Эпиграф к книге",
                    text="\n\n".join(body_epigraphs),
                    chapter_index=0,
                    depth=0,
                ))
            self._parse_sections(body, chapters, depth=1)
        else:
            logger.warning("FB2: <body> not found — книга пуста?")

        total_length = sum(len(ch.text) for ch in chapters)
        logger.debug(
            f"FB2 parsed: title={metadata.get('title')!r}, "
            f"chapters={len(chapters)}, chars={total_length:,}"
        )

        return FB2Content(
            chapters=chapters,
            total_text_length=total_length,
            metadata=metadata,
        )

    # ─── helpers ──────────────────────────────────────────────────────────────

    def _parse_sections(
        self,
        parent: ET.Element,
        chapters: List[BookChapter],
        depth: int,
    ) -> None:
        """Рекурсивно обходит <section> теги и собирает главы."""
        sections = parent.findall(f"{{{_NS}}}section")

        for idx, section in enumerate(sections):
            # Заголовок секции
            title_elem = section.find(f"{{{_NS}}}title")
            title = self._extract_text(title_elem) if title_elem is not None else f"Раздел {idx + 1}"

            # Эпиграфы секции (могут быть несколько)
            epigraph_parts: List[str] = []
            for epigraph in section.findall(f"{{{_NS}}}epigraph"):
                ep_lines = [
                    self._extract_text(p)
                    for p in epigraph.findall(f"{{{_NS}}}p")
                    if self._extract_text(p)
                ]
                author_elem = epigraph.find(f"{{{_NS}}}text-author")
                author_text = self._extract_text(author_elem)
                ep_body = "\n".join(ep_lines)
                if ep_body:
                    ep_str = f"[Эпиграф] {ep_body}"
                    if author_text:
                        ep_str += f" — {author_text}"
                    epigraph_parts.append(ep_str)

            # Текст из прямых дочерних <p> этой секции (без вложенных <section>)
            paragraphs = [
                self._extract_text(p)
                for p in section.findall(f"{{{_NS}}}p")
                if self._extract_text(p)
            ]

            # Объединяем: сначала эпиграфы, затем текст
            all_parts = epigraph_parts + paragraphs
            text = "\n\n".join(all_parts)

            # Добавляем главу если есть хоть что-то (текст или эпиграф)
            if text.strip():
                chapters.append(
                    BookChapter(
                        title=title.strip() or f"Глава {len(chapters) + 1}",
                        text=text,
                        chapter_index=len(chapters),
                        depth=depth,
                    )
                )

            # Рекурсия для вложенных <section>
            self._parse_sections(section, chapters, depth + 1)

    @staticmethod
    def _extract_text(elem: Optional[ET.Element]) -> str:
        """Возвращает весь текст элемента, включая вложенные теги."""
        if elem is None:
            return ""
        return "".join(elem.itertext()).strip()

    def _collect_epigraphs(self, parent: ET.Element) -> List[str]:
        """
        Собирает только ПРЯМЫЕ дочерние <epigraph> элемента parent
        (не вложенные в <section>), форматируя каждый как строку.
        """
        result: List[str] = []
        for epigraph in parent.findall(f"{{{_NS}}}epigraph"):
            lines = [
                self._extract_text(p)
                for p in epigraph.findall(f"{{{_NS}}}p")
                if self._extract_text(p)
            ]
            author_elem = epigraph.find(f"{{{_NS}}}text-author")
            author_text = self._extract_text(author_elem)
            body = "\n".join(lines)
            if body:
                ep_str = f"[Эпиграф] {body}"
                if author_text:
                    ep_str += f" — {author_text}"
                result.append(ep_str)
        return result

    def _extract_metadata(self, root: ET.Element) -> dict:
        """Извлекает метаданные из <description/title-info>."""
        meta: dict = {}

        title_info = root.find(f".//{{{_NS}}}description/{{{_NS}}}title-info")
        if title_info is None:
            logger.warning("FB2: <title-info> не найден — метаданные пустые")
            return meta

        # Название
        book_title = title_info.find(f"{{{_NS}}}book-title")
        meta["title"] = self._extract_text(book_title)

        # Авторы
        authors: List[str] = []
        for author in title_info.findall(f"{{{_NS}}}author"):
            first = self._extract_text(author.find(f"{{{_NS}}}first-name"))
            middle = self._extract_text(author.find(f"{{{_NS}}}middle-name"))
            last = self._extract_text(author.find(f"{{{_NS}}}last-name"))
            full = " ".join(part for part in [first, middle, last] if part)
            if full:
                authors.append(full)
        meta["authors"] = authors

        # Жанры
        meta["genres"] = [
            self._extract_text(g)
            for g in title_info.findall(f"{{{_NS}}}genre")
            if self._extract_text(g)
        ]

        # Язык книги
        lang = title_info.find(f"{{{_NS}}}lang")
        meta["language"] = self._extract_text(lang) or "ru"

        # Год и издательство (из <publish-info>)
        publish_info = root.find(
            f".//{{{_NS}}}description/{{{_NS}}}publish-info"
        )
        if publish_info is not None:
            year_elem = publish_info.find(f"{{{_NS}}}year")
            try:
                meta["publication_year"] = int(self._extract_text(year_elem))
            except (ValueError, TypeError):
                meta["publication_year"] = None

            publisher_elem = publish_info.find(f"{{{_NS}}}publisher")
            meta["publisher"] = self._extract_text(publisher_elem)
        else:
            meta["publication_year"] = None
            meta["publisher"] = ""

        return meta
