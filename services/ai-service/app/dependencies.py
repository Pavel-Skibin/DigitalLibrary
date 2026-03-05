from functools import lru_cache
from app.config import Settings, settings
from app.services.shared.qdrant_service import QdrantService
from app.services.shared.rest_client_service import RestClientService
from app.services.shared.cache_service import CacheService
from app.services.recommendations.embedding_service import EmbeddingService
from app.services.recommendations.recommendation_engine import RecommendationEngine
from app.services.rag.indexing_service import RAGIndexingService
from app.services.rag.retrieval_service import RAGRetrievalService
from app.services.rag.reranker_service import RerankerService
from app.services.rag.deepseek_client import DeepSeekClient
from app.services.rag.rag_service import RAGService
from app.services.rag.intent_classifier import IntentClassifierService
from app.services.rag.book_resolver import BookResolverService
from app.services.recommendations.nl_recommendation_service import NaturalLanguageRecommendationService
from app.services.smart_assistant import SmartAssistantService
from app.services.meta_enrichment import MetaEnrichmentService
from app.utils.task_registry import TaskRegistry, task_registry


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
def get_embedding_service_recommendations() -> EmbeddingService:
    """Get cached Embedding service for RECOMMENDATIONS (RoSBERTa 1024-dim with 'clustering:' prefix)"""
    service = EmbeddingService(
        model_name=settings.EMBEDDING_MODEL_NAME_RECOMMENDATIONS,
        embedding_dim=settings.EMBEDDING_DIM_RECOMMENDATIONS,
        batch_size=settings.EMBEDDING_BATCH_SIZE,
        use_prefix=True,  # RoSBERTa требует префиксы!
        prefix="clustering"  # Для тематической группировки (жанры, авторы, теги)
    )
    return service


@lru_cache()
def get_embedding_service_rag() -> EmbeddingService:
    """Get cached Embedding service for RAG (USER-bge-m3 1024-dim, no prefix)"""
    service = EmbeddingService(
        model_name=settings.EMBEDDING_MODEL_NAME_RAG,
        embedding_dim=settings.EMBEDDING_DIM_RAG,
        batch_size=settings.EMBEDDING_BATCH_SIZE,
        use_prefix=False  # USER-bge-m3 не требует префиксы
    )
    return service


@lru_cache()
def get_embedding_service() -> EmbeddingService:
    """
    DEPRECATED: Use get_embedding_service_recommendations() or get_embedding_service_rag()
    Fallback for backwards compatibility - returns recommendations service
    """
    return get_embedding_service_recommendations()


@lru_cache()
def get_reranker_service() -> RerankerService:
    """Get cached Reranker service instance (cross-encoder for reranking)."""
    return RerankerService(settings)


def get_recommendation_engine() -> RecommendationEngine:
    """Get Recommendation Engine instance with dependencies"""
    return RecommendationEngine(
        qdrant_service=get_qdrant_service(),
        rest_client_service=get_rest_client_service(),
        cache_service=get_cache_service(),
        embedding_service=get_embedding_service_recommendations(),  # RoSBERTa!
        settings=settings
    )


def get_rag_indexing_service() -> RAGIndexingService:
    """Get RAG Indexing Service with all dependencies injected."""
    return RAGIndexingService(
        settings=settings,
        qdrant=get_qdrant_service(),
        rest_client=get_rest_client_service(),
        embedding_service=get_embedding_service_rag(),  # USER-bge-m3!
    )


@lru_cache()
def get_deepseek_client() -> DeepSeekClient:
    """Get cached DeepSeek LLM client."""
    return DeepSeekClient(settings=settings)


def get_rag_retrieval_service() -> RAGRetrievalService:
    """Get RAG Retrieval Service (Фаза 5) with dependencies injected."""
    reranker = get_reranker_service() if settings.ENABLE_RERANKER else None
    return RAGRetrievalService(
        settings=settings,
        qdrant=get_qdrant_service(),
        embedding_service=get_embedding_service_rag(),  # USER-bge-m3!
        reranker=reranker,
    )


def get_rag_service() -> RAGService:
    """Get RAG Service (Фазы 6-7): retrieval + LLM orchestration."""
    return RAGService(
        settings=settings,
        retrieval=get_rag_retrieval_service(),
        llm=get_deepseek_client(),
    )


@lru_cache()
def get_intent_classifier() -> IntentClassifierService:
    """Get cached IntentClassifier (один экземпляр на всё приложение)."""
    return IntentClassifierService(llm=get_deepseek_client())


@lru_cache()
def get_book_resolver() -> BookResolverService:
    """Get cached BookResolverService."""
    return BookResolverService(qdrant=get_qdrant_service())


def get_nl_recommendation_service() -> NaturalLanguageRecommendationService:
    """Get NaturalLanguageRecommendationService with dependencies."""
    return NaturalLanguageRecommendationService(
        qdrant=get_qdrant_service(),
        rest_client=get_rest_client_service(),
        embedding=get_embedding_service_recommendations(),  # RoSBERTa — для вектора предпочтений
    )


def get_smart_assistant_service() -> SmartAssistantService:
    """Get SmartAssistantService — главный оркестратор чата."""
    return SmartAssistantService(
        settings=settings,
        intent_classifier=get_intent_classifier(),
        book_resolver=get_book_resolver(),
        rag_service=get_rag_service(),
        nl_rec_service=get_nl_recommendation_service(),
        rec_engine=get_recommendation_engine(),
    )


@lru_cache()
def get_meta_enrichment_service() -> MetaEnrichmentService:
    """Get cached MetaEnrichmentService (автозаполнение метаданных через DeepSeek)."""
    return MetaEnrichmentService(settings=settings)


def get_task_registry() -> TaskRegistry:
    """Get shared TaskRegistry singleton."""
    return task_registry
