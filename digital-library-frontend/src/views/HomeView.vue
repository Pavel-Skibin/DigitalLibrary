<template>
  <div class="app-layout">
    <Header />
    <div class="library-container">
      <AppSidebar :show-admin-link="isModeratorOrAdmin" />

      <main class="home-content">
        <!-- Book Recommendations Section -->
        <div class="content-section">
          <!-- Personalized Recommendations (only for authenticated users) -->
          <PersonalRecommendations v-if="isAuthenticated" />

          <!-- Popular Books (for all users) -->
          <PopularBooks />
        </div>

        <div class="content-section">
          <LibraryStats />
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from "vue";
import Header from "@/components/layout/Header.vue";
import AppSidebar from "@/components/layout/AppSidebar.vue";
import LibraryStats from "@/components/home/LibraryStats.vue";
import PersonalRecommendations from "@/components/home/PersonalRecommendations.vue";
import PopularBooks from "@/components/home/PopularBooks.vue";
import { useAdminAuth } from "@/composables/useAdminAuth";
import { getCookie } from "@/utils/cookies";

const { isAdmin, isModerator, checkAccess } = useAdminAuth();
const isModeratorOrAdmin = ref(false);

// Check if user is authenticated
const isAuthenticated = computed(() => !!getCookie("jwt"));

onMounted(async () => {
  await checkAccess();
  isModeratorOrAdmin.value = isAdmin.value || isModerator.value;
});
</script>

<style scoped src="@/assets/styles/library-common.css"></style>
<style scoped>
.home-content {
  flex: 1;
  padding: 40px 0 0 0;
  background: linear-gradient(180deg, #ffffff 0%, #f5f7fa 100%);
  overflow-x: hidden; /* Prevent horizontal scroll */
  overflow-y: auto; /* Enable vertical scroll */
}

.content-section {
  padding: 0 30px;
  max-width: 1400px;
  margin: 0 auto;
}

/* Responsive Design */

/* Large Desktops (27" screens, 1920px+) */
@media (min-width: 1920px) {
  .content-section {
    padding: 0 60px;
    max-width: 1800px;
  }
}

/* Desktop (1400px - 1920px) */
@media (max-width: 1920px) and (min-width: 1400px) {
  .content-section {
    padding: 0 35px;
    max-width: 1300px;
  }
}

/* Laptop (1024px - 1400px) */
@media (max-width: 1399px) and (min-width: 1024px) {
  .content-section {
    padding: 0 25px;
    max-width: 1100px;
  }
}

/* Tablet (768px - 1024px) */
@media (max-width: 1023px) and (min-width: 768px) {
  .content-section {
    padding: 0 20px;
    max-width: 900px;
  }
}

/* Mobile / Small Tablet (480px - 768px) */
@media (max-width: 767px) and (min-width: 480px) {
  .content-section {
    padding: 0 18px;
  }
}

/* Small Mobile (< 480px) */
@media (max-width: 479px) {
  .content-section {
    padding: 0 12px;
  }
}
</style>
