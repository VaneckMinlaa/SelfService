# Stage 1: Compilation Maven
FROM maven:3.9-eclipse-temurin-17-alpine AS builder
WORKDIR /build
COPY pom.xml .
# Résoudre les dépendances Maven
RUN mvn dependency:resolve
COPY . .
# Compiler et packager l'application (passer les tests)
RUN mvn clean package -DskipTests

# Stage 2: Runtime - Auth Service
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
EXPOSE 8081
# Health check pour vérifier la disponibilité du service
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD sh -c 'curl -f http://localhost:8081/actuator/health || exit 1'

# Démarrer l'application JAR
ENTRYPOINT ["java", "-jar", "app.jar"]
