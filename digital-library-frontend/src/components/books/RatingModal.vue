<template>
  <div class="modal-overlay" @click="$emit('close')">
    <div class="modal-content rating-modal" @click.stop>
      <h2 class="modal-title">Оценить книгу</h2>
      <p class="modal-subtitle">{{ bookTitle }}</p>

      <div class="rating-picker">
        <div class="rating-stars-large">
          <span
              v-for="starIndex in 5"
              :key="starIndex"
              class="star-clickable"
              :class="{
              'filled': starIndex <= (hoverRating || modelValue),
              'empty': starIndex > (hoverRating || modelValue)
            }"
              @mouseenter="hoverRating = starIndex"
              @mouseleave="hoverRating = 0"
              @click="$emit('update:modelValue', starIndex)"
          >★</span>
        </div>
        <p class="rating-label">
          <span v-if="hoverRating">{{ hoverRating }}</span>
          <span v-else-if="modelValue">{{ modelValue }}</span>
          <span v-else>Выберите оценку</span>
          <span v-if="hoverRating || modelValue"> из 5</span>
        </p>
      </div>

      <div class="modal-buttons">
        <button @click="$emit('close')" class="btn-secondary">
          Отмена
        </button>
        <button
            @click="$emit('submit')"
            :disabled="!modelValue || loading"
            class="btn-primary"
        >
          {{ loading ? 'Сохранение...' : 'Сохранить' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

defineProps({
  modelValue: {
    type: Number,
    default: 0
  },
  bookTitle: {
    type: String,
    required: true
  },
  loading: {
    type: Boolean,
    default: false
  }
})

defineEmits(['close', 'submit', 'update:modelValue'])

const hoverRating = ref(0)
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
}

.modal-content {
  background-color: white;
  border-radius: 12px;
  padding: 32px;
  width: 90%;
  max-width: 400px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

.modal-title {
  margin: 0 0 8px 0;
  font-size: 24px;
  font-weight: 600;
  color: #333;
  text-align: center;
}

.modal-subtitle {
  text-align: center;
  color: #666;
  font-size: 16px;
  margin-bottom: 24px;
  font-weight: 500;
}

.rating-picker {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 32px 16px;
  background-color: #f9f9f9;
  border-radius: 8px;
  margin-bottom: 24px;
}

.rating-stars-large {
  display: flex;
  gap: 8px;
  font-size: 48px;
}

.star-clickable {
  cursor: pointer;
  transition: all 0.2s ease;
  color: #ddd;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.star-clickable.filled {
  color: #FFD700;
  transform: scale(1.1);
}

.star-clickable:hover {
  transform: scale(1.2);
}

.rating-label {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.modal-buttons {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
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
}

.btn-primary:hover:not(:disabled) {
  background-color: #1976D2;
}

.btn-primary:disabled {
  background-color: #ccc;
  cursor: not-allowed;
}
</style>