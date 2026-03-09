<template>
  <div class="popular-books">
    <div class="section-header">
      <h2 class="section-title">
        <span class="icon">🔥</span>
        Популярное в библиотеке
      </h2>
      <div class="filters">
        <select
          v-model="selectedGenre"
          @change="loadBooks"
          class="genre-filter"
        >
          <option value="">Все жанры</option>
          <option v-for="genre in genres" :key="genre" :value="genre">
            {{ genre }}
          </option>
        </select>
      </div>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="loading-grid">
      <div v-for="i in 6" :key="i" class="skeleton-card"></div>
    </div>

    <!-- Error State -->
    <div v-else-if="error" class="error-state">
      <span class="icon">⚠️</span>
      <p>Не удалось загрузить популярные книги</p>
      <button @click="loadBooks" class="retry-button">Попробовать снова</button>
    </div>

    <!-- Books Grid -->
    <div v-else-if="books && books.length > 0" class="carousel-container">
      <button
        @click="scrollLeft"
        class="carousel-arrow left"
        v-if="books.length > 3"
      >
        ❮
      </button>

      <div class="books-carousel" ref="carouselRef">
        <div
          v-for="(book, index) in books"
          :key="book.book_id"
          class="popular-book-card"
          @click="goToBook(book.book_id)"
        >
          <!-- Popularity Badge -->
          <div class="popularity-badge">
            <span class="rank">#{{ index + 1 }}</span>
          </div>

          <div class="book-cover">
            <div class="book-cover-placeholder">
              <span>📖</span>
            </div>
            <img
              v-if="book.book_id"
              :src="`/api/books/${book.book_id}/cover`"
              :alt="book.title"
              @error="handleImageError"
              class="cover-image"
            />
          </div>

          <div class="book-details">
            <h3 class="book-title" :title="book.title">{{ book.title }}</h3>
            <p class="book-authors">{{ formatAuthors(book.authors) }}</p>

            <div class="book-stats">
              <span v-if="book.average_rating" class="stat">
                <span class="stat-icon">⭐</span>
                {{ formatRating(book.average_rating) }}
              </span>
              <span v-if="book.ratings_count" class="stat">
                <span class="stat-icon">💬</span>
                {{ book.ratings_count }}
              </span>
            </div>

            <div
              v-if="book.genres && book.genres.length > 0"
              class="book-genres"
            >
              <span
                v-for="genre in book.genres.slice(0, 2)"
                :key="genre"
                class="genre-tag"
              >
                {{ genre }}
              </span>
            </div>
          </div>
        </div>
      </div>

      <button
        @click="scrollRight"
        class="carousel-arrow right"
        v-if="books.length > 3"
      >
        ❯
      </button>
    </div>

    <!-- Empty State -->
    <div v-else class="empty-state">
      <span class="icon">📚</span>
      <p>Пока нет популярных книг</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { useRouter } from "vue-router";
import { useRecommendations } from "@/composables/useRecommendations";
import { formatRating } from "@/utils/formatters";

const router = useRouter();
const selectedGenre = ref("");
const carouselRef = ref(null);

const genres = ref([
  "Фантастика",
  "Детектив",
  "Роман",
  "Фэнтези",
  "Научная фантастика",
  "Классика",
  "Приключения",
  "Триллер",
]);

const {
  popularBooks: books,
  loading,
  error,
  loadPopularBooks,
} = useRecommendations();

// Load popular books on mount
onMounted(async () => {
  await loadBooks();
});

// Load books with current filters
async function loadBooks() {
  await loadPopularBooks(12, selectedGenre.value || null);
}

// Format authors
function formatAuthors(authors) {
  if (!authors || authors.length === 0) return "Автор неизвестен";
  if (authors.length === 1) return authors[0];
  if (authors.length === 2) return authors.join(", ");
  return `${authors[0]} и др.`;
}

// Format views count
function formatViews(count) {
  if (count >= 1000) {
    return `${(count / 1000).toFixed(1)}k`;
  }
  return count;
}

// Navigate to book details
function goToBook(bookId) {
  router.push(`/books/${bookId}`);
}

// Handle image error
function handleImageError(event) {
  event.target.style.display = "none";
}

// Carousel navigation
function scrollLeft() {
  if (carouselRef.value) {
    carouselRef.value.scrollBy({ left: -400, behavior: "smooth" });
  }
}

function scrollRight() {
  if (carouselRef.value) {
    carouselRef.value.scrollBy({ left: 400, behavior: "smooth" });
  }
}
</script>

<style scoped>
.popular-books {
  padding: 20px 0;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.section-title {
  color: #2d3748;
  font-size: 22px;
  font-weight: 700;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 12px;
}

.section-title .icon {
  font-size: 24px;
}

.filters {
  display: flex;
  gap: 12px;
}

.genre-filter {
  padding: 10px 16px;
  border: 2px solid #e2e8f0;
  border-radius: 8px;
  background: #ffffff;
  color: #2d3748;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  min-width: 150px;
}

.genre-filter:hover {
  border-color: #667eea;
}

.genre-filter:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

/* Loading Grid */
.loading-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 24px;
}

.skeleton-card {
  height: 180px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: loading 1.5s infinite;
  border-radius: 12px;
}

@keyframes loading {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
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

/* Carousel Container */
.carousel-container {
  position: relative;
  width: 100%;
}

.carousel-arrow {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  z-index: 10;
  background: rgba(255, 255, 255, 0.95);
  color: #667eea;
  border: 2px solid #667eea;
  border-radius: 50%;
  width: 48px;
  height: 48px;
  font-size: 24px;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  display: flex;
  align-items: center;
  justify-content: center;
}

.carousel-arrow:hover {
  background: #667eea;
  color: #ffffff;
  transform: translateY(-50%) scale(1.1);
  box-shadow: 0 6px 16px rgba(102, 126, 234, 0.3);
}

.carousel-arrow.left {
  left: -20px;
}

.carousel-arrow.right {
  right: -20px;
}

/* Books Carousel */
.books-carousel {
  display: flex;
  gap: 24px;
  overflow-x: auto;
  scroll-behavior: smooth;
  padding: 10px 0;
  scrollbar-width: thin;
  scrollbar-color: #cbd5e0 #f7fafc;
  max-width: min(100%, 680px);
  margin: 0 auto;
}

.books-carousel::-webkit-scrollbar {
  height: 8px;
}

.books-carousel::-webkit-scrollbar-track {
  background: #f7fafc;
  border-radius: 4px;
}

.books-carousel::-webkit-scrollbar-thumb {
  background: #cbd5e0;
  border-radius: 4px;
}

.books-carousel::-webkit-scrollbar-thumb:hover {
  background: #a0aec0;
}

/* Popular Book Card */
.popular-book-card {
  position: relative;
  display: flex;
  flex-direction: column;
  background: #ffffff;
  border: 2px solid #e2e8f0;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s ease;
  flex-shrink: 0;
  width: 135px;
}

.popular-book-card:hover {
  border-color: #667eea;
  transform: translateY(-4px);
  box-shadow: 0 8px 20px rgba(102, 126, 234, 0.15);
}

.popularity-badge {
  position: absolute;
  top: 12px;
  left: 12px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #ffffff;
  padding: 6px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 700;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
  z-index: 10;
}

.book-cover {
  width: 100%;
  height: 185px;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  overflow: hidden;
  flex-shrink: 0;
  position: relative;
}

.book-cover img.cover-image {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  z-index: 1;
}

.book-cover-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 64px;
  position: absolute;
  top: 0;
  left: 0;
  z-index: 0;
}

.book-details {
  padding: 12px;
  flex: 1;
  display: flex;
  flex-direction: column;
}

.book-title {
  font-size: 13px;
  font-weight: 600;
  margin: 0 0 8px 0;
  color: #2d3748;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.4;
  min-height: 44px;
}

.book-authors {
  font-size: 12px;
  color: #718096;
  margin: 0 0 12px 0;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.book-stats {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  font-size: 13px;
  color: #4a5568;
}

.stat {
  display: flex;
  align-items: center;
  gap: 4px;
}

.stat-icon {
  font-size: 14px;
}

.book-genres {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: auto;
}

.genre-tag {
  font-size: 11px;
  color: #667eea;
  background: rgba(102, 126, 234, 0.1);
  padding: 4px 8px;
  border-radius: 4px;
  font-weight: 500;
}

/* Responsive Design */

/* Large Desktops (27" screens, 1920px+) */
@media (min-width: 1920px) {
  .popular-book-card {
    width: 155px;
  }

  .section-title {
    font-size: 24px;
  }

  .books-carousel {
    max-width: min(100%, 1200px);
  }
}

/* Desktop (1400px - 1920px) */
@media (max-width: 1920px) and (min-width: 1400px) {
  .popular-book-card {
    width: 135px;
  }
}

/* Laptop (1024px - 1400px) */
@media (max-width: 1399px) and (min-width: 1024px) {
  .popular-book-card {
    width: 125px;
  }

  .section-title {
    font-size: 20px;
  }

  .books-carousel {
    max-width: min(100%, 800px);
  }
}

/* Tablet (768px - 1024px) */
@media (max-width: 1023px) and (min-width: 768px) {
  .popular-book-card {
    width: 115px;
  }

  .section-title {
    font-size: 18px;
  }

  .book-cover {
    height: 170px;
  }

  .books-carousel {
    max-width: min(100%, 520px);
  }

  .carousel-arrow {
    width: 40px;
    height: 40px;
    font-size: 20px;
  }

  .carousel-arrow.left {
    left: -15px;
  }

  .carousel-arrow.right {
    right: -15px;
  }
}

/* Mobile / Small Tablet (480px - 768px) */
@media (max-width: 767px) and (min-width: 480px) {
  .section-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }

  .section-title {
    font-size: 16px;
  }

  .popular-book-card {
    width: 110px;
  }

  .book-cover {
    height: 150px;
  }

  .book-title {
    font-size: 12px;
    min-height: 34px;
  }

  .book-authors {
    font-size: 11px;
  }

  .filters {
    width: 100%;
  }

  .genre-filter {
    flex: 1;
    min-width: auto;
  }

  .carousel-arrow {
    width: 36px;
    height: 36px;
    font-size: 18px;
  }

  .carousel-arrow.left {
    left: -10px;
  }

  .carousel-arrow.right {
    right: -10px;
  }
}

/* Small Mobile (< 480px) */
@media (max-width: 479px) {
  .popular-books {
    padding: 20px 0;
  }

  .section-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .section-title {
    font-size: 15px;
  }

  .section-title .icon {
    font-size: 18px;
  }

  .popular-book-card {
    width: 100px;
  }

  .book-cover {
    height: 130px;
  }

  .book-title {
    font-size: 11px;
    min-height: 30px;
  }

  .book-authors {
    font-size: 10px;
  }

  .book-details {
    padding: 12px;
  }

  .filters {
    width: 100%;
    flex-direction: column;
  }

  .genre-filter {
    width: 100%;
    min-width: auto;
  }

  .book-stats {
    font-size: 12px;
    gap: 8px;
  }

  .genre-tag {
    font-size: 10px;
    padding: 3px 6px;
  }

  .carousel-arrow {
    width: 32px;
    height: 32px;
    font-size: 16px;
  }

  .carousel-arrow.left {
    left: 0;
  }

  .carousel-arrow.right {
    right: 0;
  }
}
</style>
