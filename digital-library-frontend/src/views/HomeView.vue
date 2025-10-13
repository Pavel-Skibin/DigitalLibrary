<template>
  <div class="app-layout">
    <Header />
    <div class="library-container">
      <AppSidebar :show-admin-link="isModeratorOrAdmin" />

      <main class="home-content">
        <WelcomeSection />
        <LibraryStats />
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import Header from '@/components/layout/Header.vue'
import AppSidebar from '@/components/layout/AppSidebar.vue'
import WelcomeSection from '@/components/home/WelcomeSection.vue'
import LibraryStats from '@/components/home/LibraryStats.vue'
import { useAdminAuth } from '@/composables/useAdminAuth'

const { isAdmin, isModerator, checkAccess } = useAdminAuth()
const isModeratorOrAdmin = ref(false)

onMounted(async () => {
  await checkAccess()
  isModeratorOrAdmin.value = isAdmin.value || isModerator.value
})
</script>

<style scoped src="@/assets/styles/library-common.css"></style>
<style scoped>
.home-content {
  flex: 1;
  padding: 0;
  background: linear-gradient(180deg, #ffffff 0%, #f5f7fa 100%);
}
</style>