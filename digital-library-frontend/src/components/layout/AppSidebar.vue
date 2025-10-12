<template>
  <aside class="sidebar">
    <img
        src="@/assets/images/welcome-illustration.png"
        alt="Библиотека"
        class="sidebar-logo"
    />
    <nav class="sidebar-nav">
      <router-link to="/" class="nav-item" :class="{ active: $route.path === '/' }">
        <span class="icon">🏠</span> Главная
      </router-link>
      <router-link to="/books" class="nav-item" :class="{ active: $route.path === '/books' }">
        <span class="icon">📚</span> Книги
      </router-link>
      <router-link
          to="/authors"
          class="nav-item"
          :class="{ active: $route.path === '/authors' || $route.name === 'AuthorBooks' }"
      >
        <span class="icon">👤</span> Авторы
      </router-link>
      <router-link
          v-if="showAdminLink"
          to="/admin"
          class="nav-item admin-item"
          :class="{ active: $route.path === '/admin' }"
      >
        <span class="icon">⚙️</span> Панель управления
      </router-link>
    </nav>
  </aside>
</template>

<script setup>
defineProps({
  showAdminLink: {
    type: Boolean,
    default: false
  }
})
</script>