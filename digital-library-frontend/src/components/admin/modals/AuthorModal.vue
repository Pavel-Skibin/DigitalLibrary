<template>
  <div class="modal-overlay" @click="$emit('close')">
    <div class="modal-content" @click.stop>
      <h2 class="modal-title">
        {{ author ? 'Редактировать автора' : 'Добавить автора' }}
      </h2>

      <div class="form-group">
        <label>Имя</label>
        <input
            v-model="form.firstName"
            type="text"
            class="form-input"
            placeholder="Введите имя"
        />
      </div>

      <div class="form-group">
        <label>Фамилия</label>
        <input
            v-model="form.lastName"
            type="text"
            class="form-input"
            placeholder="Введите фамилию"
        />
      </div>

      <div class="modal-buttons">
        <button @click="$emit('close')" class="btn-secondary">Отмена</button>
        <button
            @click="handleSave"
            class="btn-primary"
            :disabled="!isValid || loading"
        >
          {{ loading ? 'Сохранение...' : 'Сохранить' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'

const props = defineProps({
  author: {
    type: Object,
    default: null
  },
  loading: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['close', 'save'])

const form = ref({
  firstName: '',
  lastName: ''
})

const isValid = computed(() => {
  return form.value.firstName.trim() && form.value.lastName.trim()
})

watch(() => props.author, (newAuthor) => {
  if (newAuthor) {
    form.value = {
      firstName: newAuthor.firstName,
      lastName: newAuthor.lastName
    }
  } else {
    form.value = { firstName: '', lastName: '' }
  }
}, { immediate: true })

function handleSave() {
  if (isValid.value) {
    emit('save', form.value)
  }
}
</script>