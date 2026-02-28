import { api } from "./index.js";

/**
 * @param {Object} params
 * @param {string} params.message
 * @param {number|null} params.userId
 * @param {number} [params.topK=5]
 * @param {string|null} [params.language=null]
 * @returns {Promise<ChatResponse>}
 */
export function sendChatMessage({
  message,
  userId = null,
  topK = 5,
  language = null,
}) {
  return api.post("/ai/chat", {
    message,
    user_id: userId,
    top_k: topK,
    language,
  });
}
