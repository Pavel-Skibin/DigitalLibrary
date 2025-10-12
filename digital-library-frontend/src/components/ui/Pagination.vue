<template>
  <div v-if="totalPages > 1" class="pagination">
    <button
        @click="$emit('page-change', currentPage - 1)"
        :disabled="currentPage === 0"
        class="pagination-button"
    >
      ← Назад
    </button>
    <span class="page-info">
      Страница {{ currentPage + 1 }} из {{ totalPages }}
    </span>
    <button
        @click="$emit('page-change', currentPage + 1)"
        :disabled="currentPage >= totalPages - 1"
        class="pagination-button"
    >
      Вперёд →
    </button>
  </div>
</template>

<script setup>
defineProps({
  currentPage: {
    type: Number,
    required: true
  },
  totalPages: {
    type: Number,
    required: true
  }
})

defineEmits(['page-change'])
</script>

<style scoped>
.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16px;
  margin: 24px 0;
  padding: 20px;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.pagination-button {
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

.pagination-button:hover:not(:disabled) {
  background: #2196F3;
  color: white;
  border-color: #2196F3;
  transform: translateY(-2px);
}

.pagination-button:disabled {
  opacity: 0.4;
  cursor: not-allowed;
  background: #f5f5f5;
}

.page-info {
  font-size: 15px;
  font-weight: 500;
  color: #333;
  min-width: 150px;
  text-align: center;
}

@media (max-width: 768px) {
  .pagination {
    flex-direction: column;
    gap: 12px;
  }

  .pagination-button {
    width: 100%;
  }
}
</style>