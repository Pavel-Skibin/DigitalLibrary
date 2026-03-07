-- ============================================
-- Comment Rating Service Database Schema
-- Version: 1.0
-- Description: Initial schema for comments, ratings, bookmarks
-- ============================================

-- Create comments table
-- NOTE: user_id and book_id are NOT foreign keys - they reference other microservices
CREATE TABLE comments (
    id SERIAL PRIMARY KEY,
    text VARCHAR(255) NOT NULL,
    user_id INTEGER NOT NULL,
    book_id INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

-- Indexes for comments
CREATE INDEX idx_comment_user_id ON comments(user_id);
CREATE INDEX idx_comment_book_id ON comments(book_id);
CREATE INDEX idx_comment_created_at ON comments(created_at);
CREATE INDEX idx_comment_deleted_at ON comments(deleted_at);

-- Create ratings table
-- NOTE: user_id and book_id are NOT foreign keys - they reference other microservices
CREATE TABLE ratings (
    id SERIAL PRIMARY KEY,
    rating_value INTEGER NOT NULL,
    user_id INTEGER NOT NULL,
    book_id INTEGER NOT NULL,
    CONSTRAINT ratings_rating_value_check CHECK (rating_value >= 1 AND rating_value <= 5),
    CONSTRAINT uk_rating_user_book UNIQUE (user_id, book_id)
);

-- Indexes for ratings
CREATE INDEX idx_rating_user_id ON ratings(user_id);
CREATE INDEX idx_rating_book_id ON ratings(book_id);

-- Create bookmarks table
-- NOTE: user_id and book_id are NOT foreign keys - they reference other microservices
CREATE TABLE bookmarks (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255),
    notes TEXT,
    position DOUBLE PRECISION NOT NULL,
    user_id INTEGER NOT NULL,
    book_id INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

-- Indexes for bookmarks
CREATE INDEX idx_bookmark_user_id ON bookmarks(user_id);
CREATE INDEX idx_bookmark_book_id ON bookmarks(book_id);
CREATE INDEX idx_bookmark_deleted_at ON bookmarks(deleted_at);

-- Comments
COMMENT ON TABLE comments IS 'User comments on books - user_id and book_id reference other microservices';
COMMENT ON TABLE ratings IS 'User ratings (1-5) for books - user_id and book_id reference other microservices';
COMMENT ON TABLE bookmarks IS 'User bookmarks in books - user_id and book_id reference other microservices';
COMMENT ON COLUMN comments.deleted_at IS 'Soft delete timestamp';
COMMENT ON COLUMN bookmarks.deleted_at IS 'Soft delete timestamp';
