<template>
  <div class="similar-books">
    <div class="section-header">
      <h2 class="section-title">
        <span class="icon">📚</span>
        Похожие книги
      </h2>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="loading-state">
      <div class="spinner"></div>
      <p>Ищем похожие книги...</p>
    </div>

    <!-- Error State -->
    <div v-else-if="error" class="error-state">
      <span class="icon">⚠️</span>
      <p>Не удалось загрузить похожие книги</p>
      <button @click="reload" class="retry-button">Попробовать снова</button>
    </div>

    <!-- Empty State -->
    <div v-else-if="!books || books.length === 0" class="empty-state">
      <span class="icon">🤷</span>
      <p>Похожих книг пока не найдено</p>
    </div>

    <!-- Similar Books List -->
    <div v-else class="similar-books-carousel">
      <button
        v-if="books.length > 4"
        class="carousel-button prev"
        @click="scrollLeft"
        aria-label="Предыдущая книга"
      >
        ‹
      </button>

      <div class="books-container" ref="carouselRef">
        <div
          v-for="book in books"
          :key="book.book_id"
          class="similar-book-card"
          @click="goToBook(book.book_id)"
        >
          <div class="book-cover">
            <img
              v-if="book.cover_image_url"
              :src="book.cover_image_url"
              :alt="book.title"
              @error="handleImageError"
            />
            <div v-else class="book-cover-placeholder">
              <span>📖</span>
            </div>
            <div class="similarity-score">
              {{ formatSimilarity(book.score) }}% похожа
            </div>
          </div>

          <div class="book-info">
            <h3 class="book-title" :title="book.title">{{ book.title }}</h3>
            <p class="book-authors">{{ formatAuthors(book.authors) }}</p>
            <div class="book-meta">
              <span v-if="book.average_rating" class="rating">
                ⭐ {{ formatRating(book.average_rating) }}
              </span>
              <span v-if="book.genres && book.genres.length > 0" class="genre">
                {{ book.genres[0] }}
              </span>
            </div>
          </div>
        </div>
      </div>

      <button
        v-if="books.length > 4"
        class="carousel-button next"
        @click="scrollRight"
        aria-label="Следующая книга"
      >
        ›
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { useRouter } from "vue-router";
import { useRecommendations } from "@/composables/useRecommendations";
import { formatRating } from "@/utils/formatters";

const props = defineProps({
  bookId: {
    type: [Number, String],
    required: true,
  },
  limit: {
    type: Number,
    default: 8,
  },
});

const router = useRouter();
const carouselRef = ref(null);

const {
  similarBooks: books,
  loading,
  error,
  loadSimilarBooks,
} = useRecommendations();

// Load similar books on mount
onMounted(async () => {
  await loadSimilarBooks(props.bookId, props.limit);
});

// Reload similar books
async function reload() {
  await loadSimilarBooks(props.bookId, props.limit);
}

// Format similarity score (0-1 to 0-100%)
function formatSimilarity(score) {
  if (!score) return "0";
  return Math.round(score * 100);
}

// Format authors
function formatAuthors(authors) {
  if (!authors || authors.length === 0) return "Автор неизвестен";
  if (authors.length === 1) return authors[0];
  return `${authors[0]} и др.`;
}

// Navigate to book details
function goToBook(bookId) {
  router.push(`/books/${bookId}`);
  // Reload similar books for the new book
  setTimeout(() => {
    loadSimilarBooks(bookId, props.limit);
  }, 100);
}

// Handle image error
function handleImageError(event) {
  event.target.style.display = "none";
}

// Carousel navigation
function scrollLeft() {
  if (carouselRef.value) {
    carouselRef.value.scrollBy({ left: -300, behavior: "smooth" });
  }
}

function scrollRight() {
  if (carouselRef.value) {
    carouselRef.value.scrollBy({ left: 300, behavior: "smooth" });
  }
}
</script>

<style scoped>
.similar-books {
  padding: 40px 0;
  background: #f8f9fa;
  margin: 0 -40px;
  padding-left: 40px;
  padding-right: 40px;
}

.section-header {
  margin-bottom: 30px;
}

.section-title {
  color: #2d3748;
  font-size: 24px;
  font-weight: 700;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 12px;
}

.section-title .icon {
  font-size: 28px;
}

/* Loading State */
.loading-state {
  text-align: center;
  padding: 60px 20px;
  color: #718096;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 4px solid rgba(102, 126, 234, 0.2);
  border-top-color: #667eea;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 20px;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

/* Error State */
.error-state {
  text-align: center;
  padding: 60px 20px;
  color: #718096;
}

.error-state .icon {
  font-size: 48px;
  display: block;
  margin-bottom: 16px;
}

.retry-button {
  margin-top: 20px;
  padding: 10px 24px;
  background: #667eea;
  color: #ffffff;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.retry-button:hover {
  background: #5a67d8;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

/* Empty State */
.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #718096;
}

.empty-state .icon {
  font-size: 64px;
  display: block;
  margin-bottom: 20px;
}

/* Carousel */
.similar-books-carousel {
  position: relative;
  padding: 0 50px;
}

.books-container {
  display: flex;
  gap: 20px;
  overflow-x: auto;
  scroll-behavior: smooth;
  scrollbar-width: none;
  -ms-overflow-style: none;
  padding: 10px 0;
}

.books-container::-webkit-scrollbar {
  display: none;
}

.carousel-button {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 40px;
  height: 40px;
  background: rgba(255, 255, 255, 0.95);
  border: 2px solid #e2e8f0;
  border-radius: 50%;
  font-size: 24px;
  cursor: pointer;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  color: #667eea;
}

.carousel-button:hover {
  background: #667eea;
  color: #ffffff;
  transform: translateY(-50%) scale(1.1);
  border-color: #667eea;
}

.carousel-button.prev {
  left: 0;
}

.carousel-button.next {
  right: 0;
}

/* Similar Book Card */
.similar-book-card {
  flex: 0 0 200px;
  background: #ffffff;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  border: 2px solid transparent;
}

.similar-book-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.15);
  border-color: #667eea;
}

.book-cover {
  position: relative;
  width: 100%;
  height: 280px;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  overflow: hidden;
}

.book-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.book-cover-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 64px;
}

.similarity-score {
  position: absolute;
  bottom: 12px;
  left: 12px;
  right: 12px;
  background: rgba(102, 126, 234, 0.95);
  color: #ffffff;
  padding: 6px 12px;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 700;
  text-align: center;
  backdrop-filter: blur(4px);
}

.book-info {
  padding: 16px;
}

.book-title {
  font-size: 15px;
  font-weight: 600;
  margin: 0 0 8px 0;
  color: #2d3748;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.4;
}

.book-authors {
  font-size: 13px;
  color: #718096;
  margin: 0 0 12px 0;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.book-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 12px;
}

.book-meta .rating {
  color: #f59e0b;
  font-weight: 600;
}

.book-meta .genre {
  color: #667eea;
  background: rgba(102, 126, 234, 0.1);
  padding: 3px 8px;
  border-radius: 4px;
  flex: 1;
  text-align: center;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* Responsive */
@media (max-width: 768px) {
  .similar-books {
    padding: 30px 20px;
    margin: 0 -20px;
  }

  .section-title {
    font-size: 20px;
  }

  .similar-books-carousel {
    padding: 0 30px;
  }

  .similar-book-card {
    flex: 0 0 160px;
  }

  .book-cover {
    height: 220px;
  }
}
</style>
