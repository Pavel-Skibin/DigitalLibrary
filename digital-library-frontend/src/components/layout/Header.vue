<template>
  <header class="app-header">
    <div class="header-content">
      <h2 class="header-title">📚 Библиотека</h2>
      <div class="user-info">
        <span v-if="loading" class="username loading-text">Загрузка...</span>
        <span v-else-if="username" class="username">👤 {{ username }}</span>
        <button
            @click="logout"
            class="logout-button"
            :disabled="loading"
        >
          🚪 Выйти
        </button>
      </div>
    </div>
  </header>
</template>

<script setup>
import { onMounted } from 'vue'
import { useUser } from '@/composables/useUser'

const { username, loading, fetchUserProfile, logout } = useUser()

onMounted(async () => {
  await fetchUserProfile()
})
</script>

<style scoped>
.app-header {
  background-color: #f9f2e6;
  border-bottom: 1px solid #e8d9c7;
  padding: 0;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
  z-index: 100;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
  height: 60px;
  max-width: 100%;
}

.header-title {
  font-size: 1.4rem;
  color: #5c4033;
  margin: 0;
  font-weight: 600;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.username {
  font-weight: 500;
  color: #5c4033;
}

.loading-text {
  color: #999;
  font-style: italic;
}

.logout-button {
  background: none;
  border: 1px solid #e8d9c7;
  border-radius: 6px;
  padding: 6px 12px;
  color: #5c4033;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.logout-button:hover:not(:disabled) {
  background-color: #e8d9c7;
  transform: translateY(-1px);
}

.logout-button:active:not(:disabled) {
  transform: translateY(0);
}

.logout-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>