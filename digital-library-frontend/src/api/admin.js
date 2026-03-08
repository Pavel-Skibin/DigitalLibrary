import { getCookie } from "@/utils/cookies";

const BASE = "/api/admin";

function authHeaders() {
  return {
    "Content-Type": "application/json",
    Authorization: `Bearer ${getCookie("jwt")}`,
  };
}

/** Получить статус читалки (публичный endpoint, без авторизации) */
export async function getReadingStatus() {
  const res = await fetch(`${BASE}/reading-status`);
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  return res.json(); // { readingEnabled: true/false }
}

/** Переключить читалку вкл/выкл (только ADMIN) */
export async function toggleReading() {
  const res = await fetch(`${BASE}/reading-toggle`, {
    method: "POST",
    headers: authHeaders(),
  });
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  return res.json(); // { readingEnabled: true/false, message: "..." }
}
