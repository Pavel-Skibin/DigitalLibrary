# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Copy shared common-models (dependency)
COPY shared/common-models /build/common-models
WORKDIR /build/common-models
RUN mvn clean install -DskipTests

# Copy and build storage-service
WORKDIR /build/storage-service
COPY services/storage-service/pom.xml .
RUN mvn dependency:go-offline

COPY services/storage-service/src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=build /build/storage-service/target/*.jar app.jar

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8083/actuator/health || exit 1

EXPOSE 8083

ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
