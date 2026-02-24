import torch
from transformers import AutoTokenizer, AutoModel
import numpy as np
from typing import List, Optional
from loguru import logger

from app.config import Settings


class EmbeddingService:
    """Service for generating text embeddings using ru-en-RoSBERTa"""
    
    def __init__(self, settings: Settings):
        self.settings = settings
        self.model_name = settings.EMBEDDING_MODEL_NAME
        self.device = "cuda" if torch.cuda.is_available() else "cpu"
        self.tokenizer: Optional[AutoTokenizer] = None
        self.model: Optional[AutoModel] = None
        self.initialized = False
    
    def load_model(self):
        """Load ru-en-RoSBERTa model from HuggingFace"""
        try:
            logger.info(f"Loading embedding model: {self.model_name}")
            logger.info(f"Using device: {self.device}")
            
            # Load tokenizer
            self.tokenizer = AutoTokenizer.from_pretrained(self.model_name)
            
            # Load model
            self.model = AutoModel.from_pretrained(self.model_name)
            self.model.to(self.device)
            self.model.eval()  # Set to evaluation mode
            
            self.initialized = True
            logger.info(f"Model loaded successfully on {self.device}")
            
        except Exception as e:
            logger.error(f"Failed to load embedding model: {e}")
            raise
    
    def get_embedding(self, text: str) -> np.ndarray:
        """
        Generate embedding for a single text.
        
        Args:
            text: Input text (will be automatically prefixed with "clustering: ")
        
        Returns:
            np.ndarray: 768-dimensional embedding vector
        """
        if not self.initialized:
            self.load_model()
        
        try:
            # Add prefix for ru-en-RoSBERTa
            # "clustering:" prefix is for thematic similarity tasks
            prefixed_text = f"clustering: {text}"
            
            # Tokenize
            inputs = self.tokenizer(
                prefixed_text,
                return_tensors="pt",
                truncation=True,
                max_length=512,
                padding=True
            )
            
            # Log token length for monitoring
            token_count = inputs['input_ids'].shape[1]
            if token_count >= 500:
                logger.warning(f"Text approaching token limit: {token_count}/512 tokens")
            elif token_count >= 450:
                logger.debug(f"Text uses {token_count}/512 tokens")
            
            # Move to device
            inputs = {k: v.to(self.device) for k, v in inputs.items()}
            
            # Generate embedding
            with torch.no_grad():
                outputs = self.model(**inputs)
            
            # Use [CLS] token embedding (first token)
            # Alternative: use mean pooling over all tokens
            embedding = outputs.last_hidden_state[:, 0, :].cpu().numpy()[0]
            
            # Normalize for cosine similarity
            embedding = embedding / np.linalg.norm(embedding)
            
            return embedding
            
        except Exception as e:
            logger.error(f"Failed to generate embedding: {e}")
            raise
    
    def get_embeddings_batch(self, texts: List[str]) -> np.ndarray:
        """
        Generate embeddings for multiple texts (batch processing).
        
        Args:
            texts: List of strings to embed (will be automatically prefixed with "clustering: ")
        
        Returns:
            np.ndarray: Array of shape (len(texts), 768)
        """
        if not self.initialized:
            self.load_model()
        
        try:
            # Add prefix for ru-en-RoSBERTa for all texts
            # "clustering:" prefix is for thematic similarity tasks
            prefixed_texts = [f"clustering: {text}" for text in texts]
            
            # Tokenize all texts
            inputs = self.tokenizer(
                prefixed_texts,
                return_tensors="pt",
                truncation=True,
                max_length=512,
                padding=True
            )
            
            # Log token lengths for monitoring
            token_counts = inputs['input_ids'].shape[1]
            max_tokens = inputs['attention_mask'].sum(dim=1).max().item()
            
            if max_tokens >= 500:
                logger.warning(f"Batch max token length: {max_tokens}/512 tokens")
            
            logger.debug(f"Batch size: {len(texts)}, max tokens: {max_tokens}/512")
            
            # Move to device
            inputs = {k: v.to(self.device) for k, v in inputs.items()}
            
            # Generate embeddings
            with torch.no_grad():
                outputs = self.model(**inputs)
            
            # Use [CLS] token embeddings
            embeddings = outputs.last_hidden_state[:, 0, :].cpu().numpy()
            
            # Normalize each embedding
            norms = np.linalg.norm(embeddings, axis=1, keepdims=True)
            embeddings = embeddings / norms
            
            logger.debug(f"Generated {len(embeddings)} embeddings")
            
            return embeddings
            
        except Exception as e:
            logger.error(f"Failed to generate batch embeddings: {e}")
            raise
    
    def compute_similarity(self, embedding1: np.ndarray, embedding2: np.ndarray) -> float:
        """
        Compute cosine similarity between two embeddings.
        
        Returns:
            float: Similarity score between 0 and 1
        """
        # Since embeddings are already normalized, dot product = cosine similarity
        similarity = np.dot(embedding1, embedding2)
        return float(similarity)
    
    def compute_similarities_batch(
        self,
        query_embedding: np.ndarray,
        candidate_embeddings: np.ndarray
    ) -> np.ndarray:
        """
        Compute similarities between one query and multiple candidates.
        
        Args:
            query_embedding: Shape (768,)
            candidate_embeddings: Shape (N, 768)
        
        Returns:
            np.ndarray: Similarity scores, shape (N,)
        """
        # Matrix multiplication for batch similarity
        similarities = np.dot(candidate_embeddings, query_embedding)
        return similarities
    
    def unload_model(self):
        """Free GPU/CPU memory"""
        if self.model:
            del self.model
            del self.tokenizer
            if torch.cuda.is_available():
                torch.cuda.empty_cache()
            logger.info("Model unloaded from memory")
            self.initialized = False
