# ==============================================================================
# Stage 1: Build Stage (JDK 21 + Maven Layer Caching)
# ==============================================================================
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace/app

# Copy Maven wrapper and pom.xml first to leverage Docker layer caching
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Download dependencies in a cached layer
RUN chmod +x ./mvnw && ./mvnw dependency:go-offline -B

# Copy source code and build layered JAR
COPY src src
RUN ./mvnw clean package -DskipTests -B && \
    java -Djarmode=layertools -jar target/*.jar extract --destination target/extracted

# ==============================================================================
# Stage 2: Production Runtime (Non-Root JRE 21 + Generational ZGC)
# ==============================================================================
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create non-root user for security compliance
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy extracted Spring Boot layers in order of least-to-most frequent changes
COPY --from=builder /workspace/app/target/extracted/dependencies/ ./
COPY --from=builder /workspace/app/target/extracted/spring-boot-loader/ ./
COPY --from=builder /workspace/app/target/extracted/snapshot-dependencies/ ./
COPY --from=builder /workspace/app/target/extracted/application/ ./

EXPOSE 8080

# JVM flags tuned for container memory awareness and low-latency Generational ZGC
ENTRYPOINT ["java", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:+UseZGC", \
  "-XX:+ZGenerational", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "org.springframework.boot.loader.launch.JarLauncher"]
