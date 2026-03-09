<template>
  <div class="app-layout">
    <Header />
    <div class="library-container">
      <AppSidebar :show-admin-link="isModeratorOrAdmin" />

      <main class="content">
        <h1 class="page-title">
          Книги автора: {{ authorName || "Загрузка..." }}
        </h1>

        <div class="search-box">
          <SearchInput
            v-model="searchQuery"
            placeholder="🔍 Поиск по названию книги"
          />
          <button class="search-button" @click="performSearch" title="Найти">
            🔍
          </button>
        </div>

        <div v-if="loading" class="loading-message">Загрузка книг...</div>

        <div
          v-else-if="books.length === 0 && !searchQuery.trim()"
          class="no-books-message"
        >
          У этого автора пока нет книг.
        </div>

        <div
          v-else-if="books.length === 0 && searchQuery.trim()"
          class="no-search-results"
        >
          Ничего не найдено по запросу "{{ searchQuery }}".
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

        <Pagination
          v-if="totalPages > 1 && books.length > 0"
          :current-page="currentPage"
          :total-pages="totalPages"
          @page-change="handlePageChange"
        />
      </main>

      <aside v-if="selectedBook" class="book-detail-panel">
        <button class="panel-mobile-close" @click="selectedBook = null">
          ← Назад к списку
        </button>
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

    <RatingModal
      v-if="showRatingModal"
      v-model="selectedRatingValue"
      :book-title="selectedBook?.title"
      :loading="submittingRating"
      @close="closeRatingModal"
      @submit="handleSubmitRating"
    />
  </div>
</template>
<script setup>
import { ref, computed, watch, onMounted } from "vue";
import { useRoute } from "vue-router";
import { getCookie } from "@/utils/cookies";
import { getReadingStatus } from "@/api/admin";

import Header from "@/components/layout/Header.vue";
import AppSidebar from "@/components/layout/AppSidebar.vue";
import SearchInput from "@/components/ui/SearchInput.vue";
import Pagination from "@/components/ui/Pagination.vue";
import BookCard from "@/components/books/BookCard.vue";
import BookDetailPanel from "@/components/books/BookDetailPanel.vue";
import CommentsSection from "@/components/books/CommentsSection.vue";
import RatingModal from "@/components/books/RatingModal.vue";

import { useBookCover } from "@/composables/useBookCover";
import { useBookComments } from "@/composables/useBookComments";
import { useBookRating } from "@/composables/useBookRating";
import { useUser } from "@/composables/useUser";
import { useAdminAuth } from "@/composables/useAdminAuth";
import { invalidateUserCache } from "@/api/recommendations";

const route = useRoute();
const authorId = ref(route.params.authorId);

// Состояние
const authorName = ref("");
const books = ref([]);
const selectedBook = ref(null);
const searchQuery = ref("");
const currentPage = ref(0);
const totalPages = ref(0);
const loading = ref(false);
const isSearchMode = ref(false);
const searchResultsCache = ref([]);
const pageSize = 10;

// Composables
const { coverImageUrls, fetchBookCover } = useBookCover();
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
  loadMore: loadMoreComments,
} = useBookComments();

const {
  userRating,
  submitting: submittingRating,
  loadUserRating,
  submitRating,
} = useBookRating();

const { username, userId, fetchUserProfile } = useUser();
const { isAdmin, isModerator, checkAccess } = useAdminAuth();

const isModeratorOrAdmin = computed(() => isAdmin.value || isModerator.value);

const isAuthenticated = ref(false);
const currentUserId = ref(null);

const showRatingModal = ref(false);
const selectedRatingValue = ref(0);

let debounceTimeout = null;

async function loadBooks(page = 0) {
  loading.value = true;
  try {
    const url = `/api/books/authors/${authorId.value}/books?page=${page}&size=${pageSize}`;
    const response = await fetch(url);
    if (!response.ok) throw new Error(`HTTP ${response.status}`);

    const data = await response.json();

    if (Array.isArray(data)) {
      updateBookList(data, 0, 1, data.length);
    } else {
      updateBookList(
        data.content,
        data.number,
        data.totalPages,
        data.totalElements,
      );
    }

    if (books.value.length > 0 && !authorName.value) {
      authorName.value = books.value[0].authors[0] || "Неизвестный автор";
    }
  } catch (error) {
    books.value = [];
    selectedBook.value = null;
  } finally {
    loading.value = false;
  }
}

function updateBookList(content, page, totalPagesCount, totalElements) {
  books.value = content || [];
  currentPage.value = page;
  totalPages.value = totalPagesCount;

  books.value.forEach((book) => fetchBookCover(book.id));

  if (books.value.length > 0) {
    // На мобильном не выбираем книгу автоматически — пользователь должен выбрать сам
    if (window.innerWidth >= 768) {
      selectBook(books.value[0]);
    } else {
      selectedBook.value = null;
    }
  } else {
    selectedBook.value = null;
  }
}

async function performSearch() {
  const query = searchQuery.value.trim();
  if (query === "") {
    resetToMainList();
    return;
  }
  isSearchMode.value = true;
  currentPage.value = 0;
  await loadSearchResults();
}

async function loadSearchResults() {
  loading.value = true;
  try {
    const query = encodeURIComponent(searchQuery.value.trim());
    const url = `/api/books/search?title=${query}&authorIds=${authorId.value}&page=0&size=1000`;

    const response = await fetch(url);
    if (!response.ok) throw new Error(`HTTP ${response.status}`);

    const data = await response.json();
    const results = data.content || [];
    searchResultsCache.value = results;

    applyClientPagination(0);
  } catch (error) {
    books.value = [];
    selectedBook.value = null;
    searchResultsCache.value = [];
  } finally {
    loading.value = false;
  }
}

function applyClientPagination(page) {
  const startIndex = page * pageSize;
  const endIndex = startIndex + pageSize;
  const paginatedBooks = searchResultsCache.value.slice(startIndex, endIndex);
  const totalPagesCount = Math.ceil(searchResultsCache.value.length / pageSize);

  updateBookList(
    paginatedBooks,
    page,
    totalPagesCount,
    searchResultsCache.value.length,
  );
}

function resetToMainList() {
  isSearchMode.value = false;
  searchResultsCache.value = [];
  loadBooks(0);
}

function handlePageChange(page) {
  if (isSearchMode.value) {
    applyClientPagination(page);
  } else {
    loadBooks(page);
  }
}

// Выбор книги
function selectBook(book) {
  selectedBook.value = book;
  loadBookDetails(book.id);
  loadComments(book.id);
  loadUserRating(book.id);
}

async function loadBookDetails(bookId) {
  try {
    const response = await fetch(`/api/books/${bookId}`);
    if (!response.ok) throw new Error("Ошибка загрузки деталей");
    const data = await response.json();
    // Защита от гонки: обновляем только если пользователь не выбрал другую книгу
    if (selectedBook.value?.id === bookId) {
      selectedBook.value = { ...selectedBook.value, ...data };
      fetchBookCover(bookId);
    }
  } catch (error) {}
}

async function openBookInReader() {
  if (!selectedBook.value) return;

  try {
    const { readingEnabled } = await getReadingStatus();
    if (!readingEnabled) {
      alert("📖 Чтение книг временно отключено администратором.");
      return;
    }
  } catch (e) {
    alert("Не удалось проверить доступность чтения. Попробуйте позже.");
    return;
  }

  const jwt = getCookie("jwt");
  if (!jwt) {
    window.open(`/reader.html?bookId=${selectedBook.value.id}`, "_blank");
    return;
  }

  window.open(
    `/reader.html?bookId=${selectedBook.value.id}&token=${jwt}`,
    "_blank",
  );
}

// Рейтинг
function closeRatingModal() {
  showRatingModal.value = false;
  selectedRatingValue.value = 0;
}

async function handleSubmitRating() {
  if (!selectedRatingValue.value) return;

  const success = await submitRating(
    selectedBook.value.id,
    selectedRatingValue.value,
  );
  if (success) {
    closeRatingModal();
    await loadBookDetails(selectedBook.value.id);

    // Инвалидация кеша рекомендаций после оценки
    if (userId.value) {
      try {
        await invalidateUserCache(userId.value);
      } catch (error) {}
    } else {
    }
  }
}

async function handleDeleteComment(id) {
  await deleteComment(id);
}

async function handleModerateDeleteComment(id) {
  await moderateDeleteComment(id);
}

async function handleRestoreComment(id) {
  await restoreComment(id);
}

function handleLoadMoreComments() {
  loadMoreComments(selectedBook.value.id);
}

// Аутентификация
async function checkAuthentication() {
  const jwt = getCookie("jwt");
  isAuthenticated.value = !!jwt;

  if (jwt) {
    try {
      const response = await fetch("/api/users/me", {
        headers: { Authorization: `Bearer ${jwt}` },
      });

      if (response.ok) {
        const userData = await response.json();
        currentUserId.value = userData.id;
      }
    } catch (error) {}
  }
}

// Watchers
watch(
  () => route.params.authorId,
  (newId) => {
    if (newId) {
      authorId.value = newId;
      resetToMainList();
    }
  },
);

watch(searchQuery, () => {
  const query = searchQuery.value.trim();
  if (query === "") {
    resetToMainList();
    return;
  }
  if (query.length < 3) return;

  clearTimeout(debounceTimeout);
  debounceTimeout = setTimeout(() => {
    performSearch();
  }, 300);
});

// Lifecycle
onMounted(async () => {
  // Загрузка userId для инвалидации кеша
  await fetchUserProfile();

  // ИСПРАВЛЕНИЕ: сначала проверяем права
  await checkAccess();

  // Затем загружаем данные
  await loadBooks(0);
  await checkAuthentication();
});
</script>

<style scoped src="@/assets/styles/library-common.css"></style>
<style scoped>
.search-box {
  display: flex;
  gap: 0;
  margin-bottom: 24px;
  align-items: stretch; /* Выравнивание по высоте */
}

.search-button {
  padding: 0 16px;
  font-size: 20px;
  background-color: #fff;
  color: #666;
  border: 2px solid #e0e0e0;
  border-left: none;
  border-radius: 0 8px 8px 0;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 48px; /* Фиксированная высота */
  min-width: 48px;
}

.search-button:hover {
  background-color: #2196f3;
  color: white;
  border-color: #2196f3;
}

/* Стили для input внутри SearchInput */
.search-box :deep(input),
.search-box :deep(.search-input) {
  border-radius: 8px 0 0 8px !important;
  height: 48px !important; /* Фиксированная высота */
  padding: 0 16px !important;
  border: 2px solid #e0e0e0 !important;
  font-size: 15px !important;
  box-sizing: border-box !important;
}

.search-box :deep(input:focus),
.search-box :deep(.search-input:focus) {
  border-color: #2196f3 !important;
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

.no-books-message,
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

  .search-button {
    width: 100%;
    border-radius: 8px !important;
    border: 2px solid #e0e0e0 !important;
  }

  .loading-message,
  .no-books-message,
  .no-search-results {
    padding: 40px 16px;
    font-size: 16px;
  }

  .books-list {
    gap: 12px;
  }
}

/* Очень маленькие экраны */
@media (max-width: 480px) {
  .book-detail-panel {
    padding: 16px;
  }
}
</style>
