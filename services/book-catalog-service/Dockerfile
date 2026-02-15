# ========== Stage 1: Build ==========
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Копируем pom.xml и скачиваем зависимости (кэширование слоёв)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Копируем исходники и собираем JAR
COPY src ./src
RUN mvn clean package -DskipTests

# ========== Stage 2: Runtime ==========
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Создаём непривилегированного пользователя
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Копируем JAR из build stage
COPY --from=build /app/target/*.jar app.jar

# Healthcheck
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Порт
EXPOSE 8080

# JVM параметры (можно переопределить через ENV)
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# Запуск
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]