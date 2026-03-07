-- ============================================
-- Book Catalog Service Database Migration
-- Version: 2.0
-- Description: Add rating cache columns to books table
-- ============================================

-- Add average_rating column to store cached average rating from Comment Rating Service
ALTER TABLE books
ADD COLUMN average_rating DECIMAL(3, 2) DEFAULT NULL;

-- Add ratings_count column to store cached count of ratings
ALTER TABLE books
ADD COLUMN ratings_count INTEGER DEFAULT 0 NOT NULL;

-- Add index for sorting books by rating
CREATE INDEX idx_book_avg_rating ON books(average_rating DESC NULLS LAST);

-- Add comment for documentation
COMMENT ON COLUMN books.average_rating IS 'Cached average rating (0.00-5.00) from Comment Rating Service, updated on rating changes';
COMMENT ON COLUMN books.ratings_count IS 'Cached count of ratings from Comment Rating Service, updated on rating changes';
