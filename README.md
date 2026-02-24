# 🏦 Banking Portal - Microservices Architecture

Application bancaire sécurisée avec **architecture microservices** complète : 5 services Spring Boot indépendants, authentification JWT, RabbitMQ pour la communication asynchrone, PostgreSQL par service, et API Gateway HTTPS.

## 🎯 Objectif

Développer une plateforme bancaire démonontrant les meilleures pratiques en architecture microservices :
- ✅ Séparation des responsabilités (Auth, Account, Transaction, Notification, Audit)
- ✅ Base de données isolée par service (Data isolation)
- ✅ Communication synchrone (REST) et asynchrone (RabbitMQ)
- ✅ Authentification JWT stateless
- ✅ HTTPS obligatoire
- ✅ Secrets en variables d'environnement
- ✅ Containerisation Docker complète
- ✅ Logging et audit centralisé

## 📦 Stack Technologique

### Services
- **Auth Service** (Port 8081) - Authentification JWT, gestion utilisateurs, audit logging
- **Account Service** (Port 8082) - Consultation comptes, soldes, ownership verification
- **Transaction Service** (Port 8083) - Virements, historique, événements RabbitMQ
- **Notification Service** (Port 8084) - Emails asynchrones via RabbitMQ
- **Nginx Gateway** (Port 443/80) - Reverse proxy HTTPS, load balancing

### Infrastructure
- **Frontend**: React 19 + Vite (servi par Nginx)
- **Backend**: Spring Boot 3.2.5 (Java 17)
- **Databases**: PostgreSQL 15 × 3 (auth_db, account_db, transaction_db)
- **Message Broker**: RabbitMQ 3.12
- **Proxy**: Nginx Alpine (HTTPS + routing)
- **Container**: Docker + Docker Compose

## 🚀 Quickstart

### Prérequis
```bash
docker --version       # >= 24.0
docker-compose --version  # >= 3.8
java --version        # >= 17 (optionnel, pour dev)
```

### Installation (3 étapes)

1. **Setup**
```bash
./scripts/setup.sh
```

2. **Build & Start**
```bash
./scripts/build-all.sh
docker-compose up -d
```

3. **Health Check**
```bash
./scripts/health-check.sh
```

### Premiers tests

**Demo users (password: `password123`)**:
- `alice@example.com`
- `bob@example.com`
- `charlie@example.com`

**Frontend**
```bash
open https://localhost
# Ignore SSL warning (self-signed cert)
```

**Test API via curl**
```bash
# Login
curl -X POST https://localhost/api/auth/login \
  -H "Content-Type: application/json" \
  -k \
  -d '{"email":"alice@example.com","password":"password123"}'

# Response: { "token": "eyJ...", "userId": 1, ... }

# Use token to get accounts
TOKEN="eyJ..."
curl -X GET https://localhost/api/accounts \
  -H "Authorization: Bearer $TOKEN" \
  -k
```

**RabbitMQ Management UI**
```
http://localhost:15672
Username: guest
Password: guest
```

## 📂 Structure du Projet

```
Self-Service/
├── 📂 auth-service/              Spring Boot - Auth & JWT
│   ├── src/main/java/com/banking/auth/
│   │   ├── AuthServiceApplication.java
│   │   ├── controller/AuthController.java
│   │   ├── service/AuthService.java
│   │   ├── entity/User.java
│   │   └── util/JwtProvider.java
│   ├── pom.xml
│   └── Dockerfile
│
├── 📂 account-service/           Spring Boot - Account Management
│   ├── src/main/java/com/banking/account/
│   │   ├── controller/AccountController.java
│   │   ├── service/AccountService.java
│   │   └── entity/Account.java
│   └── pom.xml
│
├── 📂 transaction-service/       Spring Boot - Transfers + RabbitMQ
│   ├── src/main/java/com/banking/transaction/
│   │   ├── controller/TransactionController.java
│   │   ├── service/TransactionService.java
│   │   ├── producer/TransferEventProducer.java
│   │   └── config/RabbitMQConfig.java
│   └── pom.xml
│
├── 📂 notification-service/      Spring Boot - Email Notifications
│   ├── src/main/java/com/banking/notification/
│   │   ├── listener/TransferEventListener.java
│   │   ├── service/EmailService.java
│   │   └── config/RabbitMQConfig.java
│   └── pom.xml
│
├── 📂 nginx/                     API Gateway + HTTPS
│   ├── nginx.conf                (Reverse proxy, routing, SSL)
│   ├── ssl/cert.pem             (Self-signed cert)
│   ├── ssl/key.pem              (Private key)
│   └── Dockerfile
│
├── 📂 frontend/                  React Web App
│   ├── src/
│   │   ├── pages/
│   │   ├── components/
│   │   ├── api/client.js         (JWT interceptor)
│   │   └── store/authStore.js    (Auth context)
│   └── package.json
│
├── 📂 database/                  SQL Schemas
│   ├── auth-init.sql             (users, audit_logs)
│   ├── account-init.sql          (accounts)
│   ├── transaction-init.sql      (transactions)
│   └── backups/                  (Automated backups)
│
├── 📂 scripts/                   Utility Scripts
│   ├── setup.sh                  (Initialize project)
│   ├── build-all.sh              (Build Docker images)
│   ├── health-check.sh           (Verify all services)
│   ├── backup-db.sh              (Backup databases)
│   └── restore-db.sh             (Restore from backup)
│
├── 📂 docs/                      Documentation
│   ├── ARCHITECTURE.md           (Detailed diagrams)
│   ├── SECURITY.md               (Security checklist)
│   ├── API.md                    (Endpoint documentation)
│   └── TROUBLESHOOTING.md        (FAQ & solutions)
│
├── docker-compose.yml            (Orchestration)
├── .env.example                  (Config template)
├── .gitignore                    (Git exclusions)
└── README.md                     (This file)
```

## 🔐 Sécurité

### JWT Authentication
```
Frontend → Nginx → {Auth/Account/Transaction Service}
         (HTTPS, TLS 1.3)

1. Client POST /api/auth/login
   ↓
2. Auth Service généère JWT token
   { userId: 1, email: "alice@example.com", exp: 3600 }
   ↓
3. Client stocke token en localStorage
4. Toutes requêtes incluent: Authorization: Bearer {token}
5. Services valident token contre Auth Service
```

### Database Isolation
- ✅ Chaque service a sa propre BD PostgreSQL
- ✅ Pas d'accès direct entre BDs
- ✅ Data sharing via API REST ou événements RabbitMQ

### Audit Logging
Toutes les actions sensibles sont loggées :
- LOGIN / FAILED_LOGIN
- LOGOUT
- VIEW_ACCOUNTS
- VIEW_TRANSACTIONS
- TRANSFER
- IP address, timestamp, success/failure

### HTTPS/TLS
- Certificats auto-signés pour dev (Nginx)
- HTTP → HTTPS redirect
- TLS 1.3 + modern ciphers

## 📝 Commandes Utiles

### Dev & Debugging
```bash
# Afficher tous les services
docker-compose ps

# Logs en temps réel
docker-compose logs -f auth-service
docker-compose logs -f rabbitmq

# Arrêter tous les services
docker-compose down

# Réinitialiser (supprimer volumes)
docker-compose down -v

# Rebuild une image après changement
docker-compose build auth-service
docker-compose up -d auth-service
```

### Database
```bash
# Accéder à psql (auth_db)
docker exec -it postgres-auth psql -U bankadmin -d auth_db

# Voir les utilisateurs
SELECT * FROM users;

# Voir audit logs
SELECT * FROM audit_logs ORDER BY created_at DESC;

# Backup manuelle
./scripts/backup-db.sh

# Restore depuis backup
./scripts/restore-db.sh backup_auth_db_20240224_120000.sql
```

### RabbitMQ
```bash
# Management UI
open http://localhost:15672 (guest/guest)

# Lister les queues
docker exec rabbitmq rabbitmqctl list_queues

# Lister les consumers
docker exec rabbitmq rabbitmqctl list_consumers
```

## 🧪 Tests

### Test Login Flow
```bash
# 1. Login
RESPONSE=$(curl -s -X POST https://localhost/api/auth/login \
  -H "Content-Type: application/json" \
  -k \
  -d '{"email":"alice@example.com","password":"password123"}')

TOKEN=$(echo $RESPONSE | jq -r '.token')
echo "Token: $TOKEN"

# 2. Get accounts
curl -s -X GET https://localhost/api/accounts \
  -H "Authorization: Bearer $TOKEN" \
  -k | jq .

# 3. Get specific account
curl -s -X GET https://localhost/api/accounts/1 \
  -H "Authorization: Bearer $TOKEN" \
  -k | jq .

# 4. Transfer money
curl -s -X POST https://localhost/api/transactions/transfer \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -k \
  -d '{
    "fromAccountId": 1,
    "toAccountId": 2,
    "amount": 100.00,
    "description": "Payment for services"
  }' | jq .
```

## 📊 Architecture Diagram

```
┌─────────────────────────────────────────────────────┐
│           INTERNET (HTTPS/TLS 1.3)                  │
└──────────────────────┬────────────────────────────┘
                       │
         ┌─────────────▼──────────────┐
         │   NGINX API Gateway        │
         │   (Port 443/80)            │
         │   ▪ Reverse proxy          │
         │   ▪ Static files (React)   │
         │   ▪ Rate limiting          │
         │   ▪ CORS headers           │
         └──────────────┬──────────────┘
                        │
          ┌─────────────┼─────────────┐
          │             │             │
    ┌─────▼─────┐ ┌─────▼─────┐ ┌─────▼─────┐
    │Auth       │ │Account    │ │Transaction│
    │Service    │ │Service    │ │Service    │
    │(8081)     │ │(8082)     │ │(8083)     │
    └─────┬─────┘ └─────┬─────┘ └─────┬─────┘
          │             │             │
    ┌─────▼─────┐ ┌─────▼─────┐ ┌─────▼─────┐
    │  Auth DB  │ │ Account DB │ │ Trans. DB  │
    │PostgreSQL │ │PostgreSQL  │ │PostgreSQL  │
    └───────────┘ └───────────┘ └───────────┘
          ▲
          └────────┬────────────┐
                   │            │
            ┌──────▼──────┐  ┌──▼────────────┐
            │  RabbitMQ   │  │Notification   │
            │(5672, 15672)│  │Service (8084) │
            └─────────────┘  │▪ Email        │
                             │▪ SMS (future) │
                             └────────────────┘
```

## 🔄 Flux Communication

### Synchrone (REST/HTTP)
```
Frontend
  ↓ HTTPS
API Gateway (Nginx)
  ↓ Route /api/accounts/*
Account Service
  ↓ HTTP (Docker network)
Auth Service (validate token)
  ↓
Account DB (PostgreSQL)
```

### Asynchrone (RabbitMQ)
```
Transaction Service
  ↓ Publie événement
RabbitMQ (transfer-completed-queue)
  ↓
Notification Service (consume async)
  ↓ Envoie email
Client ← Notification
```

## 📚 Documentation Complète

- **[ARCHITECTURE.md](docs/ARCHITECTURE.md)** - Schémas détaillés, flux de données
- **[SECURITY.md](docs/SECURITY.md)** - Checklist sécurité, JWT, HTTPS, audit
- **[API.md](docs/API.md)** - Documentation endpoints (OpenAPI/Swagger)
- **[DEPLOYMENT.md](docs/DEPLOYMENT.md)** - Production deployment, Kubernetes
- **[TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md)** - FAQ, problèmes courants

## 🛠️ Développement

### Architecture des Services

Chaque service suit le pattern **Layered Architecture** :
```
Controller (REST endpoints)
    ↓
Service (Business logic)
    ↓
Repository (Data access)
    ↓
Entity (JPA models)
    ↓
Database
```

### Dépendances entre Services

```
Frontend
  → Auth Service (validate token)
  → Account Service (requires Auth)
  → Transaction Service (requires Auth)
  → Notification Service (async, via RabbitMQ)

Tous → PostgreSQL (isolé par service)
```

### Ajouter une Nouvelle Feature

1. Identifier quel service est concerné
2. Ajouter endpoint dans Controller
3. Implémenter logique dans Service
4. Ajouter modèle dans Entity
5. Migrer BD si nécessaire (schema.sql)
6. Tester via curl ou Postman
7. Commiter et pusher

## 📋 Checklist Déploiement

- [ ] Générer nouveau JWT_SECRET
- [ ] Configurer SMTP pour notifications
- [ ] Générer certificats SSL (production)
- [ ] Mettre à jour .env avec secrets production
- [ ] Tester avec docker-compose up
- [ ] Lancer health-check.sh
- [ ] Vérifier logs (docker-compose logs)
- [ ] Tester endpoints (login, accounts, transfer)
- [ ] Vérifier notifications RabbitMQ
- [ ] Backup base de données
- [ ] Documenter variables d'env
- [ ] Commitr et pusher changements

## 🤝 Contribution

1. Créer une branche `feature/xxx`
2. Faire les changements
3. Tester localement
4. Commiter avec messages clairs
5. Pusher et créer Pull Request
6. Request review

## 📄 Licence

Projet académique - Free to use for educational purposes.

## 👨‍💼 Contact & Support

Pour questions ou problèmes :
1. Vérifier [TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md)
2. Consulter [ARCHITECTURE.md](docs/ARCHITECTURE.md)
3. Lancer `./scripts/health-check.sh`
4. Vérifier logs: `docker-compose logs`

---

**État du Projet**: ✅ MVP Complet
- ✅ 5 microservices fonctionnels
- ✅ Docker Compose orchestration
- ✅ JWT authentication
- ✅ HTTPS/TLS support
- ✅ RabbitMQ messaging
- ✅ Database isolation
- ✅ Audit logging
- ⏳ Kubernetes deployment (WIP)
- ⏳ Distributed tracing/Jaeger (WIP)
