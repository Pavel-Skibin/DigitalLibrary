# comment-rating-service

Управляет комментариями пользователей к книгам, рейтингами (1–5) и закладками (позиция в тексте).

**Порт:** 8082

## Технологии

- Spring Boot 3.4, Java 21
- Spring Data JPA, PostgreSQL
- Spring Cloud OpenFeign + OkHttp (клиенты к user-service и book-catalog-service)
- Lombok, MapStruct

## Сущности

### `comments`

```
id          INTEGER PK
user_id     INTEGER NOT NULL   -- FK к user-service (без JPA-связи)
book_id     INTEGER NOT NULL   -- FK к book-catalog-service (без JPA-связи)
text        TEXT NOT NULL
created_at  TIMESTAMP NOT NULL
deleted_at  TIMESTAMP          -- soft delete
```

Индексы: `idx_comment_book_id`, `idx_comment_user_id`, `idx_comment_created_at`, `idx_comment_deleted_at`

### `ratings`

```
id       INTEGER PK
user_id  INTEGER NOT NULL
book_id  INTEGER NOT NULL
value    INTEGER NOT NULL  -- 1..5
```

### `bookmarks`

```
id          INTEGER PK
user_id     INTEGER NOT NULL
book_id     INTEGER NOT NULL
position    DOUBLE NOT NULL    -- позиция в тексте (0.0..1.0 или номер символа)
name        VARCHAR(255)
notes       TEXT
created_at  TIMESTAMP NOT NULL
deleted_at  TIMESTAMP
```

## Межсервисное взаимодействие

При создании комментария или рейтинга сервис валидирует существование пользователя и книги:

```
CommentServiceImpl.createComment():
  1. userServiceClient.validateUser(userId)   → ValidationResponse{exists}
  2. bookServiceClient.validateBook(bookId)   → ValidationResponse{exists}
  3. commentRepository.findActiveByUserAndBook(userId, bookId)  -- нет дубликатов
  4. commentRepository.save(comment)
```

После изменения рейтинга вызывается `bookServiceClient.updateRatingCache(bookId, payload)` для обновления денормализованного кэша в book-catalog-service.

## Правила

- Один пользователь — один активный комментарий на книгу (`UNIQUE` на `(user_id, book_id)` с учётом `deleted_at`)
- Удаление комментария — soft delete (установка `deleted_at`)
- Модерация: `DELETE /api/comments/{id}/moderate` — только для `ADMIN`
- `AccessDeniedException` при попытке редактировать/удалять чужой комментарий

## API

Подробный API Reference: [../api/comment-rating-api.md](../api/comment-rating-api.md)

Контроллеры:

- `CommentController` — `/api/comments`
- `RatingController` — `/api/ratings`
- `BookmarkController` — `/api/bookmarks`

## Переменные окружения

| Переменная                 | Описание                                   |
| -------------------------- | ------------------------------------------ |
| `SPRING_DATASOURCE_URL`    | JDBC URL PostgreSQL                        |
| `JWT_SECRET`               | Секрет для верификации входящих JWT        |
| `USER_SERVICE_URL`         | URL user-service для Feign-клиента         |
| `BOOK_CATALOG_SERVICE_URL` | URL book-catalog-service для Feign-клиента |
