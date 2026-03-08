from pydantic import BaseModel
from typing import List, Dict, Optional, Any


class UserPreferences(BaseModel):
    """User preferences extracted from reading history"""
    user_id: int
    favorite_genres: List[Dict[str, Any]] = []  # [{"genre": "Фантастика", "count": 10}]
    favorite_authors: List[Dict[str, Any]] = []  # [{"author": "Author Name", "count": 5}]
    preferred_languages: List[str] = []
    average_rating_given: Optional[float] = None
    total_books_read: int = 0
    has_history: bool = False
