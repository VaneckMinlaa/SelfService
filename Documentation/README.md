# Banking Portal - Microservices Architecture

## Description du Projet

Plateforme bancaire sécurisée reposant sur une architecture orientée microservices. L'application est composée de 5 services Spring Boot distincts, implémente une authentification JWT, utilise RabbitMQ pour la gestion asynchrone des événements, et garantit l'isolation des données via une instance PostgreSQL par service. L'accès est sécurisé par une passerelle Nginx (Reverse Proxy) imposant le protocole HTTPS.

## Architecture et Composants

### Services Métiers
- **Auth Service** (Port 8081) : Gestion des identités, authentification JWT, journalisation des événements de sécurité.
- **Account Service** (Port 8082) : Gestion des comptes bancaires, vérification des soldes et des droits d'accès.
- **Transaction Service** (Port 8083) : Traitement des virements, historisation et publication des événements sur le bus de messages.
- **Notification Service** (Port 8084) : Traitement asynchrone des notifications client (envoi d'emails via RabbitMQ).
- **Nginx Gateway** (Port 443/80) : Routage HTTP/HTTPS, terminaison TLS et répartition de charge.

### Stack Technologique
- **Frontend** : React 19 + Vite
- **Backend** : Spring Boot 3.2.5 (Java 17)
- **Bases de Données** : PostgreSQL 15 (auth_db, account_db, transaction_db)
- **Message Broker** : RabbitMQ 3.12
- **Observabilité** : Prometheus, Grafana, cAdvisor
- **Conteneurisation** : Docker, Docker Compose

## Sécurité et Conformité

- **Isolation des bases de données** : Le modèle Database-per-service est strictement respecté. Aucune base de données n'est exposée sur le réseau hôte.
- **Authentification Stateless** : Implémentation du protocole JWT (JSON Web Token) avec signature HMAC-SHA512.
- **Traçabilité** : Tout événement critique (connexion, déconnexion, erreur d'authentification) est journalisé en base de données.
- **Gestion des Secrets** : Les identifiants, clés de chiffrement et mots de passe sont exclusivement gérés via variables d'environnement (`.env`).

## Guide d'Exploitation et Déploiement

### Prérequis Système
- Docker Engine >= 24.0
- Docker Compose >= 3.8

### Lancement de l'Environnement

1. **Configuration Initiale**
```bash
cp .env.example .env
# Configurer les paramètres spécifiques à l'environnement dans le fichier .env
```

2. **Construction et Démarrage**
```bash
docker compose build
docker compose up -d
```

3. **Vérification de l'État des Services**
```bash
docker compose ps
# Les services doivent afficher un statut "healthy"
```

### Commandes Utiles (Debugging et Maintenance)

**Visualisation des Logs**
Pour analyser le comportement d'un service spécifique en temps réel :
```bash
docker compose logs -f auth-service
```

**Arrêt de l'Environnement**
Pour stopper les services sans détruire les données persistantes :
```bash
docker compose down
```

**Réinitialisation Complète**
Pour supprimer les conteneurs ainsi que les volumes de données :
```bash
docker compose down -v
```

**Supervision**
L'interface Grafana est accessible sur `http://localhost:3000` (identifiants par défaut : admin/admin) pour la surveillance des métriques CPU/RAM des conteneurs via cAdvisor et Prometheus.

## Intégration Continue (CI/CD)

Le projet intègre un pipeline GitLab CI/CD (`.gitlab-ci.yml`) configuré pour :
1. Construire les images Docker pour chaque microservice.
2. Pousser les images vers un registre privé avec un tag unique basé sur le commit Git.
3. Déployer automatiquement les mises à jour sur l'environnement de production.

## Sauvegardes et Continuité d'Activité

Des scripts dédiés permettent de tester les PRA/PCA (Plan de Reprise d'Activité) :
- `Scripts/backup-db.sh` : Exécute des exports logiques (`pg_dump`) sur chaque instance PostgreSQL.
- `Scripts/restore-db.sh` : Restaure les données à partir des fichiers générés.

---
*Ce document et l'architecture associée sont conformes aux standards de sécurité bancaire en environnement distribué.*
