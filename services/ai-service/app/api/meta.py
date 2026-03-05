"""
Meta API — автозаполнение метаданных книги через DeepSeek.
"""

from typing import List, Optional

from fastapi import APIRouter, Depends, HTTPException
from pydantic import BaseModel

from app.dependencies import get_meta_enrichment_service
from app.services.meta_enrichment import MetaEnrichmentService

router = APIRouter(prefix="/meta", tags=["Meta Enrichment"])


class EnrichRequest(BaseModel):
    title:   str
    authors: List[str] = []


class EnrichResponse(BaseModel):
    description:      str
    genres:           List[str]
    tags:             List[str]
    publication_year: Optional[int]
    language:         str
    age_rating:       str
    series_name:      Optional[str]
    series_number:    Optional[int]


@router.post("/enrich", response_model=EnrichResponse)
async def enrich_book_metadata(
    request: EnrichRequest,
    service: MetaEnrichmentService = Depends(get_meta_enrichment_service),
):
    """
    Запрашивает у DeepSeek метаданные книги по названию и авторам.

    Возвращает: описание, жанры, теги, темы, год, язык, возрастной рейтинг, серию.
    Используется в панели администратора для автозаполнения формы.
    """
    title = request.title.strip()
    if not title:
        raise HTTPException(status_code=422, detail="Название книги не может быть пустым")

    try:
        meta = await service.enrich(title=title, authors=request.authors)
    except RuntimeError as exc:
        raise HTTPException(status_code=503, detail=str(exc)) from exc
    except Exception as exc:
        raise HTTPException(status_code=500, detail=f"Ошибка обогащения: {exc}") from exc

    return EnrichResponse(
        description      = meta.description,
        genres           = meta.genres,
        tags             = meta.tags,
        publication_year = meta.publication_year,
        language         = meta.language,
        age_rating       = meta.age_rating,
        series_name      = meta.series_name,
        series_number    = meta.series_number,
    )
