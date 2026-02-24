import { api } from "./index";

/**
 * API для работы с рекомендациями (AI Service через API Gateway)
 *
 * Архитектура:
 * Frontend → API Gateway (8080) → AI Service (8085)
 */

/**
 * Получить персональные рекомендации для пользователя
 * Использует ML-модель (embeddings + collaborative filtering)
 */
export async function getPersonalRecommendations(
  userId,
  limit = 10,
  options = {},
) {
  const params = new URLSearchParams({
    user_id: userId,
    limit,
    ...options,
  });

  return api.get(`/recommendations?${params}`);
}

/**
 * Получить популярные книги
 * SQL-based рекомендации по рейтингу
 */
export async function getPopularBooks(limit = 10, genre = null) {
  const params = new URLSearchParams({ limit });
  if (genre) params.append("genre", genre);

  return api.get(`/recommendations/popular?${params}`);
}

/**
 * Получить книги, похожие на конкретную книгу
 * Embeddings-based поиск похожих книг
 */
export async function getSimilarBooks(bookId, limit = 10) {
  const params = new URLSearchParams({ limit });
  return api.get(`/recommendations/similar/${bookId}?${params}`);
}

/**
 * Получить новинки
 * Недавно добавленные книги
 */
export async function getNewReleases(limit = 10, minYear = 2024) {
  const params = new URLSearchParams({ limit, min_year: minYear });
  return api.get(`/recommendations/new?${params}`);
}

/**
 * Инвалидировать кэш рекомендаций пользователя
 * Вызывать после оценки книги, добавления в избранное и т.д.
 */
export async function invalidateUserCache(userId) {
  return api.post(`/recommendations/invalidate-cache/${userId}`);
}
