<template>
  <div class="comments-section">
    <h3 class="comments-title">Комментарии</h3>

    <!-- Форма добавления комментария -->
    <div v-if="isAuthenticated && !userComment" class="comment-form">
      <textarea
          v-model="newCommentText"
          placeholder="Напишите ваш комментарий..."
          class="comment-textarea"
          rows="3"
          maxlength="1000"
      ></textarea>
      <div class="comment-form-actions">
        <span class="char-counter">{{ newCommentText.length }}/1000</span>
        <button
            @click="handleSubmit"
            :disabled="!newCommentText.trim() || submitting"
            class="btn-submit-comment"
        >
          {{ submitting ? 'Отправка...' : 'Отправить' }}
        </button>
      </div>
    </div>

    <!-- Редактирование комментария -->
    <div v-else-if="isAuthenticated && userComment && isEditing" class="comment-form">
      <textarea
          v-model="editText"
          placeholder="Редактируйте ваш комментарий..."
          class="comment-textarea"
          rows="3"
          maxlength="1000"
      ></textarea>
      <div class="comment-form-actions">
        <span class="char-counter">{{ editText.length }}/1000</span>
        <div class="edit-buttons">
          <button @click="cancelEdit" class="btn-cancel">Отмена</button>
          <button
              @click="handleUpdate"
              :disabled="!editText.trim() || submitting"
              class="btn-submit-comment"
          >
            {{ submitting ? 'Сохранение...' : 'Сохранить' }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="!isAuthenticated" class="auth-message">
      <p>Войдите, чтобы оставить комментарий</p>
    </div>

    <div v-if="loading" class="loading-comments">
      Загрузка комментариев...
    </div>

    <div v-else-if="comments.length === 0" class="no-comments">
      Пока нет комментариев. Будьте первым!
    </div>

    <div v-else class="comments-list">
      <div
          v-for="comment in comments"
          :key="comment.id"
          class="comment-item"
          :class="{
          'own-comment': comment.userId === currentUserId,
          'deleted': comment.deletedAt
        }"
      >
        <div class="comment-header">
          <div class="comment-author">
            <span class="author-icon">👤</span>
            <span class="author-name">{{ comment.userName }}</span>
            <span v-if="comment.userId === currentUserId" class="own-badge">Вы</span>
          </div>
          <div class="comment-date">
            {{ formatDate(comment.createdAt) }}
          </div>
        </div>

        <div class="comment-text" :class="{ 'deleted-text': comment.deletedAt }">
          <span v-if="comment.deletedAt" class="deleted-label">[Удалено]</span>
          {{ comment.deletedAt ? '' : comment.text }}
        </div>

        <!-- Действия для своих комментариев -->
        <div
            v-if="!comment.deletedAt && comment.userId === currentUserId && !isEditing"
            class="comment-actions"
        >
          <button @click="startEdit(comment)" class="btn-edit">Редактировать</button>
          <button @click="handleDelete(comment.id)" class="btn-delete">Удалить</button>
        </div>

        <!-- Действия модератора -->
        <div v-if="isModerator && comment.userId !== currentUserId" class="comment-actions">
          <button
              v-if="!comment.deletedAt"
              @click="handleModerateDelete(comment.id)"
              class="btn-moderate-delete"
          >
            🗑️ Удалить (модератор)
          </button>
          <button
              v-else
              @click="handleRestore(comment.id)"
              class="btn-restore"
          >
            ↩️ Восстановить
          </button>
        </div>
      </div>
    </div>

    <button
        v-if="hasMore"
        @click="$emit('load-more')"
        class="btn-load-more"
        :disabled="loading"
    >
      {{ loading ? 'Загрузка...' : 'Показать больше' }}
    </button>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { formatDate } from '@/utils/formatters'

const props = defineProps({
  comments: {
    type: Array,
    required: true
  },
  loading: {
    type: Boolean,
    default: false
  },
  submitting: {
    type: Boolean,
    default: false
  },
  isAuthenticated: {
    type: Boolean,
    required: true
  },
  currentUserId: {
    type: Number,
    default: null
  },
  isModerator: {
    type: Boolean,
    default: false
  },
  hasMore: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits([
  'submit',
  'update',
  'delete',
  'moderate-delete',
  'restore',
  'load-more'
])

const newCommentText = ref('')
const editText = ref('')
const isEditing = ref(false)
const editingCommentId = ref(null)

const userComment = computed(() => {
  return props.comments.find(c =>
      c.userId === props.currentUserId && !c.deletedAt
  )
})

function handleSubmit() {
  if (newCommentText.value.trim()) {
    emit('submit', newCommentText.value.trim())
    newCommentText.value = ''
  }
}

function startEdit(comment) {
  isEditing.value = true
  editingCommentId.value = comment.id
  editText.value = comment.text
}

function cancelEdit() {
  isEditing.value = false
  editingCommentId.value = null
  editText.value = ''
}

function handleUpdate() {
  if (editText.value.trim()) {
    emit('update', {
      id: editingCommentId.value,
      text: editText.value.trim()
    })
    cancelEdit()
  }
}

function handleDelete(id) {
  if (confirm('Удалить комментарий?')) {
    emit('delete', id)
  }
}

function handleModerateDelete(id) {
  if (confirm('Удалить этот комментарий как модератор?')) {
    emit('moderate-delete', id)
  }
}

function handleRestore(id) {
  if (confirm('Восстановить этот комментарий?')) {
    emit('restore', id)
  }
}
</script>

<style scoped>
.comments-section {
  margin-top: 32px;
  padding-top: 24px;
  border-top: 2px solid #e0e0e0;
}

.comments-title {
  font-size: 20px;
  font-weight: 600;
  color: #333;
  margin-bottom: 16px;
}

.comment-form {
  margin-bottom: 24px;
  background-color: #f9f9f9;
  padding: 16px;
  border-radius: 8px;
}

.comment-textarea {
  width: 100%;
  padding: 12px;
  font-size: 14px;
  font-family: inherit;
  border: 2px solid #e0e0e0;
  border-radius: 6px;
  resize: vertical;
  min-height: 80px;
  outline: none;
  box-sizing: border-box;
  transition: border-color 0.2s;
}

.comment-textarea:focus {
  border-color: #2196F3;
}

.comment-form-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
}

.char-counter {
  font-size: 12px;
  color: #999;
}

.edit-buttons {
  display: flex;
  gap: 8px;
}

.btn-submit-comment {
  padding: 10px 20px;
  background-color: #2196F3;
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.2s;
}

.btn-submit-comment:hover:not(:disabled) {
  background-color: #1976D2;
}

.btn-submit-comment:disabled {
  background-color: #ccc;
  cursor: not-allowed;
}

.btn-cancel {
  padding: 10px 20px;
  background-color: #fff;
  color: #666;
  border: 2px solid #e0e0e0;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-cancel:hover {
  background-color: #f5f5f5;
}

.auth-message {
  padding: 16px;
  background-color: #fff3cd;
  border: 1px solid #ffeeba;
  border-radius: 6px;
  color: #856404;
  text-align: center;
  margin-bottom: 16px;
}

.loading-comments,
.no-comments {
  padding: 24px;
  text-align: center;
  color: #999;
  font-size: 14px;
}

.comments-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.comment-item {
  background-color: #fff;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  padding: 16px;
  transition: box-shadow 0.2s;
}

.comment-item:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.comment-item.own-comment {
  background-color: #e3f2fd;
  border-color: #2196F3;
}

.comment-item.deleted {
  opacity: 0.6;
  background-color: #f5f5f5;
}

.comment-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.comment-author {
  display: flex;
  align-items: center;
  gap: 8px;
}

.author-icon {
  font-size: 18px;
}

.author-name {
  font-weight: 600;
  color: #333;
  font-size: 15px;
}

.own-badge {
  background-color: #2196F3;
  color: white;
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 600;
}

.comment-date {
  font-size: 12px;
  color: #999;
}

.comment-text {
  color: #555;
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-wrap: break-word;
}

.comment-text.deleted-text {
  font-style: italic;
  color: #999;
}

.deleted-label {
  color: #d32f2f;
  font-weight: 600;
  margin-right: 4px;
}

.comment-actions {
  margin-top: 12px;
  display: flex;
  gap: 8px;
}

.btn-edit,
.btn-delete,
.btn-moderate-delete,
.btn-restore {
  padding: 6px 12px;
  font-size: 13px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-weight: 500;
  transition: all 0.2s;
}

.btn-edit {
  background-color: #fff;
  color: #2196F3;
  border: 1px solid #2196F3;
}

.btn-edit:hover {
  background-color: #e3f2fd;
}

.btn-delete {
  background-color: #fff;
  color: #d32f2f;
  border: 1px solid #d32f2f;
}

.btn-delete:hover {
  background-color: #ffebee;
}

.btn-moderate-delete {
  background-color: #ff9800;
  color: white;
}

.btn-moderate-delete:hover {
  background-color: #f57c00;
}

.btn-restore {
  background-color: #4CAF50;
  color: white;
}

.btn-restore:hover {
  background-color: #388E3C;
}

.btn-load-more {
  width: 100%;
  padding: 12px;
  margin-top: 16px;
  background-color: #fff;
  color: #2196F3;
  border: 2px solid #2196F3;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-load-more:hover:not(:disabled) {
  background-color: #e3f2fd;
}

.btn-load-more:disabled {
  color: #ccc;
  border-color: #ccc;
  cursor: not-allowed;
}
</style>