<template>
  <div class="modal-overlay" @click="$emit('close')">
    <div class="modal-content vectorize-modal" @click.stop>
      <h2 class="modal-title">Векторизация книги</h2>
      <p class="book-name">{{ book.title }}</p>

      <!-- Текущий статус в Qdrant -->
      <div class="current-status">
        <span v-if="statusLoading" class="status-loading"
          >Проверка статуса...</span
        >
        <template v-else-if="currentStatus">
          <span
            class="status-badge"
            :class="currentStatus.recommendations_ok ? 'ok' : 'missing'"
          >
            Рекомендации:
            {{ currentStatus.recommendations_ok ? "✓ есть" : "✗ нет" }}
          </span>
          <span
            class="status-badge"
            :class="currentStatus.rag_ok ? 'ok' : 'missing'"
          >
            RAG:
            {{
              currentStatus.rag_ok
                ? `✓ ${currentStatus.rag_chunks_count} чанков`
                : "✗ нет"
            }}
          </span>
        </template>
        <span v-else class="status-loading">Статус недоступен</span>
      </div>

      <!-- Выбор режима -->
      <div v-if="!taskId" class="mode-selector">
        <label
          class="mode-option"
          :class="{ selected: mode === 'recommendations' }"
        >
          <input v-model="mode" type="radio" value="recommendations" />
          <div class="mode-info">
            <strong>Рекомендации</strong>
            <small
              >RoSBERTa — эмбеддинг метаданных книги для системы
              рекомендаций</small
            >
          </div>
        </label>

        <label class="mode-option" :class="{ selected: mode === 'rag' }">
          <input v-model="mode" type="radio" value="rag" />
          <div class="mode-info">
            <strong>RAG (Q&amp;A)</strong>
            <small
              >USER-bge-m3 — парсинг FB2, чанкинг и индексация для ответов на
              вопросы</small
            >
          </div>
        </label>

        <label class="mode-option" :class="{ selected: mode === 'both' }">
          <input v-model="mode" type="radio" value="both" />
          <div class="mode-info">
            <strong>Оба</strong>
            <small>Рекомендации + RAG последовательно</small>
          </div>
        </label>

        <label class="checkbox-option">
          <input v-model="force" type="checkbox" />
          Перезаписать существующие данные (force)
        </label>
      </div>

      <!-- Прогресс -->
      <div v-if="taskId" class="progress-section">
        <div class="progress-bar-wrap">
          <div
            class="progress-bar"
            :style="{ width: progress + '%' }"
            :class="statusClass"
          ></div>
        </div>
        <p class="progress-label">{{ progressLabel }}</p>
        <p v-if="taskStatus === 'done'" class="result-message">
          {{ resultMessage }}
        </p>
        <p v-if="taskStatus === 'error'" class="error-message">
          {{ errorMessage }}
        </p>
      </div>

      <!-- Кнопки -->
      <div class="modal-buttons">
        <button @click="$emit('close')" class="btn-secondary">
          {{
            taskStatus === "done" || taskStatus === "error"
              ? "Закрыть"
              : "Отмена"
          }}
        </button>
        <button
          v-if="!taskId"
          @click="start"
          class="btn-primary"
          :disabled="starting"
        >
          {{ starting ? "Запуск..." : "Запустить" }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from "vue";
import {
  startVectorization,
  pollTaskUntilDone,
  getVectorizationStatus,
} from "@/api/aiAdmin";

const props = defineProps({
  book: { type: Object, required: true },
});
const emit = defineEmits(["close"]);

const mode = ref("both");
const force = ref(false);
const starting = ref(false);
const taskId = ref(null);
const progress = ref(0);
const taskStatus = ref("pending");
const message = ref("");
const resultMessage = ref("");
const errorMessage = ref("");

// Статус текущей векторизации книги
const currentStatus = ref(null); // null = loading, object = loaded
const statusLoading = ref(true);

const progressLabel = computed(() => {
  if (taskStatus.value === "done") return "Готово";
  if (taskStatus.value === "error") return "Ошибка";
  if (taskStatus.value === "running") return message.value || "Выполняется...";
  return "Ожидание...";
});

const statusClass = computed(() => ({
  "bar-running":
    taskStatus.value === "running" || taskStatus.value === "pending",
  "bar-done": taskStatus.value === "done",
  "bar-error": taskStatus.value === "error",
}));

async function start() {
  starting.value = true;
  try {
    const task = await startVectorization(
      props.book.id,
      mode.value,
      force.value,
    );
    taskId.value = task.task_id;
    taskStatus.value = "pending";

    await pollTaskUntilDone(task.task_id, (t) => {
      progress.value = t.progress;
      taskStatus.value = t.status;
      message.value = t.message;
      if (t.status === "done") resultMessage.value = t.message;
      if (t.status === "error") errorMessage.value = t.error || t.message;
    });
  } catch (err) {
    taskStatus.value = "error";
    const msg = err.message || "";
    // Заменяем сырой HTML 504-страницы на понятное сообщение
    errorMessage.value =
      msg.includes("<html") || msg.includes("504") || msg.includes("Gateway")
        ? "Превышено время ожидания сервера (504). Векторизация может продолжаться в фоне — проверьте статус через несколько минут."
        : msg;
  } finally {
    starting.value = false;
  }
}

onMounted(async () => {
  try {
    currentStatus.value = await getVectorizationStatus(props.book.id);
  } catch {
    currentStatus.value = null;
  } finally {
    statusLoading.value = false;
  }
});
</script>

<style scoped>
.vectorize-modal {
  max-width: 500px !important;
}

.book-name {
  color: #5c4033;
  font-weight: 600;
  margin-bottom: 1rem;
}

.current-status {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
  margin-bottom: 1.25rem;
}

.status-loading {
  color: #999;
  font-size: 0.9rem;
}

.status-badge {
  display: inline-block;
  padding: 0.3rem 0.8rem;
  border-radius: 20px;
  font-size: 0.85rem;
  font-weight: 600;
}

.status-badge.ok {
  background: #d4edda;
  color: #155724;
  border: 1px solid #c3e6cb;
}

.status-badge.missing {
  background: #f8d7da;
  color: #721c24;
  border: 1px solid #f5c6cb;
}

.mode-selector {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  margin-bottom: 1.5rem;
}

.mode-option {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  padding: 1rem;
  border: 2px solid #d0d0d0;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.mode-option.selected {
  border-color: #5c4033;
  background: #fdf6e9;
}

.mode-option input[type="radio"] {
  margin-top: 2px;
  accent-color: #5c4033;
}

.mode-info strong {
  display: block;
  color: #333;
  margin-bottom: 2px;
}

.mode-info small {
  color: #666;
  font-size: 0.85rem;
}

.checkbox-option {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: #666;
  font-size: 0.9rem;
  cursor: pointer;
  padding: 0.5rem 0;
}

.checkbox-option input {
  accent-color: #5c4033;
}

/* Прогресс-бар */
.progress-section {
  margin-bottom: 1.5rem;
}

.progress-bar-wrap {
  height: 12px;
  background: #e0e0e0;
  border-radius: 6px;
  overflow: hidden;
  margin-bottom: 0.75rem;
}

.progress-bar {
  height: 100%;
  border-radius: 6px;
  transition: width 0.4s ease;
}

.bar-running {
  background: #1a73e8;
}
.bar-done {
  background: #28a745;
}
.bar-error {
  background: #dc3545;
}

.progress-label {
  color: #555;
  font-size: 0.9rem;
  text-align: center;
}

.result-message {
  color: #28a745;
  font-weight: 600;
  text-align: center;
  margin-top: 0.5rem;
}

.error-message {
  color: #dc3545;
  font-weight: 600;
  text-align: center;
  margin-top: 0.5rem;
  font-size: 0.9rem;
}

/* Общие стили (копируются из BookModal) */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 12px;
  padding: 2rem;
  max-width: 600px;
  width: 90%;
  max-height: 90vh;
  overflow-y: auto;
}

.modal-title {
  margin-bottom: 0.5rem;
  color: #5c4033;
}

.modal-buttons {
  display: flex;
  gap: 1rem;
  justify-content: flex-end;
  margin-top: 1.5rem;
}

.btn-primary,
.btn-secondary {
  padding: 0.75rem 1.5rem;
  border-radius: 8px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  border: none;
  font-size: 1rem;
}
.btn-primary {
  background: #5c4033;
  color: white;
}
.btn-primary:hover:not(:disabled) {
  background: #4a3329;
}
.btn-primary:disabled {
  background: #999;
  cursor: not-allowed;
}
.btn-secondary {
  background: #e8d9c7;
  color: #5c4033;
}
.btn-secondary:hover {
  background: #d9c9b7;
}
</style>
