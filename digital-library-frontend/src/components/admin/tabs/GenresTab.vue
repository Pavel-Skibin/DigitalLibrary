<template>
  <div class="tab-panel">
    <div class="panel-header">
      <h2>Управление жанрами</h2>
      <button @click="openModal()" class="btn-primary">
        + Добавить жанр
      </button>
    </div>

    <div v-if="loading" class="loading">Загрузка...</div>

    <DataTable
        v-else
        :columns="columns"
        :data="genres"
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

    <GenreModal
        v-if="showModal"
        :genre="editingGenre"
        :loading="saving"
        @close="closeModal"
        @save="handleSave"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getCookie } from '@/utils/cookies'
import DataTable from '@/components/ui/DataTable.vue'
import GenreModal from '@/components/admin/modals/GenreModal.vue'

const genres = ref([])
const loading = ref(false)
const saving = ref(false)

const showModal = ref(false)
const editingGenre = ref(null)

const columns = [
  { key: 'id', label: 'ID' },
  { key: 'name', label: 'Название' }
]

// Загрузка жанров
async function loadGenres() {
  loading.value = true
  try {
    const jwt = getCookie('jwt')
    const response = await fetch('http://localhost:8080/api/genres', {
      headers: { 'Authorization': `Bearer ${jwt}` }
    })

    if (response.ok) {
      genres.value = await response.json()
    }
  } catch (error) {
    console.error('Ошибка загрузки жанров:', error)
    alert('Не удалось загрузить жанры')
  } finally {
    loading.value = false
  }
}

// Модальное окно
function openModal(genre = null) {
  editingGenre.value = genre
  showModal.value = true
}

function closeModal() {
  showModal.value = false
  editingGenre.value = null
}

// Сохранение
async function handleSave(formData) {
  saving.value = true
  try {
    const jwt = getCookie('jwt')
    const url = editingGenre.value
        ? `http://localhost:8080/api/genres/${editingGenre.value.id}`
        : 'http://localhost:8080/api/genres'

    const params = new URLSearchParams()
    params.append('name', formData.name.trim())

    const response = await fetch(`${url}?${params.toString()}`, {
      method: editingGenre.value ? 'PUT' : 'POST',
      headers: { 'Authorization': `Bearer ${jwt}` }
    })

    if (response.ok) {
      alert(editingGenre.value ? 'Жанр обновлён!' : 'Жанр добавлен!')
      closeModal()
      await loadGenres()
    } else {
      const error = await response.text()
      alert(`Ошибка: ${error}`)
    }
  } catch (error) {
    console.error('Ошибка сохранения жанра:', error)
    alert('Не удалось сохранить жанр')
  } finally {
    saving.value = false
  }
}

// Удаление
async function handleDelete(genreId) {
  if (!confirm('Вы уверены, что хотите удалить этот жанр?')) return

  try {
    const jwt = getCookie('jwt')
    const response = await fetch(`http://localhost:8080/api/genres/${genreId}`, {
      method: 'DELETE',
      headers: { 'Authorization': `Bearer ${jwt}` }
    })

    if (response.ok) {
      alert('Жанр удалён!')
      await loadGenres()
    } else {
      const error = await response.text()
      alert(`Ошибка: ${error}`)
    }
  } catch (error) {
    console.error('Ошибка удаления жанра:', error)
    alert('Не удалось удалить жанр')
  }
}

onMounted(() => {
  loadGenres()
})
</script>