from pydantic import BaseModel, Field
from typing import List, Optional
from datetime import datetime


class BookMetadata(BaseModel):
    """Book metadata from PostgreSQL"""
    id: int
    title: str
    description: Optional[str] = None
    cover_image_path: Optional[str] = None
    publication_year: Optional[int] = None
    language: Optional[str] = None
    age_rating: Optional[str] = None
    series_name: Optional[str] = None
    series_number: Optional[int] = None
    average_rating: Optional[float] = None
    ratings_count: Optional[int] = 0
    word_count: Optional[int] = None
    authors: List[str] = Field(default_factory=list)
    genres: List[str] = Field(default_factory=list)
    tags: List[str] = Field(default_factory=list)


class BookProfile(BaseModel):
    """Book profile for embedding generation"""
    book_id: int
    text_profile: str
    metadata: BookMetadata


class BookRecommendation(BaseModel):
    """Recommendation result with score"""
    book_id: int
    title: str
    authors: List[str]
    genres: List[str]
    cover_image_path: Optional[str] = None
    average_rating: Optional[float] = None
    ratings_count: Optional[int] = 0
    views_count: Optional[int] = 0
    similarity_score: float
    final_score: float
    reason: Optional[str] = None