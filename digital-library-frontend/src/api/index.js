import { getCookie } from "@/utils/cookies";

const API_BASE_URL = "/api";

/**
 * Базовая функция для выполнения запросов
 */
async function request(endpoint, options = {}) {
  const jwt = getCookie("jwt");

  const config = {
    credentials: "include", // Важно! Для отправки/получения cookies
    headers: {
      Authorization: `Bearer ${jwt}`,
      "Content-Type": "application/json",
      ...options.headers,
    },
    ...options,
  };

  const response = await fetch(`${API_BASE_URL}${endpoint}`, config);

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(errorText || `HTTP ${response.status}`);
  }

  // Если ответ пустой (например, при DELETE), возвращаем null
  const contentType = response.headers.get("content-type");
  if (!contentType || !contentType.includes("application/json")) {
    return null;
  }

  return response.json();
}

export const api = {
  get: (endpoint) => request(endpoint),
  post: (endpoint, data) =>
    request(endpoint, {
      method: "POST",
      body: JSON.stringify(data),
    }),
  put: (endpoint, data) =>
    request(endpoint, {
      method: "PUT",
      body: JSON.stringify(data),
    }),
  delete: (endpoint) => request(endpoint, { method: "DELETE" }),
};
