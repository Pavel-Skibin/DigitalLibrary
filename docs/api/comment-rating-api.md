# 💬 API: comment-rating-service

Base URL (через gateway): `http://localhost:8080`

> Перед сохранением комментария, рейтинга или закладки сервис проверяет существование пользователя (через user-service) и книги (через book-catalog-service) по Feign.

---

## 💬 Комментарии

### GET `/api/comments/books/{bookId}?page=0&size=20`

Активные (не удалённые) комментарии к книге. Публичный эндпоинт.

**Response `200 OK`:** `Page<CommentResponse>`

```json
{
  "content": [
    {
      "id": 1,
      "userId": 5,
      "bookId": 42,
      "text": "Отличная книга!",
      "createdAt": "2025-01-15T10:30:00"
    }
  ],
  "totalElements": 87,
  "totalPages": 5
}
```

---

### GET `/api/comments/books/{bookId}/all`

Все комментарии включая удалённые.

**Auth:** `ROLE_MODERATOR` или `ROLE_ADMIN`

**Response `200 OK`:** `Page<CommentResponse>` (удалённые содержат `deletedAt`)

---

### POST `/api/comments`

Создать комментарий.

**Auth:** `ROLE_USER` или выше

**Body:**

```json
{ "bookId": 42, "text": "Очень рекомендую!" }
```

**Response `201 Created`:** `CommentResponse`

---

### PUT `/api/comments/{id}`

Редактировать свой комментарий.

**Auth:** `ROLE_USER` (только автор) / MODERATOR / ADMIN

**Body:** `"Обновлённый текст комментария"` (plain string)

**Response `200 OK`:** `CommentResponse`

---

### DELETE `/api/comments/{id}`

Мягкое удаление своего комментария (`deleted_at = now()`).

**Auth:** `ROLE_USER` (только автор)

**Response `204 No Content`**

---

### DELETE `/api/comments/{id}/moderate`

Удаление комментария модератором.

**Auth:** `ROLE_MODERATOR` или `ROLE_ADMIN`

**Response `204 No Content`**

---

### POST `/api/comments/{id}/restore`

Восстановить удалённый комментарий.

**Auth:** `ROLE_MODERATOR` или `ROLE_ADMIN`

**Response `200 OK`:** `CommentResponse`

---

## ⭐ Рейтинги

### POST `/api/ratings`

Поставить или обновить оценку. Одна оценка на пользователя per книга (upsert).

**Auth:** `ROLE_USER` или выше

**Body:**

```json
{ "bookId": 42, "rating": 5 }
```

Валидация: `rating` ∈ [1, 5].

После сохранения синхронно вызывает `PUT /api/books/{id}/rating-cache` в book-catalog-service для обновления денормализованных полей.

**Response `201 Created`:** `RatingResponse { id, userId, bookId, value }`

---

### GET `/api/ratings/me`

Все оценки текущего пользователя.

**Auth:** требуется

**Response `200 OK`:** `[{ "bookId": 42, "value": 5 }, ...]`

---

### GET `/api/ratings/books/{bookId}`

Все оценки книги.

**Response `200 OK`:** `[RatingResponse, ...]`

---

## 🔖 Закладки

### POST `/api/bookmarks`

Создать закладку.

**Auth:** `ROLE_USER` или выше

**Body:**

```json
{
  "bookId": 42,
  "position": 0.427,
  "name": "Конец главы 5",
  "notes": "Важный момент с Геральтом"
}
```

`position` — число от 0.0 до 1.0 (относительная позиция в книге).

**Response `201 Created`:** `BookmarkResponse`

---

### PUT `/api/bookmarks/{id}`

Обновить закладку (только свою).

**Auth:** требуется

**Body:** `BookmarkUpdateRequest { position, name, notes }` (все поля опциональны)

**Response `200 OK`:** `BookmarkResponse`

---

### DELETE `/api/bookmarks/{id}`

Мягкое удаление закладки.

**Auth:** требуется (только свои)

**Response `204 No Content`**

---

### GET `/api/bookmarks/book/{bookId}?page=0&size=20`

Закладки текущего пользователя в конкретной книге.

**Auth:** требуется

**Response `200 OK`:** `Page<BookmarkResponse>`

---

## ⚠️ Коды ошибок

| Статус | Ситуация                                                |
| ------ | ------------------------------------------------------- |
| 400    | Невалидный рейтинг (не 1–5) или пустой текст            |
| 401    | Не авторизован                                          |
| 403    | Попытка изменить чужой комментарий/закладку             |
| 404    | Комментарий/рейтинг/закладка не найдены                 |
| 422    | Пользователь или книга не существуют (Feign validation) |
