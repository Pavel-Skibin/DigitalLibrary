# Digital Library

Цифровая библиотека — веб-приложение для хранения, поиска, чтения и обсуждения книг с AI-ассистентом, системой рекомендаций и встроенной онлайн-читалкой FB2.

![Java](https://img.shields.io/badge/Java-20-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.4-green?logo=springboot)
![Python](https://img.shields.io/badge/Python-3.11-blue?logo=python)
![FastAPI](https://img.shields.io/badge/FastAPI-0.109-teal?logo=fastapi)
![Vue.js](https://img.shields.io/badge/Vue.js-3-brightgreen?logo=vue.js)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-blue?logo=postgresql)
![Qdrant](https://img.shields.io/badge/Qdrant-vector--db-red)
![Redis](https://img.shields.io/badge/Redis-cache-red?logo=redis)
![Docker](https://img.shields.io/badge/Docker-compose-blue?logo=docker)

---

## Что умеет система

- **Читалка** — встроенный FB2-ридер прямо в браузере, сохранение позиции чтения
- **AI-чат** — диалог с ассистентом по содержанию конкретной книги или по всей библиотеке; понимает контекст серий (Ведьмак, Гарри Поттер и т.п.)
- **Умные рекомендации** — персональные и на основе истории чтения; поиск похожих книг по векторному сходству
- **Каталог** — поиск по названию, автору, жанру, тегам; обложки, метаданные, рейтинги
- **Комментарии и рейтинги** — отзывы на книги с soft-delete и модерацией
- **Закладки** — сохранение позиции в тексте с привязкой к главе
- **Администрирование** — загрузка книг, управление пользователями, ролевой доступ
- **Мониторинг** — Prometheus метрики, Grafana дашборды

---

## Архитектура

Система построена как набор независимых микросервисов, взаимодействующих через REST. Единая точка входа — API Gateway.

```
                          ┌─────────────────┐
                          │   Vue 3 (Nginx)  │  :80
                          └────────┬────────┘
                                   │
                          ┌────────▼────────┐
                          │   API Gateway    │  :8080  Spring Cloud Gateway
                          └──┬──┬──┬──┬────┘
                             │  │  │  │
               ┌─────────────┘  │  │  └──────────────┐
               │                │  │                  │
    ┌──────────▼───┐  ┌─────────▼──┴──┐  ┌───────────▼──┐  ┌──────────────┐
    │ user-service  │  │book-catalog-  │  │comment-rating│  │storage-      │
    │  :8081  Java  │  │service :8084  │  │service :8082 │  │service :8083 │
    └──────────────┘  └───────────────┘  └──────────────┘  └──────────────┘
                                                         ┌──────────────────┐
                                                         │   ai-service      │
                                                         │   :8085  Python   │
                                                         │  FastAPI + Qdrant │
                                                         └──────────────────┘
    ┌──────────────────────────────────────────────────────────────────────┐
    │              PostgreSQL :5432  |  Redis :6379  |  Qdrant :6333       │
    └──────────────────────────────────────────────────────────────────────┘
```

Подробнее: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)

---

## Стек технологий

| Сервис                 | Язык / Фреймворк                        | БД / Хранилище            | Порт |
| ---------------------- | --------------------------------------- | ------------------------- | ---- |
| api-gateway            | Java 20, Spring Cloud Gateway 2024      | —                         | 8080 |
| user-service           | Java 20, Spring Boot 3.4, JPA, Security | PostgreSQL                | 8081 |
| comment-rating-service | Java 20, Spring Boot 3.4, OpenFeign     | PostgreSQL                | 8082 |
| storage-service        | Java 20, Spring Boot 3.4, JJWT          | Файловая система          | 8083 |
| book-catalog-service   | Java 20, Spring Boot 3.4, OpenFeign     | PostgreSQL                | 8084 |
| ai-service             | Python 3.11, FastAPI, PyTorch           | Qdrant, Redis, PostgreSQL | 8085 |
| frontend               | Vue 3, Vite 7, Axios                    | —                         | 80   |

AI-стек: `sentence-transformers` (модели `ru-en-RoSBERTa` для рекомендаций, `USER-bge-m3` для RAG), `Qdrant` (векторный поиск), `DeepSeek API` (генерация ответов), `Redis` (история диалогов, кэш).

---

## Быстрый старт

### Требования

- Docker и Docker Compose
- Папка с FB2-книгами (укажите путь в `docker-compose-microservices.yml`)

### Запуск

```bash
git clone <repo>
cd DigitalLibrary

# Укажите путь к папке с книгами в docker-compose-microservices.yml:
#   volumes:
#     - /ваш/путь/к/книгам:/app/books

docker compose -f docker-compose-microservices.yml up --build
```

После старта:

| Сервис             | URL                                 |
| ------------------ | ----------------------------------- |
| Приложение         | http://localhost                    |
| API Gateway        | http://localhost:8080               |
| Grafana            | http://localhost:3001 (admin/admin) |
| Prometheus         | http://localhost:9090               |
| AI Service Swagger | http://localhost:8085/docs          |

### Переменные окружения

Ключевые переменные для production (передаются через `docker-compose` или `.env`):

| Переменная              | Описание                                | Дефолт (dev)                                      |
| ----------------------- | --------------------------------------- | ------------------------------------------------- |
| `JWT_SECRET`            | Секрет для подписи JWT, минимум 256 бит | `your-secret-key-...`                             |
| `SPRING_DATASOURCE_URL` | JDBC URL PostgreSQL                     | `jdbc:postgresql://postgres:5432/digital_library` |
| `DEEPSEEK_API_KEY`      | Ключ DeepSeek API (ai-service)          | —                                                 |
| `REDIS_URL`             | URL Redis (ai-service)                  | `redis://localhost:6379`                          |
| `QDRANT_URL`            | URL Qdrant (ai-service)                 | `http://localhost:6333`                           |

---

## Документация

| Раздел                               | Файл                                         |
| ------------------------------------ | -------------------------------------------- |
| Бэкенд — обзор сервисов              | [docs/BACKEND.md](docs/BACKEND.md)           |
| Фронтенд                             | [docs/FRONTEND.md](docs/FRONTEND.md)         |
| Архитектурные решения                | [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) |
| Алгоритмы (RAG, рекомендации, поиск) | [docs/ALGORITHMS.md](docs/ALGORITHMS.md)     |
| Схема базы данных                    | [docs/DATABASE.md](docs/DATABASE.md)         |
| Сценарии использования               | [docs/USE_CASES.md](docs/USE_CASES.md)       |
| API Reference                        | [docs/api/](docs/api/)                       |
| Описание сервисов                    | [docs/services/](docs/services/)             |

---

## Структура репозитория

```
digital-library-frontend/   Vue 3 приложение
services/
  api-gateway/              Spring Cloud Gateway
  user-service/             Аутентификация, профили, история чтения
  book-catalog-service/     Каталог книг, авторы, жанры
  comment-rating-service/   Комментарии, рейтинги, закладки
  storage-service/          Хранение и раздача файлов (.fb2, обложки)
  ai-service/               RAG-чат, рекомендации, векторизация
shared/
  common-models/            Общие классы (JWT, исключения, security)
docs/                       Документация
docker-compose-microservices.yml
```

---


