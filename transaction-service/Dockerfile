FROM maven:3.9-eclipse-temurin-17-alpine AS builder
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:resolve
COPY . .
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
RUN adduser --disabled-password --gecos '' appuser
WORKDIR /app
COPY --from=builder /build/target/*.jar app.jar
RUN chown -R appuser:appuser /app

USER appuser
EXPOSE 8083
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD sh -c 'curl -f http://localhost:8083/actuator/health || exit 1'

ENTRYPOINT ["java", "-jar", "app.jar"]
