<template>
  <div class="modal-overlay" @click="$emit('close')">
    <div class="modal-content large" @click.stop>
      <h2 class="modal-title">
        {{ book ? "Редактировать книгу" : "Добавить книгу" }}
      </h2>

      <!-- Drag & Drop зона для загрузки FB2 -->
      <div
        v-if="!book"
        class="file-upload-zone"
        :class="{ 'drag-over': isDragOver }"
        @dragover.prevent="isDragOver = true"
        @dragleave.prevent="isDragOver = false"
        @drop.prevent="handleFileDrop"
        @click="$refs.fileInput.click()"
      >
        <div v-if="!uploadedFile" class="upload-placeholder">
          <span class="upload-icon">📚</span>
          <p><strong>Перетащите FB2 файл сюда</strong></p>
          <p class="upload-hint">или кликните для выбора файла</p>
          <input
            ref="fileInput"
            type="file"
            accept=".fb2"
            style="display: none"
            @change="handleFileSelect"
          />
        </div>
        <div v-else class="file-info">
          <span class="file-icon">✅</span>
          <div class="file-details">
            <strong>{{ uploadedFile.name }}</strong>
            <small>{{ formatFileSize(uploadedFile.size) }}</small>
          </div>
          <button @click.stop="removeFile" class="btn-remove-file">✕</button>
        </div>
      </div>

      <!-- Индикатор парсинга -->
      <div v-if="parsing" class="parsing-status">Обработка файла...</div>

      <!-- Кнопка AI-заполнения (показывается только когда есть название) -->
      <div v-if="form.title && !isEditing" class="ai-enrich-bar">
        <button
          class="btn-ai-enrich"
          :disabled="enriching || showAiPanel"
          @click="handleEnrich"
        >
          {{
            enriching
              ? "Запрос к DeepSeek..."
              : showAiPanel
                ? "Панель открыта"
                : "Заполнить метаданные через AI"
          }}
        </button>
        <span v-if="enriched && !showAiPanel" class="enrich-ok"
          >✔ Применено</span
        >
      </div>

      <!-- Панель предпросмотра AI-предложений -->
      <div v-if="showAiPanel && aiSuggestions" class="ai-suggestions-panel">
        <div class="ai-panel-header">
          <span>✨ AI-предложения — выберите что применить</span>
          <button class="ai-panel-close" @click="showAiPanel = false">✕</button>
        </div>
        <div class="ai-panel-body">
          <label v-if="aiSuggestions.description" class="ai-field-row">
            <input type="checkbox" v-model="aiSelected.description" />
            <span class="ai-field-name">Описание</span>
            <span class="ai-field-preview"
              >{{ aiSuggestions.description.slice(0, 120)
              }}{{ aiSuggestions.description.length > 120 ? "…" : "" }}</span
            >
          </label>
          <label v-if="aiSuggestions.publication_year" class="ai-field-row">
            <input type="checkbox" v-model="aiSelected.publication_year" />
            <span class="ai-field-name">Год публикации</span>
            <span class="ai-field-value">{{
              aiSuggestions.publication_year
            }}</span>
          </label>
          <label v-if="aiSuggestions.language" class="ai-field-row">
            <input type="checkbox" v-model="aiSelected.language" />
            <span class="ai-field-name">Язык</span>
            <span class="ai-field-value">{{ aiSuggestions.language }}</span>
          </label>
          <label v-if="aiSuggestions.age_rating" class="ai-field-row">
            <input type="checkbox" v-model="aiSelected.age_rating" />
            <span class="ai-field-name">Возр. рейтинг</span>
            <span class="ai-field-value">{{ aiSuggestions.age_rating }}</span>
          </label>
          <label v-if="aiSuggestions.series_name" class="ai-field-row">
            <input type="checkbox" v-model="aiSelected.series_name" />
            <span class="ai-field-name">Серия</span>
            <span class="ai-field-value"
              >{{ aiSuggestions.series_name
              }}<span v-if="aiSuggestions.series_number">
                #{{ aiSuggestions.series_number }}</span
              ></span
            >
          </label>
          <label v-if="aiSuggestions.genres?.length" class="ai-field-row">
            <input type="checkbox" v-model="aiSelected.genres" />
            <span class="ai-field-name">Жанры</span>
            <span class="ai-field-value ai-chips-preview">
              <span
                v-for="g in aiSuggestions.genres"
                :key="g"
                class="ai-chip"
                >{{ g }}</span
              >
            </span>
          </label>
          <label v-if="aiSuggestions.tags?.length" class="ai-field-row">
            <input type="checkbox" v-model="aiSelected.tags" />
            <span class="ai-field-name">Теги</span>
            <span class="ai-field-value ai-chips-preview">
              <span v-for="t in aiSuggestions.tags" :key="t" class="ai-chip">{{
                t
              }}</span>
            </span>
          </label>
        </div>
        <div class="ai-panel-footer">
          <button
            class="btn-secondary"
            style="padding: 0.4rem 1rem; font-size: 0.85rem"
            @click="showAiPanel = false"
          >
            Отмена
          </button>
          <button
            class="btn-ai-apply"
            :disabled="applying"
            @click="applyAiSuggestions"
          >
            {{ applying ? "Применяю..." : "Применить выбранное" }}
          </button>
        </div>
      </div>

      <div class="book-form">
        <!-- Название -->
        <div class="form-group">
          <label>Название *</label>
          <input
            v-model="form.title"
            type="text"
            class="form-input"
            :class="{ 'auto-filled': autoFilledFields.title }"
            placeholder="Введите название книги"
          />
          <small v-if="autoFilledFields.title" class="auto-fill-hint">
            ✨ Автозаполнено из FB2
          </small>
        </div>

        <!-- Описание -->
        <div class="form-group">
          <label>Описание</label>
          <textarea
            v-model="form.description"
            class="form-textarea"
            :class="{ 'auto-filled': autoFilledFields.description }"
            placeholder="Введите описание книги"
            rows="4"
          ></textarea>
          <small v-if="autoFilledFields.description" class="auto-fill-hint">
            Автозаполнено из FB2
          </small>
        </div>

        <!-- Год / Язык / Возр. рейтинг (строка) -->
        <div class="form-row">
          <div class="form-group">
            <label>Год публикации</label>
            <input
              v-model.number="form.publicationYear"
              type="number"
              class="form-input"
              :class="{ 'auto-filled': autoFilledFields.publicationYear }"
              placeholder="2024"
              min="1000"
              max="2100"
            />
          </div>
          <div class="form-group">
            <label>Язык</label>
            <input
              v-model="form.language"
              type="text"
              class="form-input"
              :class="{ 'auto-filled': autoFilledFields.language }"
              placeholder="ru"
              maxlength="10"
            />
          </div>
          <div class="form-group">
            <label>Возраст. рейтинг</label>
            <select
              v-model="form.ageRating"
              class="form-input"
              :class="{ 'auto-filled': autoFilledFields.ageRating }"
            >
              <option value="">—</option>
              <option>0+</option>
              <option>6+</option>
              <option>12+</option>
              <option>16+</option>
              <option>18+</option>
            </select>
          </div>
        </div>

        <!-- Серия -->
        <div class="form-row">
          <div class="form-group" style="flex: 2">
            <label>Название серии</label>
            <input
              v-model="form.seriesName"
              type="text"
              class="form-input"
              :class="{ 'auto-filled': autoFilledFields.seriesName }"
              placeholder="Гарри Поттер"
            />
          </div>
          <div class="form-group" style="flex: 1">
            <label>№ в серии</label>
            <input
              v-model.number="form.seriesNumber"
              type="number"
              class="form-input"
              :class="{ 'auto-filled': autoFilledFields.seriesNumber }"
              placeholder="1"
              min="1"
            />
          </div>
        </div>

        <!-- Авторы -->
        <div class="form-group">
          <label>Авторы *</label>
          <div class="multi-select-container">
            <input
              v-model="authorSearchQuery"
              type="text"
              placeholder="🔍 Поиск автора..."
              class="form-input"
              @input="searchAuthors"
            />
            <div class="selected-items">
              <span
                v-for="authorId in form.authorIds"
                :key="authorId"
                class="selected-tag"
                :class="{ 'matched-tag': matchedAuthors.includes(authorId) }"
              >
                {{ getAuthorName(authorId) }}
                <button @click="removeAuthor(authorId)" class="remove-tag">
                  ×
                </button>
              </span>
            </div>
            <div v-if="filteredAuthors.length > 0" class="dropdown-list">
              <div
                v-for="author in filteredAuthors"
                :key="author.id"
                @click="addAuthor(author.id)"
                class="dropdown-item"
                :class="{ selected: form.authorIds.includes(author.id) }"
              >
                {{ author.firstName }} {{ author.lastName }}
              </div>
            </div>
          </div>
        </div>

        <!-- Жанры -->
        <div class="form-group">
          <label>Жанры *</label>
          <div class="multi-select-container">
            <input
              v-model="genreSearchQuery"
              type="text"
              placeholder="🔍 Поиск жанра..."
              class="form-input"
              @input="searchGenres"
            />
            <div class="selected-items">
              <span
                v-for="genreId in form.genreIds"
                :key="genreId"
                class="selected-tag"
                :class="{ 'matched-tag': matchedGenres.includes(genreId) }"
              >
                {{ getGenreName(genreId) }}
                <button @click="removeGenre(genreId)" class="remove-tag">
                  ×
                </button>
              </span>
            </div>
            <div v-if="filteredGenres.length > 0" class="dropdown-list">
              <div
                v-for="genre in filteredGenres"
                :key="genre.id"
                @click="addGenre(genre.id)"
                class="dropdown-item"
                :class="{ selected: form.genreIds.includes(genre.id) }"
              >
                {{ genre.name }}
              </div>
            </div>
          </div>
        </div>

        <!-- Теги -->
        <div class="form-group">
          <label
            >Теги
            <span class="field-hint">(тематика, настроение, стиль)</span></label
          >
          <div class="multi-select-container">
            <input
              v-model="tagSearchQuery"
              type="text"
              placeholder="🔍 Поиск тега..."
              class="form-input"
              @input="searchTags"
            />
            <div class="selected-items">
              <span
                v-for="tagId in form.tagIds"
                :key="tagId"
                class="selected-tag"
                :class="{ 'matched-tag': matchedTags.includes(tagId) }"
              >
                {{ getTagName(tagId) }}
                <button @click="removeTag(tagId)" class="remove-tag">×</button>
              </span>
              <span v-if="!form.tagIds.length" class="no-tags-hint"
                >не выбрано</span
              >
            </div>
            <div v-if="filteredTags.length > 0" class="dropdown-list">
              <div
                v-for="tag in filteredTags"
                :key="tag.id"
                @click="addTag(tag.id)"
                class="dropdown-item"
                :class="{ selected: form.tagIds.includes(tag.id) }"
              >
                <span>{{ tag.name }}</span>
                <small v-if="tag.category" class="tag-category">{{
                  tag.category
                }}</small>
              </div>
            </div>
          </div>
        </div>

        <!-- Путь к файлу -->
        <!--        <div v-if="!uploadedFile" class="form-group">-->
        <!--          <label>Путь к файлу *</label>-->
        <!--          <input-->
        <!--              v-model="form.filePath"-->
        <!--              type="text"-->
        <!--              class="form-input"-->
        <!--              placeholder="Например: /Автор.Книга.fb2"-->
        <!--          />-->
        <!--        </div>-->
        <!--        -->
        <!--        -->
        <!--        <div v-else class="form-group">-->
        <!--          <label>Файл загружен</label>-->
        <!--          <div class="file-path-display">-->
        <!--            ✅ {{ form.filePath || 'Будет сохранён при нажатии "Сохранить"' }}-->
        <!--          </div>-->
        <!--        </div>-->
      </div>

      <div class="modal-buttons">
        <button @click="$emit('close')" class="btn-secondary">Отмена</button>
        <button
          @click="handleSave"
          class="btn-primary"
          :disabled="!isValid || loading"
        >
          {{ loading ? "Сохранение..." : "Сохранить" }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from "vue";
import { getCookie } from "@/utils/cookies";
import { enrichBookMeta } from "@/api/aiAdmin";

// === Props и emits ===
const props = defineProps({
  book: { type: Object, default: null },
  authors: { type: Array, required: true },
  genres: { type: Array, required: true },
  tags: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
});

const emit = defineEmits(["close", "save"]);

// === Состояние формы и загрузки ===
const form = ref({
  title: "",
  description: "",
  authorIds: [],
  genreIds: [],
  tagIds: [],
  filePath: "",
  publicationYear: null,
  language: "ru",
  ageRating: "",
  seriesName: "",
  seriesNumber: null,
  wordCount: null,
});

const uploadedFile = ref(null);
const isDragOver = ref(false);
const parsing = ref(false);
const enriching = ref(false);
const enriched = ref(false);
// AI panel state
const aiSuggestions = ref(null); // ответ DeepSeek
const aiSelected = ref({}); // какие поля выбрал пользователь
const showAiPanel = ref(false); // показывать ли панель
const applying = ref(false); // идёт ли применение
const autoFilledFields = ref({
  title: false,
  description: false,
  publicationYear: false,
  language: false,
  ageRating: false,
  seriesName: false,
  seriesNumber: false,
});
// Теги, созданные «на лету» при AI-обогащении (не было в props.tags)
const localNewTags = ref([]);
// Объединённый список для поиска/отображения внутри модалки
const allTagsList = computed(() => [...props.tags, ...localNewTags.value]);

const matchedAuthors = ref([]);
const matchedGenres = ref([]);
const matchedTags = ref([]);

const authorSearchQuery = ref("");
const genreSearchQuery = ref("");
const tagSearchQuery = ref("");
const filteredAuthors = ref([]);
const filteredGenres = ref([]);
const filteredTags = ref([]);

// === Валидация формы ===
const isEditing = computed(() => !!props.book);

const isValid = computed(() => {
  const hasFile = isEditing.value
    ? true // При редактировании не требуем файл/путь — он уже существует
    : uploadedFile.value; // При создании — обязателен файл

  return (
    form.value.title.trim() &&
    form.value.authorIds.length > 0 &&
    form.value.genreIds.length > 0 &&
    hasFile
  );
});

// === Работа с файлами (Drag & Drop + парсинг FB2) ===

function handleFileDrop(event) {
  isDragOver.value = false;
  const files = event.dataTransfer.files;
  if (files.length > 0) processFile(files[0]);
}

function handleFileSelect(event) {
  const files = event.target.files;
  if (files.length > 0) processFile(files[0]);
}

function removeFile() {
  uploadedFile.value = null;
  form.value.filePath = "";
  form.value.wordCount = null;
  autoFilledFields.value = {
    title: false,
    description: false,
    publicationYear: false,
    language: false,
    ageRating: false,
    seriesName: false,
    seriesNumber: false,
  };
  matchedAuthors.value = [];
  matchedGenres.value = [];
  matchedTags.value = [];
  form.value.tagIds = [];
  enriched.value = false;
}

function formatFileSize(bytes) {
  if (bytes < 1024) return bytes + " B";
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + " KB";
  return (bytes / (1024 * 1024)).toFixed(1) + " MB";
}

// Парсит FB2-файл и извлекает метаданные
async function processFile(file) {
  if (!file.name.toLowerCase().endsWith(".fb2")) {
    alert("Поддерживаются только файлы .fb2");
    return;
  }

  uploadedFile.value = file;
  parsing.value = true;

  try {
    const arrayBuffer = await file.arrayBuffer();
    const utf8Text = new TextDecoder("utf-8").decode(arrayBuffer);

    const encodingMatch = utf8Text.match(
      /^<\?xml\s+version\s*=\s*["'][\d.]+["']\s+encoding\s*=\s*["']([^"']+)["']/i,
    );
    let encoding = "utf-8";
    if (encodingMatch) encoding = encodingMatch[1].toLowerCase();

    let text;
    try {
      text = new TextDecoder(encoding).decode(arrayBuffer);
    } catch (e) {
      text = new TextDecoder("utf-8").decode(arrayBuffer);
    }

    const parser = new DOMParser();
    const xmlDoc = parser.parseFromString(text, "application/xml");
    const parseError = xmlDoc.querySelector("parsererror");
    if (parseError)
      throw new Error("Ошибка парсинга XML: " + parseError.textContent);

    const metadata = extractFB2Metadata(xmlDoc);

    if (metadata.title) {
      form.value.title = metadata.title;
      autoFilledFields.value.title = true;
    }
    if (metadata.description) {
      form.value.description = metadata.description;
      autoFilledFields.value.description = true;
    }
    if (metadata.wordCount) {
      form.value.wordCount = metadata.wordCount;
    }
    if (metadata.authors.length > 0) matchAuthors(metadata.authors);
    if (metadata.genres.length > 0) matchGenres(metadata.genres);
  } catch (error) {
    alert("Не удалось обработать файл: " + error.message);
  } finally {
    parsing.value = false;
  }
}

// Извлекает метаданные из FB2-документа
function extractFB2Metadata(xmlDoc) {
  const getElementText = (tagName, parent = xmlDoc) => {
    const el = parent.querySelector(tagName);
    return el?.textContent?.trim() || "";
  };

  const title = getElementText("book-title");

  let description = "";
  const annotation = xmlDoc.querySelector("annotation");
  if (annotation) {
    const paragraphs = Array.from(annotation.querySelectorAll("p"));
    description =
      paragraphs.length > 0
        ? paragraphs.map((p) => p.textContent.trim()).join("\n\n")
        : annotation.textContent.trim();
  }

  const authors = Array.from(xmlDoc.querySelectorAll("title-info > author"))
    .map((authorEl) => ({
      firstName: getElementText("first-name", authorEl),
      lastName: getElementText("last-name", authorEl),
    }))
    .filter((a) => a.firstName || a.lastName);

  const genres = Array.from(xmlDoc.querySelectorAll("title-info > genre"))
    .map((g) => g.textContent.trim())
    .filter((g) => g);

  // Подсчёт слов из тела книги (храним в тысячах: 121000 слов → 121)
  let wordCount = null;
  const bodyEl = xmlDoc.querySelector("body");
  if (bodyEl) {
    const rawText = bodyEl.textContent || "";
    const words = rawText
      .trim()
      .split(/\s+/)
      .filter((w) => w.length > 0);
    const thousands = Math.round(words.length / 1000);
    wordCount = thousands > 0 ? thousands : null;
  }

  return { title, description, authors, genres, wordCount };
}

// === AI-заполнение через DeepSeek ===

/** Запрашивает данные у DeepSeek и показывает панель предпросмотра */
async function handleEnrich() {
  if (!form.value.title.trim() || enriching.value) return;

  const authorNames = form.value.authorIds
    .map((id) => {
      const a = props.authors.find((a) => a.id === id);
      return a ? `${a.firstName} ${a.lastName}`.trim() : null;
    })
    .filter(Boolean);

  enriching.value = true;
  try {
    const meta = await enrichBookMeta(form.value.title.trim(), authorNames);
    aiSuggestions.value = meta;
    // По умолчанию — всё выбрано
    aiSelected.value = {
      description: !!meta.description,
      publication_year: !!meta.publication_year,
      language: !!meta.language,
      age_rating: !!meta.age_rating,
      series_name: !!meta.series_name,
      genres: !!meta.genres?.length,
      tags: !!meta.tags?.length,
    };
    showAiPanel.value = true;
  } catch (err) {
    alert("Ошибка запроса к DeepSeek: " + err.message);
  } finally {
    enriching.value = false;
  }
}

/** Применяет только отмеченные пользователем поля */
async function applyAiSuggestions() {
  if (!aiSuggestions.value || applying.value) return;
  applying.value = true;
  const meta = aiSuggestions.value;
  const sel = aiSelected.value;

  if (sel.description && meta.description) {
    form.value.description = meta.description;
  }
  if (sel.publication_year && meta.publication_year) {
    form.value.publicationYear = meta.publication_year;
  }
  if (sel.language && meta.language) {
    form.value.language = meta.language;
  }
  if (sel.age_rating && meta.age_rating) {
    form.value.ageRating = meta.age_rating;
  }
  if (sel.series_name && meta.series_name) {
    form.value.seriesName = meta.series_name;
    if (meta.series_number) form.value.seriesNumber = meta.series_number;
  }
  if (sel.genres && meta.genres?.length) {
    matchGenres(meta.genres);
  }
  if (sel.tags && meta.tags?.length) {
    await matchOrCreateTags(meta.tags);
  }

  enriched.value = true;
  showAiPanel.value = false;
  applying.value = false;
}

// === Работа с авторами ===

function matchAuthors(extractedAuthors) {
  const matched = [];
  for (const extracted of extractedAuthors) {
    // Точное совпадение
    let found = props.authors.find(
      (dbAuthor) =>
        dbAuthor.firstName.toLowerCase() ===
          extracted.firstName.toLowerCase() &&
        dbAuthor.lastName.toLowerCase() === extracted.lastName.toLowerCase(),
    );

    // Поиск по фамилии
    if (!found && extracted.lastName) {
      found = props.authors.find(
        (dbAuthor) =>
          dbAuthor.lastName.toLowerCase() === extracted.lastName.toLowerCase(),
      );
    }

    // Нечёткий поиск по фамилии
    if (!found && extracted.lastName) {
      found = props.authors.find(
        (dbAuthor) =>
          dbAuthor.lastName
            .toLowerCase()
            .includes(extracted.lastName.toLowerCase()) ||
          extracted.lastName
            .toLowerCase()
            .includes(dbAuthor.lastName.toLowerCase()),
      );
    }

    if (found && !form.value.authorIds.includes(found.id)) {
      form.value.authorIds.push(found.id);
      matched.push(found.id);
    }
  }
  matchedAuthors.value = matched;
}

function searchAuthors() {
  if (!authorSearchQuery.value.trim()) {
    filteredAuthors.value = [];
    return;
  }
  const query = authorSearchQuery.value.toLowerCase();
  filteredAuthors.value = props.authors
    .filter((author) => {
      const fullName = `${author.firstName} ${author.lastName}`.toLowerCase();
      return (
        fullName.includes(query) && !form.value.authorIds.includes(author.id)
      );
    })
    .slice(0, 10);
}

function addAuthor(authorId) {
  if (!form.value.authorIds.includes(authorId)) {
    form.value.authorIds.push(authorId);
  }
  authorSearchQuery.value = "";
  filteredAuthors.value = [];
}

function removeAuthor(authorId) {
  const index = form.value.authorIds.indexOf(authorId);
  if (index > -1) form.value.authorIds.splice(index, 1);
  const matchedIndex = matchedAuthors.value.indexOf(authorId);
  if (matchedIndex > -1) matchedAuthors.value.splice(matchedIndex, 1);
}

function getAuthorName(authorId) {
  const author = props.authors.find((a) => a.id === authorId);
  return author ? `${author.firstName} ${author.lastName}` : "Неизвестно";
}

// === Работа с жанрами ===

function matchGenres(extractedGenres) {
  const matched = [];
  for (const extracted of extractedGenres) {
    const found = props.genres.find(
      (dbGenre) => dbGenre.name.toLowerCase() === extracted.toLowerCase(),
    );
    if (found && !form.value.genreIds.includes(found.id)) {
      form.value.genreIds.push(found.id);
      matched.push(found.id);
    }
  }
  matchedGenres.value = matched;
}

function searchGenres() {
  if (!genreSearchQuery.value.trim()) {
    filteredGenres.value = [];
    return;
  }
  const query = genreSearchQuery.value.toLowerCase();
  filteredGenres.value = props.genres
    .filter((genre) => {
      return (
        genre.name.toLowerCase().includes(query) &&
        !form.value.genreIds.includes(genre.id)
      );
    })
    .slice(0, 10);
}

function addGenre(genreId) {
  if (!form.value.genreIds.includes(genreId)) {
    form.value.genreIds.push(genreId);
  }
  genreSearchQuery.value = "";
  filteredGenres.value = [];
}

function removeGenre(genreId) {
  const index = form.value.genreIds.indexOf(genreId);
  if (index > -1) form.value.genreIds.splice(index, 1);
  const matchedIndex = matchedGenres.value.indexOf(genreId);
  if (matchedIndex > -1) matchedGenres.value.splice(matchedIndex, 1);
}

function getGenreName(genreId) {
  const genre = props.genres.find((g) => g.id === genreId);
  return genre ? genre.name : "Неизвестно";
}

// === Работа с тегами ===

/**
 * Для каждого имени тега от AI:
 *  - если тег уже есть в props.tags или localNewTags — берём его ID
 *  - если нет — создаём через POST /api/tags и сохраняем в localNewTags
 */
async function matchOrCreateTags(aiTagNames) {
  const jwt = getCookie("jwt");
  const matched = [];
  const failedNames = [];

  for (const name of aiTagNames) {
    if (!name?.trim()) continue;

    // Ищем в объединённом списке (props + уже созданные в этой сессии)
    let found = allTagsList.value.find(
      (t) => t.name.toLowerCase() === name.trim().toLowerCase(),
    );

    if (!found) {
      // Тега нет — создаём (или возвращаем существующий через find-or-create)
      try {
        const res = await fetch(
          `/api/tags?name=${encodeURIComponent(name.trim())}`,
          {
            method: "POST",
            headers: { Authorization: `Bearer ${jwt}` },
          },
        );
        if (res.ok) {
          found = await res.json(); // { id, name, category }
          // Добавляем в локальный список, если его ещё нет
          if (!allTagsList.value.find((t) => t.id === found.id)) {
            localNewTags.value.push(found);
          }
        } else {
          const errText = await res.text().catch(() => String(res.status));
          failedNames.push(name);
        }
      } catch (e) {
        failedNames.push(name);
      }
    }

    if (found && !form.value.tagIds.includes(found.id)) {
      form.value.tagIds.push(found.id);
      matched.push(found.id);
    }
  }
  matchedTags.value = matched;
  if (failedNames.length) {
    alert(`Не удалось создать теги: ${failedNames.join(", ")}`);
  }
}

function searchTags() {
  if (!tagSearchQuery.value.trim()) {
    filteredTags.value = [];
    return;
  }
  const query = tagSearchQuery.value.toLowerCase();
  filteredTags.value = allTagsList.value
    .filter((tag) => {
      return (
        tag.name.toLowerCase().includes(query) &&
        !form.value.tagIds.includes(tag.id)
      );
    })
    .slice(0, 15);
}

function addTag(tagId) {
  if (!form.value.tagIds.includes(tagId)) {
    form.value.tagIds.push(tagId);
  }
  tagSearchQuery.value = "";
  filteredTags.value = [];
}

function removeTag(tagId) {
  const index = form.value.tagIds.indexOf(tagId);
  if (index > -1) form.value.tagIds.splice(index, 1);
  const matchedIndex = matchedTags.value.indexOf(tagId);
  if (matchedIndex > -1) matchedTags.value.splice(matchedIndex, 1);
}

function getTagName(tagId) {
  const tag = allTagsList.value.find((t) => t.id === tagId);
  return tag ? tag.name : "?";
}

// === Сохранение данных ===

async function handleSave() {
  if (!isValid.value) return;

  let currentFilePath = form.value.filePath;

  // Если загружен новый файл — загружаем его и получаем новый путь
  if (uploadedFile.value) {
    try {
      parsing.value = true;

      const firstAuthorId = form.value.authorIds[0];
      const author = props.authors.find((a) => a.id === firstAuthorId);
      const authorLastName = author?.lastName || "";
      const bookTitle = form.value.title || "";

      const formData = new FormData();
      formData.append("file", uploadedFile.value);
      formData.append("authorLastName", authorLastName);
      formData.append("bookTitle", bookTitle);

      const jwt = getCookie("jwt");
      const response = await fetch("/api/books/upload", {
        method: "POST",
        headers: { Authorization: `Bearer ${jwt}` },
        body: formData,
      });

      if (!response.ok) throw new Error("Ошибка загрузки файла");
      const uploadResult = await response.json();
      currentFilePath = uploadResult.filePath; // обновляем путь после загрузки
    } catch (error) {
      alert("Не удалось загрузить файл: " + error.message);
      parsing.value = false;
      return;
    } finally {
      parsing.value = false;
    }
  }

  // Формируем payload для отправки
  const payload = {
    title: form.value.title.trim(),
    description: form.value.description || "",
    authorIds: [...form.value.authorIds],
    genreIds: [...form.value.genreIds],
    tagIds: form.value.tagIds.length ? [...form.value.tagIds] : null,
    publicationYear: form.value.publicationYear || null,
    language: form.value.language || null,
    ageRating: form.value.ageRating || null,
    seriesName: form.value.seriesName || null,
    seriesNumber: form.value.seriesNumber || null,
    wordCount: form.value.wordCount || null,
  };

  // Отправляем filePath ТОЛЬКО если:
  // - это новая книга (props.book === null) → тогда filePath обязан быть (он пришёл от загрузки)
  // - или если это редактирование, но файл был загружен заново (uploadedFile.value)
  // В остальных случаях (редактирование без нового файла) — НЕ отправляем filePath
  if (uploadedFile.value || !props.book) {
    // Для новой книги filePath должен быть (иначе isValid не пропустил бы)
    // Для редактирования с новым файлом — тоже отправляем
    payload.filePath = currentFilePath;
  }
  // Если редактируем и файл не меняли — filePath НЕ включаем в payload

  emit("save", payload);
}

// === Инициализация формы при открытии ===

watch(
  () => props.book,
  async (newBook) => {
    if (newBook) {
      try {
        const jwt = getCookie("jwt");
        const response = await fetch(`/api/books/${newBook.id}`, {
          headers: { Authorization: `Bearer ${jwt}` },
        });

        if (response.ok) {
          const bookDetails = await response.json();

          const authorIds = props.authors
            .filter((a) =>
              newBook.authors.includes(`${a.firstName} ${a.lastName}`),
            )
            .map((a) => a.id);

          const genreIds = props.genres
            .filter((g) => newBook.genres.includes(g.name))
            .map((g) => g.id);

          // Теги: bookDetails.tags — массив строк (имён), сопоставляем с props.tags
          const tagIds = props.tags
            .filter((t) => (bookDetails.tags || []).includes(t.name))
            .map((t) => t.id);

          form.value = {
            title: bookDetails.title || "",
            description: bookDetails.description || "",
            authorIds: authorIds,
            genreIds: genreIds,
            tagIds: tagIds,
            filePath: bookDetails.filePath || "",
            publicationYear: bookDetails.publicationYear || null,
            language: bookDetails.language || "ru",
            ageRating: bookDetails.ageRating || "",
            seriesName: bookDetails.seriesName || "",
            seriesNumber: bookDetails.seriesNumber || null,
            wordCount: bookDetails.wordCount || null,
          };
        }
      } catch (error) {
      }
    } else {
      form.value = {
        title: "",
        description: "",
        authorIds: [],
        genreIds: [],
        tagIds: [],
        filePath: "",
        publicationYear: null,
        language: "ru",
        ageRating: "",
        seriesName: "",
        seriesNumber: null,
        wordCount: null,
      };
      autoFilledFields.value = {
        title: false,
        description: false,
        publicationYear: false,
        language: false,
        ageRating: false,
        seriesName: false,
        seriesNumber: false,
      };
      matchedAuthors.value = [];
      matchedGenres.value = [];
      matchedTags.value = [];
      enriched.value = false;
    }
  },
  { immediate: true },
);
</script>

<style scoped>
.file-upload-zone {
  border: 2px dashed #d0d0d0;
  border-radius: 12px;
  padding: 2rem;
  margin-bottom: 2rem;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s ease;
  background: #f9f9f9;
}

.file-upload-zone:hover {
  border-color: #5c4033;
  background: #fdf6e9;
}

.file-upload-zone.drag-over {
  border-color: #5c4033;
  background: #e8d9c7;
  transform: scale(1.02);
}

.upload-placeholder {
  padding: 1rem;
}

.upload-icon {
  font-size: 3rem;
  display: block;
  margin-bottom: 1rem;
}

.upload-hint {
  color: #999;
  font-size: 0.9rem;
  margin-top: 0.5rem;
}

.file-info {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1rem;
  background: white;
  border-radius: 8px;
}

.file-icon {
  font-size: 2rem;
}

.file-details {
  flex: 1;
  text-align: left;
}

.file-details strong {
  display: block;
  color: #5c4033;
}

.file-details small {
  color: #999;
  font-size: 0.85rem;
}

.btn-remove-file {
  background: #dc3545;
  color: white;
  border: none;
  border-radius: 50%;
  width: 30px;
  height: 30px;
  cursor: pointer;
  font-size: 1.2rem;
  transition: background 0.2s;
}

.btn-remove-file:hover {
  background: #c82333;
}

.parsing-status {
  text-align: center;
  padding: 1rem;
  background: #fff3cd;
  border-radius: 8px;
  margin-bottom: 1rem;
  color: #856404;
  font-weight: 500;
}

.auto-filled {
  border-color: #28a745 !important;
  background-color: #f0fff4;
}

.auto-fill-hint {
  color: #28a745;
  font-size: 0.85rem;
  margin-top: 0.25rem;
  display: block;
}

.matched-tag {
  background: #d4edda !important;
  border-color: #28a745 !important;
  color: #155724 !important;
}

.file-path-display {
  padding: 0.75rem;
  background: #f0fff4;
  border: 1px solid #28a745;
  border-radius: 6px;
  color: #155724;
  font-family: monospace;
  font-size: 0.9rem;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 12px;
  padding: 2rem;
  max-width: 600px;
  width: 90%;
  max-height: 90vh;
  overflow-y: auto;
}

.modal-content.large {
  max-width: 800px;
}

.modal-title {
  margin-bottom: 1.5rem;
  color: #5c4033;
}

.book-form {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.form-group label {
  display: block;
  margin-bottom: 0.5rem;
  color: #5c4033;
  font-weight: 600;
}

.form-input,
.form-textarea {
  width: 100%;
  padding: 0.75rem;
  border: 1px solid #d0d0d0;
  border-radius: 6px;
  font-size: 1rem;
  transition: border-color 0.2s;
}

.form-input:focus,
.form-textarea:focus {
  outline: none;
  border-color: #5c4033;
}

.form-hint {
  display: block;
  margin-top: 0.25rem;
  color: #999;
  font-size: 0.85rem;
}

.multi-select-container {
  position: relative;
}

.selected-items {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  margin-top: 0.5rem;
  min-height: 2rem;
}

.selected-tag {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.4rem 0.8rem;
  background: #e8d9c7;
  border: 1px solid #d0c5b5;
  border-radius: 20px;
  font-size: 0.9rem;
  color: #5c4033;
}

.remove-tag {
  background: none;
  border: none;
  color: #5c4033;
  font-size: 1.2rem;
  cursor: pointer;
  padding: 0;
  line-height: 1;
}

.dropdown-list {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  background: white;
  border: 1px solid #d0d0d0;
  border-radius: 6px;
  max-height: 200px;
  overflow-y: auto;
  z-index: 10;
  margin-top: 0.25rem;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.dropdown-item {
  padding: 0.75rem;
  cursor: pointer;
  transition: background 0.2s;
}

.dropdown-item:hover {
  background: #f5f5f5;
}

.dropdown-item.selected {
  background: #e8d9c7;
  color: #5c4033;
  font-weight: 600;
}

.modal-buttons {
  display: flex;
  gap: 1rem;
  justify-content: flex-end;
  margin-top: 2rem;
}

.btn-primary,
.btn-secondary {
  padding: 0.75rem 1.5rem;
  border-radius: 8px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  border: none;
  font-size: 1rem;
}

.btn-primary {
  background: #5c4033;
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background: #4a3329;
  transform: translateY(-2px);
}

.btn-primary:disabled {
  background: #999;
  cursor: not-allowed;
  transform: none;
}

.ai-enrich-bar {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.btn-ai-enrich {
  padding: 0.6rem 1.2rem;
  background: #1a73e8;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 0.95rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-ai-enrich:hover:not(:disabled) {
  background: #1557b0;
}

.btn-ai-enrich:disabled {
  background: #999;
  cursor: not-allowed;
}

.ai-suggestions-panel {
  border: 1px solid #b5946b;
  border-radius: 10px;
  background: #fffdf7;
  margin-bottom: 1.5rem;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(92, 64, 51, 0.1);
}

.ai-panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.7rem 1rem;
  background: linear-gradient(135deg, #f5e6c8, #eddcb8);
  font-weight: 600;
  font-size: 0.95rem;
  color: #5c4033;
}

.ai-panel-close {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 1.1rem;
  color: #5c4033;
  padding: 0 0.3rem;
  line-height: 1;
}

.ai-panel-body {
  padding: 0.5rem 0;
}

.ai-field-row {
  display: flex;
  align-items: flex-start;
  gap: 0.7rem;
  padding: 0.5rem 1rem;
  cursor: pointer;
  transition: background 0.15s;
}

.ai-field-row:hover {
  background: #fdf0dc;
}

.ai-field-row input[type="checkbox"] {
  margin-top: 0.2rem;
  flex-shrink: 0;
  width: 16px;
  height: 16px;
  accent-color: #8b5e3c;
}

.ai-field-name {
  min-width: 110px;
  font-weight: 600;
  font-size: 0.85rem;
  color: #5c4033;
  flex-shrink: 0;
}

.ai-field-preview {
  font-size: 0.85rem;
  color: #555;
  line-height: 1.4;
  font-style: italic;
}

.ai-field-value {
  font-size: 0.88rem;
  color: #333;
}

.ai-chips-preview {
  display: flex;
  flex-wrap: wrap;
  gap: 0.3rem;
}

.ai-chip {
  background: #e8d9c7;
  border-radius: 12px;
  padding: 0.15rem 0.6rem;
  font-size: 0.78rem;
  color: #5c4033;
  white-space: nowrap;
}

.ai-panel-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 0.8rem;
  padding: 0.7rem 1rem;
  background: #f5ece0;
  border-top: 1px solid #ddd;
}

.btn-ai-apply {
  background: linear-gradient(135deg, #8b5e3c, #5c4033);
  color: white;
  border: none;
  border-radius: 7px;
  padding: 0.45rem 1.2rem;
  font-size: 0.88rem;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.2s;
}

.btn-ai-apply:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.enrich-ok {
  color: #28a745;
  font-weight: 600;
  font-size: 0.9rem;
}

.form-row {
  display: flex;
  gap: 1rem;
}

.form-row .form-group {
  flex: 1;
}

.btn-secondary {
  background: #e8d9c7;
  color: #5c4033;
}

.btn-secondary:hover {
  background: #d9c9b7;
}

.field-hint {
  font-weight: 400;
  color: #999;
  font-size: 0.85rem;
}

.no-tags-hint {
  color: #bbb;
  font-size: 0.85rem;
  align-self: center;
}

.dropdown-item small.tag-category {
  display: block;
  color: #888;
  font-size: 0.8rem;
  margin-top: 1px;
}
</style>
