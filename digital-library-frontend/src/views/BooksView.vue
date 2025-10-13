<template>
  <div class="app-layout">
    <Header />
    <div class="library-container">
      <AppSidebar :show-admin-link="isModeratorOrAdmin" />

      <main class="content">
        <h1 class="page-title">📚 Книги</h1>

        <!-- Поиск -->
        <div class="search-box">
          <SearchInput
              v-model="searchQuery"
              placeholder="🔍 Поиск по названию книги"
              @update:model-value="handleSearchInput"
          />
          <button class="search-button" @click="performSimpleSearch" title="Найти">
            🔍
          </button>
          <button
              class="settings-button"
              @click="openExtendedSearch"
              title="Расширенный поиск"
          >
            ⚙️
          </button>
        </div>

        <!-- Список книг -->
        <div v-if="loading" class="loading-message">
          Загрузка книг...
        </div>

        <div v-else-if="books.length === 0 && !searchQuery.trim() && !isSearchMode" class="no-books-message">
          Книги не найдены
        </div>

        <div v-else-if="books.length === 0 && (searchQuery.trim() || isSearchMode)" class="no-search-results">
          По вашему запросу ничего не найдено
        </div>

        <div v-else class="books-list">
          <BookCard
              v-for="book in books"
              :key="book.id"
              :book="book"
              :cover-url="coverImageUrls[book.id]"
              @select="selectBook"
          />
        </div>

        <!-- Пагинация -->
        <Pagination
            v-if="totalPages > 1 && books.length > 0"
            :current-page="currentPage"
            :total-pages="totalPages"
            @page-change="handlePageChange"
        />
      </main>

      <!-- Правая панель с деталями -->
      <aside v-if="selectedBook" class="book-detail-panel">
        <BookDetailPanel
            :book="selectedBook"
            :cover-url="coverImageUrls[selectedBook.id]"
            :user-rating="userRating"
            :is-authenticated="isAuthenticated"
            @open-reader="openBookInReader"
            @open-rating="showRatingModal = true"
        />

        <CommentsSection
            :comments="comments"
            :loading="loadingComments"
            :submitting="submittingComment"
            :is-authenticated="isAuthenticated"
            :current-user-id="currentUserId"
            :is-moderator="isModerator"
            :has-more="hasMoreComments"
            @submit="handleSubmitComment"
            @update="handleUpdateComment"
            @delete="handleDeleteComment"
            @moderate-delete="handleModerateDeleteComment"
            @restore="handleRestoreComment"
            @load-more="handleLoadMoreComments"
        />
      </aside>
    </div>

    <!-- Модальное окно рейтинга -->
    <RatingModal
        v-if="showRatingModal"
        v-model="selectedRatingValue"
        :book-title="selectedBook?.title"
        :loading="submittingRating"
        @close="closeRatingModal"
        @submit="handleSubmitRating"
    />

    <!-- Модальное окно расширенного поиска -->
    <ExtendedSearchModal
        v-if="showExtendedSearchModal"
        :filters="extendedFilters"
        :authors="extendedAuthors"
        :genres="extendedGenres"
        @close="closeExtendedSearch"
        @search="handleExtendedSearch"
        @reset="handleResetExtendedSearch"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getCookie } from '@/utils/cookies'

import Header from '@/components/layout/Header.vue'
import AppSidebar from '@/components/layout/AppSidebar.vue'
import SearchInput from '@/components/ui/SearchInput.vue'
import Pagination from '@/components/ui/Pagination.vue'
import BookCard from '@/components/books/BookCard.vue'
import BookDetailPanel from '@/components/books/BookDetailPanel.vue'
import CommentsSection from '@/components/books/CommentsSection.vue'
import RatingModal from '@/components/books/RatingModal.vue'
import ExtendedSearchModal from '@/components/books/ExtendedSearchModal.vue'

import { useBookCover } from '@/composables/useBookCover'
import { useBookComments } from '@/composables/useBookComments'
import { useBookRating } from '@/composables/useBookRating'
import { useAdminAuth } from '@/composables/useAdminAuth'
import { useExtendedSearch } from '@/composables/useExtendedSearch'

// Состояние книг
const books = ref([])
const selectedBook = ref(null)
const loading = ref(false)
const currentPage = ref(0)
const totalPages = ref(0)
const pageSize = 10

// Поиск
const searchQuery = ref('')
const isSearchMode = ref(false)
let searchTimeout = null

// Composables
const { coverImageUrls, fetchBookCover } = useBookCover()

const {
  comments,
  loading: loadingComments,
  submitting: submittingComment,
  hasMore: hasMoreComments,
  loadComments,
  submitComment,
  updateComment,
  deleteComment,
  moderateDeleteComment,
  restoreComment,
  loadMore: loadMoreComments
} = useBookComments()

const {
  userRating,
  submitting: submittingRating,
  loadUserRating,
  submitRating
} = useBookRating()

const { isAdmin, isModerator } = useAdminAuth()
const isModeratorOrAdmin = ref(false)

const {
  authors: extendedAuthors,
  genres: extendedGenres,
  showModal: showExtendedSearchModal,
  filters: extendedFilters,
  activeFilters: activeExtendedFilters,
  loadAuthors: loadExtendedAuthors,
  loadGenres: loadExtendedGenres,
  openModal: openExtendedSearchModal,
  closeModal: closeExtendedSearchModal,
  resetFilters: resetExtendedFilters,
  buildSearchUrl,
  performSearch: performExtendedSearchFilters
} = useExtendedSearch()

// Аутентификация
const isAuthenticated = ref(false)
const currentUserId = ref(null)

// Рейтинг
const showRatingModal = ref(false)
const selectedRatingValue = ref(0)

// Загрузка книг
async function loadBooks(page = 0) {
  loading.value = true
  try {
    const response = await fetch(
        `http://localhost:8080/api/books?page=${page}&size=${pageSize}`
    )
    if (!response.ok) throw new Error('Ошибка загрузки книг')
    const data = await response.json()
    updateBookList(data.content, data.number, data.totalPages)
  } catch (error) {
    console.error('Ошибка:', error)
    alert('Не удалось загрузить книги')
  } finally {
    loading.value = false
  }
}

// Простой поиск
function handleSearchInput(value) {
  searchQuery.value = value
  const query = value.trim()

  if (query === '') {
    resetToMainList()
    return
  }

  if (query.length < 3) return

  clearTimeout(searchTimeout)
  searchTimeout = setTimeout(() => {
    performSimpleSearch()
  }, 300)
}

function performSimpleSearch() {
  const query = searchQuery.value.trim()
  if (query === '') {
    resetToMainList()
    return
  }
  isSearchMode.value = true
  activeExtendedFilters.value = null // Сброс расширенного поиска
  currentPage.value = 0
  loadSimpleSearchResults(0)
}

async function loadSimpleSearchResults(page = 0) {
  loading.value = true
  try {
    const query = encodeURIComponent(searchQuery.value.trim())
    const response = await fetch(
        `http://localhost:8080/api/books/search?title=${query}&page=${page}&size=${pageSize}`
    )
    if (!response.ok) throw new Error('Ошибка поиска')
    const data = await response.json()
    updateBookList(data.content, data.number, data.totalPages)
  } catch (error) {
    console.error('Ошибка поиска:', error)
    alert('Поиск не удался')
    books.value = []
    selectedBook.value = null
  } finally {
    loading.value = false
  }
}

// Расширенный поиск
function openExtendedSearch() {

  if (activeExtendedFilters.value) {
    extendedFilters.value = { ...activeExtendedFilters.value }
  } else {
    extendedFilters.value.title = searchQuery.value
  }
  openExtendedSearchModal(searchQuery.value)
}

function closeExtendedSearch() {
  closeExtendedSearchModal()
}

function handleExtendedSearch(filters) {
  const activeFilters = performExtendedSearchFilters(filters)
  searchQuery.value = activeFilters.title // Синхронизируем
  isSearchMode.value = true
  currentPage.value = 0
  loadExtendedSearchResults(0)
}

function handleResetExtendedSearch() {
  resetExtendedFilters()
  searchQuery.value = ''
  resetToMainList()
  closeExtendedSearch()
}

async function loadExtendedSearchResults(page = 0) {
  if (!activeExtendedFilters.value) return

  loading.value = true
  try {
    const url = buildSearchUrl(page, pageSize)
    console.log('🔍 Extended search URL:', url) // DEBUG
    const response = await fetch(url)
    if (!response.ok) throw new Error('Ошибка расширенного поиска')
    const data = await response.json()
    console.log('📊 Search results:', data) // DEBUG
    updateBookList(data.content, data.number, data.totalPages)
  } catch (error) {
    console.error('Ошибка расширенного поиска:', error)
    alert('Не удалось выполнить расширенный поиск')
  } finally {
    loading.value = false
  }
}

// Обновление списка книг
function updateBookList(content, page, totalPagesCount) {
  books.value = content || []
  currentPage.value = page
  totalPages.value = totalPagesCount

  console.log('📚 Books loaded:', books.value.length) // DEBUG
  console.log('📄 Current page:', currentPage.value) // DEBUG
  console.log('📄 Total pages:', totalPages.value) // DEBUG

  books.value.forEach(book => fetchBookCover(book.id))

  if (books.value.length > 0) {
    selectBook(books.value[0])
  } else {
    selectedBook.value = null
  }
}

// Сброс к основному списку
function resetToMainList() {
  isSearchMode.value = false
  activeExtendedFilters.value = null
  searchQuery.value = ''
  resetExtendedFilters()
  loadBooks(0)
}

// Пагинация
function handlePageChange(page) {
  console.log('📄 Page change to:', page) // DEBUG
  if (isSearchMode.value) {
    if (activeExtendedFilters.value) {
      loadExtendedSearchResults(page)
    } else {
      loadSimpleSearchResults(page)
    }
  } else {
    loadBooks(page)
  }
}

// Выбор книги
function selectBook(book) {
  selectedBook.value = book
  loadBookDetails(book.id)
  loadComments(book.id)
  loadUserRating(book.id)
}

async function loadBookDetails(bookId) {
  try {
    const response = await fetch(`http://localhost:8080/api/books/${bookId}`)
    if (!response.ok) throw new Error('Ошибка загрузки деталей')
    const data = await response.json()
    selectedBook.value = { ...selectedBook.value, ...data }
    fetchBookCover(bookId)
  } catch (error) {
    console.error('Ошибка загрузки деталей книги:', error)
  }
}

// Открытие читалки
function openBookInReader() {
  if (!selectedBook.value) return

  const jwt = getCookie('jwt')
  if (!jwt) {
    window.open(`/reader.html?bookId=${selectedBook.value.id}`, '_blank')
    return
  }

  window.open(`/reader.html?bookId=${selectedBook.value.id}&token=${jwt}`, '_blank')
}

// Рейтинг
function closeRatingModal() {
  showRatingModal.value = false
  selectedRatingValue.value = 0
}

async function handleSubmitRating() {
  if (!selectedRatingValue.value) return

  const success = await submitRating(selectedBook.value.id, selectedRatingValue.value)
  if (success) {
    closeRatingModal()
    await loadBookDetails(selectedBook.value.id)
  }
}

// Комментарии
async function handleSubmitComment(text) {
  const success = await submitComment(selectedBook.value.id, text)
  if (success) {
    // Комментарий уже добавлен в composable
  }
}

async function handleUpdateComment({ id, text }) {
  await updateComment(id, text)
}

async function handleDeleteComment(id) {
  await deleteComment(id)
}

async function handleModerateDeleteComment(id) {
  await moderateDeleteComment(id)
}

async function handleRestoreComment(id) {
  await restoreComment(id)
}

function handleLoadMoreComments() {
  loadMoreComments(selectedBook.value.id)
}

// Аутентификация
async function checkAuthentication() {
  const jwt = getCookie('jwt')
  isAuthenticated.value = !!jwt

  if (jwt) {
    try {
      const response = await fetch('http://localhost:8080/api/users/me', {
        headers: { 'Authorization': `Bearer ${jwt}` }
      })

      if (response.ok) {
        const userData = await response.json()
        currentUserId.value = userData.id
        const isMod = userData.roleName === 'ROLE_MODERATOR' || userData.roleName === 'MODERATOR'
        const isAdm = userData.roleName === 'ROLE_ADMIN' || userData.roleName === 'ADMIN'
        isModerator.value = isMod || isAdm
        isModeratorOrAdmin.value = isMod || isAdm
      }
    } catch (error) {
      console.error('Ошибка аутентификации:', error)
    }
  }
}

// Lifecycle
onMounted(async () => {
  await loadBooks(0)
  await checkAuthentication()
  await loadExtendedAuthors()
  await loadExtendedGenres()
})
</script>
<style scoped src="@/assets/styles/library-common.css"></style>
<style scoped>
.page-title {
  font-size: 32px;
  font-weight: 700;
  color: #333;
  margin: 0 0 24px 0;
  display: flex;
  align-items: center;
  gap: 12px;
}

.search-box {
  display: flex;
  gap: 0;
  margin-bottom: 24px;
  align-items: stretch;
}


.search-button,
.settings-button {
  padding: 0 16px;
  font-size: 20px;
  background-color: #fff;
  color: #666;
  border: 2px solid #e0e0e0;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 48px;
  min-width: 48px;
}

.search-button {
  border-left: none;
  border-right: none;
  border-radius: 0;
}

.search-button:hover {
  background-color: #2196F3;
  color: white;
  border-color: #2196F3;
}

.settings-button {
  border-radius: 0 8px 8px 0;
}

.settings-button:hover {
  background-color: #f5f5f5;
  border-color: #2196F3;
}

/* ОБНОВИ СТИЛИ ДЛЯ INPUT */
.search-box :deep(input),
.search-box :deep(.search-input) {
  border-radius: 8px 0 0 8px !important;
  height: 48px !important; /* ФИКСИРОВАННАЯ ВЫСОТА */
  padding: 0 16px !important;
  border: 2px solid #e0e0e0 !important;
  font-size: 15px !important;
  box-sizing: border-box !important;
}

.search-box :deep(input:focus),
.search-box :deep(.search-input:focus) {
  border-color: #2196F3 !important;
  outline: none !important;
}

.loading-message,
.no-books-message,
.no-search-results {
  text-align: center;
  padding: 60px 20px;
  color: #999;
  font-size: 18px;
  background-color: #f9f9f9;
  border-radius: 12px;
  border: 2px dashed #e0e0e0;
}

.no-search-results {
  color: #666;
}

.books-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-bottom: 24px;
}

.book-detail-panel {
  width: 350px;
  flex-shrink: 0;
  background-color: #fff;
  border-left: 1px solid #e0e0e0;
  padding: 24px;
  overflow-y: auto;
  max-height: calc(100vh - 60px);
}

/* Пагинация */
.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16px;
  margin-top: 24px;
  margin-bottom: 24px;
  padding: 20px;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.pagination button {
  padding: 10px 20px;
  border: 2px solid #e0e0e0;
  border-radius: 6px;
  background: white;
  color: #333;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.pagination button:hover:not(:disabled) {
  background: #2196F3;
  color: white;
  border-color: #2196F3;
  transform: translateY(-2px);
}

.pagination button:disabled {
  opacity: 0.4;
  cursor: not-allowed;
  background: #f5f5f5;
}

.pagination .page-info {
  font-size: 15px;
  font-weight: 500;
  color: #333;
  min-width: 150px;
  text-align: center;
}

@media (max-width: 1200px) {
  .book-detail-panel {
    width: 300px;
  }
}

@media (max-width: 968px) {
  .library-container {
    flex-direction: column;
  }

  .book-detail-panel {
    width: 100%;
    max-height: none;
    border-left: none;
    border-top: 1px solid #e0e0e0;
  }

  .pagination {
    flex-direction: column;
    gap: 12px;
  }

  .pagination button {
    width: 100%;
  }
}

@media (max-width: 768px) {
  .search-box {
    flex-direction: column;
    gap: 8px;
  }

  .search-box :deep(input),
  .search-box :deep(.search-input) {
    border-radius: 8px !important;
    width: 100%;
  }

  .search-button,
  .settings-button {
    width: 100%;
    border-radius: 8px !important;
    border: 2px solid #e0e0e0 !important;
  }
}
</style>