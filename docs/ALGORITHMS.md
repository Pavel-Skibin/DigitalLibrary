# 🧠 Алгоритмы и подходы

> [!NOTE]
> Все алгоритмы реализованы в **ai-service** (Python 3.11 / FastAPI). Остальные сервисы обращаются к нему через HTTP API.

## 💬 1. Intent Classification

Каждый запрос пользователя классифицируется за **один LLM-вызов** к DeepSeek API.

### Процесс

```mermaid
flowchart TD
    Q[Запрос пользователя] --> IC[IntentClassifierService.classify]
    IC --> LLM{LLM доступен?}
    LLM -- да --> DS[DeepSeek: system_prompt + few_shot + history + query]
    DS --> JP[_parse_llm_response: JSON → ClassifiedIntent]
    LLM -- нет --> HE[_heuristic_classify: ключевые слова]
    JP --> CI[ClassifiedIntent]
    HE --> CI
```

LLM возвращает JSON:

```json
{
  "intent": "book_question",
  "title": "Владычица озера",
  "author": "Анджей Сапковский",
  "scope": "book",
  "clean_query": "кем была Цири"
}
```

### 🏷️ Типы интентов

| Intent              | Описание                             |
| ------------------- | ------------------------------------ |
| `book_question` 🐟  | Вопрос о конкретной книге или авторе |
| `recommendation` ⭐ | Запрос на рекомендации книг          |
| `quote_search` 📝   | Поиск цитаты                         |
| `general` 🌐        | Общий вопрос по всей библиотеке      |

### 🔍 scope — область поиска

Поле `scope` определяет ширину поиска для `book_question`:

- `"book"` — пользователь назвал конкретную книгу → поиск только по ней
- `"series"` — пользователь назвал автора/серию без конкретной книги → `BookResolverService.find_series_ids()` → все книги серии

### 📞 История диалога

> [!TIP]
> История диалога хранится в Redis и автоматически подставляется в промпт — благодаря этому LLM понимает контекст уточняющих вопросов и местоимений.

Если в Redis есть история (`chat:history:{session_id}`), она добавляется в промпт перед запросом в виде блока «Предыдущий контекст диалога:». Это позволяет классификатору разрешать местоимения и уточняющие вопросы.

---

## 🔎 2. RAG Pipeline (Retrieval-Augmented Generation)

### Схема

```mermaid
sequenceDiagram
    participant U as Пользователь
    participant SA as SmartAssistant
    participant IC as IntentClassifier
    participant BR as BookResolver
    participant RS as RetrievalService
    participant LLM as DeepSeek API

    U->>SA: chat(query, session_id)
    SA->>IC: classify(query, history)
    IC-->>SA: ClassifiedIntent{intent, book_entity, scope, clean_query}
    SA->>BR: find_book_ids(title, author)
    BR-->>SA: [book_id, ...]
    SA->>RS: search_chunks(clean_query, book_ids)
    RS-->>SA: [RetrievedChunk, ...]
    SA->>LLM: generate_answer(chunks, clean_query, history)
    LLM-->>SA: answer
    SA-->>U: ChatResponse{answer, sources}
```

### 🔀 Гибридный поиск (Hybrid Search)

`RetrievalService.search_chunks()` выполняет два параллельных запроса к Qdrant:

```mermaid
flowchart LR
    Q["clean_query"] --> EMB["Embed: USER-bge-m3<br/>1024-dim vector"]
    Q --> BM25["BM25 sparse vector"]

    EMB --> DENSE["Qdrant dense search<br/>field: text_dense
топ-K результатов"]
    BM25 --> SPARSE["Qdrant sparse search<br/>field: text_sparse
топ-K результатов"]

    DENSE --> RRF["RRF Fusion<br/>k=60<br/>объединение рангов"]
    SPARSE --> RRF

    RRF --> TOP["топ-N чанков"]
    TOP --> CTX["Expand context window<br/>±RAG_CONTEXT_WINDOW соседей"]
    CTX --> DEDUP["Дедупликация<br/>(book_id, chunk_index)"]
    DEDUP --> OUT["[RetrievedChunk, ...]"]
```

1. **Dense search** — эмбеддинг запроса через `USER-bge-m3` → поиск по `text_dense`
2. **Sparse search** — BM25-индекс → поиск по `text_sparse`

Результаты объединяются через **RRF (Reciprocal Rank Fusion)**:

$$\text{RRF}(d) = \sum_{r \in R} \frac{1}{k + r(d)}, \quad k = 60$$

где $r(d)$ — позиция документа $d$ в ранжированном списке $r$.

### 📐 Контекстное окно (Context Window)

После получения топ-N чанков сервис расширяет контекст за счёт соседних чанков:

- Для каждого найденного чанка запрашиваются ±`RAG_CONTEXT_WINDOW` чанков (по `chunk_index`)
- Дедупликация по `(book_id, chunk_index)`

### 🎯 BookResolver: поиск книги по title/author

```mermaid
flowchart TD
    IN["find_book_ids(title, author)"] --> Q["query = title + author"]
    Q --> BM25S["BM25 search в<br/>books_recommendations<br/>поле: bm25_text"]
    BM25S --> CHK{"Результаты найдены?"}
    CHK -- да --> OUT["[book_id, ...]"]
    CHK -- нет --> SCROLL["Qdrant scroll<br/>фильтр: title == exact"]
    SCROLL --> CHK2{"Найдено?"}
    CHK2 -- да --> OUT
    CHK2 -- нет --> EMPTY["[]"]

    IN2["find_series_ids(series_name)"] --> SCROLL2["Qdrant scroll<br/>фильтр: series_name"]
    SCROLL2 --> PGNT["Пагинация<br/>все книги серии"]
    PGNT --> FB{"Найдено?"}
    FB -- нет --> AUTH["find_books_by_author<br/>BM25 по автору, limit=20"]
    FB -- да --> OUT2["[book_id, ...]"]
    AUTH --> OUT2
```

`BookResolverService.find_book_ids(title, author)`:

1. Строит запрос: `"{title} {author}"` (или только один из них)
2. BM25-поиск в коллекции `books_recommendations` (поле `bm25_text`)
3. Fallback: Qdrant scroll с точным фильтром по полю `title`
4. Возвращает `[book_id, ...]`

Для `scope="series"`:

- `find_series_ids(series_name)` — ищет все книги с `series_name` через scroll с пагинацией
- Fallback: `find_books_by_author(author, limit=20)` — BM25 по автору

---

## ⭐ 3. Система рекомендаций

### 👤 Персональные рекомендации

`RecommendationEngine.get_personalized_recommendations(user_id)`:

```mermaid
flowchart TD
    START(["get_personalized_recommendations(user_id)"]) --> CACHE{"Redis cache<br/>recommendations:user:{id}?"}
    CACHE -- hit --> RETURN(["Вернуть кэш"])
    CACHE -- miss --> ANCHORS["Получить якорные книги<br/>из user-service:<br/>- ratings >= 4<br/>- completed sessions<br/>- favorites"]
    ANCHORS --> HAS{"Якорные книги<br/>найдены?"}
    HAS -- нет --> COLD["Cold start:<br/>popular books<br/>по avg_rating × ln(count+1)"]
    HAS -- да --> EMBED["Получить эмбеддинги<br/>якорных книг из Qdrant"]
    EMBED --> SEARCH["Multi-vector search:<br/>похожие книги"]
    COLD --> FILTER
    SEARCH --> FILTER["Фильтры:<br/>- exclude viewed books<br/>- language filter"]
    FILTER --> SCORE["Hybrid scoring:<br/>0.7×sim + 0.3×quality"]
    SCORE --> MMR["MMR reranking:<br/>λ, MAX_PER_AUTHOR,<br/>MAX_PER_GENRE"]
    MMR --> STORE["Сохранить в Redis<br/>TTL = 1 hour"]
    STORE --> RETURN2(["Вернуть результат"])
```

### 📊 Гибридный скоринг

> [!TIP]
> `alpha=0.7` — векторное сходство весит вдвое больше, чем качество рейтинга. Логарифмическая нормализация `ratings_count` нивелирует перекос в сторону популярных книг.

`calculate_hybrid_score(vector_similarity, avg_rating, ratings_count, alpha=0.7)`:

$$\text{score} = \alpha \cdot \text{sim} + (1 - \alpha) \cdot \text{quality}$$

где:
$$\text{quality} = \frac{\text{avg\_rating}}{5} \cdot \left(0.7 + 0.3 \cdot \frac{\ln(\text{count} + 1)}{\ln(1001)}\right)$$

Логарифмическая нормализация `ratings_count` нивелирует перекос в сторону популярных книг.

### 🔥 Популярный скоринг (cold start)

`calculate_popularity_score(avg_rating, ratings_count)`:

$$\text{popularity} = \text{avg\_rating} \cdot \ln(\text{count} + 1)$$

### 🎲 MMR (Maximal Marginal Relevance)

`MMRService.apply_mmr()` перебирает кандидатов и на каждом шаге выбирает тот, у которого максимальна величина:

$$\text{MMR} = \lambda \cdot \text{Sim}(item, query) - (1 - \lambda) \cdot \max_{s \in S}[\text{Sim}(item, s)]$$

```mermaid
flowchart TD
    CANDS["Кандидаты (отсортированы по score)"] --> INIT["selected = []"]
    INIT --> LOOP{"Ещё кандидаты<br/>и len(selected) < limit?"}
    LOOP -- нет --> DONE(["Результат: selected"])
    LOOP -- да --> CALC["Для каждого кандидата c:<br/>mmr = λ×sim(c,query) - (1-λ)×max_sim(c, selected)"]
    CALC --> BEST["Выбрать c с max(mmr)"]
    BEST --> CHECK{"author_count < MAX_PER_AUTHOR<br/>AND genre_count < MAX_PER_GENRE?"}
    CHECK -- да --> ADD["selected.append(c)"]
    CHECK -- нет --> SKIP["Пропустить c"]
    ADD --> LOOP
    SKIP --> LOOP
```

Дополнительные ограничения (из `settings`):

- `MAX_PER_AUTHOR` — не более N книг одного автора в результатах
- `MAX_PER_GENRE` — не более N книг одного жанра

---

## 🗄️ 4. Векторизация книг (Indexing)

> [!NOTE]
> Векторизация одновременно индексирует книгу в две Qdrant-коллекции: чанки для RAG и мета-эмбеддинги для рекомендаций — разные модели и цели.

Процесс индексации FB2-файла в Qdrant:

```mermaid
flowchart TD
    TASK["POST /api/ai/tasks/vectorize<br/>{book_id, mode, force}"] --> DL["Скачать FB2<br/>GET /api/storage/books/{filename}"]
    DL --> PARSE["Парсинг FB2 (XML)<br/>Главы → текстовые блоки"]
    PARSE --> CHUNK["Chunking<br/>512 токенов, overlap 64"]
    CHUNK --> PARALLEL

    subgraph PARALLEL["Параллельная векторизация"]
        direction LR
        subgraph RAG["RAG pipeline"]
            E_DENSE["Embed: USER-bge-m3<br/>dense 1024-dim"]
            E_BM25["BM25 sparse vector"]
            UPSERT_RAG["Upsert в Qdrant<br/>RAG collection"]
            E_DENSE --> UPSERT_RAG
            E_BM25 --> UPSERT_RAG
        end
        subgraph RECS["Recommendations pipeline"]
            META["Метаданные книги:<br/>title + authors + genres<br/>+ series_name + description"]
            E_META["Embed: ru-en-RoSBERTa<br/>768-dim"]
            UPSERT_REC["Upsert в Qdrant<br/>recommendations collection"]
            META --> E_META --> UPSERT_REC
        end
    end

    PARALLEL --> STATUS_DONE["status = completed<br/>Redis: task status"]
```

Статус задачи отслеживается через `GET /api/ai/tasks/vectorize/status/{book_id}`.
</content>
</invoke>
