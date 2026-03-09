import { ref, computed } from "vue";
import {
  getPersonalRecommendations,
  getPopularBooks,
  getSimilarBooks,
  getNewReleases,
} from "@/api/recommendations";
import { getCookie } from "@/utils/cookies";

/**
 * Composable для работы с рекомендациями
 *
 * Поддерживает несколько типов рекомендаций:
 * - Персональные (ML-based)
 * - Популярные (SQL-based)
 * - Похожие (embeddings-based)
 * - Новинки (SQL-based)
 */
export function useRecommendations() {
  const personalRecommendations = ref(null);
  const popularBooks = ref([]);
  const similarBooks = ref([]);
  const newReleases = ref([]);

  const loading = ref(false);
  const error = ref(null);

  // Проверка авторизации
  const isAuthenticated = computed(() => !!getCookie("jwt"));

  /**
   * Загрузить персональные рекомендации
   */
  async function loadPersonalRecommendations(userId = null, limit = 10) {

    if (!isAuthenticated.value && !userId) {
      return;
    }

    loading.value = true;
    error.value = null;

    try {
      // Если userId не указан, берем из текущего пользователя
      let currentUserId = userId;

      if (!currentUserId) {
        // Получаем ID пользователя через API /users/me
        const jwt = getCookie("jwt");
        if (jwt) {
          const response = await fetch("/api/users/me", {
            headers: { Authorization: `Bearer ${jwt}` },
          });
          if (response.ok) {
            const userData = await response.json();
            currentUserId = userData.id || userData.userId;
          }
        }
      }


      if (!currentUserId) {
        error.value = "Не удалось определить пользователя";
        return;
      }

      const response = await getPersonalRecommendations(currentUserId, limit);
      personalRecommendations.value = response;
    } catch (err) {
      error.value = err.message;
    } finally {
      loading.value = false;
    }
  }

  /**
   * Загрузить популярные книги
   */
  async function loadPopularBooks(limit = 10, genre = null) {
    loading.value = true;
    error.value = null;

    try {
      const books = await getPopularBooks(limit, genre);
      popularBooks.value = books;
    } catch (err) {
      error.value = err.message;
    } finally {
      loading.value = false;
    }
  }

  /**
   * Загрузить похожие книги
   */
  async function loadSimilarBooks(bookId, limit = 10) {
    loading.value = true;
    error.value = null;

    try {
      const books = await getSimilarBooks(bookId, limit);
      similarBooks.value = books;
    } catch (err) {
      error.value = err.message;
    } finally {
      loading.value = false;
    }
  }

  /**
   * Загрузить новинки
   */
  async function loadNewReleases(limit = 10, minYear = 2024) {
    loading.value = true;
    error.value = null;

    try {
      const books = await getNewReleases(limit, minYear);
      newReleases.value = books;
    } catch (err) {
      error.value = err.message;
    } finally {
      loading.value = false;
    }
  }

  /**
   * Получить ID текущего пользователя из локального хранилища
   */
  function getCurrentUserId() {
    try {
      // Проверяем cookie JWT
      const jwt = getCookie("jwt");

      // Проверяем localStorage
      const userData = localStorage.getItem("user");

      if (userData) {
        const user = JSON.parse(userData);
        const userId = user.id || user.userId;
        return userId;
      }
    } catch (err) {
    }
    return null;
  }

  return {
    // State
    personalRecommendations,
    popularBooks,
    similarBooks,
    newReleases,
    loading,
    error,
    isAuthenticated,

    // Actions
    loadPersonalRecommendations,
    loadPopularBooks,
    loadSimilarBooks,
    loadNewReleases,
  };
}
