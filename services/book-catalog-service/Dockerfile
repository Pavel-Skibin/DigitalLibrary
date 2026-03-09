# ========== Stage 1: Build ==========
# Context: project root (needed for shared/common-models dependency)
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /build

# Build shared common-models first (book-catalog-service depends on it)
COPY shared/common-models ./common-models
WORKDIR /build/common-models
RUN mvn clean install -DskipTests -q

# Build book-catalog-service
WORKDIR /build/book-catalog-service
COPY services/book-catalog-service/pom.xml .
RUN mvn dependency:go-offline -B -q

# Copy OpenAPI specs from sibling services (required by openapi-generator-maven-plugin)
# pom.xml references: ${project.basedir}/../comment-rating-service/... and ../user-service/...
COPY services/comment-rating-service/src/main/resources/openapi /build/comment-rating-service/src/main/resources/openapi
COPY services/user-service/src/main/resources/openapi /build/user-service/src/main/resources/openapi

COPY services/book-catalog-service/src ./src
RUN mvn clean package -DskipTests -q

# ========== Stage 2: Runtime ==========
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=build /build/book-catalog-service/target/*.jar app.jar

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8091/actuator/health || exit 1

EXPOSE 8091

ENV JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8"
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]