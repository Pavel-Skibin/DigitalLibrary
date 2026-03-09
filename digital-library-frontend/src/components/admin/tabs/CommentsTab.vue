<template>
  <div class="tab-panel">
    <div class="panel-header">
      <h2>Модерация комментариев</h2>
    </div>

    <p class="info-text">Выберите книгу для просмотра комментариев</p>

    <SearchInput
        v-model="bookSearchQuery"
        placeholder="🔍 Поиск книги..."
    />

    <!-- Список найденных книг -->
    <div v-if="foundBooks.length > 0 && !selectedBook" class="books-dropdown">
      <div
          v-for="book in foundBooks"
          :key="book.id"
          @click="selectBook(book)"
          class="book-dropdown-item"
      >
        <strong>{{ book.title }}</strong>
        <span class="book-authors">{{ book.authors.join(', ') }}</span>
      </div>
    </div>

    <!-- Выбранная книга и её комментарии -->
    <div v-if="selectedBook" class="selected-book-section">
      <div class="selected-book-header">
        <h3>📖 {{ selectedBook.title }}</h3>
        <button @click="clearSelection" class="btn-secondary">
          ✕ Выбрать другую книгу
        </button>
      </div>

      <div v-if="loadingComments" class="loading">Загрузка комментариев...</div>

      <div v-else-if="comments.length === 0" class="no-comments">
        <p>У этой книги пока нет комментариев</p>
      </div>

      <div v-else class="comments-list">
        <div
            v-for="comment in comments"
            :key="comment.id"
            class="comment-card"
            :class="{ deleted: comment.deletedAt }"
        >
          <div class="comment-header">
            <div class="comment-user-info">
              <span class="comment-author">👤 {{ comment.userName }}</span>
              <span class="comment-id">(ID: {{ comment.userId }})</span>
            </div>
            <span class="comment-date">{{ formatDate(comment.createdAt) }}</span>
          </div>

          <div class="comment-text">
            <span v-if="comment.deletedAt" class="deleted-badge">
              [Удалено {{ formatDate(comment.deletedAt) }}]
            </span>
            <p>{{ comment.text }}</p>
          </div>

          <div class="comment-actions">
            <button
                v-if="!comment.deletedAt"
                @click="deleteComment(comment.id)"
                class="btn-delete-small"
            >
              🗑️ Удалить
            </button>
            <button
                v-else
                @click="restoreComment(comment.id)"
                class="btn-restore"
            >
              ↩️ Восстановить
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { getCookie } from '@/utils/cookies'
import { formatDate } from '@/utils/formatters'
import SearchInput from '@/components/ui/SearchInput.vue'

const bookSearchQuery = ref('')
const foundBooks = ref([])
const selectedBook = ref(null)
const comments = ref([])
const loadingComments = ref(false)

let searchTimeout = null

// Поиск книг
watch(bookSearchQuery, () => {
  clearTimeout(searchTimeout)

  if (!bookSearchQuery.value.trim()) {
    foundBooks.value = []
    return
  }

  searchTimeout = setTimeout(async () => {
    try {
      const jwt = getCookie('jwt')
      const response = await fetch(
          `/api/books/search?title=${encodeURIComponent(bookSearchQuery.value)}&size=10`,
          { headers: { 'Authorization': `Bearer ${jwt}` } }
      )

      if (response.ok) {
        const data = await response.json()
        foundBooks.value = data.content || []
      }
    } catch (error) {
    }
  }, 300)
})

// Выбор книги
function selectBook(book) {
  selectedBook.value = book
  foundBooks.value = []
  loadComments(book.id)
}

function clearSelection() {
  selectedBook.value = null
  comments.value = []
  bookSearchQuery.value = ''
  foundBooks.value = []
}

// Загрузка комментариев
async function loadComments(bookId) {
  loadingComments.value = true
  try {
    const jwt = getCookie('jwt')
    const response = await fetch(`/api/comments/books/${bookId}/all`, {
      headers: { 'Authorization': `Bearer ${jwt}` }
    })

    if (response.ok) {
      comments.value = await response.json()
    } else {
      alert('Не удалось загрузить комментарии')
    }
  } catch (error) {
    alert('Ошибка соединения с сервером')
  } finally {
    loadingComments.value = false
  }
}

// Удаление комментария
async function deleteComment(commentId) {
  if (!confirm('Удалить этот комментарий?')) return

  try {
    const jwt = getCookie('jwt')
    const response = await fetch(`/api/comments/${commentId}/moderate`, {
      method: 'DELETE',
      headers: { 'Authorization': `Bearer ${jwt}` }
    })

    if (response.ok) {
      alert('Комментарий удалён!')
      await loadComments(selectedBook.value.id)
    } else {
      const errorText = await response.text()
      alert(`Ошибка удаления: ${errorText}`)
    }
  } catch (error) {
    alert('Не удалось удалить комментарий')
  }
}

// Восстановление комментария
async function restoreComment(commentId) {
  if (!confirm('Восстановить этот комментарий?')) return

  try {
    const jwt = getCookie('jwt')
    const response = await fetch(`/api/comments/${commentId}/restore`, {
      method: 'POST',
      headers: { 'Authorization': `Bearer ${jwt}` }
    })

    if (response.ok) {
      alert('Комментарий восстановлён!')
      await loadComments(selectedBook.value.id)
    } else {
      const errorText = await response.text()
      alert(`Ошибка восстановления: ${errorText}`)
    }
  } catch (error) {
    alert('Не удалось восстановить комментарий')
  }
}
</script>