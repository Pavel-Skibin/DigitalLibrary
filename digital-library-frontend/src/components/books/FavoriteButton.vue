<template>
  <button
    @click="handleToggle"
    :disabled="loading"
    class="favorite-button"
    :class="{ 'is-favorite': isFavorite, 'is-loading': loading }"
    :title="isFavorite ? 'Удалить из избранного' : 'Добавить в избранное'"
  >
    <svg
      class="heart-icon"
      :class="{ filled: isFavorite }"
      viewBox="0 0 24 24"
      xmlns="http://www.w3.org/2000/svg"
    >
      <path
        d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"
      />
    </svg>
    <span class="button-text">
      {{ isFavorite ? "В избранном" : "В избранное" }}
    </span>
  </button>
</template>

<script setup>
import { ref, watch, onMounted } from "vue";
import { useFavorites } from "@/composables/useFavorites";
import { useUser } from "@/composables/useUser";
import { getCookie } from "@/utils/cookies";
import { invalidateUserCache } from "@/api/recommendations";

const props = defineProps({
  bookId: {
    type: Number,
    required: true,
  },
});

const { isFavorite, loading, checkFavoriteStatus, toggleFavorite } =
  useFavorites();

const { userId, fetchUserProfile } = useUser();

const isAuthenticated = ref(false);

onMounted(async () => {
  // Проверяем авторизацию
  const jwt = getCookie("jwt");
  isAuthenticated.value = !!jwt;

  // Загружаем профиль пользователя (включая userId)
  if (isAuthenticated.value) {
    await fetchUserProfile();
  }

  // Загружаем статус избранного если авторизован
  if (isAuthenticated.value && props.bookId) {
    await checkFavoriteStatus(props.bookId);
  }
});

watch(
  () => props.bookId,
  async (newBookId) => {
    if (newBookId && isAuthenticated.value) {
      await checkFavoriteStatus(newBookId);
    }
  },
);

async function handleToggle() {
  if (!isAuthenticated.value) {
    alert("Войдите, чтобы добавить книгу в избранное");
    return;
  }

  const success = await toggleFavorite(props.bookId);

  if (success) {
    // Инвалидация кеша рекомендаций после изменения избранного
    if (userId.value) {
      try {
        await invalidateUserCache(userId.value);
        // Уведомить другие компоненты об обновлении
        window.dispatchEvent(new CustomEvent('recommendations-invalidated'));
      } catch (error) {
      }
    } else {
    }
  } else {
    alert("Не удалось обновить избранное. Попробуйте позже.");
  }
}
</script>

<style scoped>
.favorite-button {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  border: 2px solid #e91e63;
  background-color: transparent;
  color: #e91e63;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  outline: none;
}

.favorite-button:hover:not(:disabled) {
  background-color: #fce4ec;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(233, 30, 99, 0.2);
}

.favorite-button.is-favorite {
  background-color: #e91e63;
  color: white;
}

.favorite-button.is-favorite:hover:not(:disabled) {
  background-color: #c2185b;
}

.favorite-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.favorite-button.is-loading {
  position: relative;
}

.favorite-button.is-loading::after {
  content: "";
  position: absolute;
  width: 16px;
  height: 16px;
  border: 2px solid currentColor;
  border-top-color: transparent;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
  margin-left: 8px;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.heart-icon {
  width: 20px;
  height: 20px;
  fill: none;
  stroke: currentColor;
  stroke-width: 2;
  transition: all 0.3s ease;
}

.heart-icon.filled {
  fill: currentColor;
  stroke: none;
  animation: heartPulse 0.3s ease;
}

@keyframes heartPulse {
  0% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.3);
  }
  100% {
    transform: scale(1);
  }
}

.button-text {
  line-height: 1;
}
</style>
