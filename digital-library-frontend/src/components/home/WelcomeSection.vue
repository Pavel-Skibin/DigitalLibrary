<template>
  <div class="welcome-section">
    <div class="welcome-text">
      <h1 class="page-title">
        Добро пожаловать<br>
      </h1>
      <p class="welcome-description">
        Исследуйте наш обширный каталог книг,<br>
        оставляйте отзывы и оценки, создавайте<br>
        закладки для последующего чтения.<br>
        <strong>Приятного времяпрепровождения!</strong>
      </p>
      <div class="quick-actions">
        <router-link to="/books" class="action-button primary">
          📚 Смотреть книги
        </router-link>
        <router-link to="/authors" class="action-button secondary">
          👤 Список авторов
        </router-link>
      </div>
    </div>
    <div class="welcome-image">
      <img
          src="@/assets/images/welcome-illustration.png"
          alt="Иллюстрация библиотеки"
          class="illustration"
      />
    </div>
  </div>
</template>

<script setup>
// Компонент не требует логики
</script>

<style scoped>
.welcome-section {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 60px;
  min-height: calc(100vh - 200px);
  padding: 40px;
  animation: fadeInUp 0.8s ease-out;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.welcome-text {
  flex: 1;
  max-width: 600px;
}

.page-title {
  font-size: 48px;
  font-weight: 700;
  color: #333;
  margin: 0 0 24px 0;
  line-height: 1.2;
  animation: slideInLeft 0.8s ease-out 0.2s both;
}

@keyframes slideInLeft {
  from {
    opacity: 0;
    transform: translateX(-30px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

.gradient-text {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.welcome-description {
  font-size: 18px;
  line-height: 1.8;
  color: #666;
  margin: 0 0 32px 0;
  animation: slideInLeft 0.8s ease-out 0.4s both;
}

.welcome-description strong {
  color: #333;
  font-weight: 600;
}

.quick-actions {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  animation: slideInLeft 0.8s ease-out 0.6s both;
}

.action-button {
  padding: 14px 28px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 8px;
  text-decoration: none;
  transition: all 0.3s;
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.action-button.primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.action-button.primary:hover {
  background: linear-gradient(135deg, #764ba2 0%, #667eea 100%);
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.4);
}

.action-button.secondary {
  background: white;
  color: #667eea;
  border: 2px solid #667eea;
}

.action-button.secondary:hover {
  background: #667eea;
  color: white;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.welcome-image {
  flex: 1;
  max-width: 500px;
  animation: float 3s ease-in-out infinite;
}

@keyframes float {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-20px);
  }
}

.illustration {
  width: 100%;
  height: auto;
  filter: drop-shadow(0 10px 30px rgba(0, 0, 0, 0.1));
}

@media (max-width: 1024px) {
  .welcome-section {
    gap: 40px;
    padding: 30px;
  }

  .page-title {
    font-size: 36px;
  }

  .welcome-description {
    font-size: 16px;
  }
}

@media (max-width: 768px) {
  .welcome-section {
    flex-direction: column;
    text-align: center;
    gap: 40px;
    padding: 20px;
  }

  .welcome-text {
    max-width: 100%;
  }

  .page-title {
    font-size: 32px;
  }

  .quick-actions {
    justify-content: center;
  }

  .action-button {
    flex: 1;
    justify-content: center;
  }

  .welcome-image {
    max-width: 300px;
  }
}
</style>