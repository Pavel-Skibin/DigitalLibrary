<template>
  <div class="tab-panel statistics-panel">
    <h2>📊 Статистика библиотеки</h2>

    <!-- Общая статистика системы -->
    <div class="stats-grid">
      <div class="stat-card primary">
        <div class="stat-icon">📚</div>
        <div class="stat-content">
          <div class="stat-value">{{ systemStats.totalBooks || 0 }}</div>
          <div class="stat-label">Книг в библиотеке</div>
        </div>
      </div>

      <div class="stat-card success">
        <div class="stat-icon">👥</div>
        <div class="stat-content">
          <div class="stat-value">{{ systemStats.totalUsers || 0 }}</div>
          <div class="stat-label">Активных пользователей</div>
        </div>
      </div>

      <div class="stat-card warning">
        <div class="stat-icon">⭐</div>
        <div class="stat-content">
          <div class="stat-value">{{ formatRating(systemStats.globalAverageRating) }}</div>
          <div class="stat-label">Средний рейтинг</div>
        </div>
      </div>

      <div class="stat-card info">
        <div class="stat-icon">💬</div>
        <div class="stat-content">
          <div class="stat-value">{{ systemStats.totalComments || 0 }}</div>
          <div class="stat-label">Всего комментариев</div>
        </div>
      </div>

      <div class="stat-card secondary">
        <div class="stat-icon">👤</div>
        <div class="stat-content">
          <div class="stat-value">{{ systemStats.totalAuthors || 0 }}</div>
          <div class="stat-label">Авторов</div>
        </div>
      </div>

      <div class="stat-card accent">
        <div class="stat-icon">🏷️</div>
        <div class="stat-content">
          <div class="stat-value">{{ systemStats.totalGenres || 0 }}</div>
          <div class="stat-label">Жанров</div>
        </div>
      </div>

      <div class="stat-card danger">
        <div class="stat-icon">🗑️</div>
        <div class="stat-content">
          <div class="stat-value">{{ formatPercentage(deletedCommentsPercentage) }}</div>
          <div class="stat-label">Удалённых комментариев</div>
        </div>
      </div>

      <div class="stat-card success">
        <div class="stat-icon">📊</div>
        <div class="stat-content">
          <div class="stat-value">{{ systemStats.totalRatings || 0 }}</div>
          <div class="stat-label">Всего оценок</div>
        </div>
      </div>
    </div>

    <!-- Топ книг по рейтингу -->
    <div class="stats-section">
      <h3>⭐ Топ-5 книг по рейтингу</h3>
      <div v-if="loadingTopRatedBooks" class="loading">Загрузка...</div>
      <div v-else class="top-books-list">
        <div v-for="(book, index) in topRatedBooks" :key="book.bookId" class="top-book-item">
          <div class="book-rank">{{ index + 1 }}</div>
          <div class="book-info">
            <div class="book-title">{{ book.title }}</div>
            <div class="book-stats">
              <span class="rating">⭐ {{ formatRating(book.averageRating) }}</span>
              <span class="count">📊 {{ book.ratingCount }} оценок</span>
              <span class="comments">💬 {{ book.commentCount }} комментариев</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Топ жанров -->
    <div class="stats-section">
      <h3>🏷️ Топ-5 жанров по количеству книг</h3>
      <div v-if="loadingTopGenres" class="loading">Загрузка...</div>
      <div v-else class="genre-bars">
        <div v-for="genre in topGenres" :key="genre.genreId" class="genre-bar-item">
          <div class="genre-name">{{ genre.name }}</div>
          <div class="genre-bar-container">
            <div
                class="genre-bar-fill"
                :style="{ width: getBarWidth(genre.bookCount, maxGenreBooks) }"
            ></div>
            <span class="genre-count">{{ genre.bookCount }} книг</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Топ авторов -->
    <div class="stats-section">
      <h3>✍️ Топ-5 авторов по количеству книг</h3>
      <div v-if="loadingTopAuthors" class="loading">Загрузка...</div>
      <div v-else class="author-grid">
        <div v-for="author in topAuthors" :key="author.authorId" class="author-card">
          <div class="author-avatar">{{ author.fullName.charAt(0) }}</div>
          <div class="author-name">{{ author.fullName }}</div>
          <div class="author-books">{{ author.bookCount }} книг</div>
        </div>
      </div>
    </div>

    <!-- Распределение оценок -->
    <div class="stats-section">
      <h3>📈 Распределение оценок</h3>
      <div v-if="loadingRatingDistribution" class="loading">Загрузка...</div>
      <div v-else class="rating-distribution">
        <div v-for="rating in ratingDistribution" :key="rating.ratingValue" class="rating-row">
          <div class="rating-stars">{{ '⭐'.repeat(rating.ratingValue) }}</div>
          <div class="rating-bar-container">
            <div
                class="rating-bar-fill"
                :style="{ width: getBarWidth(rating.count, maxRatingCount) }"
            ></div>
            <span class="rating-count">{{ rating.count }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Активность пользователей (только для админа) -->
    <div v-if="isAdmin" class="stats-section">
      <h3>👥 Распределение пользователей по ролям</h3>
      <div v-if="loadingUserRoles" class="loading">Загрузка...</div>
      <div v-else class="role-distribution">
        <div v-for="role in userRoleStats" :key="role.roleName" class="role-item">
          <div class="role-icon">
            {{ getRoleIcon(role.roleName) }}
          </div>
          <div class="role-info">
            <div class="role-name">{{ formatRole(role.roleName) }}</div>
            <div class="role-count">{{ role.userCount }} пользователей</div>
          </div>
        </div>
      </div>
    </div>

    <!-- Самые активные комментаторы -->
    <div class="stats-section">
      <h3>💬 Топ-5 активных комментаторов</h3>
      <div v-if="loadingActiveCommenters" class="loading">Загрузка...</div>
      <div v-else class="user-activity-list">
        <div v-for="(user, index) in topCommenters" :key="user.userId" class="user-activity-item">
          <div class="user-rank">{{ index + 1 }}</div>
          <div class="user-name">{{ user.username }}</div>
          <div class="user-activity-count">{{ user.activityCount }} комментариев</div>
        </div>
      </div>
    </div>

    <!-- Последние комментарии -->
    <div class="stats-section">
      <h3>🕒 Последние комментарии</h3>
      <div v-if="loadingRecentComments" class="loading">Загрузка...</div>
      <div v-else class="recent-comments">
        <div v-for="comment in recentComments" :key="comment.id" class="recent-comment-card">
          <div class="comment-header-info">
            <div class="comment-author">👤 {{ comment.userName }}</div>
            <div class="comment-book">📖 {{ getBookTitle(comment.bookId) }}</div>
          </div>
          <div class="comment-text">{{ comment.text }}</div>
          <div class="comment-date">{{ formatDate(comment.createdAt) }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getCookie } from '@/utils/cookies'
import { formatDate, formatRole, formatRating, formatPercentage } from '@/utils/formatters'

const props = defineProps({
  isAdmin: {
    type: Boolean,
    default: false
  }
})

// Данные статистики
const systemStats = ref({})
const deletedCommentsPercentage = ref(0)
const topRatedBooks = ref([])
const topGenres = ref([])
const topAuthors = ref([])
const ratingDistribution = ref([])
const userRoleStats = ref([])
const topCommenters = ref([])
const recentComments = ref([])
const bookTitles = ref({})

// Состояния загрузки
const loadingTopRatedBooks = ref(false)
const loadingTopGenres = ref(false)
const loadingTopAuthors = ref(false)
const loadingRatingDistribution = ref(false)
const loadingUserRoles = ref(false)
const loadingActiveCommenters = ref(false)
const loadingRecentComments = ref(false)

// Вычисляемые значения для барчартов
const maxGenreBooks = computed(() => {
  return Math.max(...topGenres.value.map(g => g.bookCount), 1)
})

const maxRatingCount = computed(() => {
  return Math.max(...ratingDistribution.value.map(r => r.count), 1)
})

// Вспомогательные функции
function getBarWidth(value, max) {
  return `${(value / max * 100)}%`
}

function getRoleIcon(roleName) {
  const normalized = roleName.replace('ROLE_', '')
  return normalized === 'ADMIN' ? '👑' : normalized === 'MODERATOR' ? '🛡️' : '👤'
}

function getBookTitle(bookId) {
  return bookTitles.value[bookId] || 'Загрузка...'
}

// API запросы
async function loadSystemStatistics() {
  try {
    const jwt = getCookie('jwt')
    const response = await fetch('/api/statistics/system', {
      headers: { 'Authorization': `Bearer ${jwt}` }
    })

    if (response.ok) {
      systemStats.value = await response.json()
    }
  } catch (error) {
    console.error('Ошибка загрузки системной статистики:', error)
  }
}

async function loadTopRatedBooks() {
  loadingTopRatedBooks.value = true
  try {
    const jwt = getCookie('jwt')
    const response = await fetch('/api/statistics/books/top-rated?minRatings=3&size=5', {
      headers: { 'Authorization': `Bearer ${jwt}` }
    })

    if (response.ok) {
      const data = await response.json()
      topRatedBooks.value = data.content || []
    }
  } catch (error) {
    console.error('Ошибка загрузки топ книг:', error)
  } finally {
    loadingTopRatedBooks.value = false
  }
}

async function loadTopGenres() {
  loadingTopGenres.value = true
  try {
    const jwt = getCookie('jwt')
    const response = await fetch('/api/statistics/genres/top-by-count?size=5', {
      headers: { 'Authorization': `Bearer ${jwt}` }
    })

    if (response.ok) {
      const data = await response.json()
      topGenres.value = data.content || []
    }
  } catch (error) {
    console.error('Ошибка загрузки топ жанров:', error)
  } finally {
    loadingTopGenres.value = false
  }
}

async function loadTopAuthors() {
  loadingTopAuthors.value = true
  try {
    const jwt = getCookie('jwt')
    const response = await fetch('/api/statistics/authors/top-by-count?size=5', {
      headers: { 'Authorization': `Bearer ${jwt}` }
    })

    if (response.ok) {
      const data = await response.json()
      topAuthors.value = data.content || []
    }
  } catch (error) {
    console.error('Ошибка загрузки топ авторов:', error)
  } finally {
    loadingTopAuthors.value = false
  }
}

async function loadRatingDistribution() {
  loadingRatingDistribution.value = true
  try {
    const jwt = getCookie('jwt')
    const response = await fetch('/api/statistics/ratings/distribution', {
      headers: { 'Authorization': `Bearer ${jwt}` }
    })

    if (response.ok) {
      ratingDistribution.value = await response.json()
    }
  } catch (error) {
    console.error('Ошибка загрузки распределения оценок:', error)
  } finally {
    loadingRatingDistribution.value = false
  }
}

async function loadUserRoleStats() {
  if (!props.isAdmin) return

  loadingUserRoles.value = true
  try {
    const jwt = getCookie('jwt')
    const response = await fetch('/api/statistics/users/by-role', {
      headers: { 'Authorization': `Bearer ${jwt}` }
    })

    if (response.ok) {
      userRoleStats.value = await response.json()
    }
  } catch (error) {
    console.error('Ошибка загрузки статистики ролей:', error)
  } finally {
    loadingUserRoles.value = false
  }
}

async function loadActiveCommenters() {
  loadingActiveCommenters.value = true
  try {
    const jwt = getCookie('jwt')
    const response = await fetch('/api/statistics/users/most-active-commenters?size=5', {
      headers: { 'Authorization': `Bearer ${jwt}` }
    })

    if (response.ok) {
      const data = await response.json()
      topCommenters.value = data.content || []
    }
  } catch (error) {
    console.error('Ошибка загрузки активных комментаторов:', error)
  } finally {
    loadingActiveCommenters.value = false
  }
}

async function loadRecentComments() {
  loadingRecentComments.value = true
  try {
    const jwt = getCookie('jwt')
    const response = await fetch('/api/statistics/comments/recent?size=5', {
      headers: { 'Authorization': `Bearer ${jwt}` }
    })

    if (response.ok) {
      const data = await response.json()
      recentComments.value = data.content || []
      await loadBookTitles()
    }
  } catch (error) {
    console.error('Ошибка загрузки последних комментариев:', error)
  } finally {
    loadingRecentComments.value = false
  }
}

async function loadBookTitles() {
  const jwt = getCookie('jwt')
  const bookIds = [...new Set(recentComments.value.map(c => c.bookId))]

  for (const bookId of bookIds) {
    try {
      const response = await fetch(`/api/books/${bookId}`, {
        headers: { 'Authorization': `Bearer ${jwt}` }
      })

      if (response.ok) {
        const book = await response.json()
        bookTitles.value[bookId] = book.title
      }
    } catch (error) {
      console.error(`Ошибка загрузки книги ${bookId}:`, error)
      bookTitles.value[bookId] = 'Неизвестная книга'
    }
  }
}

async function loadDeletedCommentsPercentage() {
  try {
    const jwt = getCookie('jwt')
    const response = await fetch('/api/statistics/comments/deleted-percentage', {
      headers: { 'Authorization': `Bearer ${jwt}` }
    })

    if (response.ok) {
      deletedCommentsPercentage.value = await response.json()
    }
  } catch (error) {
    console.error('Ошибка загрузки процента удалённых комментариев:', error)
  }
}

// Загрузка всех данных
async function loadAllStatistics() {
  await Promise.all([
    loadSystemStatistics(),
    loadTopRatedBooks(),
    loadTopGenres(),
    loadTopAuthors(),
    loadRatingDistribution(),
    loadActiveCommenters(),
    loadRecentComments(),
    loadDeletedCommentsPercentage(),
    props.isAdmin ? loadUserRoleStats() : Promise.resolve()
  ])
}

onMounted(() => {
  loadAllStatistics()
})
</script>