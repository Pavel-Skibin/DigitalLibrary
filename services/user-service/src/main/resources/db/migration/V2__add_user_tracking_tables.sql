-- ============================================
-- User Service Database Schema - User Tracking
-- Version: 2.0
-- Description: Add tables for user tracking, reading sessions, bookmarks, and favorites
-- ============================================

-- =============================================
-- 1. USER_BOOK_VIEWS: История просмотров книг
-- =============================================
CREATE TABLE user_book_views (
    id BIGSERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    book_id INTEGER NOT NULL,
    viewed_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_views_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Indexes for user_book_views
CREATE INDEX idx_views_user_id ON user_book_views(user_id);
CREATE INDEX idx_views_book_id ON user_book_views(book_id);
CREATE INDEX idx_views_user_book ON user_book_views(user_id, book_id);
CREATE INDEX idx_views_timestamp ON user_book_views(viewed_at DESC);

-- Comment
COMMENT ON TABLE user_book_views IS 'История просмотров страниц книг пользователями';
COMMENT ON COLUMN user_book_views.viewed_at IS 'Время просмотра страницы книги';


-- =============================================
-- 2. USER_READING_SESSIONS: Сессии чтения книг
-- =============================================
CREATE TABLE user_reading_sessions (
    id BIGSERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    book_id INTEGER NOT NULL,
    started_at TIMESTAMP NOT NULL DEFAULT NOW(),
    ended_at TIMESTAMP,
    duration_seconds INTEGER,
    last_position VARCHAR(255),
    is_significant BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_sessions_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Indexes for user_reading_sessions
CREATE INDEX idx_sessions_user_id ON user_reading_sessions(user_id);
CREATE INDEX idx_sessions_book_id ON user_reading_sessions(book_id);
CREATE INDEX idx_sessions_user_book ON user_reading_sessions(user_id, book_id);
CREATE INDEX idx_sessions_significant ON user_reading_sessions(user_id, book_id, is_significant) WHERE is_significant = TRUE;
CREATE INDEX idx_sessions_started ON user_reading_sessions(started_at DESC);

-- Comments
COMMENT ON TABLE user_reading_sessions IS 'Сессии чтения книг (отслеживание времени)';
COMMENT ON COLUMN user_reading_sessions.started_at IS 'Время начала чтения';
COMMENT ON COLUMN user_reading_sessions.ended_at IS 'Время окончания (NULL если ещё читает)';
COMMENT ON COLUMN user_reading_sessions.duration_seconds IS 'Длительность сессии в секундах';
COMMENT ON COLUMN user_reading_sessions.last_position IS 'Последняя позиция в книге (CFI для EPUB)';
COMMENT ON COLUMN user_reading_sessions.is_significant IS 'Значимая сессия (>= 180 сек)';


-- =============================================
-- 3. USER_BOOK_BOOKMARKS: Закладки в книгах
-- =============================================
CREATE TABLE user_book_bookmarks (
    id BIGSERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    book_id INTEGER NOT NULL,
    position VARCHAR(255) NOT NULL,
    chapter_title VARCHAR(500),
    note TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    CONSTRAINT fk_bookmarks_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uk_bookmarks_user_book_position UNIQUE (user_id, book_id, position)
);

-- Indexes for user_book_bookmarks
CREATE INDEX idx_bookmarks_user_book ON user_book_bookmarks(user_id, book_id);
CREATE INDEX idx_bookmarks_created ON user_book_bookmarks(created_at DESC);

-- Comments
COMMENT ON TABLE user_book_bookmarks IS 'Закладки пользователей в книгах';
COMMENT ON COLUMN user_book_bookmarks.position IS 'Позиция в книге (EPUB CFI, номер страницы)';
COMMENT ON COLUMN user_book_bookmarks.chapter_title IS 'Название главы';
COMMENT ON COLUMN user_book_bookmarks.note IS 'Заметка пользователя к закладке';


-- =============================================
-- 4. USER_BOOK_FAVORITES: Избранные книги (лайки)
-- =============================================
CREATE TABLE user_book_favorites (
    id BIGSERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    book_id INTEGER NOT NULL,
    added_at TIMESTAMP DEFAULT NOW(),
    CONSTRAINT fk_favorites_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uk_favorites_user_book UNIQUE (user_id, book_id)
);

-- Indexes for user_book_favorites
CREATE INDEX idx_favorites_user_id ON user_book_favorites(user_id);
CREATE INDEX idx_favorites_book_id ON user_book_favorites(book_id);
CREATE INDEX idx_favorites_added ON user_book_favorites(added_at DESC);

-- Comments
COMMENT ON TABLE user_book_favorites IS 'Избранные книги пользователей (лайки)';
COMMENT ON COLUMN user_book_favorites.added_at IS 'Время добавления в избранное';
