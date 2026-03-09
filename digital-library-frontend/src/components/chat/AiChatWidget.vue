<template>
  <!-- Floating toggle button -->
  <button
    class="chat-fab"
    :class="{ 'chat-fab--open': isOpen }"
    @click="toggleChat"
    aria-label="Открыть ассистента"
  >
    <span class="chat-fab__icon">{{ isOpen ? "✕" : "💬" }}</span>
  </button>

  <!-- Chat panel -->
  <Transition name="chat-slide">
    <div
      v-if="isOpen"
      class="chat-panel"
      role="dialog"
      aria-label="Ассистент библиотеки"
    >
      <!-- Header -->
      <div class="chat-panel__header">
        <span class="chat-panel__avatar">🤖</span>
        <div class="chat-panel__title">
          <strong>Библиотечный ассистент</strong>
          <span class="chat-panel__subtitle"
            >Спросите о книгах или получите рекомендации</span
          >
        </div>
        <span
          v-if="quota && quota.unlimited"
          class="chat-quota-badge chat-quota-badge--unlimited"
          title="Безлимитный доступ"
          >∞</span
        >
        <span
          v-else-if="quota"
          class="chat-quota-badge"
          :class="{ 'chat-quota-badge--low': quota.remaining <= 2 }"
          :title="`Использовано ${quota.used} из ${quota.limit} запросов сегодня`"
          >{{ quota.remaining }}/{{ quota.limit }}</span
        >
        <button
          class="chat-panel__close"
          @click="toggleChat"
          aria-label="Закрыть"
        >
          ✕
        </button>
      </div>

      <!-- Messages -->
      <div class="chat-panel__messages" ref="messagesRef">
        <!-- Auth gate -->
        <div v-if="!isAuthenticated" class="chat-auth-gate">
          <span class="chat-auth-gate__icon">🔒</span>
          <p class="chat-auth-gate__text">
            Войдите в аккаунт, чтобы использовать ассистента
          </p>
          <button class="chat-auth-gate__btn" @click="goToLogin">Войти</button>
        </div>

        <!-- Welcome message -->
        <div
          v-if="isAuthenticated && messages.length === 0"
          class="chat-message chat-message--bot"
        >
          <span class="chat-message__avatar">🤖</span>
          <div class="chat-message__bubble">
            Привет! Я помогу вам найти интересные книги или отвечу на вопросы по
            произведениям из нашей библиотеки. Что вас интересует?
          </div>
        </div>

        <template v-for="msg in messages" :key="msg.id">
          <!-- User message -->
          <div
            v-if="msg.role === 'user'"
            class="chat-message chat-message--user"
          >
            <div class="chat-message__bubble">{{ msg.text }}</div>
          </div>

          <!-- Bot text answer -->
          <div
            v-else-if="msg.role === 'bot'"
            class="chat-message chat-message--bot"
          >
            <span class="chat-message__avatar">🤖</span>
            <div class="chat-message__content">
              <div
                class="chat-message__bubble"
                v-html="formatAnswer(msg.text)"
              ></div>

              <!-- Sources (for book_question intent) -->
              <div
                v-if="msg.sources && msg.sources.length > 0"
                class="chat-sources"
              >
                <span class="chat-sources__label">Источники:</span>
                <div
                  v-for="src in msg.sources.slice(0, 3)"
                  :key="src.book_id + '-' + src.chunk_index"
                  class="chat-source-chip"
                  @click="goToBook(src.book_id)"
                >
                  📖 {{ src.title }}
                </div>
              </div>

              <!-- Recommendations carousel -->
              <div
                v-if="msg.recommendations && msg.recommendations.length > 0"
                class="chat-recs"
              >
                <p class="chat-recs__label">Рекомендации для вас:</p>

                <div class="chat-recs__carousel-wrapper">
                  <button
                    v-if="msg.recommendations.length > 2"
                    class="carousel-btn carousel-btn--prev"
                    @click="scrollCarousel(msg.id, -280)"
                    aria-label="Назад"
                  >
                    ‹
                  </button>

                  <div
                    class="chat-recs__carousel"
                    :ref="(el) => setCarouselRef(msg.id, el)"
                  >
                    <div
                      v-for="book in msg.recommendations"
                      :key="book.book_id"
                      class="rec-card"
                      @click="goToBook(book.book_id)"
                    >
                      <div class="rec-card__cover">
                        <img
                          v-if="book.book_id"
                          :src="`/api/books/${book.book_id}/cover`"
                          :alt="book.title"
                          @error="onImgError"
                        />
                        <div v-else class="rec-card__cover-placeholder">📖</div>
                      </div>
                      <div class="rec-card__info">
                        <p class="rec-card__title" :title="book.title">
                          {{ book.title }}
                        </p>
                        <p class="rec-card__authors">
                          {{ formatAuthors(book.authors) }}
                        </p>
                        <p v-if="book.average_rating" class="rec-card__rating">
                          ⭐ {{ book.average_rating.toFixed(1) }}
                        </p>
                        <p v-if="book.reason" class="rec-card__reason">
                          {{ book.reason }}
                        </p>
                      </div>
                    </div>
                  </div>

                  <button
                    v-if="msg.recommendations.length > 2"
                    class="carousel-btn carousel-btn--next"
                    @click="scrollCarousel(msg.id, 280)"
                    aria-label="Вперёд"
                  >
                    ›
                  </button>
                </div>
              </div>
            </div>
          </div>

          <!-- Loading indicator -->
          <div
            v-else-if="msg.role === 'loading'"
            class="chat-message chat-message--bot"
          >
            <span class="chat-message__avatar">🤖</span>
            <div class="chat-message__bubble chat-message__bubble--loading">
              <span class="dot-flashing"></span>
            </div>
          </div>

          <!-- Error message -->
          <div
            v-else-if="msg.role === 'error'"
            class="chat-message chat-message--bot"
          >
            <span class="chat-message__avatar">⚠️</span>
            <div class="chat-message__bubble chat-message__bubble--error">
              {{ msg.text }}
            </div>
          </div>
        </template>
      </div>

      <!-- Quota exhausted bar -->
      <div
        v-if="isAuthenticated && isQuotaExhausted"
        class="chat-quota-exhausted"
      >
        Дневной лимит исчерпан. Возвращайтесь завтра!
      </div>

      <!-- Input area -->
      <form class="chat-panel__input-area" @submit.prevent="sendMessage">
        <textarea
          ref="inputRef"
          v-model="inputText"
          class="chat-panel__input"
          placeholder="Спросите меня о книгах..."
          rows="1"
          :disabled="isLoading || !isAuthenticated || isQuotaExhausted"
          @keydown.enter.exact.prevent="sendMessage"
          @input="autoResize"
        ></textarea>
        <button
          type="submit"
          class="chat-panel__send"
          :disabled="
            isLoading ||
            !inputText.trim() ||
            !isAuthenticated ||
            isQuotaExhausted
          "
          aria-label="Отправить"
        >
          <span>➤</span>
        </button>
      </form>
    </div>
  </Transition>
</template>

<script setup>
import { ref, nextTick, computed } from "vue";
import { useRouter } from "vue-router";
import { sendChatMessage, getAiQuota } from "@/api/chat.js";
import { getCookie } from "@/utils/cookies.js";

const router = useRouter();

// ─── State ────────────────────────────────────────────────────────────────────
const isOpen = ref(false);
const inputText = ref("");
const messages = ref([]);
const isLoading = ref(false);
const messagesRef = ref(null);
const inputRef = ref(null);
const carouselRefs = ref({}); // { [msgId]: HTMLElement }
const sessionId = ref(null); // Храним session_id из ответа сервера
const quota = ref(null);

let msgCounter = 0;

// ─── Auth ─────────────────────────────────────────────────────────────────────
// Используем ref, а не computed — document.cookie не является реактивным,
// computed кэшировал бы false навсегда. Обновляем вручную при открытии.
const isAuthenticated = ref(!!getCookie("jwt"));
const isQuotaExhausted = computed(
  () => !!quota.value && !quota.value.unlimited && quota.value.remaining <= 0,
);

async function fetchQuota() {
  try {
    quota.value = await getAiQuota();
  } catch {
    quota.value = null;
  }
}

// ─── UI helpers ──────────────────────────────────────────────────────────────
function toggleChat() {
  isOpen.value = !isOpen.value;
  if (isOpen.value) {
    isAuthenticated.value = !!getCookie("jwt"); // перечитываем при каждом открытии
    if (isAuthenticated.value) fetchQuota();
    nextTick(() => inputRef.value?.focus());
  }
}

function autoResize(e) {
  const el = e.target;
  el.style.height = "auto";
  el.style.height = Math.min(el.scrollHeight, 120) + "px";
}

function scrollToBottom() {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight;
    }
  });
}

function setCarouselRef(id, el) {
  if (el) carouselRefs.value[id] = el;
  else delete carouselRefs.value[id];
}

function scrollCarousel(msgId, delta) {
  const el = carouselRefs.value[msgId];
  if (el) el.scrollBy({ left: delta, behavior: "smooth" });
}

function goToBook(bookId) {
  if (bookId) router.push("/books/" + bookId);
}

function goToLogin() {
  router.push("/login");
  toggleChat();
}

function onImgError(e) {
  e.target.style.display = "none";
  const ph = document.createElement("div");
  ph.className = "rec-card__cover-placeholder";
  ph.textContent = "📖";
  e.target.parentElement.appendChild(ph);
}

function formatAuthors(authors) {
  if (!authors || authors.length === 0) return "";
  if (typeof authors[0] === "string") return authors.join(", ");
  return authors.map((a) => a.name || a.full_name || String(a)).join(", ");
}

function formatAnswer(text) {
  if (!text) return "";
  return text
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/\n/g, "<br>")
    .replace(/\*\*(.*?)\*\*/g, "<strong>$1</strong>")
    .replace(/\*(.*?)\*/g, "<em>$1</em>");
}

// ─── Send message ─────────────────────────────────────────────────────────────
async function sendMessage() {
  const text = inputText.value.trim();
  if (
    !text ||
    isLoading.value ||
    !isAuthenticated.value ||
    isQuotaExhausted.value
  )
    return;

  // Add user message
  messages.value.push({ id: ++msgCounter, role: "user", text });
  inputText.value = "";
  if (inputRef.value) {
    inputRef.value.style.height = "auto";
  }
  scrollToBottom();

  // Add loading placeholder
  const loadingId = ++msgCounter;
  messages.value.push({ id: loadingId, role: "loading" });
  isLoading.value = true;
  scrollToBottom();

  try {
    const response = await sendChatMessage({
      message: text,
      topK: 6,
      sessionId: sessionId.value,
    });

    // Сохраняем session_id из ответа для последующих запросов
    if (response.session_id) sessionId.value = response.session_id;

    // Remove loading placeholder
    const idx = messages.value.findIndex((m) => m.id === loadingId);
    if (idx !== -1) messages.value.splice(idx, 1);

    // Обновляем локальный счётчик квоты
    if (quota.value && !quota.value.unlimited) {
      quota.value.remaining = Math.max(0, quota.value.remaining - 1);
      quota.value.used++;
    }

    // Add bot response
    messages.value.push({
      id: ++msgCounter,
      role: "bot",
      text: response.answer || "",
      sources: response.sources || [],
      recommendations: response.recommendations || [],
      intent: response.intent,
    });
  } catch (err) {
    const idx = messages.value.findIndex((m) => m.id === loadingId);
    if (idx !== -1) messages.value.splice(idx, 1);

    let errorText = "Не удалось получить ответ. Попробуйте ещё раз.";
    if (err.status === 401) {
      errorText = "Сессия истекла. Пожалуйста, войдите заново.";
    } else if (err.status === 429) {
      errorText = "Дневной лимит запросов исчерпан. Возвращайтесь завтра!";
      if (quota.value) quota.value.remaining = 0;
    }
    messages.value.push({
      id: ++msgCounter,
      role: "error",
      text: errorText,
    });
  } finally {
    isLoading.value = false;
    scrollToBottom();
    nextTick(() => inputRef.value?.focus());
  }
}
</script>

<style scoped>
/* ── FAB (floating button) ─────────────────────────────────────────────────── */
.chat-fab {
  position: fixed;
  bottom: 28px;
  right: 28px;
  z-index: 1000;
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: #5c4033;
  color: #fdf6e9;
  border: none;
  box-shadow: 0 4px 16px rgba(92, 64, 51, 0.45);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition:
    transform 0.2s,
    background 0.2s;
}
.chat-fab:hover {
  background: #7a5548;
  transform: scale(1.08);
}
.chat-fab--open {
  background: #a1887f;
}
.chat-fab__icon {
  font-size: 22px;
  line-height: 1;
}

/* ── Panel ─────────────────────────────────────────────────────────────────── */
.chat-panel {
  position: fixed;
  bottom: 96px;
  right: 28px;
  z-index: 999;
  width: 400px;
  max-width: calc(100vw - 48px);
  height: 580px;
  max-height: calc(100vh - 120px);
  background: #fdf6e9;
  border-radius: 16px;
  box-shadow: 0 8px 40px rgba(92, 64, 51, 0.25);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid #d7c4ae;
}

/* ── Transitions ───────────────────────────────────────────────────────────── */
.chat-slide-enter-active,
.chat-slide-leave-active {
  transition:
    opacity 0.2s,
    transform 0.25s;
}
.chat-slide-enter-from,
.chat-slide-leave-to {
  opacity: 0;
  transform: translateY(20px) scale(0.97);
}

/* ── Header ────────────────────────────────────────────────────────────────── */
.chat-panel__header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 16px;
  background: #5c4033;
  color: #fdf6e9;
  flex-shrink: 0;
}
.chat-panel__avatar {
  font-size: 24px;
}
.chat-panel__title {
  flex: 1;
  display: flex;
  flex-direction: column;
}
.chat-panel__title strong {
  font-size: 14px;
}
.chat-panel__subtitle {
  font-size: 11px;
  opacity: 0.75;
}
.chat-panel__close {
  background: none;
  border: none;
  color: #fdf6e9;
  font-size: 16px;
  cursor: pointer;
  padding: 4px;
  opacity: 0.8;
  line-height: 1;
}
.chat-panel__close:hover {
  opacity: 1;
}

/* ── Messages ──────────────────────────────────────────────────────────────── */
.chat-panel__messages {
  flex: 1;
  overflow-y: auto;
  padding: 14px 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  scroll-behavior: smooth;
}
.chat-panel__messages::-webkit-scrollbar {
  width: 4px;
}
.chat-panel__messages::-webkit-scrollbar-thumb {
  background: #c9a97a;
  border-radius: 4px;
}

/* ── Message bubbles ───────────────────────────────────────────────────────── */
.chat-message {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  max-width: 100%;
}
.chat-message--user {
  flex-direction: row-reverse;
}
.chat-message__avatar {
  font-size: 20px;
  flex-shrink: 0;
  line-height: 1.4;
}
.chat-message__content {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-width: 90%;
}
.chat-message__bubble {
  padding: 10px 14px;
  border-radius: 14px;
  font-size: 13.5px;
  line-height: 1.55;
  word-break: break-word;
}
.chat-message--user .chat-message__bubble {
  background: #5c4033;
  color: #fdf6e9;
  border-bottom-right-radius: 4px;
  max-width: 85%;
}
.chat-message--bot .chat-message__bubble {
  background: #fff8ef;
  color: #3e2a1e;
  border-bottom-left-radius: 4px;
  border: 1px solid #e8d9c4;
  max-width: 100%;
}
.chat-message__bubble--error {
  background: #fff0ee !important;
  border-color: #f4846a !important;
  color: #c62828 !important;
}

/* ── Loading dots ──────────────────────────────────────────────────────────── */
.chat-message__bubble--loading {
  padding: 14px 18px;
  min-width: 56px;
}
.dot-flashing {
  display: inline-block;
  position: relative;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #a0856d;
  animation: dot-flashing 1s infinite linear alternate;
  animation-delay: 0.5s;
}
.dot-flashing::before,
.dot-flashing::after {
  content: "";
  display: inline-block;
  position: absolute;
  top: 0;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #a0856d;
  animation: dot-flashing 1s infinite alternate;
}
.dot-flashing::before {
  left: -14px;
  animation-delay: 0s;
}
.dot-flashing::after {
  left: 14px;
  animation-delay: 1s;
}
@keyframes dot-flashing {
  0% {
    background: #a0856d;
  }
  100% {
    background: #e8d9c4;
  }
}

/* ── Sources ───────────────────────────────────────────────────────────────── */
.chat-sources {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
}
.chat-sources__label {
  font-size: 11px;
  color: #8d6e56;
  white-space: nowrap;
}
.chat-source-chip {
  font-size: 11px;
  padding: 3px 8px;
  background: #f3e8d8;
  border: 1px solid #cbb99a;
  border-radius: 20px;
  cursor: pointer;
  color: #5c4033;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 160px;
  transition: background 0.15s;
}
.chat-source-chip:hover {
  background: #e8d5be;
}

/* ── Recommendations carousel ──────────────────────────────────────────────── */
.chat-recs {
  width: 100%;
}
.chat-recs__label {
  font-size: 12px;
  color: #7a5548;
  margin-bottom: 8px;
  font-weight: 600;
}
.chat-recs__carousel-wrapper {
  position: relative;
  display: flex;
  align-items: center;
  gap: 4px;
}
.chat-recs__carousel {
  display: flex;
  gap: 10px;
  overflow-x: auto;
  scroll-snap-type: x mandatory;
  padding-bottom: 6px;
  flex: 1;
  -ms-overflow-style: none;
  scrollbar-width: none;
}
.chat-recs__carousel::-webkit-scrollbar {
  display: none;
}

/* ── Rec card ──────────────────────────────────────────────────────────────── */
.rec-card {
  flex-shrink: 0;
  width: 120px;
  background: #fff8ef;
  border: 1px solid #ddd0bc;
  border-radius: 10px;
  overflow: hidden;
  cursor: pointer;
  transition:
    transform 0.15s,
    box-shadow 0.15s;
  scroll-snap-align: start;
  display: flex;
  flex-direction: column;
}
.rec-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 6px 18px rgba(92, 64, 51, 0.15);
}
.rec-card__cover {
  width: 100%;
  height: 100px;
  overflow: hidden;
  background: #f0e6d6;
  display: flex;
  align-items: center;
  justify-content: center;
}
.rec-card__cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.rec-card__cover-placeholder {
  font-size: 36px;
  color: #c9a97a;
}
.rec-card__info {
  padding: 7px 8px;
  display: flex;
  flex-direction: column;
  gap: 3px;
}
.rec-card__title {
  font-size: 11.5px;
  font-weight: 600;
  color: #3e2a1e;
  line-height: 1.3;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.rec-card__authors {
  font-size: 10.5px;
  color: #8d6e56;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.rec-card__rating {
  font-size: 10.5px;
  color: #7a5548;
}
.rec-card__reason {
  font-size: 10px;
  color: #a0856d;
  line-height: 1.3;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* ── Carousel buttons ──────────────────────────────────────────────────────── */
.carousel-btn {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: 1px solid #c9a97a;
  background: #fdf6e9;
  color: #5c4033;
  font-size: 18px;
  line-height: 1;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.15s;
}
.carousel-btn:hover {
  background: #f0e6d6;
}

/* ── Input area ────────────────────────────────────────────────────────────── */
.chat-panel__input-area {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  padding: 10px 12px;
  border-top: 1px solid #e0cdb4;
  background: #fdf6e9;
  flex-shrink: 0;
}
.chat-panel__input {
  flex: 1;
  resize: none;
  border: 1px solid #c9a97a;
  border-radius: 10px;
  padding: 8px 12px;
  font-size: 13.5px;
  color: #3e2a1e;
  background: #fff8ef;
  outline: none;
  font-family: inherit;
  line-height: 1.5;
  overflow-y: auto;
  max-height: 120px;
  transition: border-color 0.15s;
}
.chat-panel__input:focus {
  border-color: #5c4033;
}
.chat-panel__input:disabled {
  opacity: 0.6;
}
.chat-panel__send {
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #5c4033;
  color: #fdf6e9;
  border: none;
  font-size: 16px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.15s;
}
.chat-panel__send:hover:not(:disabled) {
  background: #7a5548;
}
.chat-panel__send:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

/* ── Quota badge ─────────────────────────────────────────────────────────────────────── */
.chat-quota-badge {
  font-size: 11px;
  background: rgba(255, 255, 255, 0.2);
  color: #fdf6e9;
  padding: 2px 8px;
  border-radius: 10px;
  font-weight: 600;
  flex-shrink: 0;
  white-space: nowrap;
}
.chat-quota-badge--unlimited {
  font-size: 16px;
  letter-spacing: -0.5px;
}
.chat-quota-badge--low {
  background: rgba(244, 67, 54, 0.35);
}

/* ── Auth gate ─────────────────────────────────────────────────────────────── */
.chat-auth-gate {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 14px;
  padding: 32px 24px;
  text-align: center;
}
.chat-auth-gate__icon {
  font-size: 44px;
}
.chat-auth-gate__text {
  font-size: 14px;
  color: #5c4033;
  line-height: 1.55;
}
.chat-auth-gate__btn {
  padding: 9px 28px;
  background: #5c4033;
  color: #fdf6e9;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.15s;
}
.chat-auth-gate__btn:hover {
  background: #7a5548;
}

/* ── Quota exhausted bar ─────────────────────────────────────────────── */
.chat-quota-exhausted {
  text-align: center;
  font-size: 12.5px;
  color: #c62828;
  background: #fff0ee;
  border-top: 1px solid #f4846a;
  padding: 8px 16px;
  flex-shrink: 0;
}
</style>
