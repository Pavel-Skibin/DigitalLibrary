<template>
  <div class="app-layout">
    <Header />
    <div class="library-container">
      <AppSidebar :show-admin-link="isModeratorOrAdmin" />

      <main class="content">
        <div class="profile-view">
          <div class="profile-header">
            <h1>Личный кабинет</h1>
            <p class="username">{{ username }}</p>
          </div>

          <div class="tabs">
            <button
              v-for="tab in tabs"
              :key="tab.id"
              @click="switchTab(tab.id)"
              class="tab-button"
              :class="{ active: activeTab === tab.id }"
            >
              {{ tab.icon }} {{ tab.label }}
            </button>
          </div>

          <div class="content-wrapper">
            <!-- Main content area -->
            <div class="tab-content">
              <!-- Вкладка: Избранное -->
              <div v-if="activeTab === 'favorites'" class="favorites-section">
                <div v-if="loadingFavorites" class="loading">
                  <div class="spinner"></div>
                  <p>Загрузка избранных книг...</p>
                </div>

                <div v-else-if="favoriteBooks.length === 0" class="empty-state">
                  <p>📚 У вас пока нет избранных книг</p>
                  <button @click="$router.push('/books')" class="btn-browse">
                    Перейти к каталогу
                  </button>
                </div>

                <div v-else class="books-grid">
                  <BookCard
                    v-for="book in favoriteBooks"
                    :key="book.id"
                    :book="book"
                    :cover-url="getCoverUrl(book)"
                    @select="selectBook(book)"
                  />
                </div>
              </div>

              <!-- Вкладка: История просмотров -->
              <div v-if="activeTab === 'history'" class="history-section">
                <div v-if="loadingHistory" class="loading">
                  <div class="spinner"></div>
                  <p>Загрузка истории...</p>
                </div>

                <div v-else-if="viewedBooks.length === 0" class="empty-state">
                  <p>📖 История просмотров пуста</p>
                </div>

                <div v-else class="books-list-with-stats">
                  <div
                    v-for="item in viewedBooks"
                    :key="item.book.id"
                    class="book-item-with-stats"
                    @click="selectBook(item.book)"
                  >
                    <BookCard
                      :book="item.book"
                      :cover-url="getCoverUrl(item.book)"
                      @select="selectBook(item.book)"
                    />
                    <div class="reading-stats">
                      <div class="stat-item">
                        <span class="stat-icon">⏱️</span>
                        <span class="stat-value">{{
                          formatReadingTime(item.stats.totalReadingTimeSeconds)
                        }}</span>
                        <span class="stat-label">всего</span>
                      </div>
                      <div class="stat-item">
                        <span class="stat-icon">📚</span>
                        <span class="stat-value">{{
                          item.stats.sessionsCount
                        }}</span>
                        <span class="stat-label">{{
                          pluralizeSessions(item.stats.sessionsCount)
                        }}</span>
                      </div>
                      <div v-if="item.stats.lastReadAt" class="stat-item">
                        <span class="stat-icon">📅</span>
                        <span class="stat-value">{{
                          formatDate(item.stats.lastReadAt)
                        }}</span>
                        <span class="stat-label">последний раз</span>
                      </div>
                      <div
                        v-if="item.stats.isCompleted"
                        class="completed-badge"
                      >
                        ✓ Прочитано
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- Правая панель с деталями книги -->
            <aside v-if="selectedBook" class="book-detail-panel">
              <button class="close-panel-btn" @click="selectedBook = null">
                ✕
              </button>
              <BookDetailPanel
                :book="selectedBook"
                :cover-url="getCoverUrl(selectedBook)"
                :is-authenticated="true"
                @open-reader="openReader"
              />
            </aside>
          </div>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from "vue";
import { useRouter } from "vue-router";
import { useFavorites } from "@/composables/useFavorites";
import { useViewHistory } from "@/composables/useViewHistory";
import { api } from "@/api/index";
import { useUser } from "@/composables/useUser";
import { useAdminAuth } from "@/composables/useAdminAuth";
import Header from "@/components/layout/Header.vue";
import AppSidebar from "@/components/layout/AppSidebar.vue";
import BookCard from "@/components/books/BookCard.vue";
import BookDetailPanel from "@/components/books/BookDetailPanel.vue";

const router = useRouter();
const activeTab = ref("favorites");
const selectedBook = ref(null);

const { username: userUsername, fetchUserProfile } = useUser();
const { isModeratorOrAdmin } = useAdminAuth();
const { favorites, loading: loadingFavorites, loadFavorites } = useFavorites();
const { history, loading: loadingHistory, loadHistory } = useViewHistory();

// Загруженные полные данные книг
const favoriteBooks = ref([]);
const viewedBooks = ref([]);

const username = computed(() => userUsername.value || "Пользователь");

// Функция для получения URL обложки
function getCoverUrl(book) {
  return book?.coverUrl || "/placeholder.jpg";
}

const tabs = [
  { id: "favorites", label: "Избранное", icon: "❤️" },
  { id: "history", label: "История просмотров", icon: "📖" },
];

onMounted(async () => {
  console.log("🚀 UserProfileView mounted");
  await fetchUserProfile();
  console.log("👤 User profile loaded:", userUsername.value);
  await loadFavoritesData();
});

// Переключение вкладки
async function switchTab(tabId) {
  activeTab.value = tabId;
  selectedBook.value = null; // Закрыть панель деталей при переключении вкладки

  if (tabId === "favorites" && favoriteBooks.value.length === 0) {
    await loadFavoritesData();
  } else if (tabId === "history" && viewedBooks.value.length === 0) {
    await loadHistoryData();
  }
}

// Загрузить избранные книги с полными данными
async function loadFavoritesData() {
  console.log("🔄 Загрузка избранного...");
  try {
    await loadFavorites();
    console.log("✅ Favorites загружено:", favorites.value.length, "записей");

    // Загрузить полные данные книг
    if (favorites.value.length > 0) {
      const bookIds = favorites.value.map((f) => f.bookId);
      console.log("📚 Загружаем данные книг:", bookIds);
      const booksData = await Promise.all(
        bookIds.map((id) =>
          api.get(`/books/${id}`).catch((err) => {
            console.error(`Ошибка загрузки книги ${id}:`, err);
            return null;
          }),
        ),
      );
      favoriteBooks.value = booksData.filter((b) => b !== null);
      console.log("✅ Загружено книг:", favoriteBooks.value.length);
    } else {
      favoriteBooks.value = [];
      console.log("ℹ️ Нет избранных книг");
    }
  } catch (error) {
    console.error("❌ Ошибка загрузки избранного:", error);
    favoriteBooks.value = [];
  }
}

// Загрузить историю просмотров с полными данными
async function loadHistoryData() {
  console.log("🔄 Загрузка истории просмотров...");
  try {
    await loadHistory();
    console.log("✅ History загружено:", history.value.length, "записей");
    console.log("📊 История с статистикой:", history.value);

    // Загрузить полные данные книг и объединить со статистикой
    if (history.value.length > 0) {
      const bookIds = [...new Set(history.value.map((h) => h.bookId))]; // Уникальные ID
      console.log("📚 Загружаем данные книг:", bookIds);

      const booksData = await Promise.all(
        bookIds.map((id) =>
          api.get(`/books/${id}`).catch((err) => {
            console.error(`Ошибка загрузки книги ${id}:`, err);
            return null;
          }),
        ),
      );

      // Создаем мапу bookId -> статистика из history
      const statsMap = new Map();
      history.value.forEach((h) => {
        statsMap.set(h.bookId, {
          totalReadingTimeSeconds: h.totalReadingTimeSeconds || 0,
          sessionsCount: h.sessionsCount || 0,
          lastReadAt: h.lastReadAt,
          lastPosition: h.lastPosition,
          isCompleted: h.isCompleted || false,
          viewedAt: h.viewedAt,
        });
      });

      // Объединяем книги со статистикой
      viewedBooks.value = booksData
        .filter((b) => b !== null)
        .map((book) => ({
          book: book,
          stats: statsMap.get(book.id) || {
            totalReadingTimeSeconds: 0,
            sessionsCount: 0,
          },
        }))
        .sort((a, b) => {
          // Сортируем по времени последнего чтения (новые сверху)
          const dateA = a.stats.lastReadAt || a.stats.viewedAt;
          const dateB = b.stats.lastReadAt || b.stats.viewedAt;
          return new Date(dateB) - new Date(dateA);
        });

      console.log(
        "✅ Загружено книг со статистикой:",
        viewedBooks.value.length,
      );
    } else {
      viewedBooks.value = [];
      console.log("ℹ️ Нет истории просмотров");
    }
  } catch (error) {
    console.error("❌ Ошибка загрузки истории:", error);
    viewedBooks.value = [];
  }
}

// Форматировать время чтения
function formatReadingTime(seconds) {
  if (!seconds || seconds === 0) return "0 мин";

  const hours = Math.floor(seconds / 3600);
  const minutes = Math.floor((seconds % 3600) / 60);

  if (hours > 0) {
    return `${hours} ч ${minutes} мин`;
  }
  return `${minutes} мин`;
}

// Плюрализация "сессия"
function pluralizeSessions(count) {
  if (!count) return "сессий";

  const lastDigit = count % 10;
  const lastTwoDigits = count % 100;

  if (lastTwoDigits >= 11 && lastTwoDigits <= 14) {
    return "сессий";
  }

  if (lastDigit === 1) {
    return "сессия";
  }

  if (lastDigit >= 2 && lastDigit <= 4) {
    return "сессии";
  }

  return "сессий";
}

// Форматировать дату
function formatDate(dateString) {
  if (!dateString) return "";

  const date = new Date(dateString);
  const now = new Date();
  const diffTime = Math.abs(now - date);
  const diffDays = Math.floor(diffTime / (1000 * 60 * 60 * 24));

  if (diffDays === 0) {
    return "Сегодня";
  } else if (diffDays === 1) {
    return "Вчера";
  } else if (diffDays < 7) {
    return `${diffDays} дн. назад`;
  } else {
    return date.toLocaleDateString("ru-RU", {
      day: "numeric",
      month: "short",
    });
  }
}

// Выбрать книгу
function selectBook(book) {
  selectedBook.value = book;
}

// Открыть читалку
function openReader() {
  if (selectedBook.value) {
    window.open(`/reader.html?bookId=${selectedBook.value.id}`, "_blank");
  }
}
</script>

<style scoped>
.app-layout {
  display: flex;
  flex-direction: column;
  height: 100vh;
}

.library-container {
  display: flex;
  flex: 1;
  overflow: hidden;
}

.content {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
  background-color: #f5f5f5;
}

.profile-view {
  max-width: 1400px;
  margin: 0 auto;
}

.profile-header {
  margin-bottom: 32px;
}

.profile-header h1 {
  font-size: 32px;
  font-weight: 700;
  color: #333;
  margin: 0 0 8px 0;
}

.username {
  font-size: 18px;
  color: #666;
  margin: 0;
}

.tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 24px;
  border-bottom: 2px solid #e0e0e0;
}

.tab-button {
  padding: 12px 24px;
  background: none;
  border: none;
  border-bottom: 3px solid transparent;
  font-size: 16px;
  font-weight: 600;
  color: #666;
  cursor: pointer;
  transition: all 0.2s;
  margin-bottom: -2px;
}

.tab-button:hover {
  color: #667eea;
}

.tab-button.active {
  color: #667eea;
  border-bottom-color: #667eea;
}

.tab-content {
  min-height: 400px;
  flex: 1;
}

.content-wrapper {
  display: flex;
  gap: 24px;
  align-items: flex-start;
}

.book-detail-panel {
  position: sticky;
  top: 80px;
  width: 400px;
  flex-shrink: 0;
  max-height: calc(100vh - 100px);
  overflow-y: auto;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.close-panel-btn {
  position: absolute;
  top: 12px;
  right: 12px;
  width: 32px;
  height: 32px;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid #e0e0e0;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 18px;
  color: #666;
  transition: all 0.2s;
  z-index: 10;
}

.close-panel-btn:hover {
  background: #f5f5f5;
  color: #333;
  border-color: #ccc;
}

.books-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

.loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 300px;
  gap: 16px;
}

.spinner {
  width: 48px;
  height: 48px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #667eea;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 300px;
  gap: 20px;
  color: #666;
}

.empty-state p {
  font-size: 18px;
  margin: 0;
}

.btn-browse {
  padding: 12px 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
}

.btn-browse:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

/* Стили для истории с статистикой */
.books-list-with-stats {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.book-item-with-stats {
  position: relative;
  cursor: pointer;
}

.book-item-with-stats :deep(.book-card) {
  margin-bottom: 0;
}

.reading-stats {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 8px;
  padding: 12px 16px;
  background: linear-gradient(135deg, #f6f8fb 0%, #f0f4f8 100%);
  border-radius: 8px;
  border-left: 4px solid #667eea;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
}

.stat-icon {
  font-size: 16px;
}

.stat-value {
  font-weight: 700;
  color: #333;
}

.stat-label {
  color: #666;
  font-size: 13px;
}

.completed-badge {
  display: inline-flex;
  align-items: center;
  padding: 4px 12px;
  background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
  color: white;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  margin-left: auto;
}

/* Адаптивность */
@media (max-width: 768px) {
  .reading-stats {
    flex-direction: column;
    gap: 8px;
  }

  .stat-item {
    width: 100%;
  }

  .completed-badge {
    margin-left: 0;
    align-self: flex-start;
  }
}
</style>
