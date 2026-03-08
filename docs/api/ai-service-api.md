# 🤖 API: ai-service

Base URL (через gateway): `http://localhost:8080`

---

## 💬 AI-чат

### POST `/api/ai/chat`

Умный ассистент на основе RAG. Классифицирует интент, находит релевантные чанки в Qdrant и генерирует ответ через DeepSeek.

**Body:**

```json
{
  "user_id": 5,
  "message": "Кем была Цири в книге Владычица озера?",
  "session_id": "abc123"
}
```

| Поле       | Тип    | Описание                               |
| ---------- | ------ | -------------------------------------- |
| user_id    | int    | ID пользователя (для персонализации)   |
| message    | string | Текст сообщения                        |
| session_id | string | ID сессии для хранения истории в Redis |

**Response `200 OK`:**

```json
{
  "answer": "Цири является...",
  "intent": "book_question",
  "sources": [
    {
      "book_id": 7,
      "title": "Владычица озера",
      "chunk_text": "...релевантный отрывок..."
    }
  ],
  "recommendations": []
}
```

---

### GET `/api/ai/chat/health`

Статус LLM (DeepSeek) и Qdrant.

**Response `200 OK`:**

```json
{ "llm": "ok", "qdrant": "ok" }
```

---

## ⭐ Рекомендации

### GET `/api/recommendations/for-me?user_id=5&limit=10&language=ru&exclude_read=true`

Персональные рекомендации. Результат кэшируется в Redis на 1 час.

| Параметр     | Тип    | По умолчанию | Описание                          |
| ------------ | ------ | ------------ | --------------------------------- |
| user_id      | int    | —            | ID пользователя                   |
| limit        | int    | 10           | Кол-во рекомендаций               |
| language     | string | null         | Фильтр по языку (`ru`, `en`, ...) |
| exclude_read | bool   | true         | Исключить прочитанные             |

**Response `200 OK`:**

```json
{
  "recommendations": [
    {
      "book_id": 3,
      "title": "Кровь эльфов",
      "score": 0.91,
      "reason": "Та же серия, что вы читаете"
    }
  ],
  "strategy": "personalized"
}
```

`strategy`: `"personalized"` | `"cold_start"` | `"popular"`

---

### GET `/api/recommendations/similar/{book_id}?limit=10`

Похожие книги по векторному сходству.

**Response `200 OK`:** `[BookRecommendation, ...]`

---

### GET `/api/recommendations/popular?limit=10&genre=Фэнтези`

Популярные книги (сортировка `avg_rating × log(count+1)`).

**Response `200 OK`:** `[BookRecommendation, ...]`

---

### GET `/api/recommendations/new?limit=10&min_year=2024`

Новинки последних лет.

**Response `200 OK`:** `[BookRecommendation, ...]`

---

### POST `/api/recommendations/invalidate-cache/{user_id}`

Сбросить кэш рекомендаций пользователя.

**Response `200 OK`**

---

## 🔢 Эмбеддинги

### POST `/api/embeddings/generate`

Векторизовать одну книгу в коллекцию рекомендаций (metadata embedding).

**Body:**

```json
{ "book_id": 42 }
```

**Response `200 OK`:** `{ "book_id": 42, "status": "indexed" }`

---

### POST `/api/embeddings/batch-generate`

Пакетная векторизация.

**Body:**

```json
{ "book_ids": [1, 2, 3, 4], "batch_size": 32 }
```

**Response `200 OK`:** `{ "indexed": 4, "failed": 0 }`

---

### GET `/api/embeddings/collection-info`

Информация о Qdrant-коллекции (размер, статус).

---

## 🔎 RAG (Retrieval-Augmented Generation)

### POST `/api/rag/index-book/{book_id}?force=false`

Индексировать FB2-файл книги в RAG-коллекцию Qdrant.

1. Скачивает FB2 из storage-service
2. Разбивает на чанки (512 токенов, overlap 64)
3. Эмбеддит через USER-bge-m3 (dense) + BM25 (sparse)
4. Сохраняет в Qdrant

**Response `202 Accepted`:** `{ "book_id": 42, "status": "indexing" }`

---

### GET `/api/rag/book/{book_id}/chunks-count`

Количество чанков книги в RAG-коллекции.

**Response `200 OK`:** `{ "book_id": 42, "chunks_count": 187 }`

---

### DELETE `/api/rag/book/{book_id}/chunks`

Удалить все чанки книги из RAG.

**Response `200 OK`**

---

### POST `/api/rag/search`

Семантический поиск по чанкам без генерации ответа.

**Body:**

```json
{
  "query": "путешествие Геральта в Нильфгаард",
  "top_k": 5,
  "book_ids": [7, 8]
}
```

**Response `200 OK`:** `{ "chunks": [RetrievedChunk, ...] }`

---

### POST `/api/rag/query`

Полный RAG-запрос: поиск + генерация ответа через LLM.

**Body:**

```json
{
  "query": "В чём смысл судьбы-предназначения?",
  "book_ids": [1, 2],
  "session_id": "abc123"
}
```

**Response `200 OK`:** `{ "answer": "...", "sources": [...] }`

---

### GET `/api/rag/status`

Статус RAG-системы (Qdrant коллекция, модели).

---

## ✨ Обогащение метаданных

### POST `/api/ai/meta/enrich`

Автоматически заполнить описание, жанры и теги книги через DeepSeek.

**Body:**

```json
{
  "title": "Ведьмак",
  "authors": ["Анджей Сапковский"]
}
```

**Response `200 OK`:**

```json
{
  "description": "Сборник рассказов...",
  "genres": ["Фэнтези", "Тёмное фэнтези"],
  "tags": ["Ведьмак", "Польское фэнтези", "Маги"]
}
```

---

## ⚙️ Задачи векторизации

### POST `/api/ai/tasks/vectorize`

Запустить фоновую задачу полной векторизации книги (RAG + recommendations).

**Body:**

```json
{
  "book_id": 42,
  "mode": "full",
  "force": false
}
```

| Поле  | Значения                                   | Описание                             |
| ----- | ------------------------------------------ | ------------------------------------ |
| mode  | `"full"` \| `"rag"` \| `"recommendations"` | Что индексировать                    |
| force | bool                                       | Переиндексировать даже если уже есть |

**Response `202 Accepted`:**

```json
{
  "book_id": 42,
  "task_id": "task-uuid",
  "status": "pending"
}
```

---

### GET `/api/ai/tasks/vectorize/status/{book_id}`

Статус задачи векторизации.

**Response `200 OK`:**

```json
{
  "book_id": 42,
  "status": "completed",
  "rag_chunks": 187,
  "embedding_generated": true,
  "error": null
}
```

Статусы: `pending` | `running` | `completed` | `failed`

---

### DELETE `/api/ai/tasks/vectorize/{book_id}`

Удалить книгу из всех коллекций Qdrant (RAG + recommendations).

**Response `200 OK`**

---

## ⚠️ Коды ошибок

| Статус | Ситуация                              |
| ------ | ------------------------------------- |
| 400    | Невалидный запрос                     |
| 404    | Книга не найдена в Qdrant или storage |
| 503    | DeepSeek API или Qdrant недоступны    |
