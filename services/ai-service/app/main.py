from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from contextlib import asynccontextmanager
from loguru import logger
import time

from app.config import settings
from app.api import recommendations, embeddings, rag, chat
from app.dependencies import (
    get_qdrant_service,
    get_rest_client_service,
    get_cache_service,
    get_embedding_service_recommendations,
)


# Lifespan context manager for startup/shutdown events
@asynccontextmanager
async def lifespan(app: FastAPI):
    """
    Manage application lifespan: startup and shutdown events
    """
    # Startup
    logger.info(f"Starting {settings.SERVICE_NAME}...")
    
    try:
        # Initialize Qdrant
        qdrant_service = get_qdrant_service()
        qdrant_service.connect()
        await qdrant_service.initialize_recommendations_collection()
        await qdrant_service.initialize_rag_collection()
        logger.info("Qdrant initialized (recommendations + RAG collections)")

        # Initialize REST Client for microservices communication
        rest_client_service = get_rest_client_service()
        await rest_client_service.init_session()
        logger.info("REST Client initialized")

        # Initialize Redis
        cache_service = get_cache_service()
        await cache_service.connect()
        logger.info("Redis connected")

        # Preload recommendations embedding model (ru-en-RoSBERTa).
        # RAG embedding model (USER-bge-m3) loads lazily on first use.
        get_embedding_service_recommendations().load_model()
        logger.info("Recommendations embedding model loaded (ru-en-RoSBERTa)")

        logger.info(f"{settings.SERVICE_NAME} started on port {settings.SERVICE_PORT}")
        
    except Exception as e:
        logger.error(f"Failed to start application: {e}")
        raise
    
    yield
    
    # Shutdown
    logger.info(f"Shutting down {settings.SERVICE_NAME}...")
    
    try:
        # Disconnect services
        qdrant_service.disconnect()
        await rest_client_service.close_session()
        await cache_service.disconnect()
        
        logger.info(f"{settings.SERVICE_NAME} shut down gracefully")
        
    except Exception as e:
        logger.error(f"Error during shutdown: {e}")


# Create FastAPI app
app = FastAPI(
    title="Digital Library AI Service",
    description="""
    AI-powered service for Digital Library providing:
    - **Personalized recommendations** — anchor-based hybrid search, MMR diversity
    - **RAG assistant** — Q&A over book content via hybrid search + DeepSeek LLM
    - **Smart assistant** — intent classification, book Q&A, similar-book discovery

    Embedding models:
    - Recommendations: ai-forever/ru-en-RoSBERTa (1024-dim, prefix "clustering:")
    - RAG: deepvk/USER-bge-m3 (1024-dim, no prefix)

    Infrastructure: FastAPI, Qdrant (vector DB), Redis (cache), REST clients to other microservices.
    """,
    version="1.0.0",
    lifespan=lifespan,
    docs_url="/docs" if settings.DEBUG else None,
    redoc_url="/redoc" if settings.DEBUG else None
)

# CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # In production, specify exact origins
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# Request logging middleware
@app.middleware("http")
async def log_requests(request: Request, call_next):
    """Log all requests with timing"""
    start_time = time.time()
    
    # Log request
    logger.info(f"{request.method} {request.url.path}")

    response = await call_next(request)

    duration = time.time() - start_time
    logger.info(f"{request.method} {request.url.path} {response.status_code} ({duration:.3f}s)")
    
    return response


# Exception handler
@app.exception_handler(Exception)
async def global_exception_handler(request: Request, exc: Exception):
    """Global exception handler"""
    logger.error(f"Unhandled exception: {exc}", exc_info=True)
    return JSONResponse(
        status_code=500,
        content={
            "detail": "Internal server error",
            "error": str(exc) if settings.DEBUG else "An error occurred"
        }
    )


# Include routers
app.include_router(recommendations.router, prefix="/api")
app.include_router(embeddings.router, prefix="/api")
app.include_router(rag.router, prefix="/api")
app.include_router(chat.router, prefix="/api/ai")


# Health check endpoints
@app.get("/")
async def root():
    """Root endpoint"""
    return {
        "service": settings.SERVICE_NAME,
        "version": "1.0.0",
        "status": "running",
        "docs": "/docs" if settings.DEBUG else "disabled"
    }


@app.get("/health")
async def health_check():
    """Health check endpoint"""
    try:
        # Check Qdrant
        qdrant_service = get_qdrant_service()
        qdrant_healthy = qdrant_service.client is not None
        
        # Check REST Client
        rest_client_service = get_rest_client_service()
        rest_client_healthy = rest_client_service.session is not None
        
        # Check Redis
        cache_service = get_cache_service()
        redis_healthy = cache_service.redis is not None
        if redis_healthy:
            await cache_service.redis.ping()
        
        # Check embedding model (recommendations model — loaded at startup)
        embedding_service = get_embedding_service_recommendations()
        model_healthy = embedding_service.initialized
        
        all_healthy = all([qdrant_healthy, rest_client_healthy, redis_healthy, model_healthy])
        
        status = {
            "status": "healthy" if all_healthy else "degraded",
            "services": {
                "qdrant": "up" if qdrant_healthy else "down",
                "rest_client": "up" if rest_client_healthy else "down",
                "redis": "up" if redis_healthy else "down",
                "embedding_model": "loaded" if model_healthy else "not_loaded"
            }
        }
        
        return status
        
    except Exception as e:
        logger.error(f"Health check failed: {e}")
        return {
            "status": "unhealthy",
            "error": str(e)
        }


@app.get("/stats")
async def get_stats():
    """Service statistics"""
    try:
        qdrant_service = get_qdrant_service()
        cache_service = get_cache_service()
        
        # Get Qdrant stats
        collection_info = await qdrant_service.get_collection_info()
        
        # Get Redis stats
        cache_stats = await cache_service.get_cache_stats()
        
        return {
            "qdrant": collection_info,
            "redis": cache_stats
        }
        
    except Exception as e:
        logger.error(f"Failed to get stats: {e}")
        return {"error": str(e)}


if __name__ == "__main__":
    import uvicorn
    
    uvicorn.run(
        "app.main:app",
        host="0.0.0.0",
        port=settings.SERVICE_PORT,
        reload=settings.DEBUG,
        log_level="info"
    )
