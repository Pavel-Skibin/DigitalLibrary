<template>
  <!-- Dark overlay — shown on mobile when sidebar is open -->
  <Transition name="sidebar-fade">
    <div v-if="isMobileOpen" class="sidebar-overlay" @click="closeMobile" />
  </Transition>

  <!-- Sidebar -->
  <aside class="sidebar" :class="{ 'sidebar--open': isMobileOpen }">
    <img
      src="@/assets/images/welcome-illustration.png"
      alt="Библиотека"
      class="sidebar-logo"
    />
    <nav class="sidebar-nav">
      <router-link
        to="/"
        class="nav-item"
        :class="{ active: $route.path === '/' }"
        @click="closeMobile"
      >
        <span class="icon">🏠</span> Главная
      </router-link>
      <router-link
        to="/books"
        class="nav-item"
        :class="{ active: $route.path === '/books' }"
        @click="closeMobile"
      >
        <span class="icon">📚</span> Книги
      </router-link>
      <router-link
        to="/authors"
        class="nav-item"
        :class="{
          active: $route.path === '/authors' || $route.name === 'AuthorBooks',
        }"
        @click="closeMobile"
      >
        <span class="icon">👤</span> Авторы
      </router-link>
      <router-link
        v-if="showAdminLink"
        to="/admin"
        class="nav-item admin-item"
        :class="{ active: $route.path === '/admin' }"
        @click="closeMobile"
      >
        <span class="icon">⚙️</span> Панель управления
      </router-link>
    </nav>
  </aside>
</template>

<script setup>
import { watch } from "vue";
import { useRoute } from "vue-router";
import { useSidebar } from "@/composables/useSidebar";

const route = useRoute();
const { isMobileOpen, closeMobile } = useSidebar();

// Close sidebar on navigation (mobile UX)
watch(
  () => route.path,
  () => {
    closeMobile();
  },
);

defineProps({
  showAdminLink: {
    type: Boolean,
    default: false,
  },
});
</script>

<style scoped>
/* Overlay backdrop */
.sidebar-overlay {
  position: fixed;
  top: 60px;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.45);
  z-index: 998;
  backdrop-filter: blur(1px);
}

.sidebar-fade-enter-active,
.sidebar-fade-leave-active {
  transition: opacity 0.3s ease;
}

.sidebar-fade-enter-from,
.sidebar-fade-leave-to {
  opacity: 0;
}
</style>
