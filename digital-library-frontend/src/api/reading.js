/**
 * API для работы с сессиями чтения
 */
import { api } from "./index.js";

export const readingApi = {
  /**
   * Начать сессию чтения
   * @param {number} bookId - ID книги
   */
  startSession: (bookId) => api.post("/readings/start", { bookId }),

  /**
   * Завершить сессию чтения
   * @param {number} sessionId - ID сессии
   * @param {Object} data - данные сессии
   * @param {number} data.durationSeconds - длительность в секундах
   * @param {string} [data.lastPosition] - последняя позиция (CFI)
   */
  endSession: (sessionId, data) => api.put(`/readings/${sessionId}/end`, data),

  /**
   * Получить активные сессии чтения
   */
  getActiveSessions: () => api.get("/readings/active"),

  /**
   * Получить историю сессий чтения
   * @param {number} page - номер страницы
   * @param {number} size - размер страницы
   */
  getReadingHistory: (page = 0, size = 20) =>
    api.get(`/readings/history?page=${page}&size=${size}`),
};
