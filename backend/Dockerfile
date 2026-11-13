# ==============================================================================
# Virtual Classroom Platform - Backend Multi-Stage Production Dockerfile
# ==============================================================================

# ------------------------------------------------------------------------------
# Stage 1: Build JAR artifact using Maven Wrapper and Temurin JDK 21
# ------------------------------------------------------------------------------
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Install curl for wrapper downloads if necessary
RUN apk add --no-cache curl

# Copy Maven Wrapper configuration and project descriptor
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Ensure Unix line endings and executable permissions on mvnw
RUN tr -d '\r' < mvnw > mvnw.clean && mv mvnw.clean mvnw && chmod +x mvnw

# Pre-fetch dependencies to leverage Docker layer caching
RUN ./mvnw dependency:go-offline -B

# Copy application source code
COPY src/ src/

# Build production executable JAR (skipping unit tests during image build)
RUN ./mvnw clean package -DskipTests -B

# ------------------------------------------------------------------------------
# Stage 2: Minimal, hardened production runtime image
# ------------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine AS runner

WORKDIR /app

# Create unprivileged system group and user
RUN addgroup -S appgroup -g 10001 && \
    adduser -S appuser -u 10001 -G appgroup -h /app -s /sbin/nologin

# Create application uploads directory and grant ownership
RUN mkdir -p /app/uploads && chown -R appuser:appgroup /app

# Copy executable jar from builder stage
COPY --from=builder --chown=appuser:appgroup /app/target/*.jar app.jar

# Switch to unprivileged runtime user
USER appuser

# Environment defaults (overridable at container runtime)
ENV PORT=8080
ENV SPRING_PROFILES_ACTIVE=docker
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"

# Expose HTTP port
EXPOSE 8080

# Container healthcheck: verify Spring Boot Actuator health endpoint
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:${PORT:-8080}/actuator/health || exit 1

# Launch Spring Boot application with graceful signal propagation
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
