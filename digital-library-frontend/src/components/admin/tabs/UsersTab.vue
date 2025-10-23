<template>
  <div class="tab-panel">
    <div class="panel-header">
      <h2>Управление пользователями</h2>
    </div>

    <SearchInput
        v-model="searchQuery"
        placeholder="🔍 Поиск по имени пользователя или email..."
    />

    <div v-if="loading" class="loading">Загрузка...</div>

    <DataTable
        v-else
        :columns="columns"
        :data="users"
    >
      <template #cell-roleName="{ row }">
        {{ formatRole(row.roleName) }}
      </template>
      <template #cell-isDeleted="{ row }">
        <span :class="row.isDeleted ? 'status-banned' : 'status-active'">
          {{ row.isDeleted ? '🚫 Заблокирован' : '✅ Активен' }}
        </span>
      </template>
      <template #actions="{ row }">
        <button
            v-if="!row.isDeleted"
            @click="banUser(row.id)"
            class="btn-delete"
        >
          🚫 Забанить
        </button>
        <button
            v-else
            @click="unbanUser(row.id)"
            class="btn-restore"
        >
          ✅ Разбанить
        </button>
        <button @click="openRoleModal(row)" class="btn-edit">
          🔄 Изменить роль
        </button>
      </template>
    </DataTable>

    <Pagination
        :current-page="currentPage"
        :total-pages="totalPages"
        @page-change="loadPage"
    />

    <ChangeRoleModal
        v-if="showRoleModal"
        :user="editingUser"
        :loading="saving"
        @close="closeRoleModal"
        @save="handleRoleChange"
    />
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { getCookie } from '@/utils/cookies'
import { formatRole } from '@/utils/formatters'
import SearchInput from '@/components/ui/SearchInput.vue'
import DataTable from '@/components/ui/DataTable.vue'
import Pagination from '@/components/ui/Pagination.vue'
import ChangeRoleModal from '@/components/admin/modals/ChangeRoleModal.vue'

const users = ref([])
const loading = ref(false)
const saving = ref(false)
const searchQuery = ref('')
const currentPage = ref(0)
const totalPages = ref(0)
const pageSize = 10

const showRoleModal = ref(false)
const editingUser = ref(null)

const columns = [
  { key: 'id', label: 'ID' },
  { key: 'username', label: 'Имя пользователя' },
  { key: 'email', label: 'Email' },
  { key: 'roleName', label: 'Роль' },
  { key: 'isDeleted', label: 'Статус' }
]

let searchTimeout = null

// Загрузка пользователей
async function loadUsers(page = 0) {
  loading.value = true
  try {
    const jwt = getCookie('jwt')
    const query = searchQuery.value.trim()
    const url = query
        ? `/api/users?query=${encodeURIComponent(query)}&page=${page}&size=${pageSize}`
        : `/api/users?page=${page}&size=${pageSize}`

    const response = await fetch(url, {
      headers: { 'Authorization': `Bearer ${jwt}` }
    })

    if (response.ok) {
      const data = await response.json()
      users.value = data.content || []
      currentPage.value = data.number
      totalPages.value = data.totalPages
    } else {
      alert('Не удалось загрузить список пользователей')
    }
  } catch (error) {
    console.error('Ошибка загрузки пользователей:', error)
    alert('Ошибка соединения с сервером')
  } finally {
    loading.value = false
  }
}

function loadPage(page) {
  loadUsers(page)
}

// Поиск с задержкой
watch(searchQuery, () => {
  clearTimeout(searchTimeout)
  searchTimeout = setTimeout(() => {
    loadUsers(0)
  }, 300)
})

// Бан пользователя
async function banUser(userId) {
  if (!confirm('Заблокировать этого пользователя?')) return

  try {
    const jwt = getCookie('jwt')
    const response = await fetch(`/api/users/${userId}`, {
      method: 'DELETE',
      headers: { 'Authorization': `Bearer ${jwt}` }
    })

    if (response.ok) {
      alert('Пользователь заблокирован!')
      await loadUsers(currentPage.value)
    } else {
      const errorText = await response.text()
      alert(`Ошибка: ${errorText}`)
    }
  } catch (error) {
    console.error('Ошибка блокировки пользователя:', error)
    alert('Не удалось заблокировать пользователя')
  }
}

// Разбан пользователя
async function unbanUser(userId) {
  if (!confirm('Разблокировать этого пользователя?')) return

  try {
    const jwt = getCookie('jwt')
    const response = await fetch(`/api/users/${userId}/restore`, {
      method: 'POST',
      headers: { 'Authorization': `Bearer ${jwt}` }
    })

    if (response.ok) {
      alert('Пользователь разблокирован!')
      await loadUsers(currentPage.value)
    } else {
      const errorText = await response.text()
      alert(`Ошибка: ${errorText}`)
    }
  } catch (error) {
    console.error('Ошибка разблокировки пользователя:', error)
    alert('Не удалось разблокировать пользователя')
  }
}

// Модальное окно смены роли
function openRoleModal(user) {
  editingUser.value = user
  showRoleModal.value = true
}

function closeRoleModal() {
  showRoleModal.value = false
  editingUser.value = null
}

// Изменение роли
async function handleRoleChange(roleId) {
  saving.value = true
  try {
    const jwt = getCookie('jwt')
    const response = await fetch(`/api/users/${editingUser.value.id}/role`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${jwt}`
      },
      body: JSON.stringify(roleId)
    })

    if (response.ok) {
      alert('Роль изменена!')
      closeRoleModal()
      await loadUsers(currentPage.value)
    } else {
      const errorText = await response.text()
      alert(`Ошибка: ${errorText}`)
    }
  } catch (error) {
    console.error('Ошибка смены роли:', error)
    alert('Не удалось изменить роль')
  } finally {
    saving.value = false
  }
}

// Загружаем при монтировании
loadUsers(0)
</script>