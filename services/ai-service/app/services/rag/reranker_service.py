"""
Reranker Service — переранжирование результатов поиска через cross-encoder.

Cross-encoder оценивает релевантность (query, document) напрямую, в отличие от
bi-encoder (embedding модели), который сравнивает векторы. Cross-encoder точнее,
но медленнее, поэтому используем его для reranking топ-N результатов.
"""

import torch
import numpy as np
from typing import List, Tuple
from loguru import logger
from sentence_transformers import CrossEncoder

from app.config import Settings, settings as _settings


class RerankerService:
    """
    Переранжирование чанков с помощью cross-encoder модели.
    
    Использует BAAI/bge-reranker-v2-m3 (мультиязычная, включая русский)
    или cross-encoder/ms-marco-MiniLM-L-12-v2 (легковесная, английская).
    """

    def __init__(self, settings: Settings):
        self.settings = settings
        self.model_name = settings.RERANKER_MODEL_NAME
        if _settings.FORCE_CPU:
            self.device = "cpu"
        else:
            self.device = "cuda" if torch.cuda.is_available() else "cpu"
        self._model: CrossEncoder = None
        self.initialized = False

    def load_model(self):
        """Загружает cross-encoder модель."""
        try:
            logger.info(f"Loading reranker model: {self.model_name}")
            self._model = CrossEncoder(self.model_name, device=self.device)
            self.initialized = True
            logger.info(f"Reranker {self.model_name} loaded on {self.device}")
        except Exception as e:
            logger.error(f"Failed to load reranker model: {e}")
            raise

    def rerank(
        self,
        query: str,
        documents: List[str],
        top_k: int = 5,
    ) -> List[Tuple[int, float]]:
        """
        Переранжирует документы по релевантности к запросу.

        Args:
            query: Запрос пользователя.
            documents: Список текстов чанков (в том же порядке, что пришли из retrieval).
            top_k: Сколько топ-результатов вернуть.

        Returns:
            List[(original_index, score)] — индексы в исходном списке + скоры.
            Отсортировано по убыванию скора (самые релевантные первыми).
        """
        if not self.initialized:
            self.load_model()

        if not documents:
            return []

        try:
            # Формируем пары (query, doc)
            pairs = [[query, doc] for doc in documents]
            
            # Cross-encoder возвращает скоры релевантности
            scores = self._model.predict(pairs, show_progress_bar=False)
            
            # Сортируем по убыванию скора
            ranked_indices = np.argsort(scores)[::-1]  # descending
            
            # Берём top_k
            top_indices = ranked_indices[:top_k]
            results = [(int(idx), float(scores[idx])) for idx in top_indices]
            
            logger.debug(
                f"Reranker: {len(documents)} docs → top {len(results)} "
                f"(best score: {results[0][1]:.3f})"
            )
            return results

        except Exception as e:
            logger.error(f"Reranking failed: {e}")
            # Fallback: возвращаем исходный порядок
            return [(i, 1.0) for i in range(min(top_k, len(documents)))]

    def is_available(self) -> bool:
        """Проверяет, загружена ли модель."""
        return self.initialized
