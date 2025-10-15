
import { createApp } from 'vue';
import App from './App.vue';
import router from './router';
import axios from 'axios';






import '@/assets/styles/base.css'
import '@/assets/styles/main.css'
import '@/assets/styles/library-common.css'
import '@/assets/styles/admin-panel.css'


const apiClient = axios.create({
    baseURL: 'http://localhost:8080/api',
    withCredentials: true
});


const app = createApp(App);


app.config.globalProperties.$http = apiClient;

app.use(router).mount('#app');