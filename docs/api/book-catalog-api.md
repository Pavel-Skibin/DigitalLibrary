# 📚 API: book-catalog-service

Base URL (через gateway): `http://localhost:8080`

---

## 📚 Книги

### GET `/api/books?page=0&size=20&sort=createdAt,desc`

Список всех книг с пагинацией.

| Параметр | Тип    | Описание             |
| -------- | ------ | -------------------- |
| page     | int    | Страница (default 0) |
| size     | int    | Размер (default 20)  |
| sort     | string | Поле,направление     |

**Response `200 OK`:** `Page<BookResponse>`

---

### GET `/api/books/{id}`

Детали книги.

**Response `200 OK`:**

```json
{
  "id": 1,
  "title": "Ведьмак",
  "description": "...",
  "publicationYear": 1993,
  "language": "ru",
  "wordCount": 120000,
  "publisher": "АСТ",
  "seriesName": "Сага о Ведьмаке",
  "seriesNumber": 1,
  "ageRating": "16+",
  "filePath": "witcher.fb2",
  "coverImagePath": "covers/witcher.jpg",
  "averageRating": 4.85,
  "ratingsCount": 312,
  "authors": ["Анджей Сапковский"],
  "genres": ["Фэнтези"],
  "tags": ["Ведьмак", "Польское фэнтези"]
}
```

---

### GET `/api/books/search`

Полнотекстовый поиск и фильтрация.

| Параметр    | Тип    | Описание                 |
| ----------- | ------ | ------------------------ |
| title       | string | Поиск по названию (LIKE) |
| authorIds   | int[]  | Фильтр по ID авторов     |
| genreIds    | int[]  | Фильтр по ID жанров      |
| minRating   | double | Мин. рейтинг             |
| maxRating   | double | Макс. рейтинг            |
| page / size | int    | Пагинация                |

**Response `200 OK`:** `Page<BookResponse>`

---

### GET `/api/books/authors/{authorId}/books`

Все книги автора.

**Response `200 OK`:** `Page<BookResponse>`

---

### GET `/api/books/{bookId}/cover`

Изображение обложки (Content-Type: image/jpeg или image/png).

---

### GET `/api/books/{bookId}/download`

Скачать файл книги (Content-Disposition: attachment).

---

### GET `/api/books/{bookId}/fb2`

Содержимое FB2 как XML (Content-Type: application/xml).

---

### POST `/api/books`

Создать запись книги.

**Auth:** `ROLE_MODERATOR` или `ROLE_ADMIN`

**Body: `BookCreateRequest`**

```json
{
  "title": "Ведьмак",
  "description": "...",
  "publicationYear": 1993,
  "language": "ru",
  "seriesName": "Сага о Ведьмаке",
  "seriesNumber": 1,
  "ageRating": "16+",
  "filePath": "witcher.fb2",
  "coverImagePath": "covers/witcher.jpg",
  "authorIds": [1],
  "genreIds": [3],
  "tagIds": [7, 9]
}
```

**Response `201 Created`:** `BookResponse`

---

### PUT `/api/books/{id}`

Обновить книгу.

**Auth:** `ROLE_MODERATOR` или `ROLE_ADMIN`

**Body:** `BookUpdateRequest` (те же поля, все опциональны)

**Response `200 OK`:** `BookResponse`

---

### DELETE `/api/books/{id}`

Удалить книгу.

**Auth:** `ROLE_MODERATOR` или `ROLE_ADMIN`

**Response `204 No Content`**

---

## ✏️ Авторы

### GET `/api/authors?query=&page=0&size=20`

| Параметр | Описание       |
| -------- | -------------- |
| query    | Поиск по имени |

**Response `200 OK`:** `Page<AuthorResponse { id, firstName, lastName, booksCount }>`

---

### GET `/api/authors/{id}`

**Response `200 OK`:** `AuthorResponse`

---

### POST `/api/authors?firstName=Анджей&lastName=Сапковский`

**Auth:** `ROLE_MODERATOR` или `ROLE_ADMIN`

**Response `201 Created`:** `AuthorResponse`

---

### PUT `/api/authors/{id}?firstName=...&lastName=...`

**Auth:** `ROLE_MODERATOR` или `ROLE_ADMIN`

**Response `200 OK`:** `AuthorResponse`

---

### DELETE `/api/authors/{id}`

**Auth:** `ROLE_MODERATOR` или `ROLE_ADMIN`

**Response `204 No Content`**

---

## 🏷️ Жанры

### GET `/api/genres`

**Response `200 OK`:** `[{ "id": 1, "name": "Фэнтези" }, ...]`

---

### POST `/api/genres?name=Фэнтези`

**Auth:** `ROLE_MODERATOR` или `ROLE_ADMIN`

---

### PUT `/api/genres/{id}?name=...` / DELETE `/api/genres/{id}`

---

## 🧲 Теги

### GET `/api/tags`

**Response `200 OK`:** `[{ "id": 1, "name": "Ведьмак", "category": "Серия" }, ...]`

---

### POST `/api/tags?name=Ведьмак&category=Серия`

**Auth:** `ROLE_ADMIN`. Логика find-or-create: если тег с таким именем уже существует — вернёт его.

---

### DELETE `/api/tags/{id}`

**Auth:** `ROLE_ADMIN`

---

## 📤 Загрузка файла

### POST `/api/books/upload`

Загрузка FB2-файла через multipart.

**Auth:** `ROLE_MODERATOR` или `ROLE_ADMIN`

**Params:** `file` (multipart), `authorLastName`, `bookTitle`

**Response `200 OK`:** `{ "filename": "sapkowski-witcher.fb2" }`

---

## 📊 Статистика

### GET `/api/statistics/system`

Общая статистика системы.

```json
{
  "totalBooks": 500,
  "totalUsers": 2000,
  "totalComments": 8500,
  "totalRatings": 15000
}
```

---

### GET `/api/statistics/books/top-rated?minRatings=10&size=10`

Топ книг по рейтингу с фильтром по минимальному числу оценок.

---

### GET `/api/statistics/genres/top-by-count?size=10`

Топ жанров по количеству книг.

---

### GET `/api/statistics/authors/top-by-count?size=10`

Топ авторов по количеству книг.

---

### GET `/api/statistics/ratings/distribution`

Распределение оценок 1–5.

```json
[
  { "value": 5, "count": 8900 },
  { "value": 4, "count": 4200 },
  ...
]
```

---

### POST `/api/admin/sync-ratings-cache`

**Auth:** `ROLE_ADMIN`. Принудительная синхронизация денормализованного рейтинга из comment-rating-service.

**Response `200 OK`**

---

## ⚠️ Коды ошибок

| Статус | Ситуация                        |
| ------ | ------------------------------- |
| 400    | Невалидное тело запроса         |
| 401    | Не авторизован                  |
| 403    | Недостаточно прав               |
| 404    | Книга / автор / жанр не найдены |
