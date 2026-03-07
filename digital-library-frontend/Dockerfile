# ========== Stage 1: Build ==========
FROM node:20-alpine AS build

WORKDIR /app

# Копируем package.json и package-lock.json
COPY package*.json ./

# Устанавливаем зависимости
RUN npm ci

# Копируем исходники
COPY . .

# Собираем продакшн билд
RUN npm run build

# ========== Stage 2: Production ==========
FROM nginx:1.25-alpine

# Копируем собранные файлы из Stage 1
COPY --from=build /app/dist /usr/share/nginx/html

# Копируем статические библиотеки (foliate-js и др.) — Vite их не обрабатывает
COPY --from=build /app/libs /usr/share/nginx/html/libs

# Копируем конфигурацию Nginx
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Открываем порт
EXPOSE 80

# Запускаем Nginx
CMD ["nginx", "-g", "daemon off;"]