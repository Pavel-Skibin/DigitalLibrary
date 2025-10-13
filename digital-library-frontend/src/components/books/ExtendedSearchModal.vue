<template>
  <div class="modal-overlay" @click="$emit('close')">
    <div class="modal-content extended-search-modal" @click.stop>
      <h2 class="modal-title">🔍 Расширенный поиск</h2>

      <!-- Название -->
      <div class="form-group">
        <label>Название</label>
        <input
            v-model="localFilters.title"
            type="text"
            placeholder="Введите название книги"
            class="form-input"
        />
      </div>

      <!-- Авторы -->
      <div class="form-group">
        <label>Авторы</label>
        <input
            v-model="authorSearchQuery"
            type="text"
            placeholder="🔍 Поиск автора..."
            class="form-input search-filter-input"
        />
        <div class="checkbox-list">
          <label
              v-for="author in filteredAuthors"
              :key="author.id"
              class="checkbox-item"
          >
            <input
                type="checkbox"
                :value="author.id"
                v-model="localFilters.selectedAuthors"
            />
            <span>{{ author.firstName }} {{ author.lastName }}</span>
          </label>
          <div v-if="filteredAuthors.length === 0" class="no-results">
            Авторы не найдены
          </div>
        </div>
        <div v-if="localFilters.selectedAuthors.length > 0" class="selection-count">
          Выбрано: {{ localFilters.selectedAuthors.length }}
        </div>
      </div>

      <!-- Жанры -->
      <div class="form-group">
        <label>Жанры</label>
        <input
            v-model="genreSearchQuery"
            type="text"
            placeholder="🔍 Поиск жанра..."
            class="form-input search-filter-input"
        />
        <div class="checkbox-list">
          <label
              v-for="genre in filteredGenres"
              :key="genre.id"
              class="checkbox-item"
          >
            <input
                type="checkbox"
                :value="genre.id"
                v-model="localFilters.selectedGenres"
            />
            <span>{{ genre.name }}</span>
          </label>
          <div v-if="filteredGenres.length === 0" class="no-results">
            Жанры не найдены
          </div>
        </div>
        <div v-if="localFilters.selectedGenres.length > 0" class="selection-count">
          Выбрано: {{ localFilters.selectedGenres.length }}
        </div>
      </div>

      <!-- Рейтинг -->
      <div class="form-group">
        <label>Рейтинг</label>
        <div class="rating-range">
          <input
              v-model.number="localFilters.minRating"
              type="number"
              placeholder="От 0.0"
              min="0"
              max="5"
              step="0.1"
              class="form-input"
          />
          <span>—</span>
          <input
              v-model.number="localFilters.maxRating"
              type="number"
              placeholder="До 5.0"
              min="0"
              max="5"
              step="0.1"
              class="form-input"
          />
        </div>
      </div>

      <!-- Сортировка -->
      <div class="form-group">
        <label>Сортировка</label>
        <div class="radio-group">
          <label class="radio-item">
            <input
                type="radio"
                value="titleAsc"
                v-model="localFilters.sortBy"
            />
            <span>По названию (А-Я)</span>
          </label>
          <label class="radio-item">
            <input
                type="radio"
                value="ratingDesc"
                v-model="localFilters.sortBy"
            />
            <span>По рейтингу (убывание)</span>
          </label>
        </div>
      </div>

      <!-- Кнопки -->
      <div class="modal-buttons">
        <button @click="handleReset" class="btn-secondary">
          Сбросить
        </button>
        <button @click="handleSearch" class="btn-primary">
          Найти
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'

const props = defineProps({
  filters: {
    type: Object,
    required: true
  },
  authors: {
    type: Array,
    required: true
  },
  genres: {
    type: Array,
    required: true
  }
})

const emit = defineEmits(['close', 'search', 'reset'])

const localFilters = ref({ ...props.filters })
const authorSearchQuery = ref('')
const genreSearchQuery = ref('')

const filteredAuthors = computed(() => {
  if (!authorSearchQuery.value.trim()) {
    return props.authors
  }
  const query = authorSearchQuery.value.toLowerCase()
  return props.authors.filter(author => {
    const fullName = `${author.firstName} ${author.lastName}`.toLowerCase()
    return fullName.includes(query)
  })
})

const filteredGenres = computed(() => {
  if (!genreSearchQuery.value.trim()) {
    return props.genres
  }
  const query = genreSearchQuery.value.toLowerCase()
  return props.genres.filter(genre =>
      genre.name.toLowerCase().includes(query)
  )
})

function handleSearch() {
  emit('search', localFilters.value)
}

function handleReset() {
  localFilters.value = {
    title: '',
    selectedAuthors: [],
    selectedGenres: [],
    minRating: null,
    maxRating: null,
    sortBy: 'titleAsc'
  }
  authorSearchQuery.value = ''
  genreSearchQuery.value = ''
  emit('reset')
}

// Синхронизируем с родительским компонентом
watch(() => props.filters, (newFilters) => {
  localFilters.value = { ...newFilters }
}, { deep: true })
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  animation: fadeIn 0.2s ease-out;
}

.extended-search-modal {
  max-width: 600px;
}

.modal-content {
  background-color: white;
  border-radius: 12px;
  padding: 32px;
  width: 90%;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  animation: slideUp 0.3s ease-out;
}

.modal-title {
  margin: 0 0 24px 0;
  font-size: 24px;
  font-weight: 600;
  color: #333;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  font-weight: 500;
  color: #555;
}

.form-input {
  width: 100%;
  padding: 10px 12px;
  font-size: 15px;
  border: 2px solid #e0e0e0;
  border-radius: 6px;
  outline: none;
  box-sizing: border-box;
  transition: border-color 0.2s;
}

.form-input:focus {
  border-color: #2196F3;
}

.search-filter-input {
  margin-bottom: 8px;
  background-color: #f9f9f9;
}

.checkbox-list,
.radio-group {
  border: 2px solid #e0e0e0;
  border-radius: 6px;
  padding: 12px;
  background-color: #fafafa;
}

.checkbox-list {
  max-height: 150px;
  overflow-y: auto;
}

.checkbox-item,
.radio-item {
  display: flex;
  align-items: center;
  padding: 8px;
  cursor: pointer;
  border-radius: 4px;
  transition: background 0.15s;
}

.checkbox-item:hover,
.radio-item:hover {
  background-color: #f0f0f0;
}

.checkbox-item input,
.radio-item input {
  margin-right: 10px;
  width: 18px;
  height: 18px;
  cursor: pointer;
}

.no-results {
  padding: 16px;
  text-align: center;
  color: #999;
  font-size: 14px;
}

.selection-count {
  margin-top: 8px;
  font-size: 13px;
  color: #2196F3;
  font-weight: 500;
}

.rating-range {
  display: flex;
  gap: 12px;
  align-items: center;
}

.rating-range input {
  flex: 1;
}

.rating-range span {
  color: #999;
}

.modal-buttons {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  margin-top: 24px;
}

.btn-secondary,
.btn-primary {
  padding: 12px 24px;
  font-size: 15px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-weight: 500;
  transition: all 0.2s;
}

.btn-secondary {
  background-color: #fff;
  color: #666;
  border: 2px solid #e0e0e0;
}

.btn-secondary:hover {
  background-color: #f5f5f5;
}

.btn-primary {
  background-color: #2196F3;
  color: white;
  padding: 12px 32px;
}

.btn-primary:hover:not(:disabled) {
  background-color: #1976D2;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>