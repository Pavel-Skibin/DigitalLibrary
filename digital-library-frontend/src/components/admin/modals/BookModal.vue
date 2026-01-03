<template>
  <div class="modal-overlay" @click="$emit('close')">
    <div class="modal-content large" @click.stop>
      <h2 class="modal-title">
        {{ book ? 'Редактировать книгу' : 'Добавить книгу' }}
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
      <div v-if="parsing" class="parsing-status">
        ⏳ Обработка файла...
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
            ✨ Автозаполнено из FB2
          </small>
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
                <button @click="removeAuthor(authorId)" class="remove-tag">×</button>
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
                <button @click="removeGenre(genreId)" class="remove-tag">×</button>
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
          {{ loading ? 'Сохранение...' : 'Сохранить' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { getCookie } from '@/utils/cookies'

// === Props и emits ===
const props = defineProps({
  book: { type: Object, default: null },
  authors: { type: Array, required: true },
  genres: { type: Array, required: true },
  loading: { type: Boolean, default: false }
})

const emit = defineEmits(['close', 'save'])

// === Состояние формы и загрузки ===
const form = ref({
  title: '',
  description: '',
  authorIds: [],
  genreIds: [],
  filePath: ''
})

const uploadedFile = ref(null)
const isDragOver = ref(false)
const parsing = ref(false)
const autoFilledFields = ref({ title: false, description: false })
const matchedAuthors = ref([])
const matchedGenres = ref([])

const authorSearchQuery = ref('')
const genreSearchQuery = ref('')
const filteredAuthors = ref([])
const filteredGenres = ref([])

// === Валидация формы ===
const isEditing = computed(() => !!props.book)

const isValid = computed(() => {
  const hasFile = isEditing.value
      ? true // При редактировании не требуем файл/путь — он уже существует
      : uploadedFile.value // При создании — обязателен файл

  return (
      form.value.title.trim() &&
      form.value.authorIds.length > 0 &&
      form.value.genreIds.length > 0 &&
      hasFile
  )
})

// === Работа с файлами (Drag & Drop + парсинг FB2) ===

function handleFileDrop(event) {
  isDragOver.value = false
  const files = event.dataTransfer.files
  if (files.length > 0) processFile(files[0])
}

function handleFileSelect(event) {
  const files = event.target.files
  if (files.length > 0) processFile(files[0])
}

function removeFile() {
  uploadedFile.value = null
  form.value.filePath = ''
  autoFilledFields.value = { title: false, description: false }
  matchedAuthors.value = []
  matchedGenres.value = []
}

function formatFileSize(bytes) {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

// Парсит FB2-файл и извлекает метаданные
async function processFile(file) {
  if (!file.name.toLowerCase().endsWith('.fb2')) {
    alert('Поддерживаются только файлы .fb2')
    return
  }

  uploadedFile.value = file
  parsing.value = true

  try {
    const arrayBuffer = await file.arrayBuffer()
    const utf8Text = new TextDecoder('utf-8').decode(arrayBuffer)

    const encodingMatch = utf8Text.match(
        /^<\?xml\s+version\s*=\s*["'][\d.]+["']\s+encoding\s*=\s*["']([^"']+)["']/i
    )
    let encoding = 'utf-8'
    if (encodingMatch) encoding = encodingMatch[1].toLowerCase()

    let text
    try {
      text = new TextDecoder(encoding).decode(arrayBuffer)
    } catch (e) {
      console.warn('Не удалось декодировать с кодировкой', encoding, '- пробуем UTF-8')
      text = new TextDecoder('utf-8').decode(arrayBuffer)
    }

    const parser = new DOMParser()
    const xmlDoc = parser.parseFromString(text, 'application/xml')
    const parseError = xmlDoc.querySelector('parsererror')
    if (parseError) throw new Error('Ошибка парсинга XML: ' + parseError.textContent)

    const metadata = extractFB2Metadata(xmlDoc)

    if (metadata.title) {
      form.value.title = metadata.title
      autoFilledFields.value.title = true
    }
    if (metadata.description) {
      form.value.description = metadata.description
      autoFilledFields.value.description = true
    }
    if (metadata.authors.length > 0) matchAuthors(metadata.authors)
    if (metadata.genres.length > 0) matchGenres(metadata.genres)
  } catch (error) {
    console.error('Ошибка парсинга FB2:', error)
    alert('Не удалось обработать файл: ' + error.message)
  } finally {
    parsing.value = false
  }
}

// Извлекает метаданные из FB2-документа
function extractFB2Metadata(xmlDoc) {
  const getElementText = (tagName, parent = xmlDoc) => {
    const el = parent.querySelector(tagName)
    return el?.textContent?.trim() || ''
  }

  const title = getElementText('book-title')

  let description = ''
  const annotation = xmlDoc.querySelector('annotation')
  if (annotation) {
    const paragraphs = Array.from(annotation.querySelectorAll('p'))
    description = paragraphs.length > 0
        ? paragraphs.map(p => p.textContent.trim()).join('\n\n')
        : annotation.textContent.trim()
  }

  const authors = Array.from(xmlDoc.querySelectorAll('title-info > author')).map(authorEl => ({
    firstName: getElementText('first-name', authorEl),
    lastName: getElementText('last-name', authorEl)
  })).filter(a => a.firstName || a.lastName)

  const genres = Array.from(xmlDoc.querySelectorAll('title-info > genre'))
      .map(g => g.textContent.trim())
      .filter(g => g)

  return { title, description, authors, genres }
}

// === Работа с авторами ===

function matchAuthors(extractedAuthors) {
  const matched = []
  for (const extracted of extractedAuthors) {
    // Точное совпадение
    let found = props.authors.find(dbAuthor =>
        dbAuthor.firstName.toLowerCase() === extracted.firstName.toLowerCase() &&
        dbAuthor.lastName.toLowerCase() === extracted.lastName.toLowerCase()
    )

    // Поиск по фамилии
    if (!found && extracted.lastName) {
      found = props.authors.find(dbAuthor =>
          dbAuthor.lastName.toLowerCase() === extracted.lastName.toLowerCase()
      )
    }

    // Нечёткий поиск по фамилии
    if (!found && extracted.lastName) {
      found = props.authors.find(dbAuthor =>
          dbAuthor.lastName.toLowerCase().includes(extracted.lastName.toLowerCase()) ||
          extracted.lastName.toLowerCase().includes(dbAuthor.lastName.toLowerCase())
      )
    }

    if (found && !form.value.authorIds.includes(found.id)) {
      form.value.authorIds.push(found.id)
      matched.push(found.id)
    }
  }
  matchedAuthors.value = matched
}

function searchAuthors() {
  if (!authorSearchQuery.value.trim()) {
    filteredAuthors.value = []
    return
  }
  const query = authorSearchQuery.value.toLowerCase()
  filteredAuthors.value = props.authors
      .filter(author => {
        const fullName = `${author.firstName} ${author.lastName}`.toLowerCase()
        return fullName.includes(query) && !form.value.authorIds.includes(author.id)
      })
      .slice(0, 10)
}

function addAuthor(authorId) {
  if (!form.value.authorIds.includes(authorId)) {
    form.value.authorIds.push(authorId)
  }
  authorSearchQuery.value = ''
  filteredAuthors.value = []
}

function removeAuthor(authorId) {
  const index = form.value.authorIds.indexOf(authorId)
  if (index > -1) form.value.authorIds.splice(index, 1)
  const matchedIndex = matchedAuthors.value.indexOf(authorId)
  if (matchedIndex > -1) matchedAuthors.value.splice(matchedIndex, 1)
}

function getAuthorName(authorId) {
  const author = props.authors.find(a => a.id === authorId)
  return author ? `${author.firstName} ${author.lastName}` : 'Неизвестно'
}

// === Работа с жанрами ===

function matchGenres(extractedGenres) {
  const matched = []
  for (const extracted of extractedGenres) {
    const found = props.genres.find(dbGenre =>
        dbGenre.name.toLowerCase() === extracted.toLowerCase()
    )
    if (found && !form.value.genreIds.includes(found.id)) {
      form.value.genreIds.push(found.id)
      matched.push(found.id)
    }
  }
  matchedGenres.value = matched
}

function searchGenres() {
  if (!genreSearchQuery.value.trim()) {
    filteredGenres.value = []
    return
  }
  const query = genreSearchQuery.value.toLowerCase()
  filteredGenres.value = props.genres
      .filter(genre => {
        return genre.name.toLowerCase().includes(query) && !form.value.genreIds.includes(genre.id)
      })
      .slice(0, 10)
}

function addGenre(genreId) {
  if (!form.value.genreIds.includes(genreId)) {
    form.value.genreIds.push(genreId)
  }
  genreSearchQuery.value = ''
  filteredGenres.value = []
}

function removeGenre(genreId) {
  const index = form.value.genreIds.indexOf(genreId)
  if (index > -1) form.value.genreIds.splice(index, 1)
  const matchedIndex = matchedGenres.value.indexOf(genreId)
  if (matchedIndex > -1) matchedGenres.value.splice(matchedIndex, 1)
}

function getGenreName(genreId) {
  const genre = props.genres.find(g => g.id === genreId)
  return genre ? genre.name : 'Неизвестно'
}

// === Сохранение данных ===

async function handleSave() {
  if (!isValid.value) return

  let currentFilePath = form.value.filePath

  // Если загружен новый файл — загружаем его и получаем новый путь
  if (uploadedFile.value) {
    try {
      parsing.value = true

      const firstAuthorId = form.value.authorIds[0]
      const author = props.authors.find(a => a.id === firstAuthorId)
      const authorLastName = author?.lastName || ''
      const bookTitle = form.value.title || ''

      const formData = new FormData()
      formData.append('file', uploadedFile.value)
      formData.append('authorLastName', authorLastName)
      formData.append('bookTitle', bookTitle)

      const jwt = getCookie('jwt')
      const response = await fetch('/api/books/upload', {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${jwt}` },
        body: formData
      })

      if (!response.ok) throw new Error('Ошибка загрузки файла')
      const uploadResult = await response.json()
      currentFilePath = uploadResult.filePath // обновляем путь после загрузки
    } catch (error) {
      console.error('Ошибка загрузки файла:', error)
      alert('Не удалось загрузить файл: ' + error.message)
      parsing.value = false
      return
    } finally {
      parsing.value = false
    }
  }

  // Формируем payload для отправки
  const payload = {
    title: form.value.title.trim(),
    description: form.value.description || '',
    authorIds: [...form.value.authorIds],
    genreIds: [...form.value.genreIds]
  }

  // Отправляем filePath ТОЛЬКО если:
  // - это новая книга (props.book === null) → тогда filePath обязан быть (он пришёл от загрузки)
  // - или если это редактирование, но файл был загружен заново (uploadedFile.value)
  // В остальных случаях (редактирование без нового файла) — НЕ отправляем filePath
  if (uploadedFile.value || !props.book) {
    // Для новой книги filePath должен быть (иначе isValid не пропустил бы)
    // Для редактирования с новым файлом — тоже отправляем
    payload.filePath = currentFilePath
  }
  // Если редактируем и файл не меняли — filePath НЕ включаем в payload

  emit('save', payload)
}

// === Инициализация формы при открытии ===

watch(() => props.book, async (newBook) => {
  if (newBook) {
    try {
      const jwt = getCookie('jwt')
      const response = await fetch(`/api/books/${newBook.id}`, {
        headers: { 'Authorization': `Bearer ${jwt}` }
      })

      if (response.ok) {
        const bookDetails = await response.json()

        const authorIds = props.authors
            .filter(a => newBook.authors.includes(`${a.firstName} ${a.lastName}`))
            .map(a => a.id)

        const genreIds = props.genres
            .filter(g => newBook.genres.includes(g.name))
            .map(g => g.id)

        form.value = {
          title: bookDetails.title || '',
          description: bookDetails.description || '',
          authorIds: authorIds,
          genreIds: genreIds,
          filePath: bookDetails.filePath || ''
        }
      }
    } catch (error) {
      console.error('Ошибка загрузки деталей книги:', error)
    }
  } else {
    form.value = { title: '', description: '', authorIds: [], genreIds: [], filePath: '' }
    autoFilledFields.value = { title: false, description: false }
    matchedAuthors.value = []
    matchedGenres.value = []
  }
}, { immediate: true })
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

.btn-secondary {
  background: #e8d9c7;
  color: #5c4033;
}

.btn-secondary:hover {
  background: #d9c9b7;
}
</style>