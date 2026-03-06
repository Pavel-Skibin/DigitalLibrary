# 👤 API: user-service

Base URL (через gateway): `http://localhost:8080`

---

## 🔑 Аутентификация

### POST `/api/auth/login`

Вход в систему. Устанавливает HttpOnly-куку `jwt`.

**Body:**

```json
{ "username": "string", "password": "string" }
```

**Response `200 OK`:**

```json
{ "token": "eyJ..." }
```

Кука `jwt` устанавливается автоматически: `HttpOnly; SameSite=Strict; Secure`.

---

### POST `/api/auth/logout`

Выход. Удаляет куку `jwt`.

**Response `200 OK`**

---

## 👤 Пользователи (требуют роль ADMIN, если не указано иное)

### GET `/api/users/me`

Профиль текущего авторизованного пользователя.

**Auth:** требуется (любая роль)

**Response `200 OK`:** `UserResponse { id, username, email, role }`

---

### GET `/api/users?query=&page=0&size=20`

Список всех пользователей с поиском.

**Auth:** `ROLE_ADMIN`

| Параметр | Тип    | Описание                     |
| -------- | ------ | ---------------------------- |
| query    | string | Поиск по username/email      |
| page     | int    | Страница (default 0)         |
| size     | int    | Размер страницы (default 20) |

**Response `200 OK`:** `Page<UserResponse>`

---

### GET `/api/users/{id}`

**Response `200 OK`:** `UserResponse`

---

### GET `/api/users/username/{username}`

Поиск по имени. Публичный эндпоинт.

**Response `200 OK`:** `UserResponse`

---

### DELETE `/api/users/{id}`

Мягкое удаление пользователя (`deleted_at = now()`).

**Response `204 No Content`**

---

### POST `/api/users/{id}/restore`

Восстановление удалённого пользователя.

**Response `200 OK`:** `UserResponse`

---

### PUT `/api/users/{id}/role`

Изменить роль пользователя.

**Body:** `{ "roleId": 2 }` (1=USER, 2=MODERATOR, 3=ADMIN)

**Response `200 OK`:** `UserResponse`

---

## 📖 История просмотров

### GET `/api/users/me/history?page=0&size=20`

История просмотренных книг текущего пользователя.

**Auth:** требуется

**Response `200 OK`:** `Page<BookViewResponse>`

---

### GET `/api/users/me/history/book-ids`

Список ID просмотренных книг.

**Auth:** требуется

**Response `200 OK`:** `[1, 5, 42, ...]`

---

### POST `/api/users/me/views`

Зафиксировать просмотр книги.

**Auth:** требуется

**Body:** `{ "bookId": 42 }`

**Response `201 Created`**

---

## ❤️ Избранное

### GET `/api/favorites?page=0&size=20`

**Auth:** требуется

**Response `200 OK`:** `Page<FavoriteResponse>`

---

### GET `/api/favorites/book-ids`

Список ID книг в избранном.

**Response `200 OK`:** `[3, 7, 99]`

---

### POST `/api/favorites/{bookId}`

Добавить книгу в избранное.

**Response `201 Created`**

---

### DELETE `/api/favorites/{bookId}`

Убрать из избранного.

**Response `204 No Content`**

---

### GET `/api/favorites/{bookId}/status`

**Response `200 OK`:** `{ "isFavorite": true }`

---

## 📚 Сессии чтения

### POST `/api/readings/start`

Начать сессию чтения.

**Body:** `StartReadingSessionRequest { bookId, startPosition }`

**Response `201 Created`:** `{ "sessionId": 101 }`

---

### PUT `/api/readings/{sessionId}/update`

Heartbeat — обновить позицию и время.

**Body:** `EndReadingSessionRequest { currentPage, totalPages, durationSeconds, lastPosition }`

**Response `200 OK`**

---

### PUT `/api/readings/{sessionId}/end`

Завершить сессию.

**Body:** `EndReadingSessionRequest`

**Response `200 OK`**

---

### GET `/api/readings/active`

Активные сессии пользователя.

**Response `200 OK`:** `[ReadingSessionResponse, ...]`

---

### GET `/api/readings/history?page=0&size=20`

История сессий.

**Response `200 OK`:** `Page<ReadingSessionResponse>`

---

## ⚠️ Коды ошибок

| Статус | Ситуация                           |
| ------ | ---------------------------------- |
| 400    | Неверные данные запроса            |
| 401    | JWT отсутствует или недействителен |
| 403    | Недостаточно прав                  |
| 404    | Пользователь/сессия не найдены     |
| 409    | username или email уже занят       |
