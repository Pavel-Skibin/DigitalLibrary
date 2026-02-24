from fastapi import APIRouter, Depends, HTTPException, Body
from typing import List, Dict, Any
from pydantic import BaseModel
from loguru import logger

from app.dependencies import get_recommendation_engine
from app.services.recommendations.recommendation_engine import RecommendationEngine

router = APIRouter(prefix="/embeddings", tags=["Embeddings"])


class GenerateEmbeddingRequest(BaseModel):
    book_id: int


class BatchGenerateRequest(BaseModel):
    book_ids: List[int]
    batch_size: int = 32


class EmbeddingResponse(BaseModel):
    book_id: int
    success: bool
    message: str


class BatchEmbeddingResponse(BaseModel):
    total: int
    success: int
    failed: int
    failed_ids: List[int]


@router.post("/generate", response_model=EmbeddingResponse)
async def generate_embedding(
    request: GenerateEmbeddingRequest,
    engine: RecommendationEngine = Depends(get_recommendation_engine)
):
    """
    Генерация эмбеддинга для одной книги
    
    Используется для:
    - Добавления новых книг в систему
    - Обновления эмбеддинга при изменении метаданных книги
    
    Процесс:
    1. Извлекает метаданные книги из PostgreSQL
    2. Создает текстовый профиль (title + description + genres + tags + authors + series)
    3. Генерирует 768-мерный вектор с помощью ru-en-RoSBERTa
    4. Сохраняет в Qdrant с метаданными
    5. Кэширует в Redis
    """
    try:
        success = await engine.generate_and_store_embedding(request.book_id)
        
        if success:
            return EmbeddingResponse(
                book_id=request.book_id,
                success=True,
                message="Embedding generated and stored successfully"
            )
        else:
            raise HTTPException(
                status_code=500,
                detail=f"Failed to generate embedding for book_id={request.book_id}"
            )
        
    except Exception as e:
        logger.error(f"Failed to generate embedding: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/batch-generate", response_model=BatchEmbeddingResponse)
async def batch_generate_embeddings(
    request: BatchGenerateRequest,
    engine: RecommendationEngine = Depends(get_recommendation_engine)
):
    """
    Пакетная генерация эмбеддингов
    
    Эффективна для:
    - Первичной загрузки всех книг в систему
    - Массового обновления эмбеддингов
    - Переиндексации после изменения модели
    
    Обрабатывает книги батчами для оптимизации использования GPU/CPU.
    """
    try:
        if not request.book_ids:
            raise HTTPException(status_code=400, detail="book_ids cannot be empty")
        
        if len(request.book_ids) > 10000:
            raise HTTPException(
                status_code=400,
                detail="Too many book IDs. Please process in smaller batches (<= 10000)"
            )
        
        result = await engine.batch_generate_embeddings(
            book_ids=request.book_ids,
            batch_size=request.batch_size
        )
        
        return BatchEmbeddingResponse(**result)
        
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Failed to batch generate embeddings: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/collection-info")
async def get_collection_info(
    engine: RecommendationEngine = Depends(get_recommendation_engine)
):
    """
    Информация о Qdrant коллекции
    
    Показывает:
    - Количество векторов
    - Статус коллекции
    - Конфигурацию
    """
    try:
        info = await engine.qdrant.get_collection_info()
        return info
        
    except Exception as e:
        logger.error(f"Failed to get collection info: {e}")
        raise HTTPException(status_code=500, detail=str(e))
