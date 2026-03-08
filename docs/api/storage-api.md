# 🗂️ API: storage-service

Base URL (через gateway): `http://localhost:8080`

Хранит файлы книг (FB2, EPUB) и обложки на диске. Нет собственной базы данных.

---

## 📤 Загрузка файлов

### POST `/api/storage/upload/book`

Загрузить файл книги.

**Auth:** `ROLE_MODERATOR` или `ROLE_ADMIN`

**Request:** `multipart/form-data`

| Параметр       | Тип    | Описание                                      |
| -------------- | ------ | --------------------------------------------- |
| file           | File   | Файл книги (FB2, EPUB)                        |
| authorLastName | string | Фамилия автора (для нормализации имени файла) |
| bookTitle      | string | Название книги (для нормализации имени файла) |

Имя файла нормализуется: пробелы → дефисы, латиница, строчные буквы.

**Response `200 OK`:**

```json
{ "filename": "sapkowski-witcher.fb2" }
```

---

### POST `/api/storage/upload/cover`

Загрузить обложку книги.

**Auth:** `ROLE_MODERATOR` или `ROLE_ADMIN`

**Request:** `multipart/form-data`

| Параметр | Тип  | Описание                |
| -------- | ---- | ----------------------- |
| file     | File | Изображение (JPEG, PNG) |

**Response `200 OK`:**

```json
{ "filename": "covers/abc123.jpg" }
```

---

## 📥 Получение файлов

### GET `/api/storage/books/{filePath}`

Получить файл книги. Поддерживает вложенные пути (`**`).

| Параметр | Тип     | Описание                                                        |
| -------- | ------- | --------------------------------------------------------------- |
| download | boolean | `false` = inline (для ридера), `true` = attachment (скачивание) |

**Response `200 OK`:**

- `Content-Type: application/octet-stream`
- `Content-Disposition: inline` или `attachment; filename="..."`

---

### GET `/api/storage/covers/{filename}`

Получить обложку.

**Response `200 OK`:**

- `Content-Type: image/jpeg` (или `image/png`)

---

## 🗑️ Удаление файлов

### DELETE `/api/storage/{filePath}`

Удалить файл (поддерживает вложенные пути).

**Auth:** `ROLE_MODERATOR` или `ROLE_ADMIN`

**Response `204 No Content`**

---

## ⚙️ Конфигурация

| Переменная     | Описание                      |
| -------------- | ----------------------------- |
| `STORAGE_PATH` | Корневой путь для файлов книг |
| `COVERS_PATH`  | Путь для обложек              |

---

## ⚠️ Коды ошибок

| Статус | Ситуация                                         |
| ------ | ------------------------------------------------ |
| 400    | Неверный формат файла или отсутствующий параметр |
| 401    | Не авторизован                                   |
| 403    | Недостаточно прав                                |
| 404    | Файл не найден на диске                          |
| 413    | Превышен максимальный размер файла               |
