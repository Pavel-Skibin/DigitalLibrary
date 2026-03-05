/**
 * API-хелперы для AI-функций панели администратора:
 * - автозаполнение метаданных книги через DeepSeek
 * - запуск и отслеживание задач векторизации
 */

import { getCookie } from "@/utils/cookies";

const AI_BASE = "/api/ai";

function authHeaders() {
  return {
    "Content-Type": "application/json",
    Authorization: `Bearer ${getCookie("jwt")}`,
  };
}

// ─── Meta Enrichment ──────────────────────────────────────────────────────────

/**
 * Запрашивает у DeepSeek метаданные книги.
 * @param {string} title
 * @param {string[]} authors
 * @returns {Promise<{description, genres, tags, themes, publication_year, language, age_rating, series_name, series_number}>}
 */
export async function enrichBookMeta(title, authors = []) {
  const res = await fetch(`${AI_BASE}/meta/enrich`, {
    method: "POST",
    headers: authHeaders(),
    body: JSON.stringify({ title, authors }),
  });
  if (!res.ok) {
    const err = await res.text();
    throw new Error(err || `HTTP ${res.status}`);
  }
  return res.json();
}

// ─── Vectorization Tasks ──────────────────────────────────────────────────────

/**
 * Запускает векторизацию книги в фоне.
 * @param {number} bookId
 * @param {'recommendations'|'rag'|'both'} mode
 * @param {boolean} force  — перезаписать существующие данные
 * @returns {Promise<{task_id, status, progress, message}>}
 */
export async function startVectorization(bookId, mode = "both", force = false) {
  const res = await fetch(`${AI_BASE}/tasks/vectorize`, {
    method: "POST",
    headers: authHeaders(),
    body: JSON.stringify({ book_id: bookId, mode, force }),
  });
  if (!res.ok) {
    const err = await res.text();
    throw new Error(err || `HTTP ${res.status}`);
  }
  return res.json();
}

/**
 * Получает статус и прогресс задачи.
 * @param {string} taskId
 * @returns {Promise<{task_id, status, progress, message, result, error}>}
 */
export async function getTaskStatus(taskId) {
  const res = await fetch(`${AI_BASE}/tasks/${taskId}`, {
    headers: authHeaders(),
  });
  if (!res.ok) {
    const err = await res.text();
    throw new Error(err || `HTTP ${res.status}`);
  }
  return res.json();
}

/**
 * Проверяет, векторизована ли книга в Qdrant-коллекциях.
 * @param {number} bookId
 * @returns {Promise<{book_id, recommendations_ok, rag_ok, rag_chunks_count}>}
 */
export async function getVectorizationStatus(bookId) {
  const res = await fetch(`${AI_BASE}/tasks/vectorize/status/${bookId}`, {
    headers: authHeaders(),
  });
  if (!res.ok) {
    const err = await res.text();
    throw new Error(err || `HTTP ${res.status}`);
  }
  return res.json();
}

/**
 * Удаляет книгу из обеих Qdrant-коллекций (рекомендации + RAG).
 * Вызывается при удалении книги из каталога.
 * @param {number} bookId
 * @returns {Promise<{book_id, recommendations_deleted, rag_chunks_deleted}>}
 */
export async function deleteVectorization(bookId) {
  const res = await fetch(`${AI_BASE}/tasks/vectorize/${bookId}`, {
    method: "DELETE",
    headers: authHeaders(),
  });
  // Если книга не была векторизована — не считаем ошибкой
  if (!res.ok && res.status !== 404) {
    console.warn(
      `Не удалось очистить Qdrant для книги ${bookId}: HTTP ${res.status}`,
    );
  }
  return res.ok ? res.json() : null;
}

/**
 * Polling задачи каждые 2 секунды до завершения.
 * Вызывает onProgress(task) при каждом обновлении.
 * @param {string} taskId
 * @param {function} onProgress
 * @param {number} intervalMs
 * @returns {Promise<{task_id, status, progress, message, result, error}>}
 */
export function pollTaskUntilDone(taskId, onProgress, intervalMs = 2000) {
  return new Promise((resolve, reject) => {
    const interval = setInterval(async () => {
      try {
        const task = await getTaskStatus(taskId);
        onProgress(task);
        if (task.status === "done" || task.status === "error") {
          clearInterval(interval);
          resolve(task);
        }
      } catch (err) {
        clearInterval(interval);
        reject(err);
      }
    }, intervalMs);
  });
}
