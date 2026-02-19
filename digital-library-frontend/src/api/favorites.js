/**
 * API для работы с избранными книгами (лайками)
 */
import { api } from "./index.js";

export const favoritesApi = {
  /**
   * Получить избранные книги пользователя
   * @param {number} page - номер страницы
   * @param {number} size - размер страницы
   */
  getFavorites: (page = 0, size = 50) =>
    api.get(`/favorites?page=${page}&size=${size}`),

  /**
   * Получить ID избранных книг
   */
  getFavoriteBookIds: () => api.get("/favorites/book-ids"),

  /**
   * Добавить книгу в избранное
   * @param {number} bookId - ID книги
   */
  addToFavorites: (bookId) => api.post(`/favorites/${bookId}`),

  /**
   * Удалить из избранного
   * @param {number} bookId - ID книги
   */
  removeFromFavorites: (bookId) => api.delete(`/favorites/${bookId}`),

  /**
   * Проверить, в избранном ли книга
   * @param {number} bookId - ID книги
   */
  checkFavoriteStatus: (bookId) => api.get(`/favorites/${bookId}/status`),
};
