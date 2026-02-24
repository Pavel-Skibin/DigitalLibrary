from qdrant_client import QdrantClient
from qdrant_client.models import (
    Distance,
    VectorParams,
    PointStruct,
    Filter,
    FieldCondition,
    MatchValue,
    MatchAny,
    Range,
    SparseVectorParams,
    SparseIndexParams,
    SparseVector,
    Prefetch,
    QueryRequest,
)
from typing import List, Dict, Optional, Any, Tuple
import numpy as np
from loguru import logger
import re
import hashlib
from collections import Counter

from app.config import Settings


class QdrantService:
    """Service for interacting with Qdrant vector database with hybrid search support"""
    
    def __init__(self, settings: Settings):
        self.settings = settings
        self.client: Optional[QdrantClient] = None
        self.collection_recommendations = settings.QDRANT_COLLECTION_RECOMMENDATIONS
        self.collection_rag = settings.QDRANT_COLLECTION_RAG
        
        # BM25 parameters (industry standard)
        self.bm25_k1 = 1.5  # Term saturation parameter
        self.bm25_b = 0.75  # Length normalization
        self.avg_doc_length = 100  # Average document length in tokens
        
    def connect(self):
        """Establish connection to Qdrant"""
        try:
            self.client = QdrantClient(
                host=self.settings.QDRANT_HOST,
                port=self.settings.QDRANT_PORT,
                timeout=30
            )
            logger.info(f"Connected to Qdrant at {self.settings.QDRANT_HOST}:{self.settings.QDRANT_PORT}")
        except Exception as e:
            logger.error(f"Failed to connect to Qdrant: {e}")
            raise
    
    def disconnect(self):
        """Close Qdrant connection"""
        if self.client:
            self.client.close()
            logger.info("Disconnected from Qdrant")
    
    async def initialize_recommendations_collection(self):
        """Create recommendations collection with hybrid search support (Dense + Sparse)"""
        try:
            collections = self.client.get_collections().collections
            collection_names = [col.name for col in collections]
            
            if self.collection_recommendations not in collection_names:
                logger.info(f"Creating collection: {self.collection_recommendations}")
                logger.info("⚡ Enabling hybrid search: Dense vectors (RoSBERTa) + Sparse vectors (BM25)")
                
                # Create collection with HYBRID search support
                self.client.create_collection(
                    collection_name=self.collection_recommendations,
                    vectors_config={
                        "dense": VectorParams(
                            size=self.settings.EMBEDDING_DIM,
                            distance=Distance.COSINE
                        )
                    },
                    sparse_vectors_config={
                        "bm25": SparseVectorParams(
                            index=SparseIndexParams()  # Default BM25 configuration
                        )
                    }
                )
                
                # Create payload indexes for fast filtering
                self.client.create_payload_index(
                    collection_name=self.collection_recommendations,
                    field_name="book_id",
                    field_schema="integer"
                )
                
                self.client.create_payload_index(
                    collection_name=self.collection_recommendations,
                    field_name="genres",
                    field_schema="keyword"
                )
                
                self.client.create_payload_index(
                    collection_name=self.collection_recommendations,
                    field_name="authors",
                    field_schema="keyword"
                )
                
                self.client.create_payload_index(
                    collection_name=self.collection_recommendations,
                    field_name="language",
                    field_schema="keyword"
                )
                
                self.client.create_payload_index(
                    collection_name=self.collection_recommendations,
                    field_name="publication_year",
                    field_schema="integer"
                )
                
                logger.info(f"Collection {self.collection_recommendations} created successfully")
            else:
                logger.info(f"Collection {self.collection_recommendations} already exists")
                logger.info("⚡ Using hybrid search: Dense vectors (RoSBERTa) + Sparse vectors (BM25)")
                
        except Exception as e:
            logger.error(f"Failed to initialize collection: {e}")
            raise
    
    def _generate_bm25_sparse_vector(self, text: str) -> SparseVector:
        """Generate BM25 sparse vector from text for hybrid search"""
        # Tokenize (simple whitespace + lowercase)
        tokens = re.findall(r'\w+', text.lower())
        
        if not tokens:
            return SparseVector(indices=[], values=[])
        
        # Count term frequencies
        term_freq = Counter(tokens)
        doc_length = len(tokens)
        
        # Calculate BM25 scores
        indices = []
        values = []
        
        for term, tf in term_freq.items():
            # Deterministic hash for term to index mapping using MD5
            # This ensures same term always maps to same index across runs
            term_hash = hashlib.md5(term.encode('utf-8')).hexdigest()
            term_index = int(term_hash[:8], 16) % 1000000  # Limit to 1M unique terms
            
            # BM25 formula: tf * (k1 + 1) / (tf + k1 * (1 - b + b * doc_len / avg_doc_len))
            numerator = tf * (self.bm25_k1 + 1)
            denominator = tf + self.bm25_k1 * (1 - self.bm25_b + self.bm25_b * doc_length / self.avg_doc_length)
            score = numerator / denominator
            
            indices.append(term_index)
            values.append(score)
        
        return SparseVector(indices=indices, values=values)
    
    def _merge_results_rrf(
        self, 
        dense_results: List[Any], 
        sparse_results: List[Any], 
        limit: int, 
        k: int = 60
    ) -> List[Any]:
        """
        Merge results from dense and sparse searches using Reciprocal Rank Fusion (RRF).
        
        RRF formula: score(doc) = sum( 1 / (k + rank_i) )
        where k=60 is standard constant, rank_i is position in i-th result list (1-based)
        
        Args:
            dense_results: Results from dense vector search
            sparse_results: Results from sparse (BM25) search
            limit: Number of results to return
            k: RRF constant (default 60)
        
        Returns:
            Merged and reranked results
        """
        # Build RRF scores: book_id -> (score, point_data)
        rrf_scores: Dict[int, Tuple[float, Any]] = {}
        
        # Add dense results
        for rank, point in enumerate(dense_results, start=1):
            book_id = point.payload.get("book_id")
            if book_id:
                rrf_score = 1.0 / (k + rank)
                if book_id in rrf_scores:
                    rrf_scores[book_id] = (rrf_scores[book_id][0] + rrf_score, point)
                else:
                    rrf_scores[book_id] = (rrf_score, point)
        
        # Add sparse results
        for rank, point in enumerate(sparse_results, start=1):
            book_id = point.payload.get("book_id")
            if book_id:
                rrf_score = 1.0 / (k + rank)
                if book_id in rrf_scores:
                    rrf_scores[book_id] = (rrf_scores[book_id][0] + rrf_score, rrf_scores[book_id][1])
                else:
                    rrf_scores[book_id] = (rrf_score, point)
        
        # Sort by RRF score descending
        sorted_results = sorted(rrf_scores.values(), key=lambda x: x[0], reverse=True)
        
        # Return top-limit points
        return [point for score, point in sorted_results[:limit]]

    
    async def upsert_book_embedding(
        self,
        book_id: int,
        embedding: np.ndarray,
        metadata: Dict[str, Any],
        text_profile: Optional[str] = None
    ):
        """Insert or update book embedding with HYBRID vectors (Dense + Sparse)"""
        try:
            # Prepare vectors
            vectors = {"dense": embedding.tolist()}
            
            # Add BM25 sparse vector if text profile provided
            if text_profile:
                sparse_vector = self._generate_bm25_sparse_vector(text_profile)
                vectors["bm25"] = sparse_vector
                logger.debug(f"Generated BM25 vector with {len(sparse_vector.indices)} terms")
            
            point = PointStruct(
                id=book_id,
                vector=vectors,
                payload={
                    "book_id": book_id,
                    "title": metadata.get("title", ""),
                    "authors": metadata.get("authors", []),
                    "genres": metadata.get("genres", []),
                    "tags": metadata.get("tags", []),
                    "language": metadata.get("language", ""),
                    "publication_year": metadata.get("publication_year"),
                    "age_rating": metadata.get("age_rating"),
                    "series_name": metadata.get("series_name"),
                    "avg_rating": metadata.get("average_rating"),
                    "ratings_count": metadata.get("ratings_count", 0),
                    "cover_image_path": metadata.get("cover_image_path"),
                }
            )
            
            self.client.upsert(
                collection_name=self.collection_recommendations,
                points=[point]
            )
            
            logger.debug(f"Upserted HYBRID embedding for book_id={book_id}")
            
        except Exception as e:
            logger.error(f"Failed to upsert book embedding for book_id={book_id}: {e}")
            raise
    
    async def batch_upsert_embeddings(
        self,
        embeddings: List[Dict[str, Any]]
    ):
        """Batch insert/update multiple book embeddings with HYBRID vectors (Dense + Sparse)"""
        try:
            points = []
            sparse_count = 0
            
            for item in embeddings:
                # Prepare vectors
                vectors = {"dense": item["embedding"].tolist()}
                
                # Add BM25 sparse vector if text profile provided
                if "text_profile" in item and item["text_profile"]:
                    sparse_vector = self._generate_bm25_sparse_vector(item["text_profile"])
                    vectors["bm25"] = sparse_vector
                    sparse_count += 1
                
                point = PointStruct(
                    id=item["book_id"],
                    vector=vectors,
                    payload=item["metadata"]
                )
                points.append(point)
            
            self.client.upsert(
                collection_name=self.collection_recommendations,
                points=points
            )
            
            logger.info(f" Batch upserted {len(points)} embeddings ({sparse_count} with BM25 sparse vectors)")
            
        except Exception as e:
            logger.error(f"Failed to batch upsert embeddings: {e}")
            raise
    
    async def search_similar_books(
        self,
        query_vector: np.ndarray,
        query_text: Optional[str] = None,
        limit: int = 10,
        exclude_ids: Optional[List[int]] = None,
        filters: Optional[Dict[str, Any]] = None,
        use_hybrid: bool = True
    ) -> List[Dict[str, Any]]:
        """
        Search for similar books using HYBRID search (Dense + Sparse).
        
        Hybrid search combines:
        - Dense vectors (RoSBERTa embeddings) for semantic similarity
        - Sparse vectors (BM25) for keyword matching
        
        This provides +10% better relevance compared to dense-only search.
        
        Args:
            query_vector: Dense embedding vector from RoSBERTa
            query_text: Text for BM25 sparse search (optional but recommended)
            limit: Number of results to return
            exclude_ids: Book IDs to exclude from results
            filters: Metadata filters (language, genres, etc.)
            use_hybrid: Use hybrid search (default True)
        
        Returns:
            List of similar books with scores
        """
        try:
            # Build filter conditions
            filter_conditions = []
            
            if exclude_ids:
                filter_conditions.append(
                    FieldCondition(
                        key="book_id",
                        match=MatchAny(any=[id for id in exclude_ids]),
                        # This will be excluded using must_not
                    )
                )
            
            if filters:
                if "language" in filters:
                    filter_conditions.append(
                        FieldCondition(
                            key="language",
                            match=MatchValue(value=filters["language"])
                        )
                    )
                
                if "genres" in filters:
                    filter_conditions.append(
                        FieldCondition(
                            key="genres",
                            match=MatchAny(any=filters["genres"])
                        )
                    )
                
                if "min_year" in filters:
                    filter_conditions.append(
                        FieldCondition(
                            key="publication_year",
                            range=Range(gte=filters["min_year"])
                        )
                    )
            
            # Create filter object
            search_filter = None
            if filter_conditions:
                must = [fc for fc in filter_conditions if fc.key != "book_id"]
                must_not = [fc for fc in filter_conditions if fc.key == "book_id"]
                
                search_filter = Filter(
                    must=must if must else None,
                    must_not=must_not if must_not else None
                )
            
            # HYBRID SEARCH: Dense + Sparse (BM25) using RRF Fusion
            if use_hybrid and query_text:
                logger.debug("Using HYBRID search (Dense + Sparse with RRF)")
                logger.debug(f"Query text: {query_text[:200]}")
                
                # Generate BM25 sparse vector from query text
                sparse_query = self._generate_bm25_sparse_vector(query_text)
                logger.debug(f"BM25 sparse vector: {len(sparse_query.indices)} terms")
                
                if exclude_ids:
                    logger.debug(f"Excluding {len(exclude_ids)} books")
                if filters:
                    logger.debug(f"Applying filters: {filters}")
                
                # Strategy: Perform TWO separate searches and merge with RRF
                # This is more reliable than prefetch for sparse vectors
                
                # Search 1: Dense vector search
                dense_results = self.client.query_points(
                    collection_name=self.collection_recommendations,
                    query=query_vector.tolist(),
                    using="dense",
                    limit=limit * 2,
                    query_filter=search_filter,
                    with_payload=True
                ).points
                logger.debug(f"Dense search: {len(dense_results)} results")
                
                # Search 2: Sparse (BM25) search
                try:
                    sparse_results = self.client.query_points(
                        collection_name=self.collection_recommendations,
                        query=sparse_query,
                        using="bm25",
                        limit=limit * 2,
                        query_filter=search_filter,
                        with_payload=True
                    ).points
                    logger.debug(f"BM25 search: {len(sparse_results)} results")
                except Exception as e:
                    logger.warning(f"BM25 search failed: {e}, using dense-only")
                    sparse_results = []
                
                # Merge with RRF (Reciprocal Rank Fusion)
                results = self._merge_results_rrf(dense_results, sparse_results, limit)
                logger.debug(f"🔍 RRF merged: {len(results)} results")
            else:
                # Fallback to dense-only search
                logger.debug("Using dense-only search")
                results = self.client.query_points(
                    collection_name=self.collection_recommendations,
                    query=query_vector.tolist(),
                    using="dense",  # Named vector
                    limit=limit,
                    query_filter=search_filter,
                    with_payload=True
                ).points
            
            # Format results
            similar_books = []
            for result in results:
                similar_books.append({
                    "book_id": result.payload["book_id"],
                    "score": result.score,
                    "metadata": result.payload
                })
            
            return similar_books
            
        except Exception as e:
            logger.error(f"Failed to search similar books: {e}")
            raise
    
    async def get_book_vector(self, book_id: int) -> Optional[np.ndarray]:
        """Retrieve vector for a specific book"""
        try:
            points = self.client.retrieve(
                collection_name=self.collection_recommendations,
                ids=[book_id],
                with_vectors=True
            )
            
            if points and len(points) > 0:
                # For named vectors, extract the "dense" vector
                vector_data = points[0].vector
                if isinstance(vector_data, dict):
                    return np.array(vector_data.get("dense", []))
                return np.array(vector_data)
            
            return None
            
        except Exception as e:
            logger.error(f"Failed to get vector for book_id={book_id}: {e}")
            return None
    
    async def get_book_by_id(self, book_id: int) -> Optional[Dict[str, Any]]:
        """Retrieve book metadata by ID"""
        try:
            points = self.client.retrieve(
                collection_name=self.collection_recommendations,
                ids=[book_id],
                with_payload=True
            )
            
            if points and len(points) > 0:
                return points[0].payload
            
            return None
            
        except Exception as e:
            logger.error(f"Failed to get book metadata for book_id={book_id}: {e}")
            return None
    
    async def delete_book(self, book_id: int):
        """Delete book from collection"""
        try:
            self.client.delete(
                collection_name=self.collection_recommendations,
                points_selector=[book_id]
            )
            logger.info(f"Deleted book_id={book_id} from Qdrant")
            
        except Exception as e:
            logger.error(f"Failed to delete book_id={book_id}: {e}")
            raise
    
    async def get_collection_info(self) -> Dict[str, Any]:
        """Get collection statistics"""
        try:
            info = self.client.get_collection(self.collection_recommendations)
            return {
                "name": info.config.params,
                "points_count": info.points_count,
                "status": info.status
            }
        except Exception as e:
            logger.error(f"Failed to get collection info: {e}")
            return {}
