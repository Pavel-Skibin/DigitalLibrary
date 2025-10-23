<template>
  <div class="tab-panel">
    <div class="panel-header">
      <h2>Управление авторами</h2>
      <button @click="openModal()" class="btn-primary">
        + Добавить автора
      </button>
    </div>

    <SearchInput
        v-model="searchQuery"
        placeholder="🔍 Поиск автора..."
    />

    <div v-if="loading" class="loading">Загрузка...</div>

    <DataTable
        v-else
        :columns="columns"
        :data="authors"
    >
      <template #actions="{ row }">
        <button @click="openModal(row)" class="btn-edit">
          ✏️ Редактировать
        </button>
        <button @click="handleDelete(row.id)" class="btn-delete">
          🗑️ Удалить
        </button>
      </template>
    </DataTable>

    <Pagination
        :current-page="currentPage"
        :total-pages="totalPages"
        @page-change="loadPage"
    />

    <AuthorModal
        v-if="showModal"
        :author="editingAuthor"
        :loading="saving"
        @close="closeModal"
        @save="handleSave"
    />
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { getCookie } from '@/utils/cookies'
import SearchInput from '@/components/ui/SearchInput.vue'
import DataTable from '@/components/ui/DataTable.vue'
import Pagination from '@/components/ui/Pagination.vue'
import AuthorModal from '@/components/admin/modals/AuthorModal.vue'

const authors = ref([])
const loading = ref(false)
const saving = ref(false)
const searchQuery = ref('')
const currentPage = ref(0)
const totalPages = ref(0)
const pageSize = 10

const showModal = ref(false)
const editingAuthor = ref(null)

const columns = [
  { key: 'id', label: 'ID' },
  { key: 'firstName', label: 'Имя' },
  { key: 'lastName', label: 'Фамилия' }
]

let searchTimeout = null

// Загрузка авторов
async function loadAuthors(page = 0) {
  loading.value = true
  try {
    const jwt = getCookie('jwt')
    const query = searchQuery.value.trim()
    const url = query
        ? `/api/authors?query=${encodeURIComponent(query)}&page=${page}&size=${pageSize}`
        : `/api/authors?page=${page}&size=${pageSize}`

    const response = await fetch(url, {
      headers: { 'Authorization': `Bearer ${jwt}` }
    })

    if (response.ok) {
      const data = await response.json()
      authors.value = data.content || []
      currentPage.value = data.number
      totalPages.value = data.totalPages
    }
  } catch (error) {
    console.error('Ошибка загрузки авторов:', error)
    alert('Не удалось загрузить авторов')
  } finally {
    loading.value = false
  }
}

function loadPage(page) {
  loadAuthors(page)
}

// Поиск с задержкой
watch(searchQuery, () => {
  clearTimeout(searchTimeout)
  searchTimeout = setTimeout(() => {
    loadAuthors(0)
  }, 300)
})

// Модальное окно
function openModal(author = null) {
  editingAuthor.value = author
  showModal.value = true
}

function closeModal() {
  showModal.value = false
  editingAuthor.value = null
}

// Сохранение
async function handleSave(formData) {
  saving.value = true
  try {
    const jwt = getCookie('jwt')
    const url = editingAuthor.value
        ? `/api/authors/${editingAuthor.value.id}`
        : '/api/authors'

    const params = new URLSearchParams()
    params.append('firstName', formData.firstName.trim())
    params.append('lastName', formData.lastName.trim())

    const response = await fetch(`${url}?${params.toString()}`, {
      method: editingAuthor.value ? 'PUT' : 'POST',
      headers: { 'Authorization': `Bearer ${jwt}` }
    })

    if (response.ok) {
      alert(editingAuthor.value ? 'Автор обновлён!' : 'Автор добавлен!')
      closeModal()
      await loadAuthors(currentPage.value)
    } else {
      const error = await response.text()
      alert(`Ошибка: ${error}`)
    }
  } catch (error) {
    console.error('Ошибка сохранения автора:', error)
    alert('Не удалось сохранить автора')
  } finally {
    saving.value = false
  }
}

// Удаление
async function handleDelete(authorId) {
  if (!confirm('Вы уверены, что хотите удалить этого автора?')) return

  try {
    const jwt = getCookie('jwt')
    const response = await fetch(`/api/authors/${authorId}`, {
      method: 'DELETE',
      headers: { 'Authorization': `Bearer ${jwt}` }
    })

    if (response.ok) {
      alert('Автор удалён!')
      await loadAuthors(currentPage.value)
    } else {
      const errorMessage = await response.text()
      alert(`Ошибка: ${errorMessage}`)
    }
  } catch (error) {
    console.error('Ошибка удаления автора:', error)
    alert('Не удалось подключиться к серверу')
  }
}

// Загружаем при монтировании
loadAuthors(0)
</script>