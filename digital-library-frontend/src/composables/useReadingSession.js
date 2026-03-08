import { ref } from "vue";
import { readingApi } from "@/api/reading";

/**
 * Composable для работы с сессиями чтения
 */
export function useReadingSession() {
  const currentSession = ref(null);
  const activeSessions = ref([]);
  const loading = ref(false);
  const startTime = ref(null);

  /**
   * Начать сессию чтения
   */
  async function startSession(bookId) {
    if (!bookId) return null;

    loading.value = true;
    try {
      const session = await readingApi.startSession(bookId);
      currentSession.value = session;
      startTime.value = Date.now();
      return session;
    } catch (error) {
      console.error("Ошибка начала сессии чтения:", error);
      return null;
    } finally {
      loading.value = false;
    }
  }

  /**
   * Завершить сессию чтения
   */
  async function endSession(lastPosition = null) {
    if (!currentSession.value) {
      console.warn("No active session to end");
      return false;
    }

    const sessionId = currentSession.value.sessionId;
    const duration = startTime.value
      ? Math.floor((Date.now() - startTime.value) / 1000)
      : 0;

    loading.value = true;
    try {
      const data = {
        durationSeconds: duration,
      };

      if (lastPosition) {
        data.lastPosition = lastPosition;
      }

      await readingApi.endSession(sessionId, data);
      currentSession.value = null;
      startTime.value = null;
      return true;
    } catch (error) {
      console.error("Ошибка завершения сессии чтения:", error);
      return false;
    } finally {
      loading.value = false;
    }
  }

  /**
   * Загрузить активные сессии
   */
  async function loadActiveSessions() {
    loading.value = true;
    try {
      const sessions = await readingApi.getActiveSessions();
      activeSessions.value = sessions || [];
      return sessions;
    } catch (error) {
      console.error("Ошибка загрузки активных сессий:", error);
      activeSessions.value = [];
      return [];
    } finally {
      loading.value = false;
    }
  }

  /**
   * Автоматически завершить сессию при закрытии страницы
   */
  function setupAutoEndOnUnload(lastPosition = null) {
    const handler = () => {
      if (!currentSession.value) return;

      const sessionId = currentSession.value.sessionId;
      const duration = startTime.value
        ? Math.floor((Date.now() - startTime.value) / 1000)
        : 0;

      const data = JSON.stringify({
        durationSeconds: duration,
        lastPosition: lastPosition,
      });

      // Используем sendBeacon для гарантированной отправки при закрытии
      const jwt = getCookie("jwt");
      const url = `/api/readings/${sessionId}/end`;

      // sendBeacon не поддерживает headers, используем Blob с типом
      const blob = new Blob([data], { type: "application/json" });

      // Fallback: используем синхронный XHR если sendBeacon не сработает
      try {
        navigator.sendBeacon(url, blob);
      } catch (e) {
        console.warn("sendBeacon failed, using sync XHR");
        const xhr = new XMLHttpRequest();
        xhr.open("PUT", url, false); // false = синхронный
        xhr.setRequestHeader("Content-Type", "application/json");
        xhr.setRequestHeader("Authorization", `Bearer ${jwt}`);
        xhr.send(data);
      }
    };

    window.addEventListener("beforeunload", handler);
    window.addEventListener("pagehide", handler);

    // Возвращаем функцию для отписки
    return () => {
      window.removeEventListener("beforeunload", handler);
      window.removeEventListener("pagehide", handler);
    };
  }

  return {
    currentSession,
    activeSessions,
    loading,
    startSession,
    endSession,
    loadActiveSessions,
    setupAutoEndOnUnload,
  };
}

// Utility function
function getCookie(name) {
  const value = `; ${document.cookie}`;
  const parts = value.split(`; ${name}=`);
  if (parts.length === 2) return parts.pop().split(";").shift();
  return null;
}
