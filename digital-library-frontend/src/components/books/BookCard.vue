<template>
  <div class="book-card" @click="$emit('select', book)">
    <div class="book-cover">
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
    <div class="book-info">
      <h3>{{ book.title }}</h3>
      <p>{{ book.authors.join(", ") }}</p>
      <div class="rating-stars">
        <span
          v-for="starIndex in 5"
          :key="starIndex"
          :class="getStarClass(starIndex).class"
          :style="{
            '--fill-percentage': getStarClass(starIndex).fillPercentage + '%',
          }"
          >★</span
        >
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from "vue";

const props = defineProps({
  book: {
    type: Object,
    required: true,
  },
  coverUrl: {
    type: String,
    default: null,
  },
});

defineEmits(["select"]);

function getStarClass(starIndex) {
  const rating = props.book.averageRating || 0;

  if (starIndex <= rating) {
    return { class: "filled-star", fillPercentage: 100 };
  }

  if (starIndex - 1 < rating && starIndex > rating) {
    return {
      class: "fractional-star",
      fillPercentage: (rating - (starIndex - 1)) * 100,
    };
  }

  return { class: "empty-star", fillPercentage: 0 };
}
</script>

<style scoped>
.book-card {
  display: flex;
  gap: 16px;
  padding: 16px;
  background-color: #fff;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.book-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.book-cover {
  flex-shrink: 0;
  width: 80px;
  height: 120px;
  border-radius: 6px;
  overflow: hidden;
  background-color: #f0f0f0;
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
  font-size: 20px;
  font-weight: bold;
  text-transform: uppercase;
}

.book-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
  overflow: hidden;
}

.book-info h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #333;
  line-height: 1.4;
  overflow-wrap: break-word;
  word-break: break-word;
}

.book-info p {
  margin: 0;
  font-size: 14px;
  color: #666;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rating-stars {
  display: flex;
  gap: 2px;
  font-size: 16px;
}

.filled-star {
  color: #ffd700;
}

.empty-star {
  color: #ddd;
}

.fractional-star {
  position: relative;
  color: #ddd;
}

.fractional-star::before {
  content: "★";
  position: absolute;
  left: 0;
  color: #ffd700;
  overflow: hidden;
  width: var(--fill-percentage);
}
</style>
