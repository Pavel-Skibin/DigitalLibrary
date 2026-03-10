# book-catalog-service

Управляет каталогом книг: метаданные, авторы, жанры, теги, поиск, обложки, рейтинговый кэш.

**Порт:** 8091

## Технологии

- Spring Boot 3.4, Java 21
- Spring Data JPA, PostgreSQL
- Spring Cloud OpenFeign (клиенты к comment-rating-service)
- SpringDoc OpenAPI (Swagger UI: `/swagger-ui.html`)
- Lombok, MapStruct

## Сущности

### `books`

```
id                INTEGER PK
title             VARCHAR(500) NOT NULL
description       TEXT
publication_year  INTEGER
language          VARCHAR(10) NOT NULL
word_count        INTEGER
publisher         VARCHAR(255)
series_name       VARCHAR(255)
series_number     INTEGER
age_rating        VARCHAR(5)
file_path         VARCHAR NOT NULL   -- путь к .fb2 файлу
average_rating    DECIMAL(3,2) DEFAULT 0
ratings_count     INTEGER DEFAULT 0  -- денормализованный кэш
cover_path        VARCHAR
created_at        TIMESTAMP
```

Индексы: `idx_book_title`, `idx_book_pub_year`, `idx_book_language`, `idx_book_created`

### `authors`

```
id          INTEGER PK
first_name  VARCHAR NOT NULL
last_name   VARCHAR NOT NULL
bio         TEXT
```

### `genres`, `tags`

```
id    INTEGER PK
name  VARCHAR UNIQUE NOT NULL
```

### Связующие таблицы

- `book_authors` (book_id, author_id)
- `book_genres` (book_id, genre_id)
- `book_tags` (book_id, tag_id)

## Сервисы

| Класс               | Ответственность                                                   |
| ------------------- | ----------------------------------------------------------------- |
| `BookServiceImpl`   | CRUD книг, поиск (`searchBooksSimple`), получение FB2, скачивание |
| `AuthorServiceImpl` | CRUD авторов, валидация имён (`InvalidAuthorNameException`)       |
| `BookCoverService`  | Загрузка и получение обложек через storage-service                |

## Поиск книг

`BookRepository.searchBooksSimple(title, authorIds, language, pageable)` — JPQL-запрос с опциональными `LIKE`-фильтрами по названию, `IN`-фильтром по authorIds и фильтром по языку.

## Рейтинговый кэш

`average_rating` и `ratings_count` — денормализованные поля в таблице `books`. Обновляются через endpoint `POST /api/internal/books/{bookId}/ratings` (вызывается из comment-rating-service через OpenFeign при каждом изменении рейтинга). Ручная синхронизация: `POST /api/admin/sync-ratings-cache`.

## API

Подробный API Reference: [../api/book-catalog-api.md](../api/book-catalog-api.md)

Контроллеры:

- `BookController` — `/api/books` (CRUD, поиск, FB2, скачивание, обложки)
- `BookUploadController` — `/api/books/upload` (загрузка через multipart)
- `AuthorController` — `/api/authors`
- `GenreController` — `/api/genres`
- `TagController` — `/api/tags`
- `StatisticsController` — `/api/statistics`
- `AdminController` — `/api/admin`

## Переменные окружения

| Переменная               | Описание                            |
| ------------------------ | ----------------------------------- |
| `SPRING_DATASOURCE_URL`  | JDBC URL PostgreSQL                 |
| `JWT_SECRET`             | Секрет для верификации входящих JWT |
| `APP_BOOKS_STORAGE_PATH` | Путь к папке с FB2-файлами          |
