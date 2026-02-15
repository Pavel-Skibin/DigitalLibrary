# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Copy shared common-models (dependency)  
COPY shared/common-models /build/common-models
WORKDIR /build/common-models
RUN mvn clean install -DskipTests

# Copy and build comment-rating-service
WORKDIR /build/comment-rating-service
COPY services/comment-rating-service/pom.xml .
RUN mvn dependency:go-offline

COPY services/comment-rating-service/src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

COPY --from=build /build/comment-rating-service/target/*.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]
