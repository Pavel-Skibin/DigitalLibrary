<template>
  <div class="search-box">
    <input
        :value="modelValue"
        @input="$emit('update:modelValue', $event.target.value)"
        type="text"
        :placeholder="placeholder"
        class="search-input"
    />
  </div>
</template>

<script setup>
defineProps({
  modelValue: {
    type: String,
    default: ''
  },
  placeholder: {
    type: String,
    default: '🔍 Поиск...'
  }
})

defineEmits(['update:modelValue'])
</script>

<style scoped>
.search-box {
  display: flex;
  width: 100%;
}

.search-input {
  width: 100%;
  height: 48px;
  padding: 0 16px;
  font-size: 15px;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  outline: none;
  transition: border-color 0.2s;
  box-sizing: border-box;
}

.search-input:focus {
  border-color: #2196F3;
}
</style>