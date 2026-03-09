import { ref } from "vue";
import { favoritesApi } from "@/api/favorites";

/**
 * Composable для работы с избранными книгами (лайками)
 */
export function useFavorites() {
  const isFavorite = ref(false);
  const loading = ref(false);
  const favorites = ref([]);
  const favoriteBookIds = ref([]);

  /**
   * Проверить, в избранном ли книга
   */
  async function checkFavoriteStatus(bookId) {
    if (!bookId) return;

    loading.value = true;
    try {
      const response = await favoritesApi.checkFavoriteStatus(bookId);
      isFavorite.value = response.isFavorite;
    } catch (error) {
      isFavorite.value = false;
    } finally {
      loading.value = false;
    }
  }

  /**
   * Переключить статус избранного
   */
  async function toggleFavorite(bookId) {
    if (!bookId) return false;

    loading.value = true;
    try {
      if (isFavorite.value) {
        await favoritesApi.removeFromFavorites(bookId);
        isFavorite.value = false;
      } else {
        await favoritesApi.addToFavorites(bookId);
        isFavorite.value = true;
      }
      return true;
    } catch (error) {

      // Проверяем, не конфликт ли это (книга уже добавлена/удалена)
      if (error.message?.includes("already")) {
        // Синхронизируем состояние
        await checkFavoriteStatus(bookId);
      }

      return false;
    } finally {
      loading.value = false;
    }
  }

  /**
   * Загрузить список избранных книг
   */
  async function loadFavorites(page = 0, size = 50) {
    loading.value = true;
    try {
      const response = await favoritesApi.getFavorites(page, size);
      favorites.value = response.content || response;
      return response;
    } catch (error) {
      favorites.value = [];
      return null;
    } finally {
      loading.value = false;
    }
  }

  /**
   * Загрузить ID избранных книг
   */
  async function loadFavoriteBookIds() {
    loading.value = true;
    try {
      const ids = await favoritesApi.getFavoriteBookIds();
      favoriteBookIds.value = ids || [];
      return ids;
    } catch (error) {
      favoriteBookIds.value = [];
      return [];
    } finally {
      loading.value = false;
    }
  }

  return {
    isFavorite,
    loading,
    favorites,
    favoriteBookIds,
    checkFavoriteStatus,
    toggleFavorite,
    loadFavorites,
    loadFavoriteBookIds,
  };
}
