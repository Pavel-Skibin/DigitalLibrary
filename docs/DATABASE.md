# 🗄️ База данных

> [!NOTE]
> Каждый Java-сервис использует **собственную** PostgreSQL-БД. Сервисы не имеют прямых FK-ссылок друг на друга — межсервисная целостность обеспечивается Feign-валидацией при записи.

## 📊 Диаграммы ER

### 👤 `users_db`

```mermaid
erDiagram
    user_roles {
        int id PK
        varchar name "USER | MODERATOR | ADMIN"
    }
    users {
        int id PK
        varchar username "UNIQUE NOT NULL"
        varchar password_hash "NOT NULL"
        varchar email "UNIQUE NOT NULL"
        int role_id FK
        timestamp deleted_at "NULL = soft delete"
    }
    user_reading_sessions {
        bigint id PK
        int user_id "NOT NULL"
        int book_id "NOT NULL (external)"
        timestamp started_at "NOT NULL"
        timestamp ended_at "NULL"
        int duration_seconds "NULL"
        varchar last_position "len 255"
        boolean is_significant "duration >= 180s"
    }
    user_book_favorites {
        bigint id PK
        int user_id "NOT NULL"
        int book_id "NOT NULL (external)"
        timestamp added_at "NOT NULL"
    }
    user_book_views {
        bigint id PK
        int user_id "NOT NULL"
        int book_id "NOT NULL (external)"
        timestamp viewed_at "NOT NULL"
        int total_reading_time_seconds "default 0"
        int sessions_count "default 0"
        timestamp last_read_at "NULL"
        varchar last_position "len 255"
        boolean is_completed "default false"
    }

    user_roles ||--o{ users : "role_id"
    users ||--o{ user_reading_sessions : "user_id"
    users ||--o{ user_book_favorites : "user_id"
    users ||--o{ user_book_views : "user_id"
```

### 📚 `books_db`

```mermaid
erDiagram
    books {
        int id PK
        varchar title "NOT NULL len=500"
        text description "NULL"
        int publication_year "NULL"
        varchar language "NOT NULL default=ru"
        int word_count "NULL"
        varchar publisher "len 255"
        varchar series_name "len 255"
        int series_number "NULL"
        varchar age_rating "0+ | 12+ | 16+ | 18+"
        varchar file_path "NOT NULL"
        varchar cover_image_path "NULL"
        timestamp created_at "NOT NULL"
        decimal average_rating "3,2 — денормализовано"
        int ratings_count "NOT NULL default=0 — денормализовано"
    }
    authors {
        int id PK
        varchar first_name "NOT NULL"
        varchar last_name "NOT NULL"
    }
    genres {
        int id PK
        varchar name "UNIQUE NOT NULL"
    }
    tags {
        int id PK
        varchar name "UNIQUE NOT NULL"
        varchar category "NULL"
        boolean is_predefined "default=false"
        int usage_count "default=0"
        timestamp created_at "NOT NULL"
    }
    book_authors {
        int id PK
        int book_id FK
        int author_id FK
    }
    book_genres {
        int id PK
        int book_id FK
        int genre_id FK
    }
    book_tags {
        int id PK
        int book_id FK
        int tag_id FK
    }

    books ||--o{ book_authors : "book_id (cascade delete)"
    authors ||--o{ book_authors : "author_id (cascade delete)"
    books ||--o{ book_genres : "book_id (cascade delete)"
    genres ||--o{ book_genres : "genre_id (cascade delete)"
    books ||--o{ book_tags : "book_id (cascade delete)"
    tags ||--o{ book_tags : "tag_id (cascade delete)"
```

### 💬 `comments_ratings_db`

```mermaid
erDiagram
    comments {
        int id PK
        int user_id "NOT NULL (external)"
        int book_id "NOT NULL (external)"
        varchar text "NOT NULL len=255"
        timestamp created_at "NOT NULL"
        timestamp deleted_at "NULL — soft delete"
    }
    ratings {
        int id PK
        int user_id "NOT NULL (external)"
        int book_id "NOT NULL (external)"
        int rating_value "NOT NULL 1..5"
    }
    bookmarks {
        int id PK
        int user_id "NOT NULL (external)"
        int book_id "NOT NULL (external)"
        double position "NOT NULL 0.0..1.0"
        varchar name "len 255"
        text notes "NULL"
        timestamp created_at "NOT NULL"
        timestamp deleted_at "NULL — soft delete"
    }
```

> [!IMPORTANT]
> Связей между таблицами нет — `user_id` и `book_id` являются внешними ключами, живущими в других сервисах. Целостность обеспечивается Feign-валидацией при записи.

### 🔍 Qdrant — векторные коллекции

```mermaid
classDiagram
    class RAGChunk {
        +uint64 id
        +int book_id
        +int chunk_index
        +string text
        +float[1024] text_dense
        +SparseVector text_sparse
    }
    class BookEmbedding {
        +uint64 id
        +int book_id
        +string title
        +string authors
        +string genres
        +string series_name
        +string language
        +float[1024] embedding
        +string bm25_text
        +float avg_rating
        +int ratings_count
    }
    class RAGCollection {
        +string name
        +string dense_model = USER-bge-m3
        +string sparse_model = BM25
    }
    class RecommendationsCollection {
        +string name
        +string model = ru-en-RoSBERTa
    }

    RAGCollection "1" --> "*" RAGChunk : contains
    RecommendationsCollection "1" --> "*" BookEmbedding : contains
```

### ⚡ Redis — структура ключей

```mermaid
graph LR
    subgraph chat["Chat (ai-service)"]
        H["chat:history:{session_id}<br/>TYPE: String (JSON)<br/>TTL: CONVERSATION_HISTORY_TTL"]
        C["chat:context:{session_id}<br/>TYPE: String (JSON)<br/>TTL: CONVERSATION_HISTORY_TTL"]
    end
    subgraph recs["Recommendations (ai-service)"]
        R["rec:user:{user_id}<br/>TYPE: Binary (pickle)<br/>TTL: CACHE_TTL_RECOMMENDATIONS"]
        S["similar:book:{book_id}<br/>TYPE: Binary (pickle)<br/>TTL: CACHE_TTL_SIMILAR_BOOKS"]
        P["popular:books:{genre|all}<br/>TYPE: Binary (pickle)<br/>TTL: CACHE_TTL_SIMILAR_BOOKS"]
    end
    subgraph emb["Embeddings Cache"]
        E["emb:hash:{md5(text)}<br/>TYPE: Binary (pickle)<br/>TTL: CACHE_TTL_EMBEDDINGS"]
    end
    subgraph quota["AI Chat Quota"]
        Q["ai:quota:user:{user_id}:{yyyymmdd}<br/>TYPE: Integer<br/>TTL: до полуночи"]
    end
```

---

## 👤 `users_db` — user-service

### `user_roles`

| Колонка | Тип     | Ограничения                                      |
| ------- | ------- | ------------------------------------------------ |
| id      | integer | PK                                               |
| name    | varchar | NOT NULL, UNIQUE (`USER`, `MODERATOR`, `ADMIN`)  |

### `users`

| Колонка       | Тип       | Ограничения                   |
| ------------- | --------- | ----------------------------- |
| id            | integer   | PK, IDENTITY                  |
| username      | varchar   | NOT NULL, UNIQUE              |
| password_hash | varchar   | NOT NULL                      |
| email         | varchar   | NOT NULL, UNIQUE              |
| role_id       | integer   | FK → user_roles(id), NOT NULL |
| deleted_at    | timestamp | NULL (soft delete)            |

**Индексы:** `idx_user_deleted_at`, `idx_user_role_id`

### `user_reading_sessions`

Сессии чтения конкретной книги.

| Колонка          | Тип          | Ограничения                                |
| ---------------- | ------------ | ------------------------------------------ |
| id               | bigint       | PK                                         |
| user_id          | integer      | NOT NULL                                   |
| book_id          | integer      | NOT NULL                                   |
| started_at       | timestamp    | NOT NULL                                   |
| ended_at         | timestamp    | NULL                                       |
| duration_seconds | integer      | NULL                                       |
| last_position    | varchar(255) | NULL                                       |
| is_significant   | boolean      | default false (true если duration ≥ 180 с) |

**Индексы:** `idx_sessions_user_id`, `idx_sessions_book_id`, `idx_sessions_user_book`, `idx_sessions_started`

### `user_book_favorites`

| Колонка  | Тип       | Ограничения |
| -------- | --------- | ----------- |
| id       | bigint    | PK          |
| user_id  | integer   | NOT NULL    |
| book_id  | integer   | NOT NULL    |
| added_at | timestamp | NOT NULL    |

**Constraints:** `uk_favorites_user_book (user_id, book_id)`

### `user_book_views`

Агрегат статистики просмотров книги. Одна запись на пару (user, book).

| Колонка                    | Тип          | Ограничения   |
| -------------------------- | ------------ | ------------- |
| id                         | bigint       | PK            |
| user_id                    | integer      | NOT NULL      |
| book_id                    | integer      | NOT NULL      |
| viewed_at                  | timestamp    | NOT NULL      |
| total_reading_time_seconds | integer      | default 0     |
| sessions_count             | integer      | default 0     |
| last_read_at               | timestamp    | NULL          |
| last_position              | varchar(255) | NULL          |
| is_completed               | boolean      | default false |

**Constraints:** `uk_views_user_book (user_id, book_id)`

---

## 📚 `books_db` — book-catalog-service

### `books`

| Колонка          | Тип          | Ограничения                           |
| ---------------- | ------------ | ------------------------------------- |
| id               | integer      | PK                                    |
| title            | varchar(500) | NOT NULL                              |
| description      | text         | NULL                                  |
| publication_year | integer      | NULL                                  |
| language         | varchar(10)  | NOT NULL, default 'ru'                |
| word_count       | integer      | NULL                                  |
| publisher        | varchar(255) | NULL                                  |
| series_name      | varchar(255) | NULL                                  |
| series_number    | integer      | NULL                                  |
| age_rating       | varchar(5)   | NULL (`0+`, `12+`, `16+`, `18+`)      |
| file_path        | varchar      | NOT NULL                              |
| cover_image_path | varchar      | NULL                                  |
| created_at       | timestamp    | NOT NULL                              |
| average_rating   | decimal(3,2) | default 0.00 (денормализовано)        |
| ratings_count    | integer      | NOT NULL, default 0 (денормализовано) |

**Индексы:** `idx_book_title`, `idx_book_pub_year`, `idx_book_language`, `idx_book_created`

> [!TIP]
> `average_rating` и `ratings_count` обновляются через Feign из comment-rating-service при каждом новом рейтинге.

### `authors`

| Колонка    | Тип          | Ограничения |
| ---------- | ------------ | ----------- |
| id         | integer      | PK          |
| first_name | varchar(255) | NOT NULL    |
| last_name  | varchar(255) | NOT NULL    |

### `genres`

| Колонка | Тип          | Ограничения        |
| ------- | ------------ | ------------------ |
| id      | integer      | PK                 |
| name    | varchar(255) | NOT NULL, UNIQUE   |

### `tags`

| Колонка      | Тип          | Ограничения                           |
| ------------ | ------------ | ------------------------------------- |
| id           | integer      | PK                                    |
| name         | varchar(50)  | NOT NULL, UNIQUE                      |
| category     | varchar(50)  | NULL                                  |
| is_predefined| boolean      | DEFAULT false                         |
| usage_count  | integer      | DEFAULT 0                             |
| created_at   | timestamp    | NOT NULL, DEFAULT CURRENT_TIMESTAMP   |

### Связующие таблицы

| Таблица      | Поля                          | Ограничения                                             |
| ------------ | ----------------------------- | ------------------------------------------------------- |
| book_authors | id, book_id, author_id        | FK → books(id), authors(id), ON DELETE CASCADE          |
| book_genres  | id, book_id, genre_id         | FK → books(id), genres(id), ON DELETE CASCADE           |
| book_tags    | id, book_id, tag_id           | FK → books(id), tags(id), ON DELETE CASCADE, UNIQUE(book_id, tag_id) |

---

## 💬 `comments_ratings_db` — comment-rating-service

### `comments`

| Колонка    | Тип       | Ограничения        |
| ---------- | --------- | ------------------ |
| id         | integer   | PK                 |
| user_id    | integer   | NOT NULL           |
| book_id    | integer   | NOT NULL           |
| text       | text      | NOT NULL           |
| created_at | timestamp | NOT NULL           |
| deleted_at | timestamp | NULL (soft delete) |

**Индексы:** `idx_comment_book_id`, `idx_comment_user_id`, `idx_comment_created_at`, `idx_comment_deleted_at`

### `ratings`

| Колонка      | Тип     | Ограничения                 |
| ------------ | ------- | --------------------------- |
| id           | integer | PK                          |
| rating_value | integer | NOT NULL, 1–5               |
| user_id      | integer | NOT NULL                    |
| book_id      | integer | NOT NULL                    |

**Constraints:** `uk_rating_user_book (user_id, book_id)` — один рейтинг на пользователя

### `bookmarks`

| Колонка    | Тип          | Ограничения                        |
| ---------- | ------------ | ---------------------------------- |
| id         | integer      | PK                                 |
| user_id    | integer      | NOT NULL                           |
| book_id    | integer      | NOT NULL                           |
| position   | double       | NOT NULL (позиция в книге 0.0–1.0) |
| name       | varchar(255) | NULL                               |
| notes      | text         | NULL                               |
| created_at | timestamp    | NOT NULL                           |
| deleted_at | timestamp    | NULL (soft delete)                 |

---

## 🔍 Qdrant-коллекции — ai-service

Qdrant не является реляционной БД, но содержит два основным namespace:

| Коллекция                         | Модель                              | Назначение                              |
| --------------------------------- | ----------------------------------- | --------------------------------------- |
| RAG-коллекция (чанки)             | USER-bge-m3 (dense) + BM25 (sparse) | Текстовые чанки книг для RAG            |
| recommendations-коллекция (книги) | ru-en-RoSBERTa                      | Мета-эмбеддинги книг для поиска похожих |

Поля точки в RAG-коллекции: `book_id`, `chunk_index`, `text`, `text_dense` (vector), `text_sparse` (sparse vector).

## ⚡ Redis — ai-service

| Ключ                                | Тип    | TTL           | Содержимое               |
| ----------------------------------- | ------ | ------------- | ------------------------ |
| `chat:history:{session_id}`         | List   | настраивается | История сообщений        |
| `chat:context:{session_id}`         | String | настраивается | Активный контекст книги  |
| `recommendations:user:{user_id}`    | String | 1 час         | JSON массив рекомендаций |
| `recommendations:similar:{book_id}` | String | настраивается | Похожие книги            |
