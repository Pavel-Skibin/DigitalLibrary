import { createApp } from "vue";
import App from "./App.vue";
import router from "./router";
import axios from "axios";

import "@/assets/styles/base.css";
import "@/assets/styles/main.css";
import "@/assets/styles/library-common.css";
import "@/assets/styles/admin-panel.css";

// API Base URL - в production через Nginx proxy, в development через Vite proxy → API Gateway (8080)
const apiBaseURL =
  import.meta.env.MODE === "production"
    ? "/api" // Nginx проксирует /api на http://api-gateway:8080
    : "/api"; // Vite proxy из vite.config.js → http://localhost:8080 (API Gateway)

const apiClient = axios.create({
  baseURL: apiBaseURL,
  withCredentials: true,
});

const app = createApp(App);

app.config.globalProperties.$http = apiClient;

app.use(router).mount("#app");
