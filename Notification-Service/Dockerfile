# Stage 1: Compilation Maven
FROM maven:3.9-eclipse-temurin-17-alpine AS builder
WORKDIR /build
COPY pom.xml .
# Résoudre les dépendances Maven
RUN mvn dependency:resolve
COPY . .
# Compiler et packager l'application (passer les tests)
RUN mvn clean package -DskipTests

# Stage 2: Runtime - Notification Service
FROM eclipse-temurin:17-jre-alpine
# Créer utilisateur non-root pour la sécurité
RUN adduser --disabled-password --gecos '' appuser
WORKDIR /app
# Copier le JAR depuis le stage de compilation
COPY --from=builder /build/target/*.jar app.jar
# Définir les permissions
RUN chown -R appuser:appuser /app

# Exécuter en tant qu'utilisateur non-privilégié
USER appuser
EXPOSE 8084

# Démarrer l'application JAR
ENTRYPOINT ["java", "-jar", "app.jar"]
