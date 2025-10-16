-- Уникальный индекс: только активные пользователи (deleted_at IS NULL) по username
CREATE UNIQUE INDEX IF NOT EXISTS idx_user_username_not_deleted
    ON users (username)
    WHERE deleted_at IS NULL;

-- Уникальный индекс: только активные пользователи по email
CREATE UNIQUE INDEX IF NOT EXISTS idx_user_email_not_deleted
    ON users (email)
    WHERE deleted_at IS NULL;

-- Уникальный индекс: один активный комментарий на книгу от пользователя
CREATE UNIQUE INDEX IF NOT EXISTS idx_comment_unique_active
    ON comments (user_id, book_id)
    WHERE deleted_at IS NULL;
