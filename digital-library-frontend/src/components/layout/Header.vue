<template>
  <header class="app-header">
    <div class="header-content">
      <h2 class="header-title">📚 Библиотека</h2>
      <div class="user-info">
        <!-- Для гостей -->
        <template v-if="!isAuthenticated">
          <span class="username guest-label">👤 Гость</span>
          <button @click="goToLogin" class="login-button">🔑 Войти</button>
        </template>

        <!-- Для авторизованных пользователей -->
        <template v-else>
          <span v-if="loading" class="username loading-text">Загрузка...</span>
          <span v-else-if="username" class="username">👤 {{ username }}</span>
          <button @click="goToProfile" class="profile-button">
            📂 Мой кабинет
          </button>
          <button @click="logout" class="logout-button" :disabled="loading">
            🚪 Выйти
          </button>
        </template>
      </div>
    </div>
  </header>
</template>

<script setup>
import { onMounted, computed } from "vue";
import { useRouter } from "vue-router";
import { useUser } from "@/composables/useUser";
import { getCookie } from "@/utils/cookies";

const router = useRouter();
const { username, loading, fetchUserProfile, logout } = useUser();

// Проверка авторизации
const isAuthenticated = computed(() => {
  return !!getCookie("jwt");
});

const goToLogin = () => {
  router.push("/login");
};

const goToProfile = () => {
  router.push("/profile");
};

onMounted(async () => {
  // Загружаем профиль только если есть токен
  if (isAuthenticated.value) {
    await fetchUserProfile();
  }
});
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

.guest-label {
  color: #999;
  font-style: italic;
}

.loading-text {
  color: #999;
  font-style: italic;
}

.login-button,
.logout-button,
.profile-button {
  background: none;
  border: 1px solid #e8d9c7;
  border-radius: 6px;
  padding: 6px 12px;
  color: #5c4033;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.login-button {
  background-color: #e8d9c7;
}

.login-button:hover {
  background-color: #d9c9b7;
  transform: translateY(-1px);
}

.profile-button {
  background-color: #e8f4f8;
  border-color: #c8dde8;
}

.profile-button:hover {
  background-color: #d8e8f0;
  transform: translateY(-1px);
}

.logout-button:hover:not(:disabled) {
  background-color: #e8d9c7;
  transform: translateY(-1px);
}

.logout-button:active:not(:disabled),
.login-button:active,
.profile-button:active {
  transform: translateY(0);
}

.logout-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
