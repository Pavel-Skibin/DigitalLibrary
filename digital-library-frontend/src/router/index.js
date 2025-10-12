
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
    component: HomeView
  },
  {
    path: '/books',
    name: 'Books',
    component: BooksView
  },
  {
    path: '/authors',
    name: 'Authors',
    component: AuthorsView
  },
  {
    path: '/authors/:authorId/books',
    name: 'AuthorBooks',
    component: AuthorBooksView,
    props: true
  },
  {
    path: '/login',
    name: 'login',
    component: LoginRegisterView
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
  if (to.meta.requiresAuth) {
    const jwt = getCookie('jwt');
    if (!jwt) {
      next('/login');
      return;
    }
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