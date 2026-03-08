# api-gateway

Единая точка входа для всех внешних запросов. Реализован на **Spring Cloud Gateway** (Project Reactor, Netty) — реактивный, неблокирующий.

**Порт:** 8080

## Технологии

- Spring Boot 3.4, Java 20
- Spring Cloud Gateway 2024.0.0 (Reactive)
- Resilience4j Circuit Breaker (reactor)
- Spring Boot Actuator

## Маршрутизация

Маршруты настроены в `application.properties` через `spring.cloud.gateway.routes`:

| Префикс пути                                                                               | Upstream сервис             |
| ------------------------------------------------------------------------------------------ | --------------------------- |
| `/api/users/**`, `/api/auth/**`, `/api/readings/**`, `/api/favorites/**`                   | user-service:8081           |
| `/api/books/**`, `/api/authors/**`, `/api/genres/**`, `/api/tags/**`, `/api/statistics/**` | book-catalog-service:8084   |
| `/api/comments/**`, `/api/ratings/**`, `/api/bookmarks/**`                                 | comment-rating-service:8082 |
| `/api/storage/**`                                                                          | storage-service:8083        |
| `/api/recommendations/**`, `/api/embeddings/**`, `/api/ai/**`                              | ai-service:8085             |

## Circuit Breaker

Параметры Resilience4j из `application.properties`:

```properties
resilience4j.circuitbreaker.instances.default.slidingWindowSize=10
resilience4j.circuitbreaker.instances.default.minimumNumberOfCalls=5
resilience4j.circuitbreaker.instances.default.failureRateThreshold=50
resilience4j.circuitbreaker.instances.default.waitDurationInOpenState=10s
resilience4j.circuitbreaker.instances.default.permittedNumberOfCallsInHalfOpenState=3
resilience4j.circuitbreaker.instances.default.automaticTransitionFromOpenToHalfOpenEnabled=true
```

## CORS

Глобальная CORS-конфигурация через `spring.cloud.gateway.globalcors`:

- Разрешённые origin: `http://localhost:*`, `http://127.0.0.1:*`
- Разрешённые методы: GET, POST, PUT, DELETE, PATCH, OPTIONS
- `allow-credentials: true`

## Переменные окружения

| Переменная                            | Описание                                                |
| ------------------------------------- | ------------------------------------------------------- |
| `SERVICES_USER_SERVICE_URL`           | URL user-service (по умолчанию `http://localhost:8081`) |
| `SERVICES_BOOK_CATALOG_SERVICE_URL`   | URL book-catalog-service                                |
| `SERVICES_COMMENT_RATING_SERVICE_URL` | URL comment-rating-service                              |
| `SERVICES_STORAGE_SERVICE_URL`        | URL storage-service                                     |
| `SERVICES_AI_SERVICE_URL`             | URL ai-service                                          |

</content>
</invoke>
