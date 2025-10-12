<template>
  <div class="data-table">
    <table>
      <thead>
      <tr>
        <th v-for="column in columns" :key="column.key">
          {{ column.label }}
        </th>
        <th v-if="hasActions">Действия</th>
      </tr>
      </thead>
      <tbody>
      <tr v-for="row in data" :key="row.id">
        <td v-for="column in columns" :key="column.key">
          <slot :name="`cell-${column.key}`" :row="row">
            {{ row[column.key] }}
          </slot>
        </td>
        <td v-if="hasActions" class="actions">
          <slot name="actions" :row="row"></slot>
        </td>
      </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup>
defineProps({
  columns: {
    type: Array,
    required: true
  },
  data: {
    type: Array,
    required: true
  },
  hasActions: {
    type: Boolean,
    default: true
  }
})
</script>