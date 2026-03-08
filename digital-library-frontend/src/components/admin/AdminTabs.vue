<template>
  <div class="tabs">
    <button
      v-for="tab in availableTabs"
      :key="tab.id"
      @click="$emit('update:modelValue', tab.id)"
      :class="{
        active: modelValue === tab.id,
        'admin-only': tab.adminOnly,
      }"
      class="tab-button"
    >
      {{ tab.icon }} {{ tab.label }}
    </button>
  </div>
</template>

<script setup>
import { computed } from "vue";

const props = defineProps({
  modelValue: {
    type: String,
    required: true,
  },
  isAdmin: {
    type: Boolean,
    default: false,
  },
});

defineEmits(["update:modelValue"]);

const tabs = [
  { id: "authors", label: "Авторы", icon: "👤", adminOnly: false },
  { id: "genres", label: "Жанры", icon: "🏷️", adminOnly: false },
  { id: "books", label: "Книги", icon: "📚", adminOnly: false },
  { id: "comments", label: "Комментарии", icon: "💬", adminOnly: false },
  { id: "users", label: "Пользователи", icon: "👥", adminOnly: true },
  { id: "statistics", label: "Статистика", icon: "📊", adminOnly: false },
  { id: "settings", label: "Настройки", icon: "⚙️", adminOnly: true },
];

const availableTabs = computed(() =>
  props.isAdmin ? tabs : tabs.filter((t) => !t.adminOnly),
);
</script>
