# GUIDE DE TEST - DÉVELOPPEMENT LOCAL

## 🚀 Étapes pour Lancer le Projet en LOCAL

### 1️⃣ **Prérequis**
- ✅ Docker Desktop installé et démarré
- ✅ Docker Compose 3.8+
- ✅ Minimum 4GB RAM disponible
- ✅ Ports disponibles: 3000, 5432, 5672, 8000-8084, 9090, 3306

Vérifiez:
```powershell
docker --version
docker-compose --version
```

---

### 2️⃣ **Démarrage du Projet**

#### Option A: **Démarrage Complet (Recommandé)**
```powershell
# Terminal à la racine du projet: d:\Projects\Self-Service

# 1. Arrêter les conteneurs existants
docker-compose down -v

# 2. Construire les images
docker-compose build

# 3. Démarrer tous les services
docker-compose up -d

# 4. Attendre 30-60 secondes (initialisation des BD et démarrage des services)
```

#### Option B: **Démarrage Rapide (Si images déjà construites)**
```powershell
docker-compose up -d
```

#### Option C: **Voir les logs en temps réel**
```powershell
docker-compose logs -f
# Ou pour un service spécifique:
docker-compose logs -f auth-service
```

---

### 3️⃣ **Vérifier que TOUS les Services sont Démarrés**

```powershell
# Lister tous les conteneurs
docker-compose ps

# Résultat attendu: STATUS = "Up"
NAME                    IMAGE                          STATUS
auth-service            banking-portal/auth            Up (healthy)
account-service         banking-portal/account         Up (healthy)
transaction-service     banking-portal/transaction     Up (healthy)
notification-service    banking-portal/notification    Up (healthy)
frontend                banking-portal/frontend        Up (healthy)
nginx                   nginx:latest                   Up (healthy)
postgres-auth           postgres:15-alpine             Up (healthy)
postgres-account        postgres:15-alpine             Up (healthy)
postgres-transaction    postgres:15-alpine             Up (healthy)
rabbitmq                rabbitmq:3.12-management      Up (healthy)
prometheus              prom/prometheus                Up (healthy)
grafana                 grafana/grafana                Up (healthy)
```

---

### 4️⃣ **URLs pour Tester Chaque Service**

#### **Frontend (Interface Utilisateur)**
```
HTTP:  http://localhost:3000
HTTPS: https://localhost:3000 (self-signed)
```
- ✅ Page de login
- ✅ Consultation des comptes
- ✅ Historique des transactions
- ✅ Transferts d'argent

**Identifiants de test:**
```
Username: test_user
Password: password123
```

#### **Auth Service (Authentification)**
```
Login:       POST http://localhost:8081/api/auth/login
Register:    POST http://localhost:8081/api/auth/register
Health:      GET  http://localhost:8081/api/auth/health
Token Check: GET  http://localhost:8081/api/auth/validate
```

**Curl pour Login:**
```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test_user","password":"password123"}'
```

#### **Account Service (Gestion des Comptes)**
```
List Accounts:      GET  http://localhost:8082/api/accounts
Get Account:        GET  http://localhost:8082/api/accounts/{id}
Get Balance:        GET  http://localhost:8082/api/accounts/{id}/balance
Health:             GET  http://localhost:8082/api/accounts/health
```

**Avec JWT Token:**
```bash
curl -X GET http://localhost:8082/api/accounts \
  -H "Authorization: Bearer <JWT_TOKEN>"
```

#### **Transaction Service (Virements)**
```
Transfer:           POST http://localhost:8083/api/transactions/transfer
Get History:        GET  http://localhost:8083/api/transactions/{accountId}
Get Transaction:    GET  http://localhost:8083/api/transactions/{id}
Health:             GET  http://localhost:8083/api/transactions/health
```

**Curl pour Virement:**
```bash
curl -X POST http://localhost:8083/api/transactions/transfer \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -d '{
    "from_account_id": 1,
    "to_account_id": 2,
    "amount": 100.00,
    "description": "Test transfer"
  }'
```

#### **Notification Service (Notifications)**
```
Health:             GET  http://localhost:8084/api/notifications/health
Get Notifications:  GET  http://localhost:8084/api/notifications
```

#### **RabbitMQ Management UI (Queue Management)**
```
URL: http://localhost:15672
Username: guest
Password: guest
```
- ✅ Voir les files d'attente
- ✅ Voir les événements en attente
- ✅ Publier des messages de test

#### **Prometheus (Métriques)**
```
URL: http://localhost:9090
```
- ✅ Voir les métriques de chaque service
- ✅ Requêtes sur les timeseries
- ✅ Configuration des targets

**Requêtes utiles:**
```
http_requests_total            # Total des requêtes HTTP
jvm_memory_used_bytes          # Mémoire JVM utilisée
process_cpu_usage              # Utilisation CPU
```

#### **Grafana (Dashboards)**
```
URL: http://localhost:3000
Username: admin
Password: admin
```
- ✅ Dashboards de monitoring
- ✅ Graphiques temps-réel
- ✅ Alertes

---

### 5️⃣ **Scénario de Test Complet**

#### **Étape 1: Authentification**
```powershell
# 1. Login pour obtenir JWT Token
$response = Invoke-WebRequest -Uri "http://localhost:8081/api/auth/login" `
  -Method POST `
  -ContentType "application/json" `
  -Body '{"username":"test_user","password":"password123"}'

# Extraire le token
$token = ($response.Content | ConvertFrom-Json).token

# Afficher le token
Write-Host "JWT Token: $token"
```

#### **Étape 2: Consulter les Comptes**
```powershell
# Récupérer la liste des comptes de l'utilisateur
$accounts = Invoke-WebRequest -Uri "http://localhost:8082/api/accounts" `
  -Method GET `
  -Headers @{"Authorization"="Bearer $token"} `
  -ContentType "application/json"

Write-Host "Comptes:" ($accounts.Content | ConvertFrom-Json | ConvertTo-Json)
```

#### **Étape 3: Effectuer un Virement**
```powershell
# Effectuer un virement
$transfer = Invoke-WebRequest -Uri "http://localhost:8083/api/transactions/transfer" `
  -Method POST `
  -Headers @{"Authorization"="Bearer $token"} `
  -ContentType "application/json" `
  -Body '{"from_account_id":1,"to_account_id":2,"amount":50.00,"description":"Test transfer"}'

Write-Host "Virement:" ($transfer.Content | ConvertFrom-Json | ConvertTo-Json)
```

#### **Étape 4: Consulter l'Historique**
```powershell
# Récupérer l'historique des transactions
$history = Invoke-WebRequest -Uri "http://localhost:8083/api/transactions/1" `
  -Method GET `
  -Headers @{"Authorization"="Bearer $token"} `
  -ContentType "application/json"

Write-Host "Historique:" ($history.Content | ConvertFrom-Json | ConvertTo-Json)
```

#### **Étape 5: Vérifier RabbitMQ**
Allez sur http://localhost:15672 et:
- Cliquez sur **Queues**
- Vérifiez que la queue **transfer_events** contient les événements
- Vérifiez que le Notification Service a traité les messages

---

### 6️⃣ **Vérifier les Logs des Services**

```powershell
# Logs du service d'authentification
docker-compose logs auth-service

# Logs du service de transaction
docker-compose logs transaction-service

# Logs du service de notification
docker-compose logs notification-service

# Logs Nginx (reverse proxy)
docker-compose logs nginx

# Logs de toutes les bases de données
docker-compose logs postgres-auth
docker-compose logs postgres-account
docker-compose logs postgres-transaction

# Logs en temps réel avec suivi
docker-compose logs -f
```

---

### 7️⃣ **Tester les Health Checks**

```powershell
# Auth Service
curl http://localhost:8081/actuator/health

# Account Service
curl http://localhost:8082/actuator/health

# Transaction Service
curl http://localhost:8083/actuator/health

# Notification Service
curl http://localhost:8084/actuator/health

# Nginx
curl http://localhost/health

# RabbitMQ
curl -u guest:guest http://localhost:15672/api/health/checks/alarms
```

---

### 8️⃣ **Tester la Base de Données**

```powershell
# Accéder au conteneur PostgreSQL (Auth DB)
docker exec -it postgres-auth psql -U bankadmin -d auth_db

# Commands PostgreSQL:
\dt                    # Lister les tables
SELECT * FROM users;   # Voir les utilisateurs
SELECT * FROM audit_logs;  # Voir les logs d'audit
\q                     # Quitter

# Même pour les autres bases:
docker exec -it postgres-account psql -U bankadmin -d account_db
docker exec -it postgres-transaction psql -U bankadmin -d transaction_db
```

---

### 9️⃣ **Arrêter et Nettoyer**

```powershell
# Arrêter tous les conteneurs
docker-compose down

# Arrêter et supprimer les volumes (reset complet)
docker-compose down -v

# Supprimer les images
docker-compose down --rmi all
```

---

### 🔟 **Dépannage Courants**

#### **Erreur: "Ports déjà en utilisation"**
```powershell
# Trouver les conteneurs utilisant les ports
netstat -ano | findstr :3000
netstat -ano | findstr :8081

# Arrêter le conteneur spécifique
docker stop <container_id>
```

#### **Erreur: "Cannot connect to database"**
```powershell
# Attendre que PostgreSQL soit prêt (30-60s)
docker-compose ps

# Redémarrer la base de données
docker-compose restart postgres-auth postgres-account postgres-transaction

# Vérifier les logs
docker-compose logs postgres-auth
```

#### **Erreur: "Health check failed"**
```powershell
# Augmenter les délais de health check
# Éditez docker-compose.yml et augmentez interval, timeout, retries

# Redémarrer après modification
docker-compose down
docker-compose up -d
```

#### **Frontend ne charge pas**
- Attendez 2-3 minutes le build Vite
- Videz le cache du navigateur: Ctrl+Shift+Delete
- Vérifiez Nginx: `docker-compose logs nginx`

---

## 📊 **Dashboard de Monitoring**

### Prometheus
- **URL:** http://localhost:9090
- **Metrics disponibles:**
  - `http_requests_total{service="auth-service"}`
  - `jvm_memory_used_bytes{service="account-service"}`
  - `rabbitmq_messages_ready`

### Grafana
- **URL:** http://localhost:3000
- **Dashboards pré-configurés:**
  1. Banking Services Overview
  2. Database Performance
  3. Message Queue Status
  4. Application Errors

---

## ✅ **Checklist de Test Complet**

- [ ] Frontend accessible (http://localhost:3000)
- [ ] Login fonctionne (test_user / password123)
- [ ] Consultation des comptes réussie
- [ ] Virement effectué sans erreur
- [ ] Email de notification envoyé
- [ ] Historique des transactions visible
- [ ] RabbitMQ reçoit les événements
- [ ] Prometheus collecte les métriques
- [ ] Grafana affiche les dashboards
- [ ] Health checks: tous verts
- [ ] Logs: pas d'erreurs critiques
- [ ] Base de données: données persistantes

---

## 📝 **Notes Importantes**

✅ **Isolation des données:** Chaque service a sa propre base de données  
✅ **JWT Stateless:** Pas de session serveur  
✅ **Événements Asynchrones:** RabbitMQ gère les notifications  
✅ **Reverse Proxy:** Nginx route et sécurise  
✅ **Monitoring:** Prometheus + Grafana en place  
✅ **Health Checks:** Tous les services vérifient leur santé  

---

## 🆘 **Besoin d'Aide?**

1. Vérifiez les logs: `docker-compose logs <service>`
2. Vérifiez que Docker a assez de ressources (4GB min)
3. Assurez-vous que les ports ne sont pas utilisés
4. Attendez 30-60 secondes pour l'initialisation complète
5. Relancez avec `docker-compose restart`

---

**Généré:** 6 Mai 2026  
**Pour la plateforme bancaire:** Banking Portal - Microservices
