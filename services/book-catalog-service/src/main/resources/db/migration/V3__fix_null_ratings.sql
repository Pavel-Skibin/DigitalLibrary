-- V3: Обновление NULL рейтингов на 0.00 для правильной сортировки
-- Дата: 2026-02-17

-- Обновить все книги с NULL рейтингом на 0.00
UPDATE books
SET average_rating = 0.00
WHERE average_rating IS NULL;

-- Обновить все книги с NULL количеством оценок на 0
UPDATE books
SET ratings_count = 0
WHERE ratings_count IS NULL;

-- Установить значения по умолчанию для новых записей (опционально, если еще не установлено)
ALTER TABLE books
    ALTER COLUMN average_rating SET DEFAULT 0.00;

ALTER TABLE books
    ALTER COLUMN ratings_count SET DEFAULT 0;
