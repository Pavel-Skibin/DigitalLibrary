<template>
  <div class="app-layout">
    <Header />
    <div class="library-container">
      <AppSidebar :show-admin-link="isModeratorOrAdmin" />

      <main class="book-detail-content">
        <!-- Loading State -->
        <div v-if="loading" class="loading-state">
          <div class="spinner"></div>
          <p>Загрузка книги...</p>
        </div>

        <!-- Error State -->
        <div v-else-if="error" class="error-state">
          <span class="icon">⚠️</span>
          <h2>Не удалось загрузить книгу</h2>
          <p>{{ error }}</p>
          <button @click="$router.back()" class="back-button">
            Вернуться назад
          </button>
        </div>

        <!-- Book Details -->
        <div v-else-if="book" class="book-container">
          <!-- Back Button -->
          <button @click="$router.back()" class="back-button-small">
            <span>←</span> Назад
          </button>

          <!-- Book Header -->
          <div class="book-header">
            <div class="book-cover-large">
              <div class="book-cover-placeholder">
                <span>📖</span>
              </div>
              <img
                v-if="book.id"
                :src="`/api/books/${book.id}/cover`"
                :alt="book.title"
                @error="handleImageError"
                class="cover-image"
              />
            </div>

            <div class="book-info">
              <h1 class="book-title">{{ book.title }}</h1>

              <div class="book-meta">
                <p
                  v-if="book.authors && book.authors.length > 0"
                  class="authors"
                >
                  <span class="label">Авторы:</span>
                  <router-link
                    v-for="(author, index) in book.authors"
                    :key="author.id"
                    :to="`/authors/${author.id}/books`"
                    class="author-link"
                  >
                    {{ author.name
                    }}<span v-if="index < book.authors.length - 1">, </span>
                  </router-link>
                </p>

                <p v-if="book.genres && book.genres.length > 0" class="genres">
                  <span class="label">Жанры:</span>
                  <span
                    class="genre-tag"
                    v-for="genre in book.genres"
                    :key="genre.name"
                  >
                    {{ genre.name }}
                  </span>
                </p>

                <p v-if="book.series" class="series">
                  <span class="label">Серия:</span>
                  {{ book.series.seriesName }}
                  <span v-if="book.series.volumeNumber"
                    >(Книга {{ book.series.volumeNumber }})</span
                  >
                </p>

                <div class="stats-row">
                  <div v-if="book.publicationYear" class="stat">
                    <span class="stat-icon">📅</span>
                    {{ book.publicationYear }}
                  </div>
                  <div v-if="book.averageRating" class="stat">
                    <span class="stat-icon">⭐</span>
                    {{ formatRating(book.averageRating) }}
                  </div>
                  <div v-if="book.ratingsCount" class="stat">
                    <span class="stat-icon">💬</span>
                    {{ book.ratingsCount }} оценок
                  </div>
                  <div v-if="book.language" class="stat">
                    <span class="stat-icon">🌐</span>
                    {{ getLanguageLabel(book.language) }}
                  </div>
                </div>
              </div>

              <!-- Actions -->
              <div class="action-buttons">
                <button
                  v-if="isAuthenticated"
                  @click="openBookInReader"
                  class="primary-button"
                >
                  <span>📖</span> Читать
                </button>
                <button v-else @click="goToLogin" class="primary-button">
                  <span>🔒</span> Войти чтобы читать
                </button>

                <button
                  v-if="isAuthenticated"
                  @click="showRatingModal = true"
                  class="secondary-button"
                >
                  <span>⭐</span> Оценить
                </button>

                <FavoriteButton v-if="isAuthenticated" :book-id="book.id" />
              </div>
            </div>
          </div>

          <!-- Description -->
          <div v-if="book.description" class="book-description">
            <h2>Описание</h2>
            <p>{{ book.description }}</p>
          </div>

          <!-- Similar Books -->
          <div
            v-if="similarBooks && similarBooks.length > 0"
            class="similar-section"
          >
            <h2>Похожие книги</h2>
            <div class="similar-books-grid">
              <div
                v-for="similar in similarBooks.slice(0, 6)"
                :key="similar.book_id"
                class="similar-book-card"
                @click="goToBook(similar.book_id)"
              >
                <div class="book-cover-small">
                  <div class="book-cover-placeholder">
                    <span>📖</span>
                  </div>
                  <img
                    :src="`/api/books/${similar.book_id}/cover`"
                    :alt="similar.title"
                    @error="handleImageError"
                    class="cover-image"
                  />
                </div>
                <div class="book-info-small">
                  <h4>{{ similar.title }}</h4>
                  <p v-if="similar.authors && similar.authors.length > 0">
                    {{ similar.authors[0].name }}
                  </p>
                </div>
              </div>
            </div>
          </div>

          <!-- Comments Section -->
          <div class="comments-section">
            <h2>Комментарии</h2>
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
              @go-to-login="goToLogin"
            />
          </div>
        </div>
      </main>
    </div>

    <!-- Rating Modal -->
    <RatingModal
      v-if="showRatingModal"
      v-model="selectedRatingValue"
      :book-title="book?.title"
      :loading="submittingRating"
      @close="closeRatingModal"
      @submit="handleSubmitRating"
    />
  </div>
</template>

<script setup>
import { ref, onMounted, computed, nextTick } from "vue";
import { useRoute, useRouter } from "vue-router";
import { getCookie } from "@/utils/cookies";

import Header from "@/components/layout/Header.vue";
import AppSidebar from "@/components/layout/AppSidebar.vue";
import CommentsSection from "@/components/books/CommentsSection.vue";
import RatingModal from "@/components/books/RatingModal.vue";
import FavoriteButton from "@/components/books/FavoriteButton.vue";

import { useBookComments } from "@/composables/useBookComments";
import { useBookRating } from "@/composables/useBookRating";
import { useAdminAuth } from "@/composables/useAdminAuth";
import { useUser } from "@/composables/useUser";
import { invalidateUserCache } from "@/api/recommendations";

const route = useRoute();
const router = useRouter();

const book = ref(null);
const similarBooks = ref([]);
const loading = ref(true);
const error = ref(null);
const showRatingModal = ref(false);
const selectedRatingValue = ref(0);
const submittingRating = ref(false);

const bookId = computed(() => parseInt(route.params.id));
const isAuthenticated = computed(() => !!getCookie("jwt"));
const currentUserId = computed(() => {
  const jwt = getCookie("jwt");
  if (!jwt) return null;
  try {
    const payload = JSON.parse(atob(jwt.split(".")[1]));
    return payload.userId || payload.sub;
  } catch {
    return null;
  }
});

const { isAdmin, isModerator, checkAccess } = useAdminAuth();
const isModeratorOrAdmin = ref(false);

const { userId } = useUser();

const {
  comments,
  loadingComments,
  submittingComment,
  hasMoreComments,
  loadComments,
  submitComment,
  updateComment,
  deleteComment,
  moderateDeleteComment,
  restoreComment,
  loadMoreComments,
} = useBookComments();

const { userRating, loadUserRating, submitRating } = useBookRating();

onMounted(async () => {
  await checkAccess();
  isModeratorOrAdmin.value = isAdmin.value || isModerator.value;
  await loadBookDetails();
  await loadComments(bookId.value);
  if (isAuthenticated.value) {
    await loadUserRating(bookId.value);
  }
});

async function loadBookDetails() {
  try {
    loading.value = true;
    error.value = null;

    // Load book details
    const response = await fetch(`/api/books/${bookId.value}`);
    if (!response.ok) throw new Error("Ошибка загрузки книги");
    book.value = await response.json();

    // Load similar books
    try {
      const similarResponse = await fetch(
        `/api/recommendations/similar/${bookId.value}`,
      );
      if (similarResponse.ok) {
        similarBooks.value = await similarResponse.json();
      } else {
        similarBooks.value = [];
      }
    } catch (err) {
      console.warn("Failed to load similar books:", err);
      similarBooks.value = [];
    }
  } catch (err) {
    console.error("Failed to load book:", err);
    error.value = err.message || "Не удалось загрузить книгу";
  } finally {
    loading.value = false;
  }
}

function openBookInReader() {
  if (!book.value) return;

  const jwt = getCookie("jwt");
  if (!jwt) {
    window.open(`/reader.html?bookId=${book.value.id}`, "_blank");
    return;
  }

  window.open(`/reader.html?bookId=${book.value.id}&token=${jwt}`, "_blank");
}

function goToLogin() {
  router.push("/login");
}

async function goToBook(id) {
  if (!id) {
    console.error("Invalid book ID:", id);
    return;
  }

  // Clear current data
  loading.value = true;
  error.value = null;
  book.value = null;
  similarBooks.value = [];

  // Update route
  await router.push(`/books/${id}`);

  // Wait for route to update
  await nextTick();

  // Reload all data
  await loadBookDetails();
  await loadComments(bookId.value);
  if (isAuthenticated.value) {
    await loadUserRating(bookId.value);
  }

  // Scroll to top of book-detail-content
  const contentElement = document.querySelector(".book-detail-content");
  if (contentElement) {
    contentElement.scrollTo({ top: 0, behavior: "smooth" });
  }
}

async function handleSubmitRating() {
  if (!book.value || selectedRatingValue.value === 0) return;

  try {
    submittingRating.value = true;
    await submitRating(book.value.id, selectedRatingValue.value);
    await loadBookDetails(); // Reload to update average rating
    closeRatingModal();

    // Инвалидация кеша рекомендаций после оценки
    if (userId.value) {
      try {
        await invalidateUserCache(userId.value);
        console.log(
          "✓ Кеш рекомендаций обновлен после оценки книги",
          "userId=",
          userId.value,
        );
      } catch (error) {
        console.error("Ошибка инвалидации кеша:", error);
      }
    } else {
      console.warn("⚠ userId не найден, кеш не обновлен");
    }
  } catch (err) {
    console.error("Failed to submit rating:", err);
  } finally {
    submittingRating.value = false;
  }
}

function closeRatingModal() {
  showRatingModal.value = false;
  selectedRatingValue.value = 0;
}

async function handleSubmitComment(commentText) {
  await submitComment(bookId.value, commentText);
}

async function handleUpdateComment(commentId, newText) {
  await updateComment(commentId, newText);
}

async function handleDeleteComment(commentId) {
  await deleteComment(commentId);
}

async function handleModerateDeleteComment(commentId) {
  await moderateDeleteComment(commentId);
}

async function handleRestoreComment(commentId) {
  await restoreComment(commentId);
}

async function handleLoadMoreComments() {
  await loadMoreComments(bookId.value);
}

function handleImageError(event) {
  event.target.style.display = "none";
}

function formatRating(rating) {
  return rating.toFixed(1);
}

function getLanguageLabel(lang) {
  const labels = {
    ru: "Русский",
    en: "English",
    uk: "Українська",
    be: "Беларуская",
  };
  return labels[lang] || lang;
}
</script>

<style scoped>
.book-detail-content {
  flex: 1;
  padding: 40px 20px 20px 20px;
  max-width: 1200px;
  margin: 0 auto;
  overflow-y: auto;
  overflow-x: hidden;
}

/* Loading & Error States */
.loading-state,
.error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  text-align: center;
  padding: 40px;
}

.spinner {
  width: 50px;
  height: 50px;
  border: 4px solid #e2e8f0;
  border-top-color: #667eea;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.error-state .icon {
  font-size: 64px;
  margin-bottom: 20px;
}

.error-state h2 {
  color: #e53e3e;
  margin-bottom: 10px;
}

/* Back Button */
.back-button-small {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  background: #ffffff;
  border: 2px solid #e2e8f0;
  border-radius: 8px;
  color: #2d3748;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  margin-bottom: 20px;
}

.back-button-small:hover {
  background: #f7fafc;
  border-color: #cbd5e0;
  transform: translateX(-4px);
}

/* Book Header */
.book-header {
  display: grid;
  grid-template-columns: 300px 1fr;
  gap: 40px;
  margin-bottom: 40px;
  background: #ffffff;
  padding: 30px;
  border-radius: 16px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.05);
}

.book-cover-large {
  position: relative;
  width: 100%;
  aspect-ratio: 2/3;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.15);
}

.book-cover-placeholder {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  font-size: 64px;
}

.cover-image {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.book-info {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.book-title {
  font-size: 36px;
  font-weight: 700;
  color: #2d3748;
  margin: 0;
  line-height: 1.2;
}

.book-meta {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.book-meta p {
  margin: 0;
  font-size: 16px;
  color: #4a5568;
}

.book-meta .label {
  font-weight: 600;
  color: #2d3748;
  margin-right: 8px;
}

.author-link {
  color: #667eea;
  text-decoration: none;
  font-weight: 600;
  transition: color 0.2s;
}

.author-link:hover {
  color: #764ba2;
  text-decoration: underline;
}

.genre-tag {
  display: inline-block;
  padding: 4px 12px;
  background: #edf2f7;
  border-radius: 16px;
  font-size: 14px;
  color: #4a5568;
  margin-right: 8px;
  margin-bottom: 4px;
}

.stats-row {
  display: flex;
  gap: 20px;
  flex-wrap: wrap;
}

.stat {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 15px;
  color: #4a5568;
  font-weight: 500;
}

.stat-icon {
  font-size: 18px;
}

/* Action Buttons */
.action-buttons {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-top: auto;
}

.primary-button,
.secondary-button {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  border-radius: 8px;
  font-weight: 600;
  font-size: 16px;
  cursor: pointer;
  transition: all 0.2s;
  border: none;
}

.primary-button {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #ffffff;
}

.primary-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 16px rgba(102, 126, 234, 0.3);
}

.secondary-button {
  background: #ffffff;
  color: #667eea;
  border: 2px solid #667eea;
}

.secondary-button:hover {
  background: #f7fafc;
  transform: translateY(-2px);
}

/* Description */
.book-description {
  background: #ffffff;
  padding: 30px;
  border-radius: 16px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.05);
  margin-bottom: 40px;
}

.book-description h2 {
  font-size: 24px;
  font-weight: 700;
  color: #2d3748;
  margin: 0 0 16px 0;
}

.book-description p {
  font-size: 16px;
  line-height: 1.7;
  color: #4a5568;
  margin: 0;
}

/* Similar Books */
.similar-section {
  background: #ffffff;
  padding: 30px;
  border-radius: 16px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.05);
  margin-bottom: 40px;
}

.similar-section h2 {
  font-size: 24px;
  font-weight: 700;
  color: #2d3748;
  margin: 0 0 20px 0;
}

.similar-books-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 20px;
}

.similar-book-card {
  cursor: pointer;
  transition: transform 0.2s;
}

.similar-book-card:hover {
  transform: translateY(-4px);
}

.book-cover-small {
  position: relative;
  width: 100%;
  aspect-ratio: 2/3;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
  margin-bottom: 12px;
}

.book-cover-small .book-cover-placeholder {
  font-size: 32px;
}

.book-info-small h4 {
  font-size: 14px;
  font-weight: 600;
  color: #2d3748;
  margin: 0 0 4px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.book-info-small p {
  font-size: 12px;
  color: #718096;
  margin: 0;
}

/* Comments Section */
.comments-section {
  background: #ffffff;
  padding: 30px;
  border-radius: 16px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.05);
}

.comments-section h2 {
  font-size: 24px;
  font-weight: 700;
  color: #2d3748;
  margin: 0 0 20px 0;
}

/* Responsive Design */
@media (max-width: 1024px) {
  .book-header {
    grid-template-columns: 250px 1fr;
    gap: 30px;
    padding: 20px;
  }

  .book-title {
    font-size: 28px;
  }
}

@media (max-width: 768px) {
  .book-detail-content {
    padding: 15px;
  }

  .book-header {
    grid-template-columns: 1fr;
    gap: 20px;
    padding: 20px;
  }

  .book-cover-large {
    max-width: 250px;
    margin: 0 auto;
  }

  .book-title {
    font-size: 24px;
    text-align: center;
  }

  .book-meta {
    text-align: center;
  }

  .stats-row {
    justify-content: center;
  }

  .action-buttons {
    justify-content: center;
  }

  .similar-books-grid {
    grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
    gap: 15px;
  }
}

@media (max-width: 480px) {
  .book-title {
    font-size: 20px;
  }

  .book-description,
  .similar-section,
  .comments-section {
    padding: 20px;
  }

  .similar-books-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
