# 📘 Варианты использования

Основные сценарии взаимодействия пользователя с системой.

## 🔐 UC-1. Регистрация и вход

```mermaid
sequenceDiagram
    actor U as Пользователь
    participant FE as Frontend
    participant GW as api-gateway:8080
    participant US as user-service:8081

    U->>FE: Открывает /login
    FE->>U: Форма (вкладки Вход / Регистрация)

    alt Регистрация
        U->>FE: username, email, password
        FE->>GW: POST /api/auth/register
        GW->>US: POST /api/auth/register
        US->>US: BCrypt.hash(password)
        US-->>GW: 201 Created {id, username, token}
        GW-->>FE: 201 + Set-Cookie: jwt=...
        FE->>FE: router.push("/")
    else Вход
        U->>FE: username, password
        FE->>GW: POST /api/auth/login
        GW->>US: POST /api/auth/login
        US->>US: loadUserByUsername → matches(raw, hash)
        US-->>GW: 200 OK {token}
        GW-->>FE: 200 + Set-Cookie: jwt=... (HttpOnly, SameSite=Strict)
        FE->>FE: router.push("/")
    end
```

> [!NOTE]
> JWT сохраняется в HttpOnly-куке. Все последующие запросы через api-gateway автоматически проверяются через `JwtAuthenticationFilter`.

---

## 📤 UC-2. Загрузка книги (администратор)

```mermaid
sequenceDiagram
    actor A as Администратор
    participant FE as Frontend /admin
    participant GW as api-gateway
    participant SS as storage-service:8083
    participant BC as book-catalog-service:8091
    participant AI as ai-service:8085

    A->>FE: Форма: FB2-файл + обложка + метаданные
    FE->>GW: POST /api/storage/upload/book (multipart)
    GW->>SS: POST /api/storage/upload/book
    SS->>SS: нормализация имени файла
    SS->>SS: сохранить в STORAGE_PATH/books/
    SS-->>GW: {filename}
    GW-->>FE: {filename}

    FE->>GW: POST /api/storage/upload/cover (multipart)
    GW->>SS: POST /api/storage/upload/cover
    SS-->>GW: {coverFilename}

    FE->>GW: POST /api/books (JSON: title, author, filename, coverFilename, ...)
    GW->>BC: POST /api/books
    BC->>BC: save Book entity
    BC-->>GW: 201 {book_id}
    GW-->>FE: 201 {book_id}

    FE->>GW: POST /api/ai/tasks/vectorize {book_id}
    GW->>AI: POST /api/ai/tasks/vectorize
    AI->>SS: GET /api/storage/books/{filename} (скачать FB2)
    AI->>AI: parse FB2 → chunks (512 токенов, overlap 64)
    AI->>AI: embed: USER-bge-m3 (dense) + BM25 (sparse)
    AI->>AI: upsert to Qdrant (RAG + recommendations)
    AI-->>GW: 202 Accepted {task_id}
    A->>FE: GET /api/ai/tasks/vectorize/status/{book_id} (polling)
```

---

## 🔍 UC-3. Поиск книги в каталоге

```mermaid
sequenceDiagram
    actor U as Пользователь
    participant FE as Frontend /books
    participant GW as api-gateway
    participant BC as book-catalog-service

    U->>FE: Вводит запрос в поле поиска
    FE->>FE: debounce 300 мс
    FE->>GW: GET /api/books/search?q=ведьмак&author=Сапковский&genre=Фэнтези
    GW->>BC: GET /api/books/search
    BC->>BC: searchBooksSimple() JPQL LIKE
    BC-->>GW: Page<BookDto>
    GW-->>FE: [{id, title, author, coverUrl, avgRating}, ...]
    FE->>U: Отображает карточки книг
```

---

## 📖 UC-4. Чтение книги во встроенном ридере

```mermaid
sequenceDiagram
    actor U as Пользователь
    participant FE as Frontend /books/:id
    participant GW as api-gateway
    participant SS as storage-service
    participant US as user-service

    U->>FE: Открывает страницу книги
    FE->>GW: GET /api/books/{id}
    GW-->>FE: BookDto {filename, ...}

    U->>FE: Нажимает "Читать"
    FE->>FE: iframe → /reader.html?bookUrl=...
    FE->>GW: GET /api/storage/books/{filename}
    GW->>SS: GET /api/storage/books/{filename}
    SS-->>FE: Поток файла (Content-Type: application/octet-stream)
    FE->>FE: Foliate.js парсит FB2/EPUB в ридере

    loop Чтение
        FE->>GW: PUT /api/readings/session/update {bookId, currentPage, totalPages}
        GW->>US: PUT /api/readings/session/update
        US->>US: upsert UserReadingSession
    end

    U->>FE: Закрывает ридер
    FE->>GW: POST /api/readings/session/complete {bookId}
    GW->>US: POST /api/readings/session/complete
    US->>US: status = COMPLETED
```

---

## 🤖 UC-5. AI-чат с ассистентом

```mermaid
sequenceDiagram
    actor U as Пользователь
    participant FE as Frontend (ChatWidget)
    participant GW as api-gateway
    participant AI as ai-service
    participant RD as Redis
    participant QD as Qdrant
    participant LLM as DeepSeek API

    U->>FE: Открывает чат-виджет
    FE->>FE: genSessionId() → localStorage

    U->>FE: "Кем была Цири в книге Владычица Озера?"
    FE->>GW: POST /api/ai/chat {message, session_id}
    GW->>AI: POST /api/ai/chat

    AI->>RD: GET chat:history:{session_id}
    RD-->>AI: [предыдущие сообщения]

    AI->>LLM: classify intent (1 вызов) → {intent: "book_question", title: "...", scope: "book"}
    AI->>QD: BM25 поиск book_id по title
    AI->>QD: hybrid search (dense + sparse + RRF k=60)
    QD-->>AI: [RetrievedChunk × top_k]
    AI->>AI: expand context window (±RAG_CONTEXT_WINDOW чанков)
    AI->>LLM: generate_answer(chunks, query, history)
    LLM-->>AI: ответ

    AI->>RD: RPUSH chat:history:{session_id}
    AI-->>GW: {answer, sources: [{book_id, title, chunk_text}]}
    GW-->>FE: ChatResponse
    FE->>U: Отображает ответ со ссылками на источники
```

---

## ⭐ UC-6. Персональные рекомендации

```mermaid
sequenceDiagram
    actor U as Пользователь
    participant FE as Frontend HomeView
    participant GW as api-gateway
    participant AI as ai-service
    participant RD as Redis
    participant US as user-service
    participant QD as Qdrant

    U->>FE: Открывает главную страницу
    FE->>GW: GET /api/recommendations/personal/{user_id}
    GW->>AI: GET /api/recommendations/personal/{user_id}

    AI->>RD: GET recommendations:user:{user_id}
    alt Кэш найден
        RD-->>AI: [BookRecommendation × N]
        AI-->>GW: 200 {recommendations}
    else Кэш отсутствует
        AI->>US: GET /api/users/{id}/reading-history (якорные книги)
        US-->>AI: [{book_id, rating, sessions}, ...]
        AI->>QD: get embeddings for anchor books
        AI->>QD: multi-vector search (similar books)
        AI->>AI: hybrid scoring (0.7×sim + 0.3×quality)
        AI->>AI: MMR diversity (λ, MAX_PER_AUTHOR, MAX_PER_GENRE)
        AI->>RD: SET recommendations:user:{user_id} TTL=1h
        AI-->>GW: 200 {recommendations}
    end
    GW-->>FE: [{book_id, title, score, reason}, ...]
    FE->>U: Карусель рекомендованных книг
```

---

## ⭐ UC-7. Оценка и комментирование книги

```mermaid
sequenceDiagram
    actor U as Пользователь
    participant FE as Frontend BookDetailView
    participant GW as api-gateway
    participant CR as comment-rating-service:8082
    participant US as user-service
    participant BC as book-catalog-service

    U->>FE: Выставляет оценку (1–5 звёзд)
    FE->>GW: POST /api/ratings {bookId, rating}
    GW->>CR: POST /api/ratings
    CR->>US: GET /api/users/{id} (validateUser)
    US-->>CR: 200 OK
    CR->>BC: GET /api/books/{id} (validateBook)
    BC-->>CR: 200 OK
    CR->>CR: save Rating
    CR->>BC: PUT /api/books/{id}/rating-cache {avgRating, ratingsCount}
    BC->>BC: update Book.averageRating, Book.ratingsCount
    CR-->>GW: 201 Created
    GW-->>FE: 201

    U->>FE: Пишет комментарий
    FE->>GW: POST /api/comments {bookId, text}
    GW->>CR: POST /api/comments
    CR->>US: GET /api/users/{id}
    CR->>BC: GET /api/books/{id}
    CR->>CR: save Comment
    CR-->>FE: 201 {id, text, authorName, createdAt}
```
