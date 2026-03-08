-- ============================================
-- User Service Database Schema - Remove Duplicate Bookmarks Table
-- Version: 4.0
-- Description: Remove user_book_bookmarks table as bookmarks are managed in Comment Rating Service
-- ============================================

-- Drop table user_book_bookmarks (functionality handled by Comment Rating Service)
DROP TABLE IF EXISTS user_book_bookmarks CASCADE;

-- Comment
COMMENT ON DATABASE users_db IS 'User service database - bookmarks moved to Comment Rating Service';
