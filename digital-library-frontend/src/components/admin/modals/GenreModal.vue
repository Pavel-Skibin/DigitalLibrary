<template>
  <div class="modal-overlay" @click="$emit('close')">
    <div class="modal-content" @click.stop>
      <h2 class="modal-title">
        {{ genre ? 'Редактировать жанр' : 'Добавить жанр' }}
      </h2>

      <div class="form-group">
        <label>Название</label>
        <input
            v-model="form.name"
            type="text"
            class="form-input"
            placeholder="Введите название жанра"
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
  genre: {
    type: Object,
    default: null
  },
  loading: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['close', 'save'])

const form = ref({ name: '' })

const isValid = computed(() => form.value.name.trim())

watch(() => props.genre, (newGenre) => {
  form.value.name = newGenre ? newGenre.name : ''
}, { immediate: true })

function handleSave() {
  if (isValid.value) {
    emit('save', form.value)
  }
}
</script>