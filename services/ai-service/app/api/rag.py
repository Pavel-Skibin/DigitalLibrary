"""
RAG (Retrieval-Augmented Generation) API endpoints.
"""

from fastapi import APIRouter, Depends, HTTPException, Query
from loguru import logger

from app.dependencies import (
    get_rag_indexing_service,
    get_qdrant_service,
    get_rag_service,
)
from app.services.rag.indexing_service import RAGIndexingService
from app.services.rag.rag_service import RAGService
from app.services.shared.qdrant_service import QdrantService
from app.models.rag import (
    RAGSearchRequest,
    RAGSearchResponse,
    RAGQueryRequest,
    RAGQueryResponse,
)

router = APIRouter(prefix="/rag", tags=["RAG"])


# ─── Статус системы ────────────────────────────────────────────────────────────

@router.get("/status")
async def rag_status(qdrant: QdrantService = Depends(get_qdrant_service)):
    """
    Статус RAG-системы: количество чанков в Qdrant.
    """
    try:
        collection_info = qdrant.client.get_collection(qdrant.collection_rag)
        chunks_count = collection_info.points_count
    except Exception:
        chunks_count = None

    return {
        "status": "operational",
        "collection": qdrant.collection_rag,
        "total_chunks": chunks_count,
        "features": {
            "indexing":    "ready",
            "search":      "ready (phase 5)",
            "llm_answers": "ready (phase 6-7)",
        },
    }


# ─── Индексация книги ──────────────────────────────────────────────────────────

@router.post("/index-book/{book_id}")
async def index_book(
    book_id: int,
    force: bool = Query(
        default=False,
        description="Переиндексировать даже если чанки уже есть",
    ),
    indexing_service: RAGIndexingService = Depends(get_rag_indexing_service),
):
    """
    Индексирует книгу в Qdrant RAG-коллекцию.

    Пайплайн: FB2 fetch → parse → chunk → embed (USER-bge-m3) → Qdrant upsert.

    - **book_id**: ID книги в book-catalog-service.
    - **force**:   Если `true` — удалить существующие чанки и переиндексировать.
    """
    logger.info(f"POST /rag/index-book/{book_id}  force={force}")

    try:
        result = await indexing_service.index_book(book_id=book_id, force_reindex=force)
    except Exception as exc:
        logger.error(f"Unexpected error indexing book_id={book_id}: {exc}")
        raise HTTPException(status_code=500, detail=f"Indexing failed: {exc}") from exc

    if result.status == "error":
        raise HTTPException(status_code=422, detail=result.message)

    return {
        "book_id":         result.book_id,
        "status":          result.status,
        "total_chunks":    result.total_chunks,
        "chapters_found":  result.chapters_found,
        "message":         result.message,
    }


@router.get("/book/{book_id}/chunks-count")
async def get_book_chunks_count(
    book_id: int,
    qdrant: QdrantService = Depends(get_qdrant_service),
):
    """
    Возвращает количество чанков книги, хранящихся в Qdrant.
    Полезно для проверки после индексации.
    """
    count = qdrant.count_book_chunks(book_id)
    return {
        "book_id": book_id,
        "chunks_count": count,
        "indexed": count > 0,
    }


@router.delete("/book/{book_id}/chunks")
async def delete_book_chunks(
    book_id: int,
    qdrant: QdrantService = Depends(get_qdrant_service),
):
    """
    Удаляет все чанки книги из Qdrant RAG-коллекции.
    """
    before = qdrant.count_book_chunks(book_id)
    qdrant.delete_book_chunks(book_id)
    return {
        "book_id":  book_id,
        "deleted":  before,
        "message":  f"Deleted {before} chunks for book_id={book_id}",
    }


# ─── Semantic Search (Фаза 5) ─────────────────────────────────────────────────

@router.post("/search", response_model=RAGSearchResponse)
async def search_chunks(
    request: RAGSearchRequest,
    rag_service: RAGService = Depends(get_rag_service),
):
    """
    Семантический поиск по чанкам книг (без LLM).
    Возвращает полные тексты чанков с баллами релевантности.
    """
    logger.info(f"POST /rag/search  query={request.query!r}  top_k={request.top_k}")
    try:
        return rag_service.search(request)
    except Exception as exc:
        logger.error(f"Search error: {exc}")
        raise HTTPException(status_code=500, detail=f"Search failed: {exc}") from exc


# ─── RAG Query (Фазы 6-7) ────────────────────────────────────────────────────

@router.post("/query", response_model=RAGQueryResponse)
async def query_rag(
    request: RAGQueryRequest,
    rag_service: RAGService = Depends(get_rag_service),
):
    """
    RAG Query — задать вопрос по книгам и получить ответ от LLM.

    **Полный пайплайн:**
    1. Hybrid search: находит топ-K релевантных фрагментов текста.
    2. LLM: формирует ответ на основе найденного контекста .
    3. Возвращает ответ + источники.

    **Примеры запросов:**
    - `"Что случилось с Мартином в конце книги?"`
    - `"Как автор описывает море?"`
    - `"Какие книги Руфи упомянуты в тексте?"`

    Если LLM недоступен — возвращает найденные фрагменты без генерации ответа.
    """
    logger.info(f"POST /rag/query  query={request.query!r}  top_k={request.top_k}")
    try:
        return await rag_service.query(request)
    except Exception as exc:
        logger.error(f"RAG query error: {exc}")
        raise HTTPException(status_code=500, detail=f"RAG query failed: {exc}") from exc
