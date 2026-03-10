# ⚙️ Backend

Бэкенд Digital Library состоит из 5 Java-сервисов на Spring Boot 3 и одного Python-сервиса на FastAPI. Все сервисы изолированы и взаимодействуют через REST. Единая точка входа — API Gateway.

## 🖧 Архитектура взаимодействия

```mermaid
graph TD
    GW[API Gateway :8080]
    US[user-service :8081]
    CR[comment-rating-service :8082]
    SS[storage-service :8083]
    BC[book-catalog-service :8091]
    AI[ai-service :8085]
    PG[(PostgreSQL :5432)]
    RD[(Redis :6379)]
    QD[(Qdrant :6333)]

    GW --> US
    GW --> CR
    GW --> SS
    GW --> BC
    GW --> AI

    US --> PG
    BC --> PG
    CR --> PG
    AI --> PG
    AI --> RD
    AI --> QD

    CR -->|OpenFeign| US
    CR -->|OpenFeign| BC
    BC -->|OpenFeign| CR
```

Межсервисное взаимодействие в Java-сервисах реализовано через **Spring Cloud OpenFeign** (сгенерированные клиенты из OpenAPI-спецификаций). Ai-service общается с Java-сервисами через `aiohttp`.

## 📦 Сервисы

| Сервис                    | Порт | Назначение                                       | Подробнее                                                                |
| ------------------------- | ---- | ------------------------------------------------ | ------------------------------------------------------------------------ |
| 🚪 api-gateway            | 8080 | Маршрутизация, CORS, Circuit Breaker             | [services/api-gateway.md](services/api-gateway.md)                       |
| 👤 user-service           | 8081 | Аутентификация, JWT, профили, история, избранное | [services/user-service.md](services/user-service.md)                     |
| 💬 comment-rating-service | 8082 | Комментарии, рейтинги, закладки                  | [services/comment-rating-service.md](services/comment-rating-service.md) |
| 🗂️ storage-service        | 8083 | Хранение и раздача FB2-файлов и обложек          | [services/storage-service.md](services/storage-service.md)               |
| 📚 book-catalog-service   | 8091 | Каталог книг, авторы, жанры, поиск               | [services/book-catalog-service.md](services/book-catalog-service.md)     |
| 🤖 ai-service             | 8085 | RAG-чат, рекомендации, векторизация              | [services/ai-service.md](services/ai-service.md)                         |

## 🧩 Общие компоненты

> [!NOTE]
> Модуль `shared/common-models` — общая Maven-зависимость, подключаемая по сервисам. Позволяет избежать дублирования JWT-логики и классов исключений.

Модуль `shared/common-models` содержит классы, используемые несколькими сервисами:

- `JwtUtil` — генерация и верификация JWT (HS256, JJWT)
- `ResourceNotFoundException` — 404-ответ во всех сервисах
- `BadRequestException` — 400-ответ

Каждый сервис подключает `common-models` как локальную Maven-зависимость.

## 🛑 Маршруты API Gateway

### 📄 Схема маршрутизации

```mermaid
graph LR
    CLIENT(["Клиент<br/>Frontend / Browser"])
    GW["api-gateway :8080<br/>JwtAuthenticationFilter<br/>CircuitBreaker"]

    US["user-service :8081"]
    BC["book-catalog-service :8091"]
    CR["comment-rating-service :8082"]
    SS["storage-service :8083"]
    AI["ai-service :8085"]

    CLIENT --> GW

    GW -->|"/api/auth/**<br/>/api/users/**<br/>/api/readings/**<br/>/api/favorites/**"| US
    GW -->|"/api/books/**<br/>/api/authors/**<br/>/api/genres/**<br/>/api/tags/**<br/>/api/statistics/**"| BC
    GW -->|"/api/comments/**<br/>/api/ratings/**<br/>/api/bookmarks/**"| CR
    GW -->|"/api/storage/**"| SS
    GW -->|"/api/ai/**<br/>/api/recommendations/**<br/>/api/embeddings/**<br/>/api/rag/**"| AI
```

### 🔄 Circuit Breaker — состояния

```mermaid
stateDiagram-v2
    [*] --> CLOSED : Старт
    CLOSED --> OPEN : failures / slidingWindow >= 50%
    OPEN --> HALF_OPEN : waitDuration = 10s
    HALF_OPEN --> CLOSED : permittedCalls успешны
    HALF_OPEN --> OPEN : permittedCalls неуспешны

    CLOSED : CLOSED (normal)\nПропускает запросы\nslidingWindowSize = 10
    OPEN : OPEN\nОтклоняет всё\n→ fallback response
    HALF_OPEN : HALF_OPEN\nПропускает тестовые запросы
```

> [!IMPORTANT]
> `slidingWindowSize=10`, `failureRateThreshold=50%`, `waitDurationInOpenState=10s`

Таблица маршрутов настроена в `application.properties` через `spring.cloud.gateway.routes`:

| Маршрут                                                                                    | Upstream               |
| ------------------------------------------------------------------------------------------ | ---------------------- |
| `/api/users/**`, `/api/auth/**`, `/api/readings/**`, `/api/favorites/**`                   | user-service           |
| `/api/books/**`, `/api/authors/**`, `/api/genres/**`, `/api/tags/**`, `/api/statistics/**` | book-catalog-service   |
| `/api/comments/**`, `/api/ratings/**`, `/api/bookmarks/**`                                 | comment-rating-service |
| `/api/storage/**`                                                                          | storage-service        |
| `/api/recommendations/**`, `/api/embeddings/**`, `/api/ai/**`                              | ai-service             |

## Circuit Breaker

API Gateway использует Resilience4j с параметрами из `application.properties`:

- `slidingWindowSize=10` — размер окна для подсчёта отказов
- `failureRateThreshold=50` — порог открытия цепи (50% отказов)
- `waitDurationInOpenState=10s` — время ожидания перед полуоткрытым состоянием

## 🔑 JWT — поток аутентификации

```mermaid
sequenceDiagram
    participant C as Клиент
    participant GW as api-gateway
    participant US as user-service

    C->>GW: POST /api/auth/login {username, password}
    GW->>US: POST /api/auth/login
    US->>US: loadUserByUsername(username)
    US->>US: BCryptPasswordEncoder.matches(raw, hash)
    US->>US: JwtUtil.generateToken(username, role, expiration)
    US-->>GW: 200 OK {token}
    GW-->>C: 200 OK + Set-Cookie: jwt=... HttpOnly SameSite=Strict

    Note over C,GW: Последующие запросы

    C->>GW: GET /api/books (Cookie: jwt=eyJ...)
    GW->>GW: JwtAuthenticationFilter.filter()
    GW->>GW: JwtUtil.validateToken(token)
    GW->>GW: Inject X-User-Id, X-User-Role headers
    GW->>BC: GET /api/books + X-User-Id + X-User-Role
    BC-->>GW: 200 Page<BookResponse>
    GW-->>C: 200 Page<BookResponse>
```

## 📊 Мониторинг

```mermaid
graph LR
    US["user-service<br/>/actuator/prometheus"]
    BC["book-catalog-service<br/>/actuator/prometheus"]
    CR["comment-rating-service<br/>/actuator/prometheus"]
    GW["api-gateway<br/>/actuator/prometheus"]
    PR["Prometheus :9090<br/>scrape_interval: 15s"]
    GF["Grafana :3001<br/>Dashboards"]

    US --> PR
    BC --> PR
    CR --> PR
    GW --> PR
    PR --> GF
```

Каждый Java-сервис имеет `/actuator/health` и подключён к Prometheus через `spring-boot-starter-actuator`. Grafana-дашборды доступны на порту 3001.

## 📖 API Reference

- [user-service API](api/user-service-api.md)
- [book-catalog-service API](api/book-catalog-api.md)
- [comment-rating-service API](api/comment-rating-api.md)
- [storage-service API](api/storage-api.md)
- [ai-service API](api/ai-service-api.md)
