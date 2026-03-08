"""
Tasks API — запуск и отслеживание фоновых задач векторизации.

Эндпоинты:
  POST /tasks/vectorize  — запускает векторизацию книги в фоне
  GET  /tasks/{task_id}  — возвращает статус и прогресс задачи
"""

import asyncio
from typing import Optional

from fastapi import APIRouter, Depends, HTTPException
from pydantic import BaseModel

from app.dependencies import (
    get_recommendation_engine,
    get_rag_indexing_service,
    get_task_registry,
    get_qdrant_service,
)
from app.services.recommendations.recommendation_engine import RecommendationEngine
from app.services.rag.indexing_service import RAGIndexingService
from app.services.shared.qdrant_service import QdrantService
from app.utils.task_registry import TaskRegistry, TaskStatus

router = APIRouter(prefix="/tasks", tags=["Tasks"])


# ─── Модели ───────────────────────────────────────────────────────────────────

class VectorizeRequest(BaseModel):
    book_id: int
    mode:    str = "both"   # "recommendations" | "rag" | "both"
    force:   bool = False   # перезаписать существующие данные


class TaskStatusResponse(BaseModel):
    task_id:  str
    status:   str
    progress: int
    message:  str
    result:   Optional[dict] = None
    error:    Optional[str]  = None


class VectorizationStatusResponse(BaseModel):
    book_id:              int
    recommendations_ok:   bool   # книга есть в books_recommendations
    rag_ok:               bool   # книга есть в books_rag_chunks
    rag_chunks_count:     int    # кол-во чанков в RAG


# ─── Эндпоинты ────────────────────────────────────────────────────────────────

@router.post("/vectorize", response_model=TaskStatusResponse)
async def start_vectorization(
    request: VectorizeRequest,
    engine:    RecommendationEngine = Depends(get_recommendation_engine),
    indexing:  RAGIndexingService   = Depends(get_rag_indexing_service),
    registry:  TaskRegistry         = Depends(get_task_registry),
):
    """
    Запускает векторизацию книги в фоне и сразу возвращает task_id.

    - **mode=recommendations** — генерирует эмбеддинг метаданных (RoSBERTa) → Qdrant books_recommendations
    - **mode=rag** — парсит FB2, нарезает на чанки, индексирует (USER-bge-m3) → Qdrant books_rag_chunks
    - **mode=both** — оба шага последовательно
    - **force=true** — удалить существующие данные и переиндексировать
    """
    if request.mode not in ("recommendations", "rag", "both"):
        raise HTTPException(
            status_code=422,
            detail="mode должен быть 'recommendations', 'rag' или 'both'"
        )

    task = registry.create()

    asyncio.create_task(
        _run_vectorization(
            task_id  = task.task_id,
            book_id  = request.book_id,
            mode     = request.mode,
            force    = request.force,
            engine   = engine,
            indexing = indexing,
            registry = registry,
        )
    )

    return TaskStatusResponse(
        task_id  = task.task_id,
        status   = task.status,
        progress = task.progress,
        message  = "Задача поставлена в очередь",
    )


@router.get("/vectorize/status/{book_id}", response_model=VectorizationStatusResponse)
async def get_book_vectorization_status(
    book_id: int,
    qdrant: QdrantService = Depends(get_qdrant_service),
):
    """
    Проверяет, векторизована ли книга в Qdrant-коллекциях.

    - **recommendations_ok** — эмбеддинг рекомендаций присутствует
    - **rag_ok** — чанки для RAG проиндексированы
    - **rag_chunks_count** — количество RAG-чанков книги
    """
    from qdrant_client.models import Filter, FieldCondition, MatchValue

    # Проверяем recommendations (point ID == book_id)
    try:
        rec_points = qdrant.client.retrieve(
            collection_name=qdrant.collection_recommendations,
            ids=[book_id],
            with_payload=False,
            with_vectors=False,
        )
        recommendations_ok = len(rec_points) > 0
    except Exception:
        recommendations_ok = False

    # Проверяем RAG (поле payload book_id == book_id)
    try:
        rag_count = qdrant.client.count(
            collection_name=qdrant.collection_rag,
            count_filter=Filter(
                must=[FieldCondition(key="book_id", match=MatchValue(value=book_id))]
            ),
            exact=True,
        )
        rag_chunks_count = rag_count.count
        rag_ok = rag_chunks_count > 0
    except Exception:
        rag_ok = False
        rag_chunks_count = 0

    return VectorizationStatusResponse(
        book_id=book_id,
        recommendations_ok=recommendations_ok,
        rag_ok=rag_ok,
        rag_chunks_count=rag_chunks_count,
    )


@router.delete("/vectorize/{book_id}")
async def delete_book_from_qdrant(
    book_id: int,
    qdrant: QdrantService = Depends(get_qdrant_service),
):
    """
    Удаляет книгу из ОБЕИХ Qdrant-коллекций.

    Вызывается фронтендом при удалении книги из каталога.
    - Удаляет эмбеддинг рекомендаций (books_recommendations, point id == book_id)
    - Удаляет все RAG-чанки (books_rag_chunks, payload.book_id == book_id)
    """
    from loguru import logger

    rec_deleted = False
    rag_chunks_deleted = 0

    try:
        qdrant.client.delete(
            collection_name=qdrant.collection_recommendations,
            points_selector=[book_id],
        )
        rec_deleted = True
        logger.info(f"Deleted recommendation embedding for book_id={book_id}")
    except Exception as e:
        logger.warning(f"Could not delete recommendation embedding for book_id={book_id}: {e}")

    try:
        from qdrant_client.models import FilterSelector
        rag_chunks_deleted = qdrant.count_book_chunks(book_id)
        qdrant.delete_book_chunks(book_id)
        logger.info(f"Deleted {rag_chunks_deleted} RAG chunks for book_id={book_id}")
    except Exception as e:
        logger.warning(f"Could not delete RAG chunks for book_id={book_id}: {e}")

    return {
        "book_id":              book_id,
        "recommendations_deleted": rec_deleted,
        "rag_chunks_deleted":   rag_chunks_deleted,
    }


@router.get("/{task_id}", response_model=TaskStatusResponse)
async def get_task_status(
    task_id: str,
    registry: TaskRegistry = Depends(get_task_registry),
):
    """
    Возвращает текущий статус задачи векторизации.

    Используется фронтендом для polling прогресс-бара (раз в 2 сек).
    """
    task = registry.get(task_id)
    if task is None:
        raise HTTPException(status_code=404, detail=f"Задача {task_id} не найдена")

    return TaskStatusResponse(
        task_id  = task.task_id,
        status   = task.status,
        progress = task.progress,
        message  = task.message,
        result   = task.result,
        error    = task.error,
    )


# ─── Фоновая логика ───────────────────────────────────────────────────────────

async def _run_vectorization(
    task_id:  str,
    book_id:  int,
    mode:     str,
    force:    bool,
    engine:   RecommendationEngine,
    indexing: RAGIndexingService,
    registry: TaskRegistry,
) -> None:
    """Выполняет векторизацию в фоне, обновляет статус в registry."""
    from loguru import logger

    registry.update(task_id, status=TaskStatus.RUNNING, progress=5, message="Запуск...")

    try:
        rec_ok  = False
        rag_ok  = False
        rec_chunks = 0
        rag_chunks = 0

        # ── 1. Рекомендационные эмбеддинги ──────────────────────────────────
        if mode in ("recommendations", "both"):
            registry.update(
                task_id, progress=10,
                message="Генерация эмбеддинга рекомендаций (RoSBERTa)..."
            )
            logger.info(f"[Task {task_id}] Generating recommendation embedding for book_id={book_id}")
            await engine.generate_and_store_embedding(book_id, force_update=force)

            rec_ok = True
            registry.update(
                task_id, progress=40 if mode == "both" else 90,
                message="Эмбеддинг рекомендаций сохранён в Qdrant"
            )

        # ── 2. RAG-индексация ────────────────────────────────────────────────
        if mode in ("rag", "both"):
            start_progress = 45 if mode == "both" else 10
            registry.update(
                task_id, progress=start_progress,
                message="Загрузка и парсинг FB2 для RAG..."
            )
            logger.info(f"[Task {task_id}] RAG indexing book_id={book_id}, force={force}")
            result = await indexing.index_book(book_id=book_id, force_reindex=force)
            rag_chunks = result.total_chunks or 0

            if result.status == "error":
                raise RuntimeError(f"RAG indexing error: {result.message}")

            rag_ok = True
            registry.update(
                task_id, progress=95,
                message=f"RAG: проиндексировано {rag_chunks} чанков"
            )

        # ── Итог ─────────────────────────────────────────────────────────────
        summary_parts = []
        if rec_ok:
            summary_parts.append("эмбеддинг рекомендаций сохранён")
        if rag_ok:
            summary_parts.append(f"RAG: {rag_chunks} чанков")

        registry.update(
            task_id,
            status   = TaskStatus.DONE,
            progress = 100,
            message  = "Готово: " + ", ".join(summary_parts),
            result   = {
                "book_id":           book_id,
                "recommendations_ok": rec_ok,
                "rag_ok":            rag_ok,
                "rag_chunks":        rag_chunks,
            },
        )
        logger.info(f"[Task {task_id}] Vectorization complete for book_id={book_id}")

    except Exception as exc:
        logger.error(f"[Task {task_id}] Vectorization failed: {exc}")
        registry.update(
            task_id,
            status   = TaskStatus.ERROR,
            progress = 0,
            message  = f"Ошибка: {exc}",
            error    = str(exc),
        )
