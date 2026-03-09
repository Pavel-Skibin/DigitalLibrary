import redis.asyncio as aioredis
import pickle
import hashlib
import json
from typing import Optional, List, Any
import numpy as np
from loguru import logger

from app.config import Settings


class CacheService:
    """Service for Redis caching"""
    
    def __init__(self, settings: Settings):
        self.settings = settings
        self.redis: Optional[aioredis.Redis] = None
    
    async def connect(self):
        """Connect to Redis"""
        try:
            redis_url = f"redis://{self.settings.REDIS_HOST}:{self.settings.REDIS_PORT}/{self.settings.REDIS_DB}"
            if self.settings.REDIS_PASSWORD:
                redis_url = f"redis://:{self.settings.REDIS_PASSWORD}@{self.settings.REDIS_HOST}:{self.settings.REDIS_PORT}/{self.settings.REDIS_DB}"
            
            self.redis = await aioredis.from_url(
                redis_url,
                encoding="utf-8",
                decode_responses=False  # We'll handle encoding manually
            )
            
            await self.redis.ping()
            logger.info(f"Connected to Redis at {self.settings.REDIS_HOST}:{self.settings.REDIS_PORT}")
            
        except Exception as e:
            logger.error(f"Failed to connect to Redis: {e}")
            raise
    
    async def disconnect(self):
        """Close Redis connection"""
        if self.redis:
            await self.redis.close()
            logger.info("Disconnected from Redis")
    
    # ========== Recommendations Cache ==========
    
    async def get_cached_recommendations(self, user_id: int) -> Optional[List[dict]]:
        """Get cached personalized recommendations for user"""
        key = f"rec:user:{user_id}"
        try:
            data = await self.redis.get(key)
            if data:
                result = pickle.loads(data)
                logger.debug(f"Cache HIT for recommendations user_id={user_id}")
                return result
            logger.debug(f"Cache MISS for recommendations user_id={user_id}")
            return None
        except Exception as e:
            logger.error(f"Failed to get cached recommendations: {e}")
            return None
    
    async def cache_recommendations(self, user_id: int, data: List[dict]):
        """Cache personalized recommendations"""
        key = f"rec:user:{user_id}"
        try:
            serialized = pickle.dumps(data)
            await self.redis.setex(
                key,
                self.settings.CACHE_TTL_RECOMMENDATIONS,
                serialized
            )
            logger.debug(f"Cached recommendations for user_id={user_id}")
        except Exception as e:
            logger.error(f"Failed to cache recommendations: {e}")
    
    async def invalidate_user_recommendations(self, user_id: int):
        """Invalidate user's recommendation cache (called when user rates/favorites)"""
        key = f"rec:user:{user_id}"
        try:
            await self.redis.delete(key)
            logger.info(f"Invalidated recommendations cache for user_id={user_id}")
        except Exception as e:
            logger.error(f"Failed to invalidate user cache: {e}")
    
    # ========== Similar Books Cache ==========
    
    async def get_cached_similar_books(self, book_id: int) -> Optional[List[dict]]:
        """Get cached similar books"""
        key = f"similar:book:{book_id}"
        try:
            data = await self.redis.get(key)
            if data:
                result = pickle.loads(data)
                logger.debug(f"Cache HIT for similar books book_id={book_id}")
                return result
            logger.debug(f"Cache MISS for similar books book_id={book_id}")
            return None
        except Exception as e:
            logger.error(f"Failed to get cached similar books: {e}")
            return None
    
    async def cache_similar_books(self, book_id: int, data: List[dict]):
        """Cache similar books"""
        key = f"similar:book:{book_id}"
        try:
            serialized = pickle.dumps(data)
            await self.redis.setex(
                key,
                self.settings.CACHE_TTL_SIMILAR_BOOKS,
                serialized
            )
            logger.debug(f"Cached similar books for book_id={book_id}")
        except Exception as e:
            logger.error(f"Failed to cache similar books: {e}")
    
    # ========== Embeddings Cache ==========
    
    async def get_cached_embedding(self, text: str) -> Optional[np.ndarray]:
        """Get cached embedding by text hash"""
        text_hash = self._hash_text(text)
        key = f"emb:hash:{text_hash}"
        try:
            data = await self.redis.get(key)
            if data:
                embedding = pickle.loads(data)
                logger.debug(f"Cache HIT for embedding hash={text_hash[:8]}...")
                return embedding
            logger.debug(f"Cache MISS for embedding hash={text_hash[:8]}...")
            return None
        except Exception as e:
            logger.error(f"Failed to get cached embedding: {e}")
            return None
    
    async def cache_embedding(self, text: str, embedding: np.ndarray):
        """Cache embedding by text hash"""
        text_hash = self._hash_text(text)
        key = f"emb:hash:{text_hash}"
        try:
            serialized = pickle.dumps(embedding)
            await self.redis.setex(
                key,
                self.settings.CACHE_TTL_EMBEDDINGS,
                serialized
            )
            logger.debug(f"Cached embedding hash={text_hash[:8]}...")
        except Exception as e:
            logger.error(f"Failed to cache embedding: {e}")
    
    # ========== Popular Books Cache ==========
    
    async def get_cached_popular_books(self, genre: Optional[str] = None) -> Optional[List[dict]]:
        """Get cached popular books"""
        key = f"popular:books:{genre or 'all'}"
        try:
            data = await self.redis.get(key)
            if data:
                result = pickle.loads(data)
                logger.debug(f"Cache HIT for popular books genre={genre}")
                return result
            return None
        except Exception as e:
            logger.error(f"Failed to get cached popular books: {e}")
            return None
    
    async def cache_popular_books(self, data: List[dict], genre: Optional[str] = None):
        """Cache popular books"""
        key = f"popular:books:{genre or 'all'}"
        try:
            serialized = pickle.dumps(data)
            await self.redis.setex(
                key,
                self.settings.CACHE_TTL_SIMILAR_BOOKS,  # Same TTL as similar books
                serialized
            )
            logger.debug(f"Cached popular books genre={genre}")
        except Exception as e:
            logger.error(f"Failed to cache popular books: {e}")
    
    # ========== AI Chat Quota ==========

    async def get_ai_quota_used(self, user_id: int) -> int:
        """Get the number of AI chat requests used today by this user."""
        from datetime import date
        key = f"ai:quota:user:{user_id}:{date.today().strftime('%Y%m%d')}"
        try:
            count = await self.redis.get(key)
            return int(count) if count else 0
        except Exception as e:
            logger.error(f"Failed to get AI quota for user_id={user_id}: {e}")
            return 0

    async def increment_ai_quota(self, user_id: int) -> int:
        """Increment AI quota counter and set daily TTL. Returns the new count."""
        from datetime import date, datetime, timedelta
        today_str = date.today().strftime('%Y%m%d')
        key = f"ai:quota:user:{user_id}:{today_str}"
        try:
            count = await self.redis.incr(key)
            if count == 1:
                now = datetime.now()
                midnight = datetime(now.year, now.month, now.day) + timedelta(days=1)
                await self.redis.expireat(key, int(midnight.timestamp()))
            return count
        except Exception as e:
            logger.error(f"Failed to increment AI quota for user_id={user_id}: {e}")
            return 1

    # ========== Utility Methods ==========
    
    def _hash_text(self, text: str) -> str:
        """Generate MD5 hash of text for caching"""
        return hashlib.md5(text.encode('utf-8')).hexdigest()
    
    async def clear_all_cache(self):
        """Clear all cache (use with caution!)"""
        try:
            await self.redis.flushdb()
            logger.warning("Cleared all Redis cache")
        except Exception as e:
            logger.error(f"Failed to clear cache: {e}")
    
    async def get_cache_stats(self) -> dict:
        """Get Redis cache statistics"""
        try:
            info = await self.redis.info()
            return {
                "used_memory": info.get("used_memory_human"),
                "connected_clients": info.get("connected_clients"),
                "total_keys": await self.redis.dbsize(),
                "hit_rate": "N/A"  # Redis doesn't track this by default
            }
        except Exception as e:
            logger.error(f"Failed to get cache stats: {e}")
            return {}
