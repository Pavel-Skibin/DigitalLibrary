import asyncpg
from typing import List, Dict, Optional, Set, Any
from loguru import logger

from app.config import Settings
from app.models.book import BookMetadata
from app.models.user import UserPreferences


class PostgresService:
    """Service for interacting with PostgreSQL databases (3 separate DBs)"""
    
    def __init__(self, settings: Settings):
        self.settings = settings
        self.books_pool: Optional[asyncpg.Pool] = None
        self.users_pool: Optional[asyncpg.Pool] = None
        self.comments_pool: Optional[asyncpg.Pool] = None
    
    async def connect(self):
        """Create connection pools for all 3 databases"""
        try:
            # Books DB (Book Catalog Service)
            self.books_pool = await asyncpg.create_pool(
                host=self.settings.BOOKS_DB_HOST,
                port=self.settings.BOOKS_DB_PORT,
                database=self.settings.BOOKS_DB_NAME,
                user=self.settings.BOOKS_DB_USER,
                password=self.settings.BOOKS_DB_PASSWORD,
                min_size=3,
                max_size=10
            )
            logger.info(f"Connected to Books DB at {self.settings.BOOKS_DB_HOST}:{self.settings.BOOKS_DB_PORT}/{self.settings.BOOKS_DB_NAME}")
            
            # Users DB (User Service)
            self.users_pool = await asyncpg.create_pool(
                host=self.settings.USERS_DB_HOST,
                port=self.settings.USERS_DB_PORT,
                database=self.settings.USERS_DB_NAME,
                user=self.settings.USERS_DB_USER,
                password=self.settings.USERS_DB_PASSWORD,
                min_size=3,
                max_size=10
            )
            logger.info(f"Connected to Users DB at {self.settings.USERS_DB_HOST}:{self.settings.USERS_DB_PORT}/{self.settings.USERS_DB_NAME}")
            
            # Comments/Ratings DB (Comment Rating Service)
            self.comments_pool = await asyncpg.create_pool(
                host=self.settings.COMMENTS_DB_HOST,
                port=self.settings.COMMENTS_DB_PORT,
                database=self.settings.COMMENTS_DB_NAME,
                user=self.settings.COMMENTS_DB_USER,
                password=self.settings.COMMENTS_DB_PASSWORD,
                min_size=2,
                max_size=8
            )
            logger.info(f"Connected to Comments DB at {self.settings.COMMENTS_DB_HOST}:{self.settings.COMMENTS_DB_PORT}/{self.settings.COMMENTS_DB_NAME}")
            
        except Exception as e:
            logger.error(f"Failed to connect to PostgreSQL databases: {e}")
            raise
    
    async def disconnect(self):
        """Close all connection pools"""
        if self.books_pool:
            await self.books_pool.close()
        if self.users_pool:
            await self.users_pool.close()
        if self.comments_pool:
            await self.comments_pool.close()
        logger.info("Disconnected from all PostgreSQL databases")
    
    async def get_user_anchor_books(self, user_id: int, limit: int = 10) -> List[int]:
        """
        Get user's anchor books (books they loved) for recommendations.
        
        Priority:
        1. High ratings (4-5 stars) - weight 5
        2. Completed books - weight 4
        3. Favorites - weight 5
        4. Long reading time (>1 hour) - weight 3
        """
        query = """
        WITH anchor_books AS (
            -- High ratings (4-5 stars)
            SELECT book_id, 5 as weight, created_at as timestamp
            FROM ratings 
            WHERE user_id = $1 AND rating_value >= 4
            
            UNION ALL
            
            -- Completed books
            SELECT book_id, 4 as weight, last_read_at as timestamp
            FROM user_book_views 
            WHERE user_id = $1 AND is_completed = TRUE
            
            UNION ALL
            
            -- Favorites
            SELECT book_id, 5 as weight, added_at as timestamp
            FROM user_book_favorites 
            WHERE user_id = $1
            
            UNION ALL
            
            -- Long reading time (>1 hour = 3600 seconds)
            SELECT book_id, 3 as weight, last_read_at as timestamp
            FROM user_book_views 
            WHERE user_id = $1 AND total_reading_time_seconds > 3600
        )
        SELECT book_id
        FROM anchor_books
        GROUP BY book_id
        ORDER BY SUM(weight) DESC, MAX(timestamp) DESC
        LIMIT $2
        """
        
        try:
            async with self.pool.acquire() as conn:
                rows = await conn.fetch(query, user_id, limit)
                book_ids = [row['book_id'] for row in rows]
                logger.debug(f"Found {len(book_ids)} anchor books for user_id={user_id}")
                return book_ids
        except Exception as e:
            logger.error(f"Failed to get anchor books for user_id={user_id}: {e}")
            return []
    
    async def get_books_metadata(self, book_ids: List[int]) -> List[BookMetadata]:
        """Get metadata for multiple books"""
        if not book_ids:
            return []
        
        query = """
        SELECT 
            b.id,
            b.title,
            b.description,
            b.cover_image_path,
            b.publication_year,
            b.language,
            b.age_rating,
            b.series_name,
            b.series_number,
            b.average_rating,
            b.ratings_count,
            COALESCE(array_agg(DISTINCT g.name) FILTER (WHERE g.name IS NOT NULL), ARRAY[]::text[]) as genres,
            COALESCE(array_agg(DISTINCT CONCAT(a.first_name, ' ', a.last_name)) FILTER (WHERE a.first_name IS NOT NULL), ARRAY[]::text[]) as authors,
            COALESCE(array_agg(DISTINCT t.name) FILTER (WHERE t.name IS NOT NULL), ARRAY[]::text[]) as tags
        FROM books b
        LEFT JOIN book_genres bg ON b.id = bg.book_id
        LEFT JOIN genres g ON bg.genre_id = g.id
        LEFT JOIN book_authors ba ON b.id = ba.book_id
        LEFT JOIN authors a ON ba.author_id = a.id
        LEFT JOIN book_tags bt ON b.id = bt.book_id
        LEFT JOIN tags t ON bt.tag_id = t.id
        WHERE b.id = ANY($1::int[])
        GROUP BY b.id
        """
        
        try:
            async with self.pool.acquire() as conn:
                rows = await conn.fetch(query, book_ids)
                
                books = []
                for row in rows:
                    book = BookMetadata(
                        id=row['id'],
                        title=row['title'],
                        description=row['description'],
                        cover_image_path=row['cover_image_path'],
                        publication_year=row['publication_year'],
                        language=row['language'],
                        age_rating=row['age_rating'],
                        series_name=row['series_name'],
                        series_number=row['series_number'],
                        average_rating=float(row['average_rating']) if row['average_rating'] else None,
                        ratings_count=row['ratings_count'],
                        authors=list(row['authors']) if row['authors'] else [],
                        genres=list(row['genres']) if row['genres'] else [],
                        tags=list(row['tags']) if row['tags'] else []
                    )
                    books.append(book)
                
                logger.debug(f"Retrieved metadata for {len(books)} books")
                return books
                
        except Exception as e:
            logger.error(f"Failed to get books metadata: {e}")
            return []
    
    async def get_user_read_books(self, user_id: int) -> Set[int]:
        """Get set of book IDs that user has read (for exclusion)"""
        query = """
        SELECT DISTINCT book_id 
        FROM user_book_views 
        WHERE user_id = $1
        """
        
        try:
            async with self.pool.acquire() as conn:
                rows = await conn.fetch(query, user_id)
                book_ids = {row['book_id'] for row in rows}
                logger.debug(f"User {user_id} has read {len(book_ids)} books")
                return book_ids
        except Exception as e:
            logger.error(f"Failed to get read books for user_id={user_id}: {e}")
            return set()
    
    async def get_user_preferences(self, user_id: int) -> UserPreferences:
        """Analyze user preferences from reading history"""
        
        # Get favorite genres
        genres_query = """
        SELECT g.name as genre, COUNT(*) as count
        FROM user_book_views ubv
        JOIN book_genres bg ON ubv.book_id = bg.book_id
        JOIN genres g ON bg.genre_id = g.id
        WHERE ubv.user_id = $1 
          AND ubv.total_reading_time_seconds > 1800  -- Read for >30 minutes
        GROUP BY g.name
        ORDER BY count DESC
        LIMIT 5
        """
        
        # Get favorite authors
        authors_query = """
        SELECT CONCAT(a.first_name, ' ', a.last_name) as author, COUNT(*) as count
        FROM user_book_views ubv
        JOIN book_authors ba ON ubv.book_id = ba.book_id
        JOIN authors a ON ba.author_id = a.id
        WHERE ubv.user_id = $1 
          AND ubv.total_reading_time_seconds > 1800
        GROUP BY a.id, a.first_name, a.last_name
        ORDER BY count DESC
        LIMIT 5
        """
        
        # Get average rating and total books
        stats_query = """
        SELECT 
            COUNT(DISTINCT ubv.book_id) as total_books,
            AVG(r.rating_value) as avg_rating
        FROM user_book_views ubv
        LEFT JOIN ratings r ON ubv.user_id = r.user_id AND ubv.book_id = r.book_id
        WHERE ubv.user_id = $1
        """
        
        # Get preferred languages
        languages_query = """
        SELECT DISTINCT b.language
        FROM user_book_views ubv
        JOIN books b ON ubv.book_id = b.id
        WHERE ubv.user_id = $1 AND b.language IS NOT NULL
        """
        
        try:
            async with self.pool.acquire() as conn:
                genres_rows = await conn.fetch(genres_query, user_id)
                authors_rows = await conn.fetch(authors_query, user_id)
                stats_row = await conn.fetchrow(stats_query, user_id)
                languages_rows = await conn.fetch(languages_query, user_id)
                
                preferences = UserPreferences(
                    user_id=user_id,
                    favorite_genres=[{"genre": row['genre'], "count": row['count']} for row in genres_rows],
                    favorite_authors=[{"author": row['author'], "count": row['count']} for row in authors_rows],
                    preferred_languages=[row['language'] for row in languages_rows],
                    average_rating_given=float(stats_row['avg_rating']) if stats_row and stats_row['avg_rating'] else None,
                    total_books_read=stats_row['total_books'] if stats_row else 0,
                    has_history=stats_row['total_books'] > 0 if stats_row else False
                )
                
                return preferences
                
        except Exception as e:
            logger.error(f"Failed to get user preferences for user_id={user_id}: {e}")
            return UserPreferences(user_id=user_id, has_history=False)
    
    async def get_all_book_ids(self, limit: Optional[int] = None) -> List[int]:
        """Get all book IDs (for initial embedding generation)"""
        query = "SELECT id FROM books ORDER BY id"
        if limit:
            query += f" LIMIT {limit}"
        
        try:
            async with self.pool.acquire() as conn:
                rows = await conn.fetch(query)
                return [row['id'] for row in rows]
        except Exception as e:
            logger.error(f"Failed to get all book IDs: {e}")
            return []
    
    async def get_popular_books(
        self,
        limit: int = 10,
        genre: Optional[str] = None,
        min_ratings: int = 5
    ) -> List[BookMetadata]:
        """Get popular books by rating and popularity"""
        
        query = """
        SELECT 
            b.id,
            b.title,
            b.description,
            b.cover_image_path,
            b.publication_year,
            b.language,
            b.age_rating,
            b.series_name,
            b.series_number,
            b.average_rating,
            b.ratings_count,
            COALESCE(array_agg(DISTINCT g.name) FILTER (WHERE g.name IS NOT NULL), ARRAY[]::text[]) as genres,
            COALESCE(array_agg(DISTINCT CONCAT(a.first_name, ' ', a.last_name)) FILTER (WHERE a.first_name IS NOT NULL), ARRAY[]::text[]) as authors,
            COALESCE(array_agg(DISTINCT t.name) FILTER (WHERE t.name IS NOT NULL), ARRAY[]::text[]) as tags
        FROM books b
        LEFT JOIN book_genres bg ON b.id = bg.book_id
        LEFT JOIN genres g ON bg.genre_id = g.id
        LEFT JOIN book_authors ba ON b.id = ba.book_id
        LEFT JOIN authors a ON ba.author_id = a.id
        LEFT JOIN book_tags bt ON b.id = bt.book_id
        LEFT JOIN tags t ON bt.tag_id = t.id
        WHERE b.ratings_count >= $1
        """
        
        params = [min_ratings]
        
        if genre:
            query += " AND EXISTS (SELECT 1 FROM book_genres bg2 JOIN genres g2 ON bg2.genre_id = g2.id WHERE bg2.book_id = b.id AND g2.name = $2)"
            params.append(genre)
        
        query += """
        GROUP BY b.id
        ORDER BY (b.average_rating * LN(b.ratings_count + 1)) DESC
        LIMIT $""" + str(len(params) + 1)
        params.append(limit)
        
        try:
            async with self.pool.acquire() as conn:
                rows = await conn.fetch(query, *params)
                
                books = []
                for row in rows:
                    book = BookMetadata(
                        id=row['id'],
                        title=row['title'],
                        description=row['description'],
                        cover_image_path=row['cover_image_path'],
                        publication_year=row['publication_year'],
                        language=row['language'],
                        age_rating=row['age_rating'],
                        series_name=row['series_name'],
                        series_number=row['series_number'],
                        average_rating=float(row['average_rating']) if row['average_rating'] else None,
                        ratings_count=row['ratings_count'],
                        authors=list(row['authors']) if row['authors'] else [],
                        genres=list(row['genres']) if row['genres'] else [],
                        tags=list(row['tags']) if row['tags'] else []
                    )
                    books.append(book)
                
                return books
                
        except Exception as e:
            logger.error(f"Failed to get popular books: {e}")
            return []
    
    async def get_new_releases(self, limit: int = 10, min_year: int = 2024) -> List[BookMetadata]:
        """Get new book releases"""
        query = """
        SELECT 
            b.id,
            b.title,
            b.description,
            b.cover_image_path,
            b.publication_year,
            b.language,
            b.age_rating,
            b.series_name,
            b.series_number,
            b.average_rating,
            b.ratings_count,
            COALESCE(array_agg(DISTINCT g.name) FILTER (WHERE g.name IS NOT NULL), ARRAY[]::text[]) as genres,
            COALESCE(array_agg(DISTINCT CONCAT(a.first_name, ' ', a.last_name)) FILTER (WHERE a.first_name IS NOT NULL), ARRAY[]::text[]) as authors,
            COALESCE(array_agg(DISTINCT t.name) FILTER (WHERE t.name IS NOT NULL), ARRAY[]::text[]) as tags
        FROM books b
        LEFT JOIN book_genres bg ON b.id = bg.book_id
        LEFT JOIN genres g ON bg.genre_id = g.id
        LEFT JOIN book_authors ba ON b.id = ba.book_id
        LEFT JOIN authors a ON ba.author_id = a.id
        LEFT JOIN book_tags bt ON b.id = bt.book_id
        LEFT JOIN tags t ON bt.tag_id = t.id
        WHERE b.publication_year >= $1
        GROUP BY b.id
        ORDER BY b.created_at DESC
        LIMIT $2
        """
        
        try:
            async with self.pool.acquire() as conn:
                rows = await conn.fetch(query, min_year, limit)
                
                books = []
                for row in rows:
                    book = BookMetadata(
                        id=row['id'],
                        title=row['title'],
                        description=row['description'],
                        cover_image_path=row['cover_image_path'],
                        publication_year=row['publication_year'],
                        language=row['language'],
                        age_rating=row['age_rating'],
                        series_name=row['series_name'],
                        series_number=row['series_number'],
                        average_rating=float(row['average_rating']) if row['average_rating'] else None,
                        ratings_count=row['ratings_count'],
                        authors=list(row['authors']) if row['authors'] else [],
                        genres=list(row['genres']) if row['genres'] else [],
                        tags=list(row['tags']) if row['tags'] else []
                    )
                    books.append(book)
                
                return books
                
        except Exception as e:
            logger.error(f"Failed to get new releases: {e}")
            return []
