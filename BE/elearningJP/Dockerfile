# Stage 1: Build Stage
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy Maven wrapper and configuration
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN tr -d '\r' < mvnw > mvnw_unix && mv mvnw_unix mvnw && chmod +x mvnw

# Pre-fetch dependencies for layer caching
RUN ./mvnw dependency:go-offline -B

# Copy application source code and build
COPY src ./src
RUN ./mvnw clean package -DskipTests

# Stage 2: Production Runtime Stage
FROM eclipse-temurin:21-jre-alpine AS runner

WORKDIR /app

# Create non-root system user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy compiled JAR from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Set ownership to non-root user
RUN chown -R appuser:appgroup /app
USER appuser

EXPOSE 8080

ENV SPRING_PROFILES_ACTIVE=dev \
    JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"

# Healthcheck
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/v3/api-docs || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
