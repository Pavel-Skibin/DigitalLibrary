<template>
  <div class="library-stats">
    <h2 class="stats-title">Наша библиотека в цифрах</h2>
    <div class="stats-grid">
      <div class="stat-card">

        <div class="stat-value">{{ loading ? '...' : stats.totalBooks }}</div>
        <div class="stat-label">Книг</div>
      </div>
      <div class="stat-card">

        <div class="stat-value">{{ loading ? '...' : stats.totalAuthors }}</div>
        <div class="stat-label">Авторов</div>
      </div>
      <div class="stat-card">

        <div class="stat-value">{{ loading ? '...' : stats.totalGenres }}</div>
        <div class="stat-label">Жанров</div>
      </div>
      <div class="stat-card">

        <div class="stat-value">{{ loading ? '...' : formatRating(stats.globalAverageRating) }}</div>
        <div class="stat-label">Средний рейтинг</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { formatRating } from '@/utils/formatters'

const stats = ref({
  totalBooks: 0,
  totalAuthors: 0,
  totalGenres: 0,
  globalAverageRating: 0
})

const loading = ref(true)

async function loadStats() {
  try {
    const response = await fetch('http://localhost:8080/api/statistics/system')
    if (response.ok) {
      stats.value = await response.json()
    }
  } catch (error) {
    console.error('Ошибка загрузки статистики:', error)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadStats()
})
</script>

<style scoped>
.library-stats {
  margin-top: 60px;
  padding: 40px;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  border-radius: 16px;
  animation: fadeIn 1s ease-out 0.8s both;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

.stats-title {
  text-align: center;
  font-size: 28px;
  font-weight: 700;
  color: #333;
  margin: 0 0 32px 0;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 24px;
}

.stat-card {
  background: white;
  padding: 24px;
  border-radius: 12px;
  text-align: center;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  transition: transform 0.3s, box-shadow 0.3s;
}

.stat-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
}

.stat-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

.stat-value {
  font-size: 36px;
  font-weight: 700;
  color: #667eea;
  margin-bottom: 8px;
}

.stat-label {
  font-size: 14px;
  color: #666;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

@media (max-width: 768px) {
  .library-stats {
    padding: 30px 20px;
    margin-top: 40px;
  }

  .stats-title {
    font-size: 24px;
  }

  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 16px;
  }

  .stat-value {
    font-size: 28px;
  }
}
</style>