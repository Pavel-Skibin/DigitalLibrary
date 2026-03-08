import numpy as np
from typing import List, Dict, Any
from loguru import logger


def calculate_hybrid_score(
    vector_similarity: float,
    avg_rating: float,
    ratings_count: int,
    alpha: float = 0.7
) -> float:
    """
    Calculate hybrid score combining vector similarity and rating quality.
    
    Formula:
        Final_Score = alpha × similarity + (1 - alpha) × normalized_rating
    
    Where:
        normalized_rating = (avg_rating / 5.0) × log_scale(ratings_count)
    
    Args:
        vector_similarity: Cosine similarity from Qdrant (0 to 1)
        avg_rating: Average rating (0 to 5)
        ratings_count: Number of ratings
        alpha: Weight for similarity vs rating (default 0.7)
    
    Returns:
        float: Final hybrid score (0 to 1)
    """
    # Handle None values
    if ratings_count is None:
        ratings_count = 0
    if avg_rating is None:
        avg_rating = 0.0
    
    # Normalize rating to 0-1 scale
    normalized_rating = avg_rating / 5.0 if avg_rating else 0.0
    
    # Apply logarithmic scaling to ratings count to avoid popularity bias
    # Books with 1 rating vs 100 ratings: log(1+1) = 0.69, log(100+1) = 4.62
    # Normalize to 0-1 by dividing by log(max_reasonable_ratings + 1)
    max_ratings_log = np.log(1000 + 1)  # Assume 1000 is "very popular"
    ratings_weight = np.log(ratings_count + 1) / max_ratings_log
    ratings_weight = min(ratings_weight, 1.0)  # Cap at 1.0
    
    # Combine rating value with popularity
    quality_score = normalized_rating * (0.7 + 0.3 * ratings_weight)
    
    # Final hybrid score
    final_score = alpha * vector_similarity + (1 - alpha) * quality_score
    
    return float(final_score)


def calculate_popularity_score(
    avg_rating: float,
    ratings_count: int
) -> float:
    """
    Calculate popularity score for cold start recommendations.
    
    Formula:
        popularity = avg_rating × log(ratings_count + 1)
    
    This balances quality (rating) with popularity (count).
    """
    # Handle None values
    if ratings_count is None:
        ratings_count = 0
    if avg_rating is None:
        avg_rating = 0.0
    
    if not avg_rating or ratings_count == 0:
        return 0.0
    
    score = avg_rating * np.log(ratings_count + 1)
    return float(score)


def normalize_scores(scores: List[float]) -> List[float]:
    """
    Normalize scores to 0-1 range using min-max scaling.
    """
    if not scores:
        return []
    
    min_score = min(scores)
    max_score = max(scores)
    
    if max_score == min_score:
        return [0.5] * len(scores)  # All scores are equal
    
    normalized = [(s - min_score) / (max_score - min_score) for s in scores]
    return normalized


def calculate_diversity_penalty(
    candidate_genres: List[str],
    selected_genres: List[str]
) -> float:
    """
    Calculate penalty for genre diversity (used in MMR).
    
    Returns:
        float: Penalty factor (0 to 1, lower means more penalty)
    """
    if not selected_genres:
        return 1.0  # No penalty for first item
    
    # Count genre overlap
    overlap = len(set(candidate_genres) & set(selected_genres))
    total = len(set(candidate_genres) | set(selected_genres))
    
    if total == 0:
        return 1.0
    
    # Higher overlap = higher penalty
    similarity = overlap / total
    penalty = 1.0 - (similarity * 0.5)  # Reduce score by up to 50%
    
    return penalty


def rerank_by_diversity(
    candidates: List[Dict[str, Any]],
    score_key: str = "final_score"
) -> List[Dict[str, Any]]:
    """
    Rerank candidates to ensure diversity without MMR.
    Simply interleave different authors/genres.
    """
    if not candidates:
        return []
    
    # Sort by score initially
    sorted_candidates = sorted(candidates, key=lambda x: x.get(score_key, 0), reverse=True)
    
    reranked = []
    authors_used = {}
    genres_used = {}
    
    # First pass: diversify
    for candidate in sorted_candidates:
        authors = candidate.get("authors", [])
        genres = candidate.get("genres", [])
        
        # Count usage
        author_count = sum(authors_used.get(a, 0) for a in authors)
        genre_count = sum(genres_used.get(g, 0) for g in genres)
        
        # Penalize if overused
        penalty = 1.0
        if author_count > 0:
            penalty *= 0.9 ** author_count
        if genre_count > 0:
            penalty *= 0.95 ** genre_count
        
        candidate["diversity_score"] = candidate.get(score_key, 0) * penalty
        
        # Update counts
        for author in authors:
            authors_used[author] = authors_used.get(author, 0) + 1
        for genre in genres:
            genres_used[genre] = genres_used.get(genre, 0) + 1
        
        reranked.append(candidate)
    
    # Sort by diversity_score
    reranked.sort(key=lambda x: x["diversity_score"], reverse=True)
    
    logger.debug(f"Reranked {len(reranked)} candidates for diversity")
    
    return reranked
