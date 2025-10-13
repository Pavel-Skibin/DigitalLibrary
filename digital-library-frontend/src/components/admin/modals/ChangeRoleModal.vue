<template>
  <div class="modal-overlay" @click="$emit('close')">
    <div class="modal-content" @click.stop>
      <h2 class="modal-title">Изменить роль пользователя</h2>
      <p><strong>Пользователь:</strong> {{ user?.username }}</p>

      <div class="form-group">
        <label>Выберите роль</label>
        <select v-model="selectedRoleId" class="form-input">
          <option v-for="role in roles" :key="role.id" :value="role.id">
            {{ formatRole(role.name) }}
          </option>
        </select>
      </div>

      <div class="modal-buttons">
        <button @click="$emit('close')" class="btn-secondary">Отмена</button>
        <button
            @click="handleSave"
            class="btn-primary"
            :disabled="loading"
        >
          {{ loading ? 'Сохранение...' : 'Сохранить' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { formatRole } from '@/utils/formatters'

const props = defineProps({
  user: {
    type: Object,
    default: null
  },
  loading: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['close', 'save'])

const roles = [
  { id: 1, name: 'ADMIN' },
  { id: 2, name: 'MODERATOR' },
  { id: 3, name: 'USER' }
]

const selectedRoleId = ref(null)

watch(() => props.user, (newUser) => {
  if (newUser) {
    const currentRole = roles.find(r => r.name === newUser.roleName || `ROLE_${r.name}` === newUser.roleName)
    selectedRoleId.value = currentRole ? currentRole.id : 3
  }
}, { immediate: true })

function handleSave() {
  emit('save', selectedRoleId.value)
}
</script>