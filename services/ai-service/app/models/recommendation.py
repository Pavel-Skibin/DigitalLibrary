from pydantic import BaseModel, Field
from typing import List, Optional
from .book import BookRecommendation


class RecommendationRequest(BaseModel):
    """Request for personalized recommendations"""
    user_id: int
    limit: int = Field(default=10, ge=1, le=50)
    genre: Optional[str] = None
    language: Optional[str] = None
    exclude_read: bool = True


class RecommendationResponse(BaseModel):
    """Response with recommendations"""
    user_id: int
    recommendations: List[BookRecommendation]
    total: int
    source: str  # "personalized", "cold_start", "popular"
    cached: bool = False
