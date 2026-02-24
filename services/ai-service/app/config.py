from pydantic_settings import BaseSettings
from typing import Optional


class Settings(BaseSettings):
    """Application settings loaded from environment variables"""
    
    # Service
    SERVICE_NAME: str = "ai-service"
    SERVICE_PORT: int = 8085
    DEBUG: bool = True
    
    # Other Microservices (REST API)
    # AI Service does NOT connect to PostgreSQL directly!
    # Instead, it uses REST APIs of other microservices
    USER_SERVICE_URL: str = "http://localhost:8081"
    BOOK_CATALOG_SERVICE_URL: str = "http://localhost:8091"
    COMMENT_RATING_SERVICE_URL: str = "http://localhost:8082"
    
    # Qdrant (Vector Database - AI Service writes embeddings here)
    QDRANT_HOST: str = "localhost"
    QDRANT_PORT: int = 6333
    QDRANT_GRPC_PORT: int = 6334
    QDRANT_COLLECTION_RECOMMENDATIONS: str = "books_recommendations"
    QDRANT_COLLECTION_RAG: str = "books_rag_chunks"
    
    # Redis (Cache - AI Service caches REST API responses here)
    REDIS_HOST: str = "localhost"
    REDIS_PORT: int = 6379
    REDIS_DB: int = 0
    REDIS_PASSWORD: Optional[str] = None
    
    # Embedding Model
    EMBEDDING_MODEL_NAME: str = "ai-forever/ru-en-RoSBERTa"
    EMBEDDING_DIM: int = 1024
    EMBEDDING_BATCH_SIZE: int = 32
    
    # Recommendation params
    TOP_K_SIMILAR: int = 50
    FINAL_RECOMMENDATIONS: int = 10
    MMR_DIVERSITY_LAMBDA: float = 0.5
    MAX_PER_AUTHOR: int = 5
    MAX_PER_GENRE: int = 7
    
    # Cache TTL (seconds)
    CACHE_TTL_RECOMMENDATIONS: int = 3600
    CACHE_TTL_SIMILAR_BOOKS: int = 604800
    CACHE_TTL_EMBEDDINGS: int = 2592000
    
    # Logging
    LOG_LEVEL: str = "INFO"
    
    # Security
    INTERNAL_API_KEY: str = "change-this-secret-key-in-production"
    JWT_PASSTHROUGH: bool = True
    
    # Message Queue (optional)
    ENABLE_MESSAGE_QUEUE: bool = False
    MESSAGE_QUEUE_TYPE: str = "redis"
    MESSAGE_QUEUE_STREAM_BOOK_CREATED: str = "book:created"
    MESSAGE_QUEUE_STREAM_BOOK_UPDATED: str = "book:updated"
    MESSAGE_QUEUE_CONSUMER_GROUP: str = "ai-service-embeddings"
    
    # Hybrid Search
    ENABLE_HYBRID_SEARCH: bool = True
    HYBRID_SEARCH_WEIGHT_DENSE: float = 0.7
    HYBRID_SEARCH_WEIGHT_SPARSE: float = 0.3
    
    # Anchor Books Weights (для расчета важности книг пользователя)
    # Чем выше вес - тем сильнее книга влияет на рекомендации
    ANCHOR_WEIGHT_RATING_5: int = 5      # Оценка 5★
    ANCHOR_WEIGHT_RATING_4: int = 4      # Оценка 4★
    ANCHOR_WEIGHT_COMPLETED: int = 4     # Прочитано до конца
    ANCHOR_WEIGHT_FAVORITE: int = 5      # В избранном
    
    # Прогрессивные веса по времени чтения (чем дольше читал - тем выше вес)
    ANCHOR_WEIGHT_READ_1H: int = 1       # Читал 30мин-1час
    ANCHOR_WEIGHT_READ_2H: int = 2       # Читал 1-2 часа
    ANCHOR_WEIGHT_READ_5H: int = 3       # Читал 2-5 часов
    ANCHOR_WEIGHT_READ_10H: int = 4      # Читал 5-10 часов
    ANCHOR_WEIGHT_READ_LONG: int = 5     # Читал >10 часов
    
    class Config:
        env_file = ".env"
        case_sensitive = False


# Global settings instance
settings = Settings()
