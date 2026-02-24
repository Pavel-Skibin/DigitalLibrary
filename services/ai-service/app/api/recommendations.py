from fastapi import APIRouter, Depends, HTTPException, Query
from typing import List, Optional
from loguru import logger

from app.dependencies import get_recommendation_engine
from app.services.recommendations.recommendation_engine import RecommendationEngine
from app.models.book import BookRecommendation
from app.models.recommendation import RecommendationRequest, RecommendationResponse

router = APIRouter(prefix="/recommendations", tags=["Recommendations"])


@router.get("", response_model=RecommendationResponse)
@router.get("/", response_model=RecommendationResponse)
async def get_recommendations(
    user_id: int = Query(..., description="ID пользователя"),
    limit: int = Query(10, ge=1, le=50, description="Количество рекомендаций"),
    language: Optional[str] = Query(None, description="Фильтр по языку (ru, en)"),
    exclude_read: bool = Query(True, description="Исключить прочитанные книги"),
    engine: RecommendationEngine = Depends(get_recommendation_engine)
):
    """
    Основной эндпоинт для получения рекомендаций
    Алиас для /for-me
    """
    return await get_personalized_recommendations(user_id, limit, language, exclude_read, engine)


@router.get("/for-me", response_model=RecommendationResponse)
async def get_personalized_recommendations(
    user_id: int = Query(..., description="ID пользователя"),
    limit: int = Query(10, ge=1, le=50, description="Количество рекомендаций"),
    language: Optional[str] = Query(None, description="Фильтр по языку (ru, en)"),
    exclude_read: bool = Query(True, description="Исключить прочитанные книги"),
    engine: RecommendationEngine = Depends(get_recommendation_engine)
):
    """
    Персональные рекомендации "Специально для вас"
    
    Алгоритм:
    - Анализирует книги, которые пользователь оценил высоко, прочитал полностью или добавил в избранное
    - Находит похожие книги через векторный поиск
    - Применяет гибридное ранжирование (схожесть + рейтинг)
    - Обеспечивает разнообразие через MMR
    """
    try:
        recommendations = await engine.get_personalized_recommendations(
            user_id=user_id,
            limit=limit,
            language=language,
            exclude_read=exclude_read
        )
        
        # Determine source
        source = "personalized" if recommendations else "empty"
        
        return RecommendationResponse(
            user_id=user_id,
            recommendations=recommendations,
            total=len(recommendations),
            source=source,
            cached=False
        )
        
    except Exception as e:
        logger.error(f"Failed to get personalized recommendations: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/similar/{book_id}", response_model=List[BookRecommendation])
async def get_similar_books(
    book_id: int,
    limit: int = Query(10, ge=1, le=50, description="Количество похожих книг"),
    engine: RecommendationEngine = Depends(get_recommendation_engine)
):
    """
    Книги, похожие на конкретную книгу
    
    Использует векторный поиск для нахождения книг с похожим содержанием,
    жанрами, тегами и авторским стилем.
    """
    try:
        similar_books = await engine.get_similar_to_book(
            book_id=book_id,
            limit=limit
        )
        
        if not similar_books:
            raise HTTPException(
                status_code=404,
                detail=f"No similar books found for book_id={book_id}"
            )
        
        return similar_books
        
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Failed to get similar books: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/popular", response_model=List[BookRecommendation])
async def get_popular_books(
    limit: int = Query(10, ge=1, le=50, description="Количество книг"),
    genre: Optional[str] = Query(None, description="Фильтр по жанру"),
    engine: RecommendationEngine = Depends(get_recommendation_engine)
):
    """
    Популярные книги
    
    Формула популярности: average_rating × log(ratings_count + 1)
    
    Это балансирует качество (высокий рейтинг) и популярность (много оценок).
    """
    try:
        popular_books = await engine.get_popular_books(
            limit=limit,
            genre=genre
        )
        
        return popular_books
        
    except Exception as e:
        logger.error(f"Failed to get popular books: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/new", response_model=List[BookRecommendation])
async def get_new_releases(
    limit: int = Query(10, ge=1, le=50, description="Количество книг"),
    min_year: int = Query(2024, description="Минимальный год издания"),
    engine: RecommendationEngine = Depends(get_recommendation_engine)
):
    """
    Новинки
    
    Книги, добавленные недавно и изданные после указанного года.
    """
    try:
        new_releases = await engine.get_new_releases(
            limit=limit,
            min_year=min_year
        )
        
        return new_releases
        
    except Exception as e:
        logger.error(f"Failed to get new releases: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/invalidate-cache/{user_id}")
async def invalidate_user_cache(
    user_id: int,
    engine: RecommendationEngine = Depends(get_recommendation_engine)
):
    """
    Сбросить кэш рекомендаций пользователя
    
    ⚡ Критична для качества рекомендаций! ⚡
    
    Вызывается когда пользователь:
    - Оценивает книгу (любой рейтинг)
    - Добавляет/удаляет из избранного
    - Завершает чтение книги (progress = 100%)
    - Добавляет книгу в "прочитанные"
    
    Без инвалидации кэша пользователь будет видеть устаревшие рекомендации,
    которые не учитывают его новые предпочтения.
    """
    try:
        await engine.invalidate_cache(user_id)
        logger.info(f"✓ Cache invalidated for user_id={user_id}")
        return {"status": "success", "message": f"Cache invalidated for user_id={user_id}"}
        
    except Exception as e:
        logger.error(f"Failed to invalidate cache: {e}")
        raise HTTPException(status_code=500, detail=str(e))
