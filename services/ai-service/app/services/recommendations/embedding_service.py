import torch
import numpy as np
from typing import List, Optional
from loguru import logger
from sentence_transformers import SentenceTransformer

from app.config import Settings, settings


class EmbeddingService:
    """
    Сервис генерации эмбеддингов текста через sentence-transformers.

    Поддерживает разные модели:
    - ai-forever/ru-en-RoSBERTa (1024-dim RoBERTa-large) - для recommendations (требует prefix "clustering:")
    - deepvk/USER-bge-m3 (1024-dim XLM-RoBERTa) - для RAG (Q&A по содержимому, без префиксов)
    """

    def __init__(
        self, 
        model_name: str, 
        embedding_dim: int, 
        batch_size: int = 32,
        use_prefix: bool = False,
        prefix: str = "clustering"
    ):
        self.model_name = model_name
        self.embedding_dim = embedding_dim
        self.batch_size = batch_size
        self.use_prefix = use_prefix  # Для RoSBERTa = True
        self.prefix = prefix  # "clustering" для recommendations
        if settings.FORCE_CPU:
            self.device = "cpu"
        else:
            self.device = "cuda" if torch.cuda.is_available() else "cpu"
        self._model: Optional[SentenceTransformer] = None
        self.initialized = False

    def load_model(self):
        """Загружает модель через sentence-transformers."""
        try:
            logger.info(f"Loading embedding model: {self.model_name} ({self.embedding_dim}-dim)")
            if self.device == "cuda":
                gpu_name = torch.cuda.get_device_name(0)
                vram_gb  = torch.cuda.get_device_properties(0).total_memory / 1024**3
                logger.info(f"GPU: {gpu_name} ({vram_gb:.1f} GB VRAM)")
            elif settings.FORCE_CPU:
                logger.info("FORCE_CPU=true — running on CPU (GPU disabled for testing)")
            else:
                logger.warning(
                    "CUDA not available, running on CPU. "
                    "Install CUDA-enabled PyTorch for faster inference."
                )

            self._model = SentenceTransformer(self.model_name, device=self.device)
            self._model.eval()
            self.initialized = True
            logger.info(f"Model {self.model_name} loaded on {self.device}")

        except Exception as e:
            logger.error(f"Failed to load embedding model: {e}")
            raise

    # ─── Single text ───────────────────────────────────────────────────────────

    def get_embedding(self, text: str) -> np.ndarray:
        """
        Возвращает нормализованный вектор для одного текста.

        Args:
            text: Входной текст

        Returns:
            np.ndarray shape (embedding_dim,) - нормализованный вектор.
        """
        if not self.initialized:
            self.load_model()

        try:
            # RoSBERTa требует префикс "clustering:" для recommendations
            if self.use_prefix:
                try:
                    # Пробуем использовать prompt_name (sentence-transformers>=2.4.0)
                    embedding = self._model.encode(
                        text,
                        prompt_name=self.prefix,
                        normalize_embeddings=True,
                        show_progress_bar=False,
                    )
                except TypeError:
                    # Fallback: добавляем префикс вручную
                    prefixed_text = f"{self.prefix}: {text}"
                    embedding = self._model.encode(
                        prefixed_text,
                        normalize_embeddings=True,
                        show_progress_bar=False,
                    )
            else:
                embedding = self._model.encode(
                    text,
                    normalize_embeddings=True,
                    show_progress_bar=False,
                )
            return np.array(embedding, dtype=np.float32)
        except Exception as e:
            logger.error(f"Failed to generate embedding: {e}")
            raise

    # ─── Batch ────────────────────────────────────────────────────────────────

    def get_embeddings_batch(self, texts: List[str]) -> np.ndarray:
        """
        Возвращает матрицу нормализованных векторов.

        Args:
            texts: Список текстов

        Returns:
            np.ndarray shape (N, embedding_dim) - матрица нормализованных векторов
        """
        if not self.initialized:
            self.load_model()

        try:
            # RoSBERTa требует префикс "clustering:" для recommendations
            if self.use_prefix:
                try:
                    # Пробуем использовать prompt_name (sentence-transformers>=2.4.0)
                    embeddings = self._model.encode(
                        texts,
                        prompt_name=self.prefix,
                        batch_size=self.batch_size,
                        normalize_embeddings=True,
                        show_progress_bar=False,
                    )
                except TypeError:
                    # Fallback: добавляем префикс вручную
                    prefixed_texts = [f"{self.prefix}: {text}" for text in texts]
                    embeddings = self._model.encode(
                        prefixed_texts,
                        batch_size=self.batch_size,
                        normalize_embeddings=True,
                        show_progress_bar=False,
                    )
            else:
                embeddings = self._model.encode(
                    texts,
                    batch_size=self.batch_size,
                    normalize_embeddings=True,
                    show_progress_bar=False,
                )
            logger.debug(f"Generated {len(embeddings)} embeddings (batch)")
            return np.array(embeddings, dtype=np.float32)
        except Exception as e:
            logger.error(f"Failed to generate batch embeddings: {e}")
            raise

    # ─── Similarity ───────────────────────────────────────────────────────────

    def compute_similarity(self, embedding1: np.ndarray, embedding2: np.ndarray) -> float:
        """Косинусное сходство (вектора уже нормализованы → просто dot)."""
        return float(np.dot(embedding1, embedding2))

    def compute_similarities_batch(
        self,
        query_embedding: np.ndarray,
        candidate_embeddings: np.ndarray,
    ) -> np.ndarray:
        """Сходство одного query со всеми кандидатами (matrix multiply)."""
        return np.dot(candidate_embeddings, query_embedding)

    # ─── Memory management ────────────────────────────────────────────────────

    def unload_model(self):
        """Освобождает GPU/CPU память."""
        if self._model is not None:
            del self._model
            self._model = None
            if self.device == "cuda":
                torch.cuda.empty_cache()
            logger.info("Model unloaded from memory")
            self.initialized = False
