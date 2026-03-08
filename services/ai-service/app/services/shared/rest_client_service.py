"""
REST Client Service для взаимодействия с другими микросервисами.

AI Service НЕ должен напрямую обращаться к БД других сервисов.
Вместо этого используем их REST API.
"""

import aiohttp
from typing import List, Dict, Optional, Set
from loguru import logger

from app.config import Settings
from app.models.book import BookMetadata
from app.models.user import UserPreferences


class RestClientService:
    """Service for calling REST APIs of other microservices"""
    
    def __init__(self, settings: Settings):
        self.settings = settings
        self.session: Optional[aiohttp.ClientSession] = None
        
        # Service URLs
        self.user_service_url = settings.USER_SERVICE_URL
        self.book_catalog_url = settings.BOOK_CATALOG_SERVICE_URL
        self.comment_rating_url = settings.COMMENT_RATING_SERVICE_URL
        
        # Internal API key for security
        self.internal_api_key = settings.INTERNAL_API_KEY
    
    async def init_session(self):
        """Initialize HTTP session"""
        self.session = aiohttp.ClientSession(
            headers={"X-Internal-API-Key": self.internal_api_key}
        )
        logger.info("REST Client session initialized")
    
    async def close_session(self):
        """Close HTTP session"""
        if self.session:
            await self.session.close()
            logger.info("REST Client session closed")
    
    # ===========================================
    # USER SERVICE APIs
    # ===========================================
    
    async def get_user_viewed_book_ids(self, user_id: int) -> Set[int]:
        """
        Get list of books user has viewed (for exclusion in recommendations).
        
        Endpoint: GET /api/users/me/history/book-ids
        Note: This requires authentication, so we'll use internal endpoint or modify
        """
        try:
            # TODO: Add internal endpoint to User Service
            url = f"{self.user_service_url}/api/internal/users/{user_id}/viewed-books"
            async with self.session.get(url) as response:
                if response.status == 200:
                    book_ids = await response.json()
                    return set(book_ids)
                else:
                    logger.error(f"Failed to get viewed books for user {user_id}: {response.status}")
                    return set()
        except Exception as e:
            logger.error(f"Error calling User Service for viewed books: {e}")
            return set()
    
    async def get_user_history(self, user_id: int) -> List[Dict]:
        """
        Get user's reading history with statistics.
        
        Returns list of:
        {
            "bookId": int,
            "totalReadingTimeSeconds": int,
            "sessionsCount": int,
            "lastReadAt": str,
            "isCompleted": bool
        }
        
        Endpoint: GET /api/internal/users/{userId}/history
        """
        try:
            # TODO: Add internal endpoint to User Service
            url = f"{self.user_service_url}/api/internal/users/{user_id}/history"
            async with self.session.get(url) as response:
                if response.status == 200:
                    data = await response.json()
                    # Extract from pagination if needed
                    if isinstance(data, dict) and 'content' in data:
                        return data['content']
                    return data
                else:
                    logger.error(f"Failed to get history for user {user_id}: {response.status}")
                    return []
        except Exception as e:
            logger.error(f"Error calling User Service for history: {e}")
            return []
    
    async def get_user_favorite_book_ids(self, user_id: int) -> List[int]:
        """
        Get user's favorite books.
        
        Endpoint: GET /api/internal/users/{userId}/favorites
        """
        try:
            # TODO: Add internal endpoint to User Service
            url = f"{self.user_service_url}/api/internal/users/{user_id}/favorites"
            async with self.session.get(url) as response:
                if response.status == 200:
                    book_ids = await response.json()
                    return book_ids
                else:
                    logger.error(f"Failed to get favorites for user {user_id}: {response.status}")
                    return []
        except Exception as e:
            logger.error(f"Error calling User Service for favorites: {e}")
            return []
    
    # ===========================================
    # COMMENT RATING SERVICE APIs
    # ===========================================
    
    async def get_user_ratings(self, user_id: int) -> List[Dict]:
        """
        Get user's ratings for books.
        
        Returns list of:
        {
            "bookId": int,
            "ratingValue": int (1-5)
        }
        
        Endpoint: GET /api/internal/users/{userId}/ratings
        """
        try:
            # TODO: Add internal endpoint to Comment Rating Service
            url = f"{self.comment_rating_url}/api/internal/users/{user_id}/ratings"
            async with self.session.get(url) as response:
                if response.status == 200:
                    ratings = await response.json()
                    return ratings
                else:
                    logger.error(f"Failed to get ratings for user {user_id}: {response.status}")
                    return []
        except Exception as e:
            logger.error(f"Error calling Comment Rating Service for ratings: {e}")
            return []
    
    # ===========================================
    # BOOK CATALOG SERVICE APIs
    # ===========================================
    
    async def get_books_metadata(self, book_ids: List[int]) -> List[BookMetadata]:
        """
        Get metadata for multiple books.
        
        Endpoint: POST /api/internal/books/batch
        """
        if not book_ids:
            return []
        
        try:
            url = f"{self.book_catalog_url}/api/internal/books/batch"
            async with self.session.post(url, json=book_ids) as response:
                if response.status == 200:
                    books_map = await response.json()
                    
                    # Convert to BookMetadata objects
                    books = []
                    for book_id_str, book_data in books_map.items():
                        book = BookMetadata(
                            id=book_data.get('id'),
                            title=book_data.get('title'),
                            description=book_data.get('description'),
                            cover_image_path=book_data.get('coverImagePath'),
                            publication_year=book_data.get('yearPublished'),   # OpenAPI: yearPublished
                            language=book_data.get('language'),
                            age_rating=book_data.get('ageRating'),
                            series_name=book_data.get('seriesName'),
                            series_number=book_data.get('seriesNumber'),
                            average_rating=book_data.get('averageRating'),
                            ratings_count=book_data.get('ratingsCount'),
                            word_count=book_data.get('wordCount'),             # было пропущено!
                            authors=book_data.get('authors', []),
                            genres=book_data.get('genres', []),
                            tags=book_data.get('tags', [])
                        )
                        books.append(book)
                    
                    logger.debug(f"Retrieved metadata for {len(books)} books from Book Catalog Service")
                    return books
                else:
                    logger.error(f"Failed to get books metadata: {response.status}")
                    return []
        except Exception as e:
            logger.error(f"Error calling Book Catalog Service for metadata: {e}")
            return []
    
    async def get_book_metadata(self, book_id: int) -> Optional[BookMetadata]:
        """
        Get metadata for single book.
        
        Endpoint: GET /api/internal/books/{id}
        """
        try:
            url = f"{self.book_catalog_url}/api/internal/books/{book_id}"
            async with self.session.get(url) as response:
                if response.status == 200:
                    book_data = await response.json()
                    
                    book = BookMetadata(
                        id=book_data.get('id'),
                        title=book_data.get('title'),
                        description=book_data.get('description'),
                        cover_image_path=book_data.get('coverImagePath'),
                        publication_year=book_data.get('yearPublished'),       # OpenAPI: yearPublished
                        language=book_data.get('language'),
                        age_rating=book_data.get('ageRating'),
                        series_name=book_data.get('seriesName'),
                        series_number=book_data.get('seriesNumber'),
                        average_rating=book_data.get('averageRating'),
                        ratings_count=book_data.get('ratingsCount'),
                        word_count=book_data.get('wordCount'),
                        authors=book_data.get('authors', []),
                        genres=book_data.get('genres', []),
                        tags=book_data.get('tags', [])
                    )
                    
                    return book
                elif response.status == 404:
                    logger.warning(f"Book {book_id} not found in Book Catalog Service")
                    return None
                else:
                    logger.error(f"Failed to get book {book_id}: {response.status}")
                    return None
        except Exception as e:
            logger.error(f"Error calling Book Catalog Service for book {book_id}: {e}")
            return None

    async def get_book_fb2(self, book_id: int) -> Optional[str]:
        """
        Получает сырой FB2 XML контент книги из book-catalog-service.

        Endpoint: GET /api/books/{id}/fb2

        Returns:
            Строка с XML-содержимым FB2 или None при ошибке.
        """
        try:
            url = f"{self.book_catalog_url}/api/books/{book_id}/fb2"
            async with self.session.get(url) as response:
                if response.status == 200:
                    fb2_xml = await response.text(encoding="utf-8", errors="replace")
                    logger.debug(f"Fetched FB2 for book_id={book_id}: {len(fb2_xml):,} chars")
                    return fb2_xml
                elif response.status == 404:
                    logger.warning(f"FB2 not found for book_id={book_id}")
                    return None
                else:
                    logger.error(f"Failed to fetch FB2 for book_id={book_id}: HTTP {response.status}")
                    return None
        except Exception as exc:
            logger.error(f"Error fetching FB2 for book_id={book_id}: {exc}")
            return None

    async def get_all_book_ids(self, limit: Optional[int] = None) -> List[int]:
        """
        Get all book IDs for initial embedding generation.
        
        Endpoint: GET /api/internal/books/ids?limit=X
        """
        try:
            url = f"{self.book_catalog_url}/api/internal/books/ids"
            params = {}
            if limit:
                params['limit'] = limit
            
            async with self.session.get(url, params=params) as response:
                if response.status == 200:
                    book_ids = await response.json()
                    return book_ids
                else:
                    logger.error(f"Failed to get all book IDs: {response.status}")
                    return []
        except Exception as e:
            logger.error(f"Error calling Book Catalog Service for all book IDs: {e}")
            return []
    
    # ===========================================
    # AGGREGATED DATA FOR RECOMMENDATIONS
    # ===========================================
    
    async def get_user_anchor_books(self, user_id: int, limit: int = 10) -> List[Dict[str, int]]:
        """
        Get user's anchor books (books they loved) for recommendations WITH WEIGHTS.
        
        Weights are configurable in Settings:
        - Rating 5★ = ANCHOR_WEIGHT_RATING_5 (default: 5)
        - Rating 4★ = ANCHOR_WEIGHT_RATING_4 (default: 4)
        - Completed = ANCHOR_WEIGHT_COMPLETED (default: 4)
        - Favorite = ANCHOR_WEIGHT_FAVORITE (default: 5)
        - Long read (>1h) = ANCHOR_WEIGHT_LONG_READ (default: 3)
        
        Returns: [{"book_id": 123, "weight": 17}, ...] sorted by weight desc.
        """
        try:
            # Get ratings with different weights for 4★ and 5★ (configurable)
            ratings = await self.get_user_ratings(user_id)
            high_rated_books = {}
            for r in ratings:
                rating_value = r.get('ratingValue', 0)
                if rating_value == 5:
                    high_rated_books[r['bookId']] = self.settings.ANCHOR_WEIGHT_RATING_5
                elif rating_value == 4:
                    high_rated_books[r['bookId']] = self.settings.ANCHOR_WEIGHT_RATING_4
            
            # Get history
            history = await self.get_user_history(user_id)
            
            completed_books = {
                h['bookId']: self.settings.ANCHOR_WEIGHT_COMPLETED
                for h in history
                if h.get('isCompleted', False)
            }
            
            # Progressive weights based on total reading time (чем дольше читал - тем больше вес)
            reading_time_books = {}
            for h in history:
                book_id = h['bookId']
                total_seconds = h.get('totalReadingTimeSeconds', 0)
                
                # Градация весов по времени чтения
                if total_seconds >= 36000:  # >= 10 часов
                    reading_time_books[book_id] = self.settings.ANCHOR_WEIGHT_READ_LONG
                elif total_seconds >= 18000:  # >= 5 часов
                    reading_time_books[book_id] = self.settings.ANCHOR_WEIGHT_READ_10H
                elif total_seconds >= 7200:  # >= 2 часа
                    reading_time_books[book_id] = self.settings.ANCHOR_WEIGHT_READ_5H
                elif total_seconds >= 3600:  # >= 1 час
                    reading_time_books[book_id] = self.settings.ANCHOR_WEIGHT_READ_2H
                elif total_seconds >= 1800:  # >= 30 минут
                    reading_time_books[book_id] = self.settings.ANCHOR_WEIGHT_READ_1H
            
            # Get favorites
            favorite_ids = await self.get_user_favorite_book_ids(user_id)
            favorite_books = {book_id: self.settings.ANCHOR_WEIGHT_FAVORITE for book_id in favorite_ids}
            
            # Aggregate weights
            book_weights = {}
            for book_dict in [high_rated_books, completed_books, reading_time_books, favorite_books]:
                for book_id, weight in book_dict.items():
                    book_weights[book_id] = book_weights.get(book_id, 0) + weight
            
            # Sort by weight and return top N WITH WEIGHTS
            sorted_books = sorted(book_weights.items(), key=lambda x: x[1], reverse=True)
            anchor_books_with_weights = [
                {"book_id": book_id, "weight": weight}
                for book_id, weight in sorted_books[:limit]
            ]
            
            logger.debug(f"Found {len(anchor_books_with_weights)} anchor books for user_id={user_id} with weights")
            return anchor_books_with_weights
            
        except Exception as e:
            logger.error(f"Failed to get anchor books for user_id={user_id}: {e}")
            return []
    
    async def get_user_preferences(self, user_id: int) -> UserPreferences:
        """
        Analyze user preferences from reading history and ratings.
        
        This requires aggregating data from Book Catalog Service to analyze genres, authors, etc.
        """
        try:
            # Get anchor books (now returns list with weights)
            anchor_books = await self.get_user_anchor_books(user_id, limit=50)
            
            if not anchor_books:
                return UserPreferences(user_id=user_id, has_history=False)
            
            # Extract book IDs from anchor books
            anchor_book_ids = [item["book_id"] for item in anchor_books]
            
            # Get metadata for these books
            books = await self.get_books_metadata(anchor_book_ids)
            
            # Analyze genres
            genre_counts = {}
            for book in books:
                for genre in book.genres:
                    genre_counts[genre] = genre_counts.get(genre, 0) + 1
            
            favorite_genres = [
                {"genre": genre, "count": count}
                for genre, count in sorted(genre_counts.items(), key=lambda x: x[1], reverse=True)[:5]
            ]
            
            # Analyze authors
            author_counts = {}
            for book in books:
                for author in book.authors:
                    author_counts[author] = author_counts.get(author, 0) + 1
            
            favorite_authors = [
                {"author": author, "count": count}
                for author, count in sorted(author_counts.items(), key=lambda x: x[1], reverse=True)[:5]
            ]
            
            # Analyze languages
            languages = list(set(book.language for book in books if book.language))
            
            # Get average rating
            ratings = await self.get_user_ratings(user_id)
            avg_rating = sum(r['ratingValue'] for r in ratings) / len(ratings) if ratings else None
            
            preferences = UserPreferences(
                user_id=user_id,
                favorite_genres=favorite_genres,
                favorite_authors=favorite_authors,
                preferred_languages=languages,
                average_rating_given=avg_rating,
                total_books_read=len(anchor_book_ids),
                has_history=True
            )
            
            return preferences
            
        except Exception as e:
            logger.error(f"Failed to get user preferences for user_id={user_id}: {e}")
            return UserPreferences(user_id=user_id, has_history=False)
    
    async def get_user_read_books(self, user_id: int) -> Set[int]:
        """Get set of book IDs that user has read (for exclusion)"""
        return await self.get_user_viewed_book_ids(user_id)
    
    async def get_popular_books(
        self,
        limit: int = 10,
        genre: Optional[str] = None,
        min_ratings: int = 5
    ) -> List[BookMetadata]:
        """
        Get popular books.
        
        Endpoint: GET /api/books?sort=rating&minRatings=X
        """
        try:
            url = f"{self.book_catalog_url}/api/books"
            params = {
                'size': limit,
                'sort': 'averageRating,desc',
                'minRatings': min_ratings
            }
            
            if genre:
                params['genre'] = genre
            
            async with self.session.get(url, params=params) as response:
                if response.status == 200:
                    data = await response.json()
                    
                    # Parse paginated response
                    books_data = data.get('content', []) if isinstance(data, dict) else data
                    
                    # Convert to BookMetadata
                    books = []
                    for book_data in books_data:
                        book = BookMetadata(
                            id=book_data.get('id'),
                            title=book_data.get('title'),
                            description=book_data.get('description'),
                            cover_image_path=book_data.get('coverImagePath'),
                            publication_year=book_data.get('publicationYear'),
                            language=book_data.get('language'),
                            age_rating=book_data.get('ageRating'),
                            series_name=book_data.get('seriesName'),
                            series_number=book_data.get('seriesNumber'),
                            average_rating=book_data.get('averageRating'),
                            ratings_count=book_data.get('ratingsCount'),
                            word_count=book_data.get('wordCount'),
                            authors=book_data.get('authors', []),
                            genres=book_data.get('genres', []),
                            tags=book_data.get('tags', [])
                        )
                        books.append(book)
                    
                    return books
                else:
                    logger.error(f"Failed to get popular books: {response.status}")
                    return []
        except Exception as e:
            logger.error(f"Error calling Book Catalog Service for popular books: {e}")
            return []
    
    async def get_new_releases(
        self,
        limit: int = 10,
        min_year: Optional[int] = None
    ) -> List[BookMetadata]:
        """
        Get new releases.
        
        Endpoint: GET /api/books?sort=createdAt,desc
        """
        try:
            url = f"{self.book_catalog_url}/api/books"
            params = {
                'size': limit,
                'sort': 'createdAt,desc'
            }
            
            if min_year:
                params['minYear'] = min_year
            
            async with self.session.get(url, params=params) as response:
                if response.status == 200:
                    data = await response.json()
                    books_data = data.get('content', []) if isinstance(data, dict) else data
                    
                    books = []
                    for book_data in books_data:
                        book = BookMetadata(
                            id=book_data.get('id'),
                            title=book_data.get('title'),
                            description=book_data.get('description'),
                            cover_image_path=book_data.get('coverImagePath'),
                            publication_year=book_data.get('publicationYear'),
                            language=book_data.get('language'),
                            age_rating=book_data.get('ageRating'),
                            series_name=book_data.get('seriesName'),
                            series_number=book_data.get('seriesNumber'),
                            average_rating=book_data.get('averageRating'),
                            ratings_count=book_data.get('ratingsCount'),
                            word_count=book_data.get('wordCount'),
                            authors=book_data.get('authors', []),
                            genres=book_data.get('genres', []),
                            tags=book_data.get('tags', [])
                        )
                        books.append(book)
                    
                    return books
                else:
                    logger.error(f"Failed to get new releases: {response.status}")
                    return []
        except Exception as e:
            logger.error(f"Error calling Book Catalog Service for new releases: {e}")
            return []
