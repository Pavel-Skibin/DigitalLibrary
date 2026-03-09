import { api } from "./index.js";

/**
 * @param {Object} params
 * @param {string} params.message
 * @param {number|null} params.userId
 * @param {number} [params.topK=5]
 * @param {string|null} [params.language=null]
 * @param {string|null} [params.sessionId=null]
 * @returns {Promise<ChatResponse>}
 */
export function sendChatMessage({
  message,
  userId = null,
  topK = 5,
  language = null,
  sessionId = null,
}) {
  return api.post("/ai/chat", {
    message,
    user_id: userId,
    top_k: topK,
    language,
    ...(sessionId ? { session_id: sessionId } : {}),
  });
}

/**
 * Получить информацию о квоте запросов к AI-ассистенту для текущего пользователя.
 * @returns {Promise<{role: string, unlimited: boolean, used: number, limit: number|null, remaining: number|null}>}
 */
export function getAiQuota() {
  return api.get("/ai/chat/quota");
}
