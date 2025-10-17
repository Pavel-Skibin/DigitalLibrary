<template>
  <div class="modal-overlay" @click="$emit('close')">
    <div class="modal-content large" @click.stop>
      <h2 class="modal-title">
        {{ book ? 'Редактировать книгу' : 'Добавить книгу' }}
      </h2>

      <div class="book-form">
        <!-- Название -->
        <div class="form-group">
          <label>Название *</label>
          <input
              v-model="form.title"
              type="text"
              class="form-input"
              placeholder="Введите название книги"
          />
        </div>

        <!-- Описание -->
        <div class="form-group">
          <label>Описание</label>
          <textarea
              v-model="form.description"
              class="form-textarea"
              placeholder="Введите описание книги"
              rows="4"
          ></textarea>
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
        <div class="form-group">
          <label>Путь к файлу *</label>
          <input
              v-model="form.filePath"
              type="text"
              class="form-input"
              placeholder="Например: D:\books\book.fb2"
          />
          <small class="form-hint">Укажите полный путь к файлу книги в формате FB2</small>
        </div>
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

const props = defineProps({
  book: {
    type: Object,
    default: null
  },
  authors: {
    type: Array,
    required: true
  },
  genres: {
    type: Array,
    required: true
  },
  loading: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['close', 'save'])

const form = ref({
  title: '',
  description: '',
  authorIds: [],
  genreIds: [],
  filePath: ''
})

const authorSearchQuery = ref('')
const genreSearchQuery = ref('')
const filteredAuthors = ref([])
const filteredGenres = ref([])

const isValid = computed(() => {
  return (
      form.value.title.trim() &&
      form.value.authorIds.length > 0 &&
      form.value.genreIds.length > 0 &&
      form.value.filePath.trim()
  )
})

// Инициализация формы
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
    form.value = {
      title: '',
      description: '',
      authorIds: [],
      genreIds: [],
      filePath: ''
    }
  }
}, { immediate: true })

// Поиск авторов
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
  if (index > -1) {
    form.value.authorIds.splice(index, 1)
  }
}

function getAuthorName(authorId) {
  const author = props.authors.find(a => a.id === authorId)
  return author ? `${author.firstName} ${author.lastName}` : 'Неизвестно'
}

// Поиск жанров
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
  if (index > -1) {
    form.value.genreIds.splice(index, 1)
  }
}

function getGenreName(genreId) {
  const genre = props.genres.find(g => g.id === genreId)
  return genre ? genre.name : 'Неизвестно'
}

function handleSave() {
  if (isValid.value) {
    emit('save', form.value)
  }
}
</script>