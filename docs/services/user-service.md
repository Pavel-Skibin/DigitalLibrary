# user-service

Отвечает за аутентификацию, управление пользователями, историю чтения и избранное.

**Порт:** 8081

## Технологии

- Spring Boot 3.4, Java 20
- Spring Security + JJWT (JWT HS256)
- Spring Data JPA, PostgreSQL
- Spring Cloud OpenFeign
- Lombok, MapStruct

## Сущности

### `users`

```
id              INTEGER PK
username        VARCHAR UNIQUE NOT NULL
password_hash   VARCHAR NOT NULL
role_id         FK → user_roles
email           VARCHAR UNIQUE NOT NULL
deleted_at      TIMESTAMP  -- soft delete
```

Индексы: `idx_user_deleted_at`, `idx_user_role_id`

### `user_roles`

```
id    INTEGER PK
name  VARCHAR  -- "USER" | "ADMIN"
```

### `user_reading_sessions`

```
id                BIGINT PK
user_id           INTEGER NOT NULL
book_id           INTEGER NOT NULL
started_at        TIMESTAMP NOT NULL
ended_at          TIMESTAMP
duration_seconds  INTEGER
last_position     VARCHAR(255)
is_significant    BOOLEAN DEFAULT FALSE
```

Индексы: `idx_sessions_user_id`, `idx_sessions_book_id`, `idx_sessions_user_book`, `idx_sessions_started`

### `user_book_favorites`

Хранит избранные книги пользователя (userId + bookId с `deleted_at`-soft-delete).

### `user_book_views`

Хранит историю просмотров книг пользователем (`viewed_at`, `view_count`).

## Сервисы

| Класс                       | Ответственность                                                            |
| --------------------------- | -------------------------------------------------------------------------- |
| `AuthService`               | Регистрация (BCrypt hash), login через `AuthenticationManager`, выдача JWT |
| `UserService`               | CRUD пользователей, soft-delete, restore                                   |
| `UserReadingSessionService` | Старт/обновление/завершение сессий чтения                                  |
| `UserBookFavoriteService`   | Добавление/удаление избранного                                             |
| `UserBookViewService`       | Запись и получение истории просмотров                                      |

## Аутентификация

`AuthService.register()` — создаёт пользователя с ролью `USER`, генерирует JWT через `JwtUtil.generateToken(CustomUserDetails, userId)`.

`AuthService.login()` — делегирует Spring `AuthenticationManager`, после успеха возвращает JWT.

JWT-токен содержит `userId` как claim, используется downstream-сервисами для получения `userId` без обращения к user-service.

## API

Подробный API Reference: [../api/user-service-api.md](../api/user-service-api.md)

Контроллеры:

- `AuthController` — `/api/auth` (login, logout, register)
- `UserController` — `/api/users` (CRUD, поиск)
- `UserReadingSessionController` — `/api/readings` (сессии чтения)
- `UserBookFavoriteController` — `/api/favorites` (избранное)
- `UserBookViewController` — `/api/users/me/history` (история просмотров)

## Переменные окружения

| Переменная                   | Описание                                 |
| ---------------------------- | ---------------------------------------- |
| `SPRING_DATASOURCE_URL`      | JDBC URL PostgreSQL                      |
| `SPRING_DATASOURCE_USERNAME` | Имя пользователя БД                      |
| `SPRING_DATASOURCE_PASSWORD` | Пароль БД                                |
| `JWT_SECRET`                 | Секрет для подписи JWT (минимум 256 бит) |

</content>
</invoke>
