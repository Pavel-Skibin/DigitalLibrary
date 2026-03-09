<template>
  <div class="personal-recommendations">
    <div class="section-header">
      <h2 class="section-title">
        <span class="icon">🎯</span>
        Рекомендации для вас
      </h2>
      <p v-if="recommendations?.source" class="recommendation-source">
        {{ getSourceLabel(recommendations.source) }}
      </p>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="loading-state">
      <div class="spinner"></div>
      <p>Подбираем книги специально для вас...</p>
    </div>

    <!-- Error State -->
    <div v-else-if="error" class="error-state">
      <span class="icon">⚠️</span>
      <p>Не удалось загрузить рекомендации</p>
      <button @click="reload" class="retry-button">Попробовать снова</button>
    </div>

    <!-- Empty State -->
    <div
      v-else-if="
        !recommendations ||
        !recommendations.recommendations ||
        recommendations.recommendations.length === 0
      "
      class="empty-state"
    >
      <span class="icon">📚</span>
      <h3>Пока нет рекомендаций</h3>
      <p>
        Оцените несколько книг, чтобы мы могли подобрать для вас подходящие
        произведения
      </p>
    </div>

    <!-- Recommendations Grid -->
    <div v-else class="carousel-container">
      <button
        @click="scrollLeft"
        class="carousel-arrow left"
        v-if="recommendations.recommendations.length > 3"
      >
        ❮
      </button>

      <div class="recommendations-carousel" ref="carouselRef">
        <div
          v-for="book in recommendations.recommendations"
          :key="book.book_id"
          class="book-card"
          @click="goToBook(book.book_id)"
        >
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
            <div class="book-score">⭐ {{ formatScore(book.final_score) }}</div>
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
        @click="scrollRight"
        class="carousel-arrow right"
        v-if="recommendations.recommendations.length > 3"
      >
        ❯
      </button>
    </div>

    <!-- View All Button -->
    <div
      v-if="
        recommendations &&
        recommendations.total > recommendations.recommendations.length
      "
      class="view-all"
    >
      <button @click="loadMore" class="view-all-button">
        Показать ещё рекомендации
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { useRouter } from "vue-router";
import { useRecommendations } from "@/composables/useRecommendations";
import { formatRating } from "@/utils/formatters";

const router = useRouter();
const carouselRef = ref(null);

const {
  personalRecommendations: recommendations,
  loading,
  error,
  isAuthenticated,
  loadPersonalRecommendations,
} = useRecommendations();

// Load recommendations on mount
onMounted(async () => {
  if (isAuthenticated.value) {
    await loadPersonalRecommendations(null, 12);
  } else {
  }
});

// Reload recommendations
async function reload() {
  await loadPersonalRecommendations(null, 12);
}

// Format score
function formatScore(score) {
  return score ? score.toFixed(2) : "—";
}

// Format authors
function formatAuthors(authors) {
  if (!authors || authors.length === 0) return "Автор неизвестен";
  if (authors.length === 1) return authors[0];
  return `${authors[0]} и др.`;
}

// Get source label
function getSourceLabel(source) {
  const labels = {
    personalized: "На основе ваших предпочтений",
    collaborative: "Что читают похожие пользователи",
    popular: "Популярное в библиотеке",
    cold_start: "Подборка для вас",
  };
  return labels[source] || "";
}

// Navigate to book details
function goToBook(bookId) {
  router.push(`/books/${bookId}`);
}

// Load more recommendations
async function loadMore() {
  const currentLimit = recommendations.value?.recommendations?.length || 12;
  await loadPersonalRecommendations(null, currentLimit + 12);
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
.personal-recommendations {
  padding: 30px 32px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  margin: 0 auto 20px;
  border-radius: 16px;
  max-width: 900px;
  box-sizing: border-box;
}

.section-header {
  margin-bottom: 20px;
  text-align: center;
  max-width: 800px;
  margin-left: auto;
  margin-right: auto;
}

.section-title {
  color: #ffffff;
  font-size: 24px;
  font-weight: 700;
  margin: 0 0 10px 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
}

.section-title .icon {
  font-size: 26px;
}

.recommendation-source {
  color: rgba(255, 255, 255, 0.9);
  font-size: 14px;
  margin: 0;
}

/* Loading State */
.loading-state {
  text-align: center;
  padding: 60px 20px;
  color: #ffffff;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 4px solid rgba(255, 255, 255, 0.3);
  border-top-color: #ffffff;
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
  color: #ffffff;
}

.error-state .icon {
  font-size: 48px;
  display: block;
  margin-bottom: 16px;
}

.retry-button {
  margin-top: 20px;
  padding: 10px 24px;
  background: #ffffff;
  color: #667eea;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.retry-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

/* Empty State */
.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #ffffff;
}

.empty-state .icon {
  font-size: 64px;
  display: block;
  margin-bottom: 20px;
}

.empty-state h3 {
  font-size: 24px;
  margin: 0 0 12px 0;
}

.empty-state p {
  font-size: 16px;
  opacity: 0.9;
  max-width: 500px;
  margin: 0 auto;
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
  border: 2px solid #ffffff;
  border-radius: 50%;
  width: 48px;
  height: 48px;
  font-size: 24px;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
}

.carousel-arrow:hover {
  background: #ffffff;
  color: #764ba2;
  transform: translateY(-50%) scale(1.1);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.3);
}

.carousel-arrow.left {
  left: -20px;
}

.carousel-arrow.right {
  right: -20px;
}

/* Recommendations Carousel */
.recommendations-carousel {
  display: flex;
  gap: 16px;
  overflow-x: auto;
  scroll-behavior: smooth;
  padding: 10px 0;
  scrollbar-width: thin;
  scrollbar-color: rgba(255, 255, 255, 0.5) rgba(255, 255, 255, 0.1);
  max-width: 100%;
}

.recommendations-carousel::-webkit-scrollbar {
  height: 8px;
}

.recommendations-carousel::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.1);
  border-radius: 4px;
}

.recommendations-carousel::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.5);
  border-radius: 4px;
}

.recommendations-carousel::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.7);
}

/* Book Card */
.book-card {
  background: #ffffff;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  width: 135px;
}

.book-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2);
}

.book-cover {
  position: relative;
  width: 100%;
  height: 185px;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  overflow: hidden;
  flex-shrink: 0;
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

.book-score {
  position: absolute;
  top: 12px;
  right: 12px;
  background: rgba(0, 0, 0, 0.7);
  color: #ffffff;
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  z-index: 2;
}

.book-info {
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

.book-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 12px;
  margin-top: auto;
}

.book-meta .rating {
  color: #f59e0b;
  font-weight: 600;
}

.book-meta .genre {
  color: #667eea;
  background: rgba(102, 126, 234, 0.1);
  padding: 4px 8px;
  border-radius: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 120px;
}

/* View All */
.view-all {
  text-align: center;
  margin-top: 30px;
}

.view-all-button {
  padding: 12px 32px;
  background: #ffffff;
  color: #667eea;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.view-all-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

/* Responsive Design */

/* Large Desktops (27" screens, 1920px+) */
@media (min-width: 1920px) {
  .book-card {
    width: 155px;
  }

  .section-title {
    font-size: 26px;
  }

  .personal-recommendations {
    padding: 50px 40px;
    max-width: 1100px;
  }
}

/* Desktop (1400px - 1920px) */
@media (max-width: 1919px) and (min-width: 1400px) {
  .book-card {
    width: 140px;
  }

  .personal-recommendations {
    max-width: 1000px;
  }
}

/* Laptop (1024px - 1400px) */
@media (max-width: 1399px) and (min-width: 1024px) {
  .book-card {
    width: 125px;
  }

  .section-title {
    font-size: 22px;
  }

  .personal-recommendations {
    padding: 28px 28px;
    max-width: 860px;
  }
}

/* Tablet (768px - 1024px) */
@media (max-width: 1023px) and (min-width: 768px) {
  .book-card {
    width: 115px;
  }

  .section-title {
    font-size: 20px;
  }

  .personal-recommendations {
    padding: 22px 20px;
    margin: 0 auto 20px;
    max-width: 100%;
  }

  .book-cover {
    height: 170px;
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
  .personal-recommendations {
    padding: 25px 18px;
    margin: 0 0 18px 0;
    border-radius: 12px;
  }

  .section-title {
    font-size: 18px;
  }

  .section-title .icon {
    font-size: 22px;
  }

  .book-card {
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

  .view-all-button {
    padding: 10px 24px;
    font-size: 14px;
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
  .personal-recommendations {
    padding: 20px 15px;
    margin: 0 0 15px 0;
    border-radius: 8px;
  }

  .section-title {
    font-size: 16px;
    flex-direction: column;
    gap: 8px;
  }

  .section-title .icon {
    font-size: 18px;
  }

  .recommendation-source {
    font-size: 11px;
  }

  .book-card {
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

  .book-info {
    padding: 12px;
  }

  .book-meta {
    font-size: 11px;
    gap: 8px;
  }

  .view-all-button {
    padding: 10px 20px;
    font-size: 14px;
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
