# 🏗️ Архитектура системы

## 🌐 Общий обзор

Digital Library — микросервисное приложение с единственной точкой входа через API Gateway.

> [!NOTE]
> Все клиентские запросы проходят через **api-gateway** — единственный публичный сервис в сети. Внутренние сервисы недоступны напрямую из браузера.

### 🗺️ Системный обзор

```mermaid
graph TD
    U(["Пользователь<br/>Браузер"])
    FE["Frontend<br/>Vue 3 + Vite / Nginx<br/>:80"]
    GW["api-gateway<br/>Spring Cloud Gateway<br/>:8080"]

    US["user-service<br/>Spring Boot 3.4<br/>:8081"]
    CR["comment-rating-service<br/>Spring Boot 3.4<br/>:8082"]
    SS["storage-service<br/>Spring Boot 3.4<br/>:8083"]
    BC["book-catalog-service<br/>Spring Boot 3.4<br/>:8084"]
    AI["ai-service<br/>FastAPI 0.109 / Python 3.11<br/>:8085"]

    PG_U[("users_db<br/>PostgreSQL")]
    PG_B[("books_db<br/>PostgreSQL")]
    PG_C[("comments_db<br/>PostgreSQL")]
    FS[("Filesystem<br/>FB2 / Covers")]
    RD[("Redis<br/>:6379")]
    QD[("Qdrant<br/>:6333")]
    DS(["DeepSeek API<br/>External LLM"])

    U -->|HTTP| FE
    FE -->|REST /api/| GW

    GW -->|"/api/auth/** /api/users/**<br/>/api/readings/** /api/favorites/**"| US
    GW -->|"/api/books/** /api/authors/**<br/>/api/genres/** /api/tags/**"| BC
    GW -->|"/api/comments/**<br/>/api/ratings/** /api/bookmarks/**"| CR
    GW -->|"/api/storage/**"| SS
    GW -->|"/api/ai/** /api/recommendations/**<br/>/api/embeddings/** /api/rag/**"| AI

    US --- PG_U
    BC --- PG_B
    CR --- PG_C
    SS --- FS
    AI --- RD
    AI --- QD
    AI -->|HTTPS| DS

    CR -. "Feign: validateUser" .-> US
    CR -. "Feign: validateBook<br/>updateRatingCache" .-> BC
    AI -. "HTTP: reading history<br/>favorites, viewed books" .-> US
    AI -. "HTTP: book metadata<br/>all book IDs" .-> BC
```

### 🔗 Межсервисные Feign-вызовы

```mermaid
graph LR
    CR["comment-rating-service"]
    BC["book-catalog-service"]
    US["user-service"]
    AI["ai-service"]

    CR -->|"GET /api/internal/users/{id}<br/>validateUser"| US
    CR -->|"GET /api/internal/books/{id}<br/>validateBook"| BC
    CR -->|"PUT /api/internal/books/{id}/rating-cache<br/>обновить avgRating + ratingsCount"| BC
    AI -->|"GET /api/internal/users/{id}/history<br/>якорные книги для рекомендаций"| US
    AI -->|"GET /api/internal/users/{id}/favorites"| US
    AI -->|"GET /api/internal/users/{id}/viewed-books"| US
    AI -->|"GET /api/internal/books/{id}<br/>метаданные книги"| BC
    AI -->|"GET /api/internal/books/all-ids"| BC
    AI -->|"GET /api/storage/books/{filename}<br/>скачать FB2 для индексации"| SS
```

### 🐳 Деплоймент (Docker Compose)

```mermaid
graph TD
    subgraph docker["Docker Compose Network"]
        FE["frontend<br/>nginx:alpine<br/>port 80"]
        GW["api-gateway<br/>openjdk:20<br/>port 8080"]
        US["user-service<br/>openjdk:20<br/>port 8081"]
        CR["comment-rating-service<br/>openjdk:20<br/>port 8082"]
        SS["storage-service<br/>openjdk:20<br/>port 8083"]
        BC["book-catalog-service<br/>openjdk:20<br/>port 8084"]
        AI["ai-service<br/>python:3.11<br/>port 8085"]
        PG["postgres:17<br/>port 5432"]
        RD["redis:7<br/>port 6379"]
        QD["qdrant/qdrant<br/>port 6333/6334"]
        PR["prometheus<br/>port 9090"]
        GF["grafana<br/>port 3000"]
    end

    FE --> GW
    GW --> US
    GW --> BC
    GW --> CR
    GW --> SS
    GW --> AI
    US --> PG
    BC --> PG
    CR --> PG
    AI --> PG
    AI --> RD
    AI --> QD
    US --> PR
    BC --> PR
    CR --> PR
    GW --> PR
    PR --> GF
```

## 🧩 Компоненты

### 🚪 api-gateway (Spring Cloud Gateway 2024.0.0)

Единственная точка входа. Выполняет:

- Маршрутизацию по Path predicate
- JWT-аутентификацию (`JwtAuthenticationFilter`)
- CORS (разрешены `http://localhost:*` и `http://127.0.0.1:*`)
- Circuit Breaker (Resilience4j)

| Маршрут                | Path                                                                                       |
| ---------------------- | ------------------------------------------------------------------------------------------ |
| user-service           | `/api/users/**`, `/api/auth/**`, `/api/readings/**`, `/api/favorites/**`                   |
| book-catalog-service   | `/api/books/**`, `/api/authors/**`, `/api/genres/**`, `/api/tags/**`, `/api/statistics/**` |
| comment-rating-service | `/api/comments/**`, `/api/ratings/**`, `/api/bookmarks/**`                                 |
| storage-service        | `/api/storage/**`                                                                          |
| ai-service             | `/api/recommendations/**`, `/api/embeddings/**`, `/api/ai/**`                              |

### 👤 user-service (Spring Boot 3.4, Java 20)

Авторизация, профили, история чтения. Собственная база PostgreSQL `users_db`.

### 📚 book-catalog-service (Spring Boot 3.4, Java 20)

Каталог книг, авторов, жанров, тегов. Хранит денормализованный рейтинг (обновляется через Feign из comment-rating-service). База `books_db`.

### 💬 comment-rating-service (Spring Boot 3.4, Java 20)

Отзывы, оценки (1–5), закладки. Перед сохранением валидирует user и book через OpenFeign. База `comments_db`.

### 🗂️ storage-service (Spring Boot 3.4, Java 20)

Хранение файлов (FB2, обложки) на диске. Нет собственной БД.

### 🤖 ai-service (Python 3.11, FastAPI 0.109)

RAG, персональные рекомендации, векторизация. Работает с Qdrant (векторный поиск) и Redis (кэш, история диалогов).

## 📋 Принятые архитектурные решения

> [!TIP]
> ADR (Architecture Decision Record) — короткие записи о том, **почему** было принято то или иное решение, а не только **что** было сделано.

### ADR-001: Single Database per Service

Каждый Java-сервис имеет собственную PostgreSQL базу (`users_db`, `books_db`, `comments_db`).

Причина: изоляция данных, независимое масштабирование и развёртывание, отсутствие cross-service JOIN.

Компромисс: межсервисная валидация через OpenFeign вместо FK-ограничений.

### ADR-002: API Gateway как единственная точка входа

Frontend общается только с api-gateway. Внутренние сервисы не доступны напрямую из сети.

Причина: единое место для JWT-проверки, CORS, Circuit Breaker.

### ADR-003: Денормализованный рейтинг в book-catalog

Поля `average_rating` и `ratings_count` хранятся в таблице `books` и обновляются через Feign-вызов из comment-rating-service при каждом новом рейтинге.

Причина: исключить cross-service запрос при каждой загрузке каталога.

### ADR-004: ai-service на Python (не Java)

Причина: экосистема ML/NLP (sentence-transformers, PyTorch, Qdrant Python SDK) значительно богаче в Python. Интеграция через HTTP API.

### ADR-005: HttpOnly Cookie для JWT 🔐

JWT не хранится в localStorage, а устанавливается как `HttpOnly; SameSite=Strict; Secure` через api-gateway.

Причина: защита от XSS-атак.

### ADR-006: Мягкое удаление (Soft Delete) для комментариев

Поле `deleted = true` вместо физического DELETE.

Причина: сохранение контекста в ветках обсуждений.

## 📐 Диаграмма архитектуры

> draw.io-диаграмма находится в [`docs/architecture.drawio`](../docs/architecture.drawio) (откроется в draw.io или VS Code с плагином).

## 📊 Мониторинг

Prometheus scrape + Grafana дашборды (конфигурация в `old_docker/prometheus/` и `old_docker/grafana/`).

| Метрика                   | Источник                        |
| ------------------------- | ------------------------------- |
| HTTP request rate/latency | Actuator `/actuator/prometheus` |
| JVM memory/GC             | Spring Boot Actuator            |
| Circuit Breaker state     | Resilience4j metrics            |
