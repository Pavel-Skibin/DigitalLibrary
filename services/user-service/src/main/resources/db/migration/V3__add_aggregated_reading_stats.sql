-- ============================================
-- User Service Database Schema - Aggregated Reading Statistics
-- Version: 3.0
-- Description: Add aggregated reading statistics to user_book_views for recommendation system
-- ============================================

-- =============================================
-- Extend USER_BOOK_VIEWS with aggregated statistics
-- =============================================

-- Add new columns for aggregated reading statistics
ALTER TABLE user_book_views 
ADD COLUMN total_reading_time_seconds INTEGER DEFAULT 0,
ADD COLUMN sessions_count INTEGER DEFAULT 0,
ADD COLUMN last_read_at TIMESTAMP,
ADD COLUMN last_position VARCHAR(255),
ADD COLUMN is_completed BOOLEAN DEFAULT FALSE;

-- Add indexes for efficient querying
CREATE INDEX idx_views_total_reading_time ON user_book_views(total_reading_time_seconds DESC);
CREATE INDEX idx_views_last_read_at ON user_book_views(last_read_at DESC);
CREATE INDEX idx_views_is_completed ON user_book_views(user_id, is_completed);

-- Comments for new columns
COMMENT ON COLUMN user_book_views.total_reading_time_seconds IS 'Общее время чтения книги в секундах (сумма всех сессий)';
COMMENT ON COLUMN user_book_views.sessions_count IS 'Количество сессий чтения этой книги';
COMMENT ON COLUMN user_book_views.last_read_at IS 'Время последней сессии чтения';
COMMENT ON COLUMN user_book_views.last_position IS 'Последняя позиция в книге (для продолжения чтения)';
COMMENT ON COLUMN user_book_views.is_completed IS 'Флаг завершения чтения книги';

-- =============================================
-- Migrate existing data (populate from sessions)
-- =============================================

-- Update aggregated statistics from existing reading sessions
UPDATE user_book_views v
SET 
    total_reading_time_seconds = COALESCE(s.total_time, 0),
    sessions_count = COALESCE(s.session_count, 0),
    last_read_at = s.last_session,
    last_position = s.latest_position
FROM (
    SELECT 
        user_id,
        book_id,
        SUM(COALESCE(duration_seconds, 0)) as total_time,
        COUNT(*) as session_count,
        MAX(COALESCE(ended_at, started_at)) as last_session,
        (ARRAY_AGG(last_position ORDER BY COALESCE(ended_at, started_at) DESC))[1] as latest_position
    FROM user_reading_sessions
    WHERE ended_at IS NOT NULL OR duration_seconds IS NOT NULL
    GROUP BY user_id, book_id
) s
WHERE v.user_id = s.user_id AND v.book_id = s.book_id;

-- =============================================
-- Add constraint: unique user-book pair
-- =============================================

-- Ensure one record per user-book combination for aggregated stats
-- First, remove duplicates if any exist
DELETE FROM user_book_views a
USING user_book_views b
WHERE a.id > b.id 
  AND a.user_id = b.user_id 
  AND a.book_id = b.book_id;

-- Add unique constraint
ALTER TABLE user_book_views 
ADD CONSTRAINT uk_views_user_book UNIQUE (user_id, book_id);

-- Update comment for table
COMMENT ON TABLE user_book_views IS 'История просмотров и агрегированная статистика чтения книг';
