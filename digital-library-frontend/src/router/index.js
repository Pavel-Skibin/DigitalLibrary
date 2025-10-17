import { createRouter, createWebHistory } from 'vue-router';
import BooksView from '../views/BooksView.vue';
import AuthorsView from '../views/AuthorsView.vue';
import AuthorBooksView from '../views/AuthorBooksView.vue';
import HomeView from '../views/HomeView.vue';
import LoginRegisterView from '../views/LoginRegisterView.vue';
import AdminPanelView from '../views/AdminPanelView.vue';

const routes = [
  {
    path: '/',
    name: 'Home',
    component: HomeView,
    meta: { requiresAuth: false } // ← Гостям разрешён доступ
  },
  {
    path: '/books',
    name: 'Books',
    component: BooksView,
    meta: { requiresAuth: false } // ← Гостям разрешён доступ
  },
  {
    path: '/authors',
    name: 'Authors',
    component: AuthorsView,
    meta: { requiresAuth: false } // ← Гостям разрешён доступ
  },
  {
    path: '/authors/:authorId/books',
    name: 'AuthorBooks',
    component: AuthorBooksView,
    props: true,
    meta: { requiresAuth: false } // ← Гостям разрешён доступ
  },
  {
    path: '/login',
    name: 'login',
    component: LoginRegisterView,
    meta: { requiresAuth: false, guestOnly: true } // ← Только для неавторизованных
  },
  {
    path: '/admin',
    name: 'AdminPanel',
    component: AdminPanelView,
    meta: { requiresAuth: true, requiresRole: ['ROLE_MODERATOR', 'ROLE_ADMIN'] }
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});


router.beforeEach((to, from, next) => {
  const jwt = getCookie('jwt');
  const isAuthenticated = !!jwt;

  // Если страница требует авторизации
  if (to.meta.requiresAuth && !isAuthenticated) {
    next('/login');
    return;
  }

  // Если авторизованный пользователь пытается зайти на страницу логина
  if (to.meta.guestOnly && isAuthenticated) {
    next('/');
    return;
  }

  next();
});

function getCookie(name) {
  const value = `; ${document.cookie}`;
  const parts = value.split(`; ${name}=`);
  if (parts.length === 2) return parts.pop().split(';').shift();
  return null;
}

export default router;