<template>
  <div class="app-layout">
    <Header/>
    <div class="library-container">
      <AppSidebar :show-admin-link="true"/>

      <main class="content admin-content">
        <h1 class="page-title">
          <span class="icon">⚙️</span>
          Панель {{ isAdmin ? 'администратора' : 'модератора' }}
        </h1>

        <AdminTabs v-model="activeTab" :is-admin="isAdmin"/>

        <component :is="currentTabComponent"/>
      </main>
    </div>
  </div>
</template>

<script setup>
import {ref, computed, onMounted} from 'vue'
import {useRouter} from 'vue-router'
import Header from '@/components/layout/Header.vue'
import AppSidebar from '@/components/layout/AppSidebar.vue'
import AdminTabs from '@/components/admin/AdminTabs.vue'

import AuthorsTab from '@/components/admin/tabs/AuthorsTab.vue'
import GenresTab from '@/components/admin/tabs/GenresTab.vue'
import BooksTab from '@/components/admin/tabs/BooksTab.vue'
import CommentsTab from '@/components/admin/tabs/CommentsTab.vue'
import UsersTab from '@/components/admin/tabs/UsersTab.vue'
import StatisticsTab from '@/components/admin/tabs/StatisticsTab.vue'

import {useAdminAuth} from '@/composables/useAdminAuth'

const router = useRouter()
const {isAdmin, checkAccess} = useAdminAuth()

const activeTab = ref('authors')

const currentTabComponent = computed(() => {
  const tabs = {
    authors: AuthorsTab,
    genres: GenresTab,
    books: BooksTab,
    comments: CommentsTab,
    users: UsersTab,
    statistics: StatisticsTab
  }
  return tabs[activeTab.value]
})

onMounted(async () => {
  const hasAccess = await checkAccess()
  if (!hasAccess) {
    alert('У вас нет доступа к панели управления')
    router.push('/')
  }
})
</script>

