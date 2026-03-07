-- ============================================
-- Book Catalog Service Database Schema
-- Version: 1.0
-- Description: Initial schema for books, authors, genres, tags
-- ============================================

-- Create authors table
CREATE TABLE authors (
    id SERIAL PRIMARY KEY,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL
);

-- Indexes for authors
CREATE INDEX idx_author_first_name ON authors(first_name);
CREATE INDEX idx_author_last_name ON authors(last_name);

-- Create genres table
CREATE TABLE genres (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);

-- Indexes for genres
CREATE INDEX idx_genre_name ON genres(name);

-- Create tags table
CREATE SEQUENCE IF NOT EXISTS tags_id_seq;
CREATE TABLE tags (
    id INTEGER PRIMARY KEY DEFAULT nextval('tags_id_seq'),
    name VARCHAR(50) NOT NULL UNIQUE,
    category VARCHAR(50),
    is_predefined BOOLEAN DEFAULT false,
    usage_count INTEGER DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for tags
CREATE INDEX idx_tag_name ON tags(name);
CREATE INDEX idx_tag_category ON tags(category);
CREATE INDEX idx_tag_predefined ON tags(is_predefined);
CREATE INDEX idx_tag_usage ON tags(usage_count DESC);

-- Create books table
CREATE TABLE books (
    id SERIAL PRIMARY KEY,
    title VARCHAR(500) NOT NULL,
    description TEXT,
    file_path VARCHAR(255) NOT NULL,
    cover_image_path VARCHAR(255),
    publication_year INTEGER,
    language VARCHAR(10) NOT NULL DEFAULT 'ru',
    publisher VARCHAR(255),
    series_name VARCHAR(255),
    series_number INTEGER,
    age_rating VARCHAR(5),
    word_count INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for books
CREATE INDEX idx_book_title ON books(title);
CREATE INDEX idx_book_publication_year ON books(publication_year);
CREATE INDEX idx_book_language ON books(language);
CREATE INDEX idx_book_series ON books(series_name);
CREATE INDEX idx_book_word_count ON books(word_count);
CREATE INDEX idx_book_created_at ON books(created_at);

-- Create book_authors junction table
CREATE TABLE book_authors (
    id SERIAL PRIMARY KEY,
    book_id INTEGER NOT NULL,
    author_id INTEGER NOT NULL,
    CONSTRAINT fk_book_authors_book FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE CASCADE,
    CONSTRAINT fk_book_authors_author FOREIGN KEY (author_id) REFERENCES authors(id) ON DELETE CASCADE
);

-- Indexes for book_authors
CREATE INDEX idx_book_authors_book ON book_authors(book_id);
CREATE INDEX idx_book_authors_author ON book_authors(author_id);

-- Create book_genres junction table
CREATE TABLE book_genres (
    id SERIAL PRIMARY KEY,
    book_id INTEGER NOT NULL,
    genre_id INTEGER NOT NULL,
    CONSTRAINT fk_book_genres_book FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE CASCADE,
    CONSTRAINT fk_book_genres_genre FOREIGN KEY (genre_id) REFERENCES genres(id) ON DELETE CASCADE
);

-- Indexes for book_genres
CREATE INDEX idx_book_genres_book ON book_genres(book_id);
CREATE INDEX idx_book_genres_genre ON book_genres(genre_id);

-- Create book_tags junction table
CREATE SEQUENCE IF NOT EXISTS book_tags_id_seq;
CREATE TABLE book_tags (
    id INTEGER PRIMARY KEY DEFAULT nextval('book_tags_id_seq'),
    book_id INTEGER NOT NULL,
    tag_id INTEGER NOT NULL,
    CONSTRAINT book_tags_book_id_tag_id_key UNIQUE (book_id, tag_id),
    CONSTRAINT fk_book_tags_book FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE CASCADE,
    CONSTRAINT fk_book_tags_tag FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
);

-- Indexes for book_tags
CREATE INDEX idx_book_tags_book ON book_tags(book_id);
CREATE INDEX idx_book_tags_tag ON book_tags(tag_id);

-- Comments
COMMENT ON TABLE books IS 'Book catalog with metadata';
COMMENT ON TABLE authors IS 'Authors of books';
COMMENT ON TABLE genres IS 'Book genres/categories';
COMMENT ON TABLE tags IS 'Book tags for categorization';
COMMENT ON TABLE book_authors IS 'Many-to-many relationship between books and authors';
COMMENT ON TABLE book_genres IS 'Many-to-many relationship between books and genres';
COMMENT ON TABLE book_tags IS 'Many-to-many relationship between books and tags';
