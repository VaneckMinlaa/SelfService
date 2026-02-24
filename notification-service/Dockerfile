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
EXPOSE 8084

ENTRYPOINT ["java", "-jar", "app.jar"]
