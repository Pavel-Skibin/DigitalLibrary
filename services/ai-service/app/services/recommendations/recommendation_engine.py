from typing import List, Optional, Dict, Any
import numpy as np
from loguru import logger

from app.config import Settings
from app.models.book import BookMetadata, BookRecommendation
from app.models.user import UserPreferences
from app.services.shared.qdrant_service import QdrantService
from app.services.shared.rest_client_service import RestClientService
from app.services.shared.cache_service import CacheService
from app.services.recommendations.embedding_service import EmbeddingService
from app.services.recommendations.mmr_service import MMRService
from app.utils.text_processor import create_book_text_profile
from app.utils.scoring import calculate_hybrid_score, calculate_popularity_score


class RecommendationEngine:
    """
    Main recommendation engine orchestrating all recommendation logic.
    """
    
    def __init__(
        self,
        qdrant_service: QdrantService,
        rest_client_service: RestClientService,
        cache_service: CacheService,
        embedding_service: EmbeddingService,
        settings: Settings
    ):
        self.qdrant = qdrant_service
        self.rest_client = rest_client_service
        self.cache = cache_service
        self.embedding = embedding_service
        self.settings = settings
        self.mmr = MMRService(settings)
    
    async def get_personalized_recommendations(
        self,
        user_id: int,
        limit: int = 10,
        language: Optional[str] = None,
        exclude_read: bool = True
    ) -> List[BookRecommendation]:
        """
        Get personalized recommendations for a user.
        
        Algorithm:
        1. Check cache
        2. Get user's anchor books (highly rated, completed, favorites)
        3. If no anchor books -> cold start
        4. Get embeddings for anchor books from Qdrant
        5. Search for similar books (multi-vector search)
        6. Apply filters (exclude read, language, etc.)
        7. Hybrid scoring (vector similarity + rating quality)
        8. Apply MMR for diversity
        9. Cache results
        """
        logger.info(f"Getting personalized recommendations for user_id={user_id}, limit={limit}")
        
        # Check cache
        cached = await self.cache.get_cached_recommendations(user_id)
        if cached:
            logger.info(f"Returning cached recommendations for user_id={user_id}")
            return [BookRecommendation(**item) for item in cached[:limit]]
        
        # Get user's anchor books WITH WEIGHTS
        anchor_books = await self.rest_client.get_user_anchor_books(user_id, limit=10)
        
        if not anchor_books:
            logger.info(f"No anchor books for user_id={user_id}, using cold start")
            return await self._cold_start_recommendations(user_id, limit, language)
        
        logger.info(f"Found {len(anchor_books)} anchor books with weights: {anchor_books}")
        
        # Get books to exclude (already read)
        exclude_ids = set()
        if exclude_read:
            exclude_ids = await self.rest_client.get_user_read_books(user_id)
            logger.debug(f"Excluding {len(exclude_ids)} already read books")
            
            # Warning if excluding too many books
            if len(exclude_ids) > 1600:
                logger.warning(f"User {user_id} has read {len(exclude_ids)} books — may cause sparse results")
                logger.warning("Consider disabling exclude_read or falling back to cold start")
        
        # Get embeddings for anchor books WITH WEIGHTS
        anchor_embeddings = []
        anchor_weights = []
        anchor_metadata = []  # Collect metadata for BM25 query generation
        
        for item in anchor_books:
            book_id = item["book_id"]
            weight = item["weight"]
            
            embedding = await self.qdrant.get_book_vector(book_id)
            if embedding is not None:
                anchor_embeddings.append(embedding)
                anchor_weights.append(weight)
                
                # Get book metadata from Qdrant
                book_info = await self.qdrant.get_book_by_id(book_id)
                if book_info:
                    anchor_metadata.append(book_info)
                    logger.debug(f"Anchor book {book_id}: {book_info.get('title', 'N/A')}")
                else:
                    logger.warning(f"No metadata found for anchor book {book_id}")
        
        logger.info(f"Collected {len(anchor_metadata)} anchor books with metadata")
        
        if not anchor_embeddings:
            logger.warning(f"No embeddings found for anchor books, using cold start")
            return await self._cold_start_recommendations(user_id, limit, language)
        
        # WEIGHTED average of anchor embeddings (books with higher engagement get more influence)
        anchor_embeddings_array = np.array(anchor_embeddings)
        anchor_weights_array = np.array(anchor_weights)
        
        # Normalize weights to sum to 1
        normalized_weights = anchor_weights_array / anchor_weights_array.sum()
        
        # Weighted average: sum(embedding * weight) for each dimension
        user_preference_vector = np.average(anchor_embeddings_array, axis=0, weights=normalized_weights)
        
        logger.info(f"Created weighted user preference vector (weights: {anchor_weights}, normalized: {normalized_weights.tolist()})")
        
        # Generate query text for BM25 from anchor books
        query_text = self._create_user_preference_text(anchor_metadata)
        logger.debug(f"Generated query text for hybrid search: {query_text[:100]}...")
        
        # Search for similar books using HYBRID search
        filters = {}
        if language:
            filters["language"] = language
        
        similar_books = await self.qdrant.search_similar_books(
            query_vector=user_preference_vector,
            query_text=query_text,  # Enable hybrid search
            limit=self.settings.TOP_K_SIMILAR,
            exclude_ids=list(exclude_ids),
            filters=filters,
            use_hybrid=True
        )
        
        if not similar_books:
            logger.warning("No similar books found, using popular books")
            return await self._cold_start_recommendations(user_id, limit, language)
        
        logger.debug(f"Found {len(similar_books)} similar books")
        
        # Convert to recommendations with hybrid scoring
        recommendations = []
        for item in similar_books:
            metadata = item["metadata"]
            vector_score = item["score"]
            
            # Calculate hybrid score
            final_score = calculate_hybrid_score(
                vector_similarity=vector_score,
                avg_rating=metadata.get("avg_rating") or 0.0,
                ratings_count=metadata.get("ratings_count") or 0,
                alpha=0.7
            )
            
            rec = BookRecommendation(
                book_id=metadata["book_id"],
                title=metadata["title"],
                authors=metadata.get("authors", []),
                genres=metadata.get("genres", []),
                cover_image_path=metadata.get("cover_image_path"),
                average_rating=metadata.get("avg_rating"),
                ratings_count=metadata.get("ratings_count", 0),
                views_count=0,
                similarity_score=vector_score,
                final_score=final_score,
                reason="На основе книг, которые вам понравились"
            )
            recommendations.append(rec)
        
        # Sort by final score
        recommendations.sort(key=lambda x: x.final_score, reverse=True)
        
        # Apply MMR for diversity
        recommendations_dict = [rec.dict() for rec in recommendations]
        diverse_recommendations = self.mmr.apply_mmr(
            candidates=recommendations_dict,
            final_count=limit
        )
        
        # Convert back to BookRecommendation
        final_recommendations = [BookRecommendation(**item) for item in diverse_recommendations]
        
        # Cache results
        cache_data = [rec.dict() for rec in final_recommendations]
        await self.cache.cache_recommendations(user_id, cache_data)
        
        logger.info(f"Returning {len(final_recommendations)} personalized recommendations")
        
        return final_recommendations
    
    async def get_similar_to_book(
        self,
        book_id: int,
        limit: int = 10
    ) -> List[BookRecommendation]:
        """
        Get books similar to a specific book.
        """
        logger.info(f"Getting similar books for book_id={book_id}, limit={limit}")
        
        # Check cache
        cached = await self.cache.get_cached_similar_books(book_id)
        if cached:
            logger.info(f"Returning cached similar books for book_id={book_id}")
            return [BookRecommendation(**item) for item in cached[:limit]]
        
        # Get book embedding
        book_embedding = await self.qdrant.get_book_vector(book_id)
        
        if book_embedding is None:
            logger.error(f"No embedding found for book_id={book_id}")
            return []
        
        # Search for similar books
        similar_books = await self.qdrant.search_similar_books(
            query_vector=book_embedding,
            limit=limit + 1,  # +1 because the book itself might be included
            exclude_ids=[book_id]
        )
        
        # Convert to recommendations
        recommendations = []
        for item in similar_books[:limit]:
            metadata = item["metadata"]
            
            rec = BookRecommendation(
                book_id=metadata["book_id"],
                title=metadata["title"],
                authors=metadata.get("authors", []),
                genres=metadata.get("genres", []),
                cover_image_path=metadata.get("cover_image_path"),
                average_rating=metadata.get("avg_rating"),
                ratings_count=metadata.get("ratings_count", 0),
                views_count=0,
                similarity_score=item["score"],
                final_score=item["score"],  # For similar books, use pure similarity
                reason="Похожее на эту книгу"
            )
            recommendations.append(rec)
        
        # Cache results
        cache_data = [rec.dict() for rec in recommendations]
        await self.cache.cache_similar_books(book_id, cache_data)
        
        logger.info(f"Returning {len(recommendations)} similar books")
        
        return recommendations

    async def get_similar_to_books(
        self,
        book_ids: List[int],
        limit: int = 10,
        exclude_ids: Optional[List[int]] = None,
    ) -> List[BookRecommendation]:
        """
        Get books similar to a set of books (multi-anchor).

        Algorithm:
          1. Fetch dense vector for each anchor book from Qdrant.
          2. Average the vectors (equal weights — все якоря равнозначны).
          3. Build BM25 query text from anchor metadata.
          4. Hybrid search (dense + BM25) in books_recommendations.
          5. Hybrid scoring (vector similarity + rating).
          6. MMR for diversity.

        Args:
            book_ids:    List of anchor book IDs (1..N).
            limit:       How many results to return.
            exclude_ids: IDs to exclude (usually the anchor books themselves).
        """
        logger.info(f"get_similar_to_books: anchors={book_ids}, limit={limit}")

        # ── 1. Collect embeddings ─────────────────────────────────────────────
        embeddings: List[np.ndarray] = []
        anchor_metadata: List[dict] = []
        not_found: List[int] = []

        for bid in book_ids:
            vec = await self.qdrant.get_book_vector(bid)
            if vec is not None:
                embeddings.append(vec)
                meta = await self.qdrant.get_book_by_id(bid)
                if meta:
                    anchor_metadata.append(meta)
            else:
                not_found.append(bid)

        if not_found:
            logger.warning(f"No embeddings for book_ids={not_found}")

        if not embeddings:
            logger.error("No embeddings found for any anchor book")
            return []

        # ── 2. Average vector ─────────────────────────────────────────────────
        query_vector = np.mean(np.stack(embeddings), axis=0)

        # ── 3. BM25 query text from anchor metadata ────────────────────────────
        query_text = self._create_user_preference_text(anchor_metadata) if anchor_metadata else None

        # ── 4. Hybrid search ──────────────────────────────────────────────────
        exclude = list(set((exclude_ids or []) + book_ids))
        similar = await self.qdrant.search_similar_books(
            query_vector=query_vector,
            query_text=query_text,
            limit=self.settings.TOP_K_SIMILAR,
            exclude_ids=exclude,
            use_hybrid=bool(query_text),
        )

        if not similar:
            logger.warning("No similar books found for multi-anchor query")
            return []

        # ── 5. Hybrid scoring ─────────────────────────────────────────────────
        recommendations: List[BookRecommendation] = []
        for item in similar:
            meta = item["metadata"]
            vscore = item["score"]
            final_score = calculate_hybrid_score(
                vector_similarity=vscore,
                avg_rating=meta.get("avg_rating") or 0.0,
                ratings_count=meta.get("ratings_count") or 0,
                alpha=0.7,
            )
            # Build human-readable reason from anchor titles
            if anchor_metadata:
                anchor_titles = [m.get("title", "") for m in anchor_metadata if m.get("title")]
                reason_str = "Похоже на: " + ", ".join(f"«{t}»" for t in anchor_titles[:3])
            else:
                reason_str = "Похожее на выбранные книги"

            recommendations.append(BookRecommendation(
                book_id=meta["book_id"],
                title=meta["title"],
                authors=meta.get("authors", []),
                genres=meta.get("genres", []),
                cover_image_path=meta.get("cover_image_path"),
                average_rating=meta.get("avg_rating"),
                ratings_count=meta.get("ratings_count", 0),
                views_count=0,
                similarity_score=vscore,
                final_score=final_score,
                reason=reason_str,
            ))

        recommendations.sort(key=lambda x: x.final_score, reverse=True)

        # ── 6. MMR diversity ──────────────────────────────────────────────────
        diverse = self.mmr.apply_mmr(
            candidates=[r.dict() for r in recommendations],
            final_count=limit,
        )
        return [BookRecommendation(**d) for d in diverse]

    async def get_popular_books(
        self,
        limit: int = 10,
        genre: Optional[str] = None
    ) -> List[BookRecommendation]:
        """
        Get popular books (for cold start or browse).
        """
        logger.info(f"Getting popular books, limit={limit}, genre={genre}")
        
        # Check cache
        cached = await self.cache.get_cached_popular_books(genre)
        if cached:
            return [BookRecommendation(**item) for item in cached[:limit]]
        
        # Get popular books from database
        books = await self.rest_client.get_popular_books(limit=limit, genre=genre)
        
        # Convert to recommendations
        recommendations = []
        for book in books:
            popularity_score = calculate_popularity_score(
                avg_rating=book.average_rating or 0,
                ratings_count=book.ratings_count
            )
            
            rec = BookRecommendation(
                book_id=book.id,
                title=book.title,
                authors=book.authors,
                genres=book.genres,
                cover_image_path=book.cover_image_path,
                average_rating=book.average_rating,
                ratings_count=book.ratings_count,
                views_count=0,  # TODO: Add views tracking
                similarity_score=0.0,
                final_score=popularity_score,
                reason="Популярное сейчас"
            )
            recommendations.append(rec)
        
        # Cache results
        cache_data = [rec.dict() for rec in recommendations]
        await self.cache.cache_popular_books(cache_data, genre)
        
        return recommendations
    
    async def get_new_releases(
        self,
        limit: int = 10,
        min_year: int = 2024
    ) -> List[BookRecommendation]:
        """
        Get new book releases.
        """
        logger.info(f"Getting new releases, limit={limit}, min_year={min_year}")
        
        books = await self.rest_client.get_new_releases(limit=limit, min_year=min_year)
        
        recommendations = []
        for book in books:
            rec = BookRecommendation(
                book_id=book.id,
                title=book.title,
                authors=book.authors,
                genres=book.genres,
                cover_image_path=book.cover_image_path,
                average_rating=book.average_rating,
                ratings_count=book.ratings_count,
                views_count=0,
                similarity_score=0.0,
                final_score=book.average_rating or 0.0,
                reason="Новинка"
            )
            recommendations.append(rec)
        
        return recommendations
    
    async def _cold_start_recommendations(
        self,
        user_id: int,
        limit: int,
        language: Optional[str] = None
    ) -> List[BookRecommendation]:
        """
        Cold start recommendations for new users or users without history.
        
        Strategy:
        1. Get user preferences (if any)
        2. If has favorite genres -> popular in those genres
        3. Otherwise -> overall popular books
        4. Mix with new releases
        """
        logger.info(f"Cold start recommendations for user_id={user_id}")
        
        # Get user preferences
        preferences = await self.rest_client.get_user_preferences(user_id)
        
        # Try genre-based recommendations if user has preferences
        if preferences.favorite_genres:
            top_genre = preferences.favorite_genres[0]["genre"]
            logger.debug(f"User's favorite genre: {top_genre}")
            popular_books = await self.get_popular_books(limit=limit, genre=top_genre)
        else:
            # General popular books
            popular_books = await self.get_popular_books(limit=limit)
        
        # Mix with new releases (20% of results)
        new_releases_count = max(1, limit // 5)
        new_releases = await self.get_new_releases(limit=new_releases_count)
        
        # Combine
        combined = popular_books + new_releases
        combined = combined[:limit]
        
        return combined
    
    async def generate_and_store_embedding(self, book_id: int) -> bool:
        """
        Generate embedding for a book and store in Qdrant.
        
        Returns:
            bool: True if successful, False otherwise
        """
        try:
            logger.info(f"Generating embedding for book_id={book_id}")
            
            # Get book metadata
            books = await self.rest_client.get_books_metadata([book_id])
            if not books:
                logger.error(f"Book not found: book_id={book_id}")
                return False
            
            book = books[0]
            
            # Create text profile
            text_profile = create_book_text_profile(book)
            
            # Check cache
            cached_embedding = await self.cache.get_cached_embedding(text_profile)
            if cached_embedding is not None:
                embedding = cached_embedding
                logger.debug(f"Using cached embedding for book_id={book_id}")
            else:
                # Generate embedding
                embedding = self.embedding.get_embedding(text_profile)
                
                # Cache embedding
                await self.cache.cache_embedding(text_profile, embedding)
            
            # Prepare metadata for Qdrant
            metadata = {
                "book_id": book.id,
                "title": book.title,
                "authors": book.authors,
                "genres": book.genres,
                "tags": book.tags,
                "language": book.language,
                "publication_year": book.publication_year,
                "age_rating": book.age_rating,
                "series_name": book.series_name,
                "word_count": book.word_count,
                "average_rating": float(book.average_rating) if book.average_rating else None,
                "ratings_count": book.ratings_count,
                "cover_image_path": book.cover_image_path
            }
            
            # Store in Qdrant with HYBRID vectors (Dense + BM25 Sparse)
            await self.qdrant.upsert_book_embedding(
                book_id=book_id,
                embedding=embedding,
                metadata=metadata,
                text_profile=text_profile  # For BM25 sparse vector generation
            )
            
            logger.info(f"Successfully generated and stored embedding for book_id={book_id}")
            return True
            
        except Exception as e:
            logger.error(f"Failed to generate embedding for book_id={book_id}: {e}")
            return False
    
    async def batch_generate_embeddings(
        self,
        book_ids: List[int],
        batch_size: int = 32
    ) -> Dict[str, Any]:
        """
        Generate embeddings for multiple books in batches.
        
        Returns:
            Dict with success/failure statistics
        """
        logger.info(f"Batch generating embeddings for {len(book_ids)} books")
        
        success_count = 0
        failed_ids = []
        
        # Process in batches
        for i in range(0, len(book_ids), batch_size):
            batch_ids = book_ids[i:i + batch_size]
            logger.info(f"Processing batch {i // batch_size + 1}: {len(batch_ids)} books")
            
            # Get metadata for batch
            books = await self.rest_client.get_books_metadata(batch_ids)
            
            if not books:
                logger.warning(f"No books found for batch {i // batch_size + 1}")
                failed_ids.extend(batch_ids)
                continue
            
            # Create text profiles
            text_profiles = [create_book_text_profile(book) for book in books]
            
            # Generate embeddings
            try:
                embeddings = self.embedding.get_embeddings_batch(text_profiles)
                
                # Prepare for batch upsert
                embedding_data = []
                for book, embedding, text_profile in zip(books, embeddings, text_profiles):
                    metadata = {
                        "book_id": book.id,
                        "title": book.title,
                        "authors": book.authors,
                        "genres": book.genres,
                        "tags": book.tags,
                        "language": book.language,
                        "publication_year": book.publication_year,
                        "age_rating": book.age_rating,
                        "series_name": book.series_name,
                        "word_count": book.word_count,
                        "average_rating": float(book.average_rating) if book.average_rating else None,
                        "ratings_count": book.ratings_count
                    }
                    
                    embedding_data.append({
                        "book_id": book.id,
                        "embedding": embedding,
                        "metadata": metadata,
                        "text_profile": text_profile  # передаём для генерации BM25-запроса
                    })
                
                # Batch upsert to Qdrant
                await self.qdrant.batch_upsert_embeddings(embedding_data)
                
                success_count += len(books)
                logger.info(f"Batch {i // batch_size + 1} completed successfully")
                
            except Exception as e:
                logger.error(f"Failed to process batch {i // batch_size + 1}: {e}")
                failed_ids.extend(batch_ids)
        
        result = {
            "total": len(book_ids),
            "success": success_count,
            "failed": len(failed_ids),
            "failed_ids": failed_ids
        }
        
        logger.info(f"Batch embedding generation completed: {result}")
        
        return result
    
    def _create_user_preference_text(self, anchor_books: List[Dict[str, Any]]) -> str:
        """
        Create query text for BM25 hybrid search from user's anchor books.
        
        Combines titles, genres, and authors from favorite books to create
        a text representation of user preferences for keyword-based search.
        """
        if not anchor_books:
            return ""
        
        parts = []
        
        # Collect titles (most important for keyword matching)
        titles = [book.get("title", "") for book in anchor_books if book.get("title")]
        if titles:
            parts.extend(titles[:3])  # Top 3 titles
        
        # Collect genres
        all_genres = []
        for book in anchor_books:
            genres = book.get("genres", [])
            if isinstance(genres, list):
                all_genres.extend(genres)
        
        if all_genres:
            # Count genre frequency
            from collections import Counter
            genre_counts = Counter(all_genres)
            top_genres = [genre for genre, _ in genre_counts.most_common(3)]
            parts.append(" ".join(top_genres))
        
        # Collect authors (secondary importance)
        all_authors = []
        for book in anchor_books:
            authors = book.get("authors", [])
            if isinstance(authors, list):
                all_authors.extend(authors)
        
        if all_authors:
            from collections import Counter
            author_counts = Counter(all_authors)
            top_authors = [author for author, _ in author_counts.most_common(2)]
            parts.append(" ".join(top_authors))
        
        # Join all parts
        query_text = " ".join(parts)
        
        # Limit to reasonable length (BM25 works better with focused queries)
        if len(query_text) > 500:
            query_text = query_text[:500]
        
        return query_text
    
    async def invalidate_cache(self, user_id: int):
        """
        Invalidate cached recommendations for a user.
        
        Should be called when:
        - User rates a book
        - User adds a book to favorites
        - User completes reading a book
        - User's reading preferences change significantly
        
        This ensures fresh recommendations on next request.
        """
        try:
            await self.cache.invalidate_user_recommendations(user_id)
            logger.info(f"Cache invalidated for user_id={user_id}")
        except Exception as e:
            logger.error(f"Failed to invalidate cache for user_id={user_id}: {e}")
            raise

