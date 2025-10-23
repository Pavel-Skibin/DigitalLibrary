<template>
  <div class="modal-overlay" @click="$emit('close')">
    <div class="modal-content auth-prompt-modal" @click.stop>
      <div class="modal-icon">🔒</div>
      <h2 class="modal-title">Требуется авторизация</h2>
      <p class="modal-message">
        Чтобы <strong>{{ actionDescription }}</strong>, необходимо войти в систему или зарегистрироваться.
      </p>

      <div class="modal-buttons">
        <button @click="$emit('close')" class="btn-secondary">
          Отмена
        </button>
        <button @click="$emit('login')" class="btn-primary">
          Войти / Регистрация
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  actionDescription: {
    type: String,
    required: true
  }
})

defineEmits(['close', 'login'])
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  animation: fadeIn 0.2s ease-in-out;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.modal-content {
  background: white;
  border-radius: 12px;
  padding: 2rem;
  max-width: 450px;
  width: 90%;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  animation: slideUp 0.3s ease-out;
}

@keyframes slideUp {
  from {
    transform: translateY(20px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

.auth-prompt-modal {
  text-align: center;
}

.modal-icon {
  font-size: 3rem;
  margin-bottom: 1rem;
}

.modal-title {
  font-size: 1.5rem;
  color: #5c4033;
  margin-bottom: 1rem;
}

.modal-message {
  font-size: 1rem;
  color: #666;
  line-height: 1.6;
  margin-bottom: 2rem;
}

.modal-message strong {
  color: #5c4033;
  font-weight: 600;
}

.modal-buttons {
  display: flex;
  gap: 1rem;
  justify-content: center;
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

.btn-primary:hover {
  background: #4a3329;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(92, 64, 51, 0.3);
}

.btn-secondary {
  background: #e8d9c7;
  color: #5c4033;
}

.btn-secondary:hover {
  background: #d9c9b7;
}

.btn-primary:active,
.btn-secondary:active {
  transform: translateY(0);
}
</style>