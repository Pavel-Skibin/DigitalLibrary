# storage-service

Хранит и раздаёт файлы книг (`.fb2`) и обложки. Авторизация через JWT.

**Порт:** 8083

## Технологии

- Spring Boot 3.4, Java 21
- Spring Security + JJWT (верификация входящих JWT)
- Spring Boot Validation
- `java.nio.file` — работа с файловой системой

## Хранение файлов

Два типа хранилищ, пути настраиваются через `StorageProperties`:

| Тип            | Свойство              | Монтирование (Docker) |
| -------------- | --------------------- | --------------------- |
| Книги (`.fb2`) | `storage.books-path`  | `/app/books`          |
| Обложки        | `storage.covers-path` | `/app/covers`         |

Сохранение файлов: `FileStorageService.uploadBook()` — строит имя файла из автора и названия книги (транслитерация + нормализация), сохраняет в `books-path`. Возвращает `FileUploadResponse{filePath, originalName, savedName, size}`.

Валидация: пустой файл → `FileStorageException("File is empty")`.

## API

| Method   | Endpoint                         | Описание                                                                   |
| -------- | -------------------------------- | -------------------------------------------------------------------------- |
| `POST`   | `/api/storage/upload/book`       | Загрузить FB2-файл (`multipart/form-data`, поля `file`, `author`, `title`) |
| `POST`   | `/api/storage/upload/cover`      | Загрузить обложку (`multipart/form-data`, поле `file`)                     |
| `GET`    | `/api/storage/books/**`          | Получить FB2-файл по пути (стриминг через `Resource`)                      |
| `GET`    | `/api/storage/covers/{filename}` | Получить обложку                                                           |
| `DELETE` | `/api/storage/**`                | Удалить файл по пути                                                       |

Подробный API Reference: [../api/storage-api.md](../api/storage-api.md)

## Переменные окружения

| Переменная            | Описание                            |
| --------------------- | ----------------------------------- |
| `STORAGE_BOOKS_PATH`  | Абсолютный путь к папке с книгами   |
| `STORAGE_COVERS_PATH` | Абсолютный путь к папке с обложками |
| `JWT_SECRET`          | Секрет для верификации входящих JWT |
