<template>
  <div class="tab-panel settings-panel">
    <h2>⚙️ Настройки системы</h2>

    <div class="settings-card">
      <div class="setting-row">
        <div class="setting-info">
          <span class="setting-icon">📖</span>
          <div class="setting-text">
            <h3>Режим чтения книг</h3>
            <p class="setting-desc">
              Если выключено — пользователи видят только метаданные книг
              (описание, авторов, оценки). Открыть книгу в читалке невозможно.
            </p>
          </div>
        </div>
        <div class="setting-control">
          <div
            class="toggle-switch"
            :class="{ active: readingEnabled, loading: isLoading }"
            @click="toggle"
            :title="
              readingEnabled
                ? 'Нажмите чтобы отключить'
                : 'Нажмите чтобы включить'
            "
          >
            <div class="toggle-thumb"></div>
          </div>
          <span class="toggle-label" :class="{ enabled: readingEnabled }">
            {{ readingEnabled ? "✅ Включено" : "🔴 Отключено" }}
          </span>
        </div>
      </div>

      <div
        v-if="statusMessage"
        class="status-message"
        :class="{ success: !error, error: error }"
      >
        {{ statusMessage }}
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { getReadingStatus, toggleReading } from "@/api/admin";

const readingEnabled = ref(false);
const isLoading = ref(false);
const statusMessage = ref("");
const error = ref(false);

onMounted(async () => {
  try {
    const data = await getReadingStatus();
    readingEnabled.value = data.readingEnabled;
  } catch (e) {
    statusMessage.value = "Не удалось загрузить статус";
    error.value = true;
  }
});

async function toggle() {
  if (isLoading.value) return;
  isLoading.value = true;
  statusMessage.value = "";
  error.value = false;
  try {
    const data = await toggleReading();
    readingEnabled.value = data.readingEnabled;
    statusMessage.value = data.message;
  } catch (e) {
    statusMessage.value = "Ошибка: " + e.message;
    error.value = true;
  } finally {
    isLoading.value = false;
  }
}
</script>

<style scoped>
.settings-panel h2 {
  margin-bottom: 24px;
  color: var(--text-primary, #222);
}

.settings-card {
  background: var(--bg-secondary, #f8f9fa);
  border: 1px solid var(--border-color, #ddd);
  border-radius: 12px;
  padding: 24px;
  max-width: 700px;
}

.setting-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
}

.setting-info {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  flex: 1;
}

.setting-icon {
  font-size: 2rem;
  line-height: 1;
}

.setting-text h3 {
  margin: 0 0 6px;
  font-size: 1.1rem;
  color: var(--text-primary, #222);
}

.setting-desc {
  margin: 0;
  font-size: 0.88rem;
  color: var(--text-secondary, #666);
  line-height: 1.5;
}

.setting-control {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.toggle-switch {
  width: 64px;
  height: 34px;
  border-radius: 17px;
  background: #ccc;
  position: relative;
  cursor: pointer;
  transition: background 0.3s;
}

.toggle-switch.active {
  background: #4caf50;
}

.toggle-switch.loading {
  opacity: 0.6;
  cursor: not-allowed;
}

.toggle-thumb {
  position: absolute;
  top: 3px;
  left: 3px;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: white;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
  transition: transform 0.3s;
}

.toggle-switch.active .toggle-thumb {
  transform: translateX(30px);
}

.toggle-label {
  font-size: 0.85rem;
  font-weight: 600;
  color: #999;
  white-space: nowrap;
}

.toggle-label.enabled {
  color: #4caf50;
}

.status-message {
  margin-top: 16px;
  padding: 10px 14px;
  border-radius: 8px;
  font-size: 0.9rem;
}

.status-message.success {
  background: #e8f5e9;
  color: #2e7d32;
  border: 1px solid #a5d6a7;
}

.status-message.error {
  background: #ffebee;
  color: #c62828;
  border: 1px solid #ef9a9a;
}
</style>
