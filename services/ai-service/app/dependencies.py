from functools import lru_cache
from app.config import Settings, settings
from app.services.shared.qdrant_service import QdrantService
from app.services.shared.rest_client_service import RestClientService
from app.services.shared.cache_service import CacheService
from app.services.recommendations.embedding_service import EmbeddingService
from app.services.recommendations.recommendation_engine import RecommendationEngine


@lru_cache()
def get_settings() -> Settings:
    """Get cached settings instance"""
    return settings


@lru_cache()
def get_qdrant_service() -> QdrantService:
    """Get cached Qdrant service instance"""
    return QdrantService(settings)


@lru_cache()
def get_rest_client_service() -> RestClientService:
    """Get cached REST Client service instance"""
    return RestClientService(settings)


@lru_cache()
def get_cache_service() -> CacheService:
    """Get cached Redis service instance"""
    return CacheService(settings)


@lru_cache()
def get_embedding_service() -> EmbeddingService:
    """Get cached Embedding service instance"""
    return EmbeddingService(settings)


def get_recommendation_engine() -> RecommendationEngine:
    """Get Recommendation Engine instance with dependencies"""
    return RecommendationEngine(
        qdrant_service=get_qdrant_service(),
        rest_client_service=get_rest_client_service(),
        cache_service=get_cache_service(),
        embedding_service=get_embedding_service(),
        settings=settings
    )
