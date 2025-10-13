<template>
  <div class="book-detail-content">
    <!-- Обложка -->
    <div class="detail-cover">
      <img
          v-if="coverUrl"
          :src="coverUrl"
          :alt="book.title"
          class="cover-image"
      />
      <div v-else class="placeholder-cover">
        {{ book.title.slice(0, 4) }}
      </div>
    </div>

    <!-- Информация о книге -->
    <h2 class="book-title">{{ book.title }}</h2>
    <p class="book-author">{{ book.authors.join(', ') }}</p>

    <!-- Рейтинг -->
    <div class="rating-stars large">
      <span
          v-for="starIndex in 5"
          :key="starIndex"
          :class="getStarClass(starIndex).class"
          :style="{ '--fill-percentage': getStarClass(starIndex).fillPercentage + '%' }"
      >★</span>
      <span class="average-rating">
        {{ formatRating(book.averageRating) }}
      </span>
    </div>

    <!-- Описание -->
    <p class="book-description">
      {{ book.description || 'Описание отсутствует.' }}
    </p>

    <!-- Кнопка "Читать" -->
    <button type="button" class="read-button" @click="$emit('open-reader')">
      📖 Читать
    </button>

    <!-- Секция рейтинга -->
    <div class="rating-section">
      <div class="rating-info">
        <div class="rating-display">
          <div class="rating-stars-interactive">
            <span
                v-for="starIndex in 5"
                :key="starIndex"
                :class="getStarClass(starIndex).class"
                :style="{ '--fill-percentage': getStarClass(starIndex).fillPercentage + '%' }"
            >★</span>
          </div>
          <span class="rating-value">
            {{ formatRating(book.averageRating) }}
          </span>
        </div>
        <div v-if="userRating" class="user-rating-badge">
          Ваша оценка: {{ userRating.value }} ★
        </div>
      </div>

      <button
          v-if="isAuthenticated"
          @click="$emit('open-rating')"
          class="btn-rate"
      >
        {{ userRating ? 'Изменить оценку' : 'Оценить книгу' }}
      </button>

      <div v-else class="auth-message-inline">
        Войдите, чтобы оценить книгу
      </div>
    </div>
  </div>
</template>

<script setup>
import { formatRating } from '@/utils/formatters'

const props = defineProps({
  book: {
    type: Object,
    required: true
  },
  coverUrl: {
    type: String,
    default: null
  },
  userRating: {
    type: Object,
    default: null
  },
  isAuthenticated: {
    type: Boolean,
    required: true
  }
})

defineEmits(['open-reader', 'open-rating'])

function getStarClass(starIndex) {
  const rating = props.book.averageRating || 0

  if (starIndex <= rating) {
    return { class: 'filled-star', fillPercentage: 100 }
  }

  if (starIndex - 1 < rating && starIndex > rating) {
    return {
      class: 'fractional-star',
      fillPercentage: (rating - (starIndex - 1)) * 100
    }
  }

  return { class: 'empty-star', fillPercentage: 0 }
}
</script>

<style scoped>
.book-detail-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-cover {
  width: 100%;
  height: 400px;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
  margin-bottom: 8px;
}

.cover-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.placeholder-cover {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  font-size: 48px;
  font-weight: bold;
  text-transform: uppercase;
}

.book-title {
  font-size: 24px;
  font-weight: 700;
  color: #333;
  margin: 0;
  line-height: 1.3;
}

.book-author {
  font-size: 16px;
  color: #666;
  margin: 0;
  font-weight: 500;
}

.rating-stars {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 20px;
}

.rating-stars.large {
  font-size: 24px;
  gap: 6px;
  margin: 8px 0;
}

.filled-star {
  color: #FFD700;
}

.empty-star {
  color: #ddd;
}

.fractional-star {
  position: relative;
  color: #ddd;
}

.fractional-star::before {
  content: '★';
  position: absolute;
  left: 0;
  color: #FFD700;
  overflow: hidden;
  width: var(--fill-percentage);
}

.average-rating {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin-left: 8px;
}

.book-description {
  font-size: 14px;
  line-height: 1.6;
  color: #555;
  margin: 8px 0;
  white-space: pre-wrap;
}

.read-button {
  width: 100%;
  padding: 14px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
  margin-top: 8px;
}

.read-button:hover {
  background: linear-gradient(135deg, #764ba2 0%, #667eea 100%);
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(102, 126, 234, 0.4);
}

.read-button:active {
  transform: translateY(0);
}

.rating-section {
  margin-top: 24px;
  padding: 20px;
  background-color: #f9f9f9;
  border-radius: 8px;
  border: 2px solid #e0e0e0;
}

.rating-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 12px;
}

.rating-display {
  display: flex;
  align-items: center;
  gap: 12px;
}

.rating-stars-interactive {
  display: flex;
  gap: 4px;
  font-size: 24px;
}

.rating-value {
  font-size: 20px;
  font-weight: 600;
  color: #333;
}

.user-rating-badge {
  padding: 6px 12px;
  background-color: #4CAF50;
  color: white;
  border-radius: 16px;
  font-size: 13px;
  font-weight: 600;
}

.btn-rate {
  width: 100%;
  padding: 12px;
  background-color: #FF9800;
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.3s, transform 0.2s;
}

.btn-rate:hover {
  background-color: #F57C00;
  transform: translateY(-2px);
}

.btn-rate:active {
  transform: translateY(0);
}

.auth-message-inline {
  text-align: center;
  padding: 12px;
  color: #999;
  font-size: 14px;
  font-style: italic;
}

@media (max-width: 768px) {
  .detail-cover {
    height: 300px;
  }

  .book-title {
    font-size: 20px;
  }

  .rating-info {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>