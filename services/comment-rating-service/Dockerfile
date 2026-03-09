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
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=build /build/comment-rating-service/target/*.jar app.jar

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8082/actuator/health || exit 1

EXPOSE 8082

ENV JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8"
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
