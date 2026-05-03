# ============================================
# E-COMMERCE BACKEND - MULTI-STAGE DOCKERFILE
# Java 21 LTS + Spring Boot
# ============================================

# ═══════════════════════════════════════════════════════════
# BUILD STAGE - Compile and build the application
# ═══════════════════════════════════════════════════════════
FROM gradle:8.14-jdk21 AS builder

WORKDIR /app

# Copy Gradle wrapper and build files
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./

# Copy source code
COPY src src

# Build the application (skip tests for faster builds)
# Use --no-daemon to avoid Gradle daemon in Docker
RUN gradle bootJar --no-daemon

# ═══════════════════════════════════════════════════════════
# RUNTIME STAGE - Run the application
# ═══════════════════════════════════════════════════════════
FROM eclipse-temurin:21-jre

# Set working directory
WORKDIR /app

# Create non-root user for security
RUN groupadd -g 1001 eshop && \
    useradd -u 1001 -g eshop -s /bin/sh eshop

# Create necessary directories with proper permissions
RUN mkdir -p /var/log/eshop /var/eshop/uploads && \
    chown -R eshop:eshop /var/log/eshop /var/eshop /app

# Copy built JAR from builder stage
COPY --from=builder --chown=eshop:eshop /app/build/libs/*.jar app.jar

# Switch to non-root user
USER eshop

# Health check - verify application is running
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD bash -c "exec 3<>/dev/tcp/localhost/8082" || exit 1

# Expose application port
EXPOSE 8082

# JVM Options for containerized environment
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+UseG1GC \
    -XX:+UseStringDeduplication \
    -Djava.security.egd=file:/dev/./urandom"

# Run application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
