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
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

COPY --from=build /build/storage-service/target/*.jar app.jar

# Create uploads directory
RUN mkdir -p /app/uploads

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "app.jar"]
