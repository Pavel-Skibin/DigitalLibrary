import { createApp } from 'vue';
import App from './App.vue';
import router from './router';
import axios from 'axios';

import '@/assets/styles/base.css'
import '@/assets/styles/main.css'
import '@/assets/styles/library-common.css'
import '@/assets/styles/admin-panel.css'


const apiBaseURL = import.meta.env.MODE === 'production'
    ? '/api'
    : 'http://localhost:8080/api';

const apiClient = axios.create({
    baseURL: apiBaseURL,
    withCredentials: true
});

const app = createApp(App);

app.config.globalProperties.$http = apiClient;

app.use(router).mount('#app');