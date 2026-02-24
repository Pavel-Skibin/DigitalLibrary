from typing import List, Dict, Any, Set
import numpy as np
from loguru import logger

from app.config import Settings


class MMRService:
    """
    Maximal Marginal Relevance (MMR) for ensuring diversity in recommendations.
    
    MMR balances relevance and diversity by selecting items that are:
    1. Similar to the query (high relevance)
    2. Dissimilar to already selected items (high diversity)
    
    Formula:
        MMR = λ × Sim(item, query) - (1 - λ) × max[Sim(item, selected)]
    """
    
    def __init__(self, settings: Settings):
        self.settings = settings
        self.lambda_param = settings.MMR_DIVERSITY_LAMBDA
        self.max_per_author = settings.MAX_PER_AUTHOR
        self.max_per_genre = settings.MAX_PER_GENRE
    
    def apply_mmr(
        self,
        candidates: List[Dict[str, Any]],
        final_count: int,
        embeddings: np.ndarray = None
    ) -> List[Dict[str, Any]]:
        """
        Apply MMR to rerank candidates for diversity.
        
        Args:
            candidates: List of candidate items with scores
            final_count: Number of items to select
            embeddings: Optional embeddings for similarity computation
        
        Returns:
            List of reranked candidates
        """
        if not candidates:
            return []
        
        if len(candidates) <= final_count:
            return candidates
        
        logger.debug(f"Applying MMR to {len(candidates)} candidates, selecting {final_count}")
        
        # If we have embeddings, use full MMR algorithm
        if embeddings is not None and len(embeddings) == len(candidates):
            return self._mmr_with_embeddings(candidates, embeddings, final_count)
        
        # Otherwise, use heuristic diversity filtering
        return self._diversity_filtering(candidates, final_count)
    
    def _mmr_with_embeddings(
        self,
        candidates: List[Dict[str, Any]],
        embeddings: np.ndarray,
        final_count: int
    ) -> List[Dict[str, Any]]:
        """
        Full MMR algorithm using embedding similarities.
        """
        selected_indices = []
        selected_embeddings = []
        remaining_indices = list(range(len(candidates)))
        
        # Select first item (highest score)
        first_idx = max(remaining_indices, key=lambda i: candidates[i].get("final_score", 0))
        selected_indices.append(first_idx)
        selected_embeddings.append(embeddings[first_idx])
        remaining_indices.remove(first_idx)
        
        # Iteratively select remaining items
        while len(selected_indices) < final_count and remaining_indices:
            best_mmr_score = -float('inf')
            best_idx = None
            
            for idx in remaining_indices:
                # Relevance score (from original ranking)
                relevance = candidates[idx].get("final_score", 0)
                
                # Compute similarity to selected items
                similarities = []
                for selected_emb in selected_embeddings:
                    sim = np.dot(embeddings[idx], selected_emb)
                    similarities.append(sim)
                
                max_similarity = max(similarities) if similarities else 0
                
                # MMR score
                mmr_score = self.lambda_param * relevance - (1 - self.lambda_param) * max_similarity
                
                if mmr_score > best_mmr_score:
                    best_mmr_score = mmr_score
                    best_idx = idx
            
            if best_idx is not None:
                selected_indices.append(best_idx)
                selected_embeddings.append(embeddings[best_idx])
                remaining_indices.remove(best_idx)
            else:
                break
        
        # Return selected candidates in order
        result = [candidates[idx] for idx in selected_indices]
        logger.debug(f"MMR selected {len(result)} diverse items")
        
        return result
    
    def _diversity_filtering(
        self,
        candidates: List[Dict[str, Any]],
        final_count: int
    ) -> List[Dict[str, Any]]:
        """
        Heuristic diversity filtering based on authors and genres.
        
        Rules:
        - Max N books per author
        - Max M books per genre
        - Prefer diversity while maintaining high scores
        """
        selected = []
        author_counts: Dict[str, int] = {}
        genre_counts: Dict[str, int] = {}
        
        # Sort by score
        sorted_candidates = sorted(
            candidates,
            key=lambda x: x.get("final_score", 0),
            reverse=True
        )
        
        for candidate in sorted_candidates:
            if len(selected) >= final_count:
                break
            
            # Check author constraint
            authors = candidate.get("authors", [])
            author_violation = any(
                author_counts.get(author, 0) >= self.max_per_author
                for author in authors
            )
            
            # Check genre constraint
            genres = candidate.get("genres", [])
            genre_violation = any(
                genre_counts.get(genre, 0) >= self.max_per_genre
                for genre in genres
            )
            
            # If constraints are met, add to selection
            if not author_violation and not genre_violation:
                selected.append(candidate)
                
                # Update counts
                for author in authors:
                    author_counts[author] = author_counts.get(author, 0) + 1
                for genre in genres:
                    genre_counts[genre] = genre_counts.get(genre, 0) + 1
        
        # If we didn't get enough items (constraints too strict), fill with remaining high-scoring items
        if len(selected) < final_count:
            remaining = [c for c in sorted_candidates if c not in selected]
            needed = final_count - len(selected)
            selected.extend(remaining[:needed])
        
        logger.debug(f"Diversity filtering: {len(selected)} items selected")
        logger.debug(f"Author distribution: {author_counts}")
        logger.debug(f"Genre distribution: {genre_counts}")
        
        return selected
    
    def ensure_diversity(
        self,
        recommendations: List[Dict[str, Any]]
    ) -> List[Dict[str, Any]]:
        """
        Post-process recommendations to ensure diversity.
        This is a lighter version of MMR that just reorders items.
        """
        if len(recommendations) <= 3:
            return recommendations  # Too few to diversify
        
        diversified = []
        used_authors: Set[str] = set()
        used_genres: Set[str] = set()
        remaining = recommendations.copy()
        
        # First pass: pick diverse items
        while remaining and len(diversified) < len(recommendations):
            best_candidate = None
            best_diversity_score = -1
            
            for candidate in remaining:
                authors = set(candidate.get("authors", []))
                genres = set(candidate.get("genres", []))
                
                # Calculate diversity score (prefer items with new authors/genres)
                new_authors = len(authors - used_authors)
                new_genres = len(genres - used_genres)
                diversity_score = new_authors * 2 + new_genres
                
                # Combine with original score
                original_score = candidate.get("final_score", 0)
                combined_score = original_score * 0.7 + diversity_score * 0.3
                
                if combined_score > best_diversity_score:
                    best_diversity_score = combined_score
                    best_candidate = candidate
            
            if best_candidate:
                diversified.append(best_candidate)
                used_authors.update(best_candidate.get("authors", []))
                used_genres.update(best_candidate.get("genres", []))
                remaining.remove(best_candidate)
            else:
                break
        
        return diversified
