<template>
  <div class="app-layout">
    <Header />
    <div class="library-container">
      <AppSidebar :show-admin-link="isModeratorOrAdmin" />

      <main class="content">
        <h1 class="page-title">📚 Авторы</h1>

        <SearchInput
            v-model="localSearchQuery"
            placeholder="🔍 Поиск автора"
            @update:model-value="handleSearchInput"
        />

        <div v-if="loading" class="loading-message">
          Загрузка авторов...
        </div>

        <div v-else-if="authors.length === 0 && localSearchQuery.trim()" class="no-results-message">
          По запросу "{{ localSearchQuery }}" ничего не найдено
        </div>

        <div v-else-if="authors.length === 0" class="no-results-message">
          Список авторов пуст
        </div>

        <div v-else class="authors-list">
          <AuthorCard
              v-for="author in authors"
              :key="author.id"
              :author="author"
              @select="handleSelectAuthor"
          />
        </div>

        <Pagination
            v-if="totalPages > 1"
            :current-page="currentPage"
            :total-pages="totalPages"
            @page-change="handlePageChange"
        />
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'

import Header from '@/components/layout/Header.vue'
import AppSidebar from '@/components/layout/AppSidebar.vue'
import SearchInput from '@/components/ui/SearchInput.vue'
import Pagination from '@/components/ui/Pagination.vue'
import AuthorCard from '@/components/authors/AuthorCard.vue'

import { useAuthors } from '@/composables/useAuthors'
import { useAdminAuth } from '@/composables/useAdminAuth'

const router = useRouter()

const {
  authors,
  loading,
  currentPage,
  totalPages,
  searchQuery,
  isSearchMode,
  loadAuthors,
  handleSearchInput: handleSearch,
  nextPage,
  prevPage
} = useAuthors()

const { isAdmin, isModerator, checkAccess } = useAdminAuth()

// ИСПРАВЛЕНИЕ: используем computed вместо ref
const isModeratorOrAdmin = computed(() => isAdmin.value || isModerator.value)

// Локальная копия searchQuery для v-model
const localSearchQuery = ref('')

function handleSearchInput(value) {
  localSearchQuery.value = value
  handleSearch(value)
}

function handleSelectAuthor(author) {
  router.push({
    name: 'AuthorBooks',
    params: {
      authorId: author.id,
      authorName: author.fullName
    }
  })
}

function handlePageChange(page) {
  if (page > currentPage.value) {
    nextPage()
  } else {
    prevPage()
  }
}

onMounted(async () => {

  await checkAccess()
  await loadAuthors(0)
})
</script>

<style scoped src="@/assets/styles/library-common.css"></style>

<style scoped>
.page-title {
  font-size: 32px;
  font-weight: 700;
  color: #333;
  margin: 0 0 24px 0;
  display: flex;
  align-items: center;
  gap: 12px;
}

.authors-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
  margin-bottom: 32px;
}

.loading-message,
.no-results-message {
  text-align: center;
  padding: 60px 20px;
  color: #999;
  font-size: 18px;
  background-color: #f9f9f9;
  border-radius: 12px;
  border: 2px dashed #e0e0e0;
}

.no-results-message {
  color: #666;
}

@media (max-width: 1024px) {
  .authors-list {
    grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
    gap: 16px;
  }
}

@media (max-width: 768px) {
  .authors-list {
    grid-template-columns: 1fr;
    gap: 12px;
  }

  .page-title {
    font-size: 24px;
  }
}
</style>