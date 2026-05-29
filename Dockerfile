# Stage 1: Build
FROM docker.io/library/eclipse-temurin:25-jdk-alpine AS builder

WORKDIR /app

# Copy Gradle wrapper and build files
COPY gradlew .
COPY gradle/ gradle/
COPY build.gradle .
COPY settings.gradle .

# Make gradlew executable
RUN chmod +x gradlew

# Download dependencies (layer caching)
RUN ./gradlew dependencies --no-daemon || true

# Copy source code
COPY src/ src/

# Build the application (skip tests for Docker build)
RUN ./gradlew bootJar --no-daemon -x test -x spotlessApply

# Stage 2: Runtime
FROM docker.io/library/eclipse-temurin:25-jre-alpine

WORKDIR /app

# Create non-root user
RUN addgroup -S sentinel && adduser -S sentinel -G sentinel

# Copy JAR from builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Set ownership
RUN chown -R sentinel:sentinel /app

USER sentinel

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

# Run with preview features enabled (required for StructuredTaskScope)
ENTRYPOINT ["java", "--enable-preview", "-jar", "app.jar"]
