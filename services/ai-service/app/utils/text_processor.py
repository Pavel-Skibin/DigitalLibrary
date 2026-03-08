from typing import List, Optional
from app.models.book import BookMetadata


def create_book_text_profile(book: BookMetadata) -> str:
    """
    Create text profile for embedding generation.
    
    Formula:
    {title}
    {description}
    Жанры: {genre1}, {genre2}, {genre3}
    Теги: {tag1}, {tag2}, {tag3}
    Автор: {author1}, {author2}
    Серия: {series_name}
    
    This represents the semantic essence of the book for vector search.
    
    Note: Maximum length is controlled to stay within 512 tokens limit.
          ru-en-RoSBERTa tokenizer: ~1 token ≈ 3-4 chars for Russian text.
          
          Realistic calculation:
          - Prefix "clustering: " → ~3 tokens
          - Title (~100 chars) → ~25 tokens
          - Description (1000 chars) → ~250 tokens
          - Genres (~100 chars) → ~25 tokens
          - Tags (~200 chars) → ~50 tokens
          - Authors (~100 chars) → ~25 tokens
          - Series (~100 chars) → ~25 tokens
          ──────────────────────────────────
          TOTAL: ~400 tokens out of 512
          
          This leaves 20% safety margin while preserving full book descriptions.
    """
    parts = []
    
    # Title (most important)
    if book.title:
        parts.append(book.title)
    
    # Description (preserving full meaning is crucial for recommendations)
    if book.description:
        # Limit to 1000 chars (~250 tokens) - enough for meaningful descriptions
        # while staying well within 512 token limit
        desc = book.description[:1000] if len(book.description) > 1000 else book.description
        parts.append(desc)
    
    # Genres
    if book.genres:
        genres_str = ", ".join(book.genres[:5])  # Limit to 5 genres
        parts.append(f"Жанры: {genres_str}")
    
    # Tags (similar to genres but more specific)
    if book.tags:
        tags_str = ", ".join(book.tags[:10])  # Limit to 10 tags
        parts.append(f"Теги: {tags_str}")
    
    # Authors
    if book.authors:
        authors_str = ", ".join(book.authors)
        parts.append(f"Автор: {authors_str}")
    
    # Series (helps group related books)
    if book.series_name:
        parts.append(f"Серия: {book.series_name}")
    
    # Combine all parts
    text_profile = "\n".join(parts)
    
    return text_profile


def normalize_text(text: str) -> str:
    """Normalize text for embedding (lowercase, remove extra spaces)"""
    # Convert to lowercase
    text = text.lower()
    
    # Remove multiple spaces
    text = " ".join(text.split())
    
    return text


def truncate_text(text: str, max_tokens: int = 512) -> str:
    """
    Truncate text to maximum number of tokens.
    Rough approximation: 1 token ≈ 4 characters for Russian
    """
    max_chars = max_tokens * 4
    if len(text) > max_chars:
        return text[:max_chars] + "..."
    return text


def extract_keywords(text: str, top_n: int = 10) -> List[str]:
    """
    Extract top keywords from text (simple frequency-based approach).
    For more sophisticated extraction, use TF-IDF or KeyBERT.
    """
    # Remove common stop words
    stop_words = {
        "и", "в", "на", "с", "по", "для", "как", "что", "это", "от",
        "к", "о", "у", "а", "но", "или", "же", "бы", "так", "из"
    }
    
    # Tokenize and filter
    words = text.lower().split()
    words = [w.strip(".,!?;:()[]{}\"'") for w in words]
    words = [w for w in words if len(w) > 3 and w not in stop_words]
    
    # Count frequencies
    freq = {}
    for word in words:
        freq[word] = freq.get(word, 0) + 1
    
    # Sort by frequency
    sorted_words = sorted(freq.items(), key=lambda x: x[1], reverse=True)
    
    return [word for word, count in sorted_words[:top_n]]
