from fastapi import APIRouter

router = APIRouter(prefix="/rag", tags=["RAG"])


@router.get("/status")
async def rag_status():
    """
    RAG (Retrieval-Augmented Generation) status
    
    This module will be implemented in the future for:
    - LLM-powered book assistant
    - Question answering about books
    - Semantic search in book contents
    """
    return {
        "status": "not_implemented",
        "message": "RAG module is planned for future implementation",
        "planned_features": [
            "LLM-powered library assistant",
            "Question answering about books",
            "Semantic search within book contents",
            "Book summaries and analysis"
        ]
    }
