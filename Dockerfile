# ==============================================================================
# Multi-Stage Dockerfile for FinInsight Backend
# ==============================================================================

# --- Stage 1: Build & Package ---
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /workspace

# Copy Maven wrapper & POM to leverage Docker cache
COPY pom.xml mvnw ./
COPY .mvn .mvn

# Download dependencies offline
RUN chmod +x ./mvnw && ./mvnw dependency:go-offline -B

# Copy application source code
COPY src src

# Build executable JAR without running tests (tests run in CI/CD pipeline)
RUN ./mvnw clean package -DskipTests -B

# --- Stage 2: Minimal Production Runtime ---
FROM eclipse-temurin:21-jre-alpine AS runner

# Install curl/wget for health check
RUN apk --no-cache add wget

# Create non-root user and group for security
RUN addgroup -S fininsight && adduser -S fininsight -G fininsight

WORKDIR /app

# Copy built JAR from build stage
COPY --from=builder /workspace/target/*.jar app.jar

# Set ownership to non-root user
RUN chown -R fininsight:fininsight /app

# Switch to non-root user
USER fininsight:fininsight

# Container-aware JVM memory configuration
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0 -Djava.security.egd=file:/dev/./urandom"
ENV SERVER_PORT=8080

EXPOSE 8080

# Health check via Spring Boot Actuator
HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
