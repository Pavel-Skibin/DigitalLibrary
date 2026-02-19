/**
 * API для работы с историей просмотров
 */
import { api } from "./index.js";

export const viewHistoryApi = {
  /**
   * Получить историю просмотров пользователя
   * @param {number} page - номер страницы
   * @param {number} size - размер страницы
   */
  getHistory: (page = 0, size = 50) =>
    api.get(`/users/me/history?page=${page}&size=${size}`),

  /**
   * Получить ID просмотренных книг
   */
  getViewedBookIds: () => api.get("/users/me/history/book-ids"),
};
