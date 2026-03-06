# 🎨 Frontend

Фронтенд — Single Page Application на Vue 3 + Vite 7, раздаётся через Nginx.

## 🛠️ Технологии

| Библиотека       | Версия | Назначение                |
| ---------------- | ------ | ------------------------- |
| 💭 Vue 3         | 3.x    | Реактивный UI framework   |
| ⚡ Vite          | 7.x    | Сборщик, dev server       |
| 🛣️ Vue Router 4  | 4.x    | Клиентская маршрутизация  |
| 📡 Axios / Fetch | —      | HTTP-запросы              |
| 📖 Foliate.js    | —      | Встроенный ридер FB2/EPUB |

## 📁 Структура проекта

```
src/
├── main.js               # Инициализация Vue-приложения
├── App.vue               # Корневой компонент, layout-обёртка
├── router/
│   └── index.js          # Маршруты, navigation guards
├── api/
│   ├── index.js          # Базовый fetch-клиент (Bearer JWT из cookie)
│   ├── authors.js
│   ├── chat.js           # POST /api/ai/chat
│   ├── favorites.js
│   ├── reading.js
│   ├── recommendations.js
│   ├── viewHistory.js
│   └── aiAdmin.js        # Задачи векторизации
├── views/
│   ├── HomeView.vue       # Главная: рекомендации + новинки
│   ├── BooksView.vue      # Каталог с поиском и фильтрами
│   ├── BookDetailView.vue # Страница книги: ридер, оценки, комментарии
│   ├── AuthorsView.vue    # Список авторов
│   ├── AuthorBooksView.vue# Книги выбранного автора
│   ├── LoginRegisterView.vue # Вход / Регистрация (вкладки)
│   ├── UserProfileView.vue   # Профиль: история, избранное, закладки
│   └── AdminPanelView.vue    # Управление книгами (ROLE_ADMIN, ROLE_MODERATOR)
├── components/
│   ├── layout/
│   │   ├── Header.vue    # Навигация, поиск, аватар
│   │   └── AppSidebar.vue
│   ├── chat/
│   │   └── AiChatWidget.vue  # Всплывающий чат-виджет
│   ├── books/            # BookCard, BookFilters, BookReader,...
│   ├── home/             # RecommendationCarousel,...
│   ├── authors/
│   ├── admin/
│   ├── ui/               # Переиспользуемые UI-примитивы
│   └── icons/
└── composables/
    ├── useUser.js
    ├── useAuthors.js
    ├── useBookComments.js
    ├── useBookCover.js
    ├── useBookRating.js
    ├── useExtendedSearch.js
    ├── useFavorites.js
    ├── useReadingSession.js
    ├── useRecommendations.js
    ├── useViewHistory.js
    └── useAdminAuth.js
```

## 🛣️ Маршруты

### 🌳 Дерево маршрутизации

```mermaid
graph TD
    ROOT["/"] --> HOME["HomeView<br/>Рекомендации + новинки"]
    ROOT --> BOOKS["/books<br/>BooksView<br/>Каталог + поиск"]
    ROOT --> DETAIL["/books/:id<br/>BookDetailView<br/>Страница книги"]
    ROOT --> AUTHORS["/authors<br/>AuthorsView<br/>Список авторов"]
    ROOT --> AUTHOR_BOOKS["/authors/:authorId/books<br/>AuthorBooksView"]
    ROOT --> LOGIN["/login<br/>LoginRegisterView<br/>guestOnly"]
    ROOT --> PROFILE["/profile<br/>UserProfileView<br/>requiresAuth"]
    ROOT --> ADMIN["/admin<br/>AdminPanelView<br/>requiresRole: ADMIN | MODERATOR"]

    style LOGIN fill:#e3f2fd
    style PROFILE fill:#fff3e0
    style ADMIN fill:#fce4ec
```

### 🛡️ Navigation Guard

```mermaid
flowchart TD
    NAV["router.beforeEach(to, from, next)"]
    CHECK_JWT{"jwt cookie<br/>присутствует?"}
    CHECK_AUTH{"to.meta.requiresAuth?"}
    CHECK_ROLE{"to.meta.requiresRole?"}
    CHECK_GUEST{"to.meta.guestOnly?"}
    HAS_ROLE{"userRole ∈<br/>requiresRole?"}

    NAV --> CHECK_JWT
    CHECK_JWT --> CHECK_AUTH

    CHECK_AUTH -- "да, нет jwt" --> REDIRECT_LOGIN["redirect /login"]
    CHECK_AUTH -- "да, есть jwt" --> CHECK_ROLE
    CHECK_AUTH -- нет --> CHECK_GUEST

    CHECK_ROLE -- есть требование --> HAS_ROLE
    CHECK_ROLE -- нет требования --> NEXT["next()"]

    HAS_ROLE -- да --> NEXT
    HAS_ROLE -- нет --> REDIRECT_HOME["redirect /"]

    CHECK_GUEST -- "да и есть jwt" --> REDIRECT_HOME
    CHECK_GUEST -- нет --> NEXT
```

### 📋 Таблица маршрутов

| Путь                       | Компонент         | Auth required                          |
| -------------------------- | ----------------- | -------------------------------------- |
| `/`                        | HomeView          | Нет                                    |
| `/books`                   | BooksView         | Нет                                    |
| `/books/:id`               | BookDetailView    | Нет                                    |
| `/authors`                 | AuthorsView       | Нет                                    |
| `/authors/:authorId/books` | AuthorBooksView   | Нет                                    |
| `/login`                   | LoginRegisterView | Нет (только гости)                     |
| `/profile`                 | UserProfileView   | Да                                     |
| `/admin`                   | AdminPanelView    | Да (`ROLE_ADMIN` или `ROLE_MODERATOR`) |

Navigation guard в `router/index.js` читает куку `jwt` и перенаправляет:

- неавторизованных → `/login` если `requiresAuth`
- авторизованных → `/` если `guestOnly`

## 📡 API-клиент

`src/api/index.js` реализует базовый `fetch`-клиент. JWT читается из куки через `getCookie("jwt")` и передаётся в заголовке `Authorization: Bearer <token>`. Все запросы идут на `/api` (проксируется через Nginx → api-gateway:8080).

### 📦 Структура API-модулей

```mermaid
graph TD
    subgraph api["src/api/"]
        IDX["index.js<br/>fetch wrapper<br/>Bearer JWT"]
        AUTH_API["(auth встроен в index.js)<br/>POST /api/auth/login<br/>POST /api/auth/logout"]
        AB["authors.js<br/>GET /api/authors/**"]
        CH["chat.js<br/>POST /api/ai/chat"]
        FAV["favorites.js<br/>GET/POST/DELETE /api/favorites/**"]
        RD["reading.js<br/>POST/PUT /api/readings/**"]
        RC["recommendations.js<br/>GET /api/recommendations/**"]
        VH["viewHistory.js<br/>GET /api/users/me/history"]
        AI_ADMIN["aiAdmin.js<br/>POST /api/ai/tasks/vectorize"]
    end

    IDX --> AUTH_API
    IDX --> AB
    IDX --> CH
    IDX --> FAV
    IDX --> RD
    IDX --> RC
    IDX --> VH
    IDX --> AI_ADMIN
```

### 🏛️ Иерархия компонентов (основные страницы)

```mermaid
graph TD
    APP["App.vue"] --> LAY_H["layout/Header.vue<br/>навигация, поиск, аватар"]
    APP --> LAY_S["layout/AppSidebar.vue"]
    APP --> CHAT_W["chat/AiChatWidget.vue<br/>всплывающий чат"]
    APP --> RV["router-view"]

    RV --> HOME_V["HomeView"]
    RV --> BOOKS_V["BooksView"]
    RV --> DETAIL_V["BookDetailView"]
    RV --> AUTH_V["LoginRegisterView"]
    RV --> PROF_V["UserProfileView"]
    RV --> ADMIN_V["AdminPanelView"]

    HOME_V --> HC["home/RecommendationCarousel<br/>GET /api/recommendations/for-me"]

    BOOKS_V --> BC_CARD["books/BookCard"]
    BOOKS_V --> BC_FILT["books/BookFilters"]

    DETAIL_V --> READER["books/BookReader<br/>iframe → reader.html<br/>Foliate.js"]
    DETAIL_V --> RATING["books/BookRating<br/>useBookRating composable"]
    DETAIL_V --> COMM["books/BookComments<br/>useBookComments composable"]
    DETAIL_V --> FAVBTN["books/FavoriteButton<br/>useFavorites composable"]

    ADMIN_V --> UPLOAD["admin/BookUploadForm"]
    ADMIN_V --> BOOK_LIST["admin/AdminBookList"]
```

```js
// Пример использования
import { api } from "@/api/index.js";
const books = await api.get("/books?page=0&size=20");
await api.post("/ratings", { bookId: 42, rating: 5 });
```

## 🤖 AI Chat Widget

> [!TIP]
> `session_id` генерируется один раз и сохраняется в `localStorage`. История диалога хранится на сервере в Redis — не теряется при перезагрузке страницы.

`AiChatWidget.vue` — всплывающий виджет (кнопка в углу страницы). Работает на всех страницах.

- `session_id` генерируется один раз и сохраняется в `localStorage`
- История диалога хранится на сервере в Redis (`chat:history:{session_id}`)
- Каждое сообщение: `POST /api/ai/chat { message, session_id }`
- Ответ содержит поле `sources` — список текстовых фрагментов из книг (ссылки-источники)

## 📖 Встроенный ридер

`/reader.html` (статический файл в `public/`) загружает Foliate.js и принимает параметр `?bookUrl=...` для чтения FB2/EPUB прямо в браузере.

Текущая позиция отправляется через `useReadingSession` composable: `PUT /api/readings/session/update`.

## 🚀 Сборка и запуск

```bash
# Dev-сервер
cd digital-library-frontend
npm install
npm run dev        # http://localhost:5173

# Production build
npm run build      # dist/
```

В Docker книги раздаются через Nginx (конфиг: `digital-library-frontend/nginx.conf`). Nginx проксирует `/api/*` → `api-gateway:8080`.
