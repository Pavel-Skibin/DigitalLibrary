import { ref } from "vue";
import { viewHistoryApi } from "@/api/viewHistory";

/**
 * Composable для работы с историей просмотров
 */
export function useViewHistory() {
  const history = ref([]);
  const loading = ref(false);
  const totalPages = ref(0);
  const totalElements = ref(0);

  /**
   * Загрузить историю просмотров
   */
  async function loadHistory(page = 0, size = 50) {
    loading.value = true;
    try {
      const response = await viewHistoryApi.getHistory(page, size);
      history.value = response.content || response;
      totalPages.value = response.totalPages || 1;
      totalElements.value = response.totalElements || history.value.length;
      return response;
    } catch (error) {
      console.error("Ошибка загрузки истории просмотров:", error);
      history.value = [];
      return null;
    } finally {
      loading.value = false;
    }
  }

  /**
   * Загрузить ID просмотренных книг
   */
  async function loadViewedBookIds() {
    try {
      const ids = await viewHistoryApi.getViewedBookIds();
      return ids || [];
    } catch (error) {
      console.error("Ошибка загрузки ID просмотренных книг:", error);
      return [];
    }
  }

  return {
    history,
    loading,
    totalPages,
    totalElements,
    loadHistory,
    loadViewedBookIds,
  };
}
