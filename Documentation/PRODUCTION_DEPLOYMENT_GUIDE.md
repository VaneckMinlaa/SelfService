# GUIDE COMPLET DE DÉPLOIEMENT PRODUCTION - VPS
# Banking Portal - Microservices Architecture

## Table des matières
1. [Architecture Production](#architecture-production)
2. [Prérequis Serveur VPS](#prérequis-serveur-vps)
3. [Préparation du Déploiement](#préparation-du-déploiement)
4. [Déploiement Complet](#déploiement-complet)
5. [Post-Déploiement](#post-déploiement)
6. [Monitoring & Alertes](#monitoring--alertes)
7. [Backup & Disaster Recovery](#backup--disaster-recovery)
8. [Troubleshooting](#troubleshooting)
9. [Checkliste Sécurité](#checkliste-sécurité)

---

## Architecture Production

### Infrastructure Recommandée VPS
```
┌─────────────────────────────────────┐
│         Internet / Client            │
│      (HTTPS Port 443)               │
└────────────┬────────────────────────┘
             │
┌────────────▼────────────────────────┐
│      Nginx + Certbot                │
│  (Reverse Proxy + SSL/TLS)          │
│  Let's Encrypt (Auto-renew)        │
└────────────┬────────────────────────┘
             │
┌────────────┼────────────────────────────────┐
│            │                                 │
├─────────────┼─────────────┬─────────────┐    │
│             │             │             │    │
│  Auth Svc  Account Svc   Transaction   │    │
│  (8081)    (8082)        Svc (8083)    │    │
│             │             │             │    │
├─────────────┼─────────────┴─────────────┤   │
│  Postgres   │  Postgres  │  Postgres    │   │
│  (auth_db)  │  (account_  │  (trans_db) │   │
│             │  db)        │             │   │
│                                        │   │
│        RabbitMQ (5672)                 │   │
│        Notification Service            │   │
│        Prometheus + Grafana            │   │
│                                        │   │
│        Docker Network: banking-network │   │
└────────────────────────────────────────┘   │
                                             │
```

### Ressources Requises
- **CPU**: 4 cores (minimum 2)
- **RAM**: 8GB (minimum 4GB)
- **Disque**: 50GB SSD (minimum 20GB)
- **Bande passante**: 10 Mbps minimum
- **OS**: Ubuntu 22.04 LTS (recommandé)

---

## Prérequis Serveur VPS

### 1. Installation Docker & Docker Compose

```bash
# Mise à jour système
sudo apt update && sudo apt upgrade -y

# Installation Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh

# Vérification
docker --version
docker-compose --version

# Utilisateur docker sans sudo (optionnel)
sudo usermod -aG docker $USER
newgrp docker
```

### 2. Configuration du Firewall

```bash
# UFW (Ubuntu)
sudo ufw enable
sudo ufw allow 22/tcp      # SSH
sudo ufw allow 80/tcp      # HTTP (Let's Encrypt renewal)
sudo ufw allow 443/tcp     # HTTPS
sudo ufw allow 15672/tcp   # RabbitMQ Management (optionnel, localhost seulement)
```

### 3. Création de Répertoires

```bash
# Répertoires de données
sudo mkdir -p /opt/banking-app
sudo mkdir -p /backups/banking
sudo mkdir -p /var/log/banking
sudo chown -R $USER:$USER /opt/banking-app /backups/banking /var/log/banking
```

### 4. Configuration DNS

Pointer votre domaine vers l'IP du VPS:
```
banking.example.com -> VPS_IP_ADDRESS
```

---

## Préparation du Déploiement

### 1. Cloner le Projet

```bash
cd /opt/banking-app
git clone <votre-repo> .
cd /opt/banking-app
```

### 2. Configuration des Secrets

**IMPORTANT**: Ne JAMAIS commiter les secrets!

```bash
# Copier le template
cp .env.production .env

# Éditer et sécuriser
nano .env
```

Configuration des valeurs critiques:

```env
# Générer des clés sécurisées
JWT_SECRET=$(openssl rand -base64 32)
DB_PASSWORD=$(openssl rand -base64 24)
RABBITMQ_PASSWORD=$(openssl rand -base64 24)

# Obtenir from:
# - BREVO_API_KEY: https://app.brevo.com/account/api
# - SMTP credentials: https://app.brevo.com/smtp-credentials
# - GRAFANA_PASSWORD: Générer une clé forte

# Permissions sécurisées
chmod 600 .env
```

### 3. Pré-configuration Nginx

```bash
# Remplacer DOMAIN par votre domaine dans nginx-prod.conf
sed -i 's|DOMAIN|banking.example.com|g' Docker/nginx-prod.conf
sed -i 's|YOUR_REGISTRY_URL|registry.example.com|g' Docker/nginx-prod.conf
```

---

## Déploiement Complet

### Option 1: Script Automatisé (Recommandé)

```bash
# Rendre le script exécutable
chmod +x Scripts/deploy-prod.sh

# Lancer le déploiement
./Scripts/deploy-prod.sh banking.example.com registry.example.com latest
```

Le script effectue automatiquement:
- Vérifications pré-déploiement
- Sauvegarde des bases de données
- Configuration Let's Encrypt
- Démarrage des services
- Vérifications de santé

### Option 2: Déploiement Manuel Étape par Étape

#### Étape 1: Let's Encrypt Certificate Setup

```bash
# Créer répertoires
mkdir -p certbot/{conf,www}

# Générer certificat
docker run -it --rm \
  -v $(pwd)/certbot/conf:/etc/letsencrypt \
  -v $(pwd)/certbot/www:/var/www/certbot \
  -p 80:80 \
  certbot/certbot certonly \
  --standalone \
  --agree-tos \
  --no-eff-email \
  --email admin@banking.example.com \
  -d banking.example.com
```

#### Étape 2: Démarrer les Services

```bash
# Pull les images depuis registry
docker-compose -f docker-compose.prod.yml pull

# Démarrer tous les services
docker-compose -f docker-compose.prod.yml up -d

# Vérifier le statut
docker-compose -f docker-compose.prod.yml ps

# Attendre l'initialisation (1-2 min)
docker-compose -f docker-compose.prod.yml logs -f
```

#### Étape 3: Vérification

```bash
# Santé des services
docker-compose -f docker-compose.prod.yml ps

# Tester API auth
curl -k https://banking.example.com/health

# Consulter les logs
docker-compose -f docker-compose.prod.yml logs nginx
docker-compose -f docker-compose.prod.yml logs auth-service
```

---

## Post-Déploiement

### 1. Initialisation Base de Données

```bash
# Les tables seront créées automatiquement par Hibernate
# (JPA ddl-auto: validate)
# Vérifier les logs
docker-compose -f docker-compose.prod.yml logs auth-service | grep "Hibernate"
```

### 2. Configurer Admin Grafana

```bash
# Récupérer le password Grafana depuis .env
grep GRAFANA_PASSWORD .env

# Accéder à Grafana
# https://banking.example.com/grafana
# User: admin
# Password: <GRAFANA_PASSWORD>

# Ajouter Prometheus comme data source
# URL: http://prometheus:9090
```

### 3. Test de Functionality

```bash
# 1. Vérifier health endpoints
curl -k https://banking.example.com/health

# 2. Test login (créer compte)
curl -X POST https://banking.example.com/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"SecurePass123!"}'

# 3. Consulter comptes
curl -H "Authorization: Bearer <token>" \
  https://banking.example.com/api/accounts
```

### 4. Configurer Certificat Auto-Renewal

Certbot renouvelle automatiquement via le conteneur certbot déjà inclus.
Vérifier:

```bash
docker-compose -f docker-compose.prod.yml ps | grep certbot

# Logs de renewal
docker-compose -f docker-compose.prod.yml logs certbot
```

---

## Monitoring & Alertes

### 1. Accès Prometheus

```
https://banking.example.com/metrics
```

### 2. Dashboards Grafana

```
https://banking.example.com/grafana
```

### 3. Métriques Clés à Monitorer

```
# Santé des services
up{job=~"auth-service|account-service|..."}

# Latence API
http_request_duration_seconds_bucket{service="..."}

# Erreurs
http_requests_total{status=~"5.."}

# Base de données
pg_stat_activity_count

# RabbitMQ
rabbitmq_messages_ready_total
```

### 4. Alertes Recommandées

Créer des règles dans Prometheus:

```yaml
groups:
  - name: banking-alerts
    rules:
      - alert: ServiceDown
        expr: up{job=~"auth-service|account-service|..."} == 0
        for: 5m
        
      - alert: HighErrorRate
        expr: rate(http_requests_total{status=~"5.."}[5m]) > 0.01
        
      - alert: DatabaseConnectionPool
        expr: pg_stat_activity_count > 20
```

---

## Backup & Disaster Recovery

### 1. Backup Automatisé (Quotidien)

```bash
# Créer cron job
crontab -e

# Ajouter:
0 2 * * * /opt/banking-app/Scripts/backup-db.sh >> /var/log/banking/backup.log 2>&1
```

### 2. Backup Manuel

```bash
# Full backup
./Scripts/backup-db.sh

# Vérifier
ls -lah /backups/banking/
```

### 3. Restauration depuis Backup

```bash
# Stop services
docker-compose -f docker-compose.prod.yml down

# Restaurer les données
docker-compose -f docker-compose.prod.yml up -d postgres-auth
docker exec -i postgres-auth psql -U bankadmin < /backups/banking/YYYY-MM-DD/auth_db.sql

# Redémarrer tous les services
docker-compose -f docker-compose.prod.yml up -d
```

### 4. Backup Cloud (Recommandé)

```bash
# Uploader les backups vers S3/GCS
aws s3 sync /backups/banking s3://my-backup-bucket/banking

# Ou avec rsync
rsync -avz /backups/banking/ backup-server:/backups/banking/
```

---

## Troubleshooting

### Service ne démarre pas

```bash
# Vérifier les logs
docker-compose -f docker-compose.prod.yml logs <service_name>

# Vérifier la santé
docker-compose -f docker-compose.prod.yml ps

# Redémarrer un service
docker-compose -f docker-compose.prod.yml restart <service_name>
```

### Base de données inaccessible

```bash
# Vérifier la connexion PostgreSQL
docker exec postgres-auth pg_isready -U bankadmin

# Vérifier les variables d'environnement
docker-compose -f docker-compose.prod.yml config | grep POSTGRES
```

### Certificat SSL invalide

```bash
# Vérifier le certificat
openssl x509 -in certbot/conf/live/banking.example.com/fullchain.pem -text -noout

# Renouveler manuellement
docker exec certbot certbot renew --force-renewal
```

### Problèmes de mémoire

```bash
# Vérifier l'utilisation
docker stats

# Augmenter la limite (docker-compose.prod.yml)
services:
  postgres-auth:
    mem_limit: 2g
    memswap_limit: 3g
```

---

## Checkliste Sécurité

### Pre-Deployment
- [ ] Tous les secrets changeables (.env) ne sont pas en git
- [ ] Firewall activé et ports restreints
- [ ] SSH configuré (clés, désactiver root login)
- [ ] Domaine pointe vers le VPS
- [ ] Certificat SSL obtenu

### Deployment
- [ ] HTTPS/TLS activé (Let's Encrypt)
- [ ] JWT_SECRET généré (32 caractères min)
- [ ] DB_PASSWORD fort (30+ caractères)
- [ ] RABBITMQ credentials modifiés
- [ ] RabbitMQ management UI protégé (pas d'accès public)
- [ ] Nginx headers de sécurité activés
- [ ] CORS correctement configuré

### Post-Deployment
- [ ] Accès admin Grafana sécurisé
- [ ] Logs centralisés configurés
- [ ] Sauvegardes automatisées en place
- [ ] Monitoring actif
- [ ] Alertes configurées
- [ ] Certificat auto-renewal vérifié
- [ ] Backup cloud configuré

### Maintenance Régulière
- [ ] Vérifier backups (hebdo)
- [ ] Mettre à jour images Docker (mensuel)
- [ ] Analyser logs (quotidien)
- [ ] Tester restoration de backup (mensuel)
- [ ] Revoir règles firewall (trimestriel)

---

## Commandes Utiles Production

```bash
# Voir logs en temps réel
docker-compose -f docker-compose.prod.yml logs -f nginx

# Redémarrer tous les services
docker-compose -f docker-compose.prod.yml restart

# Mettre à jour une image
docker-compose -f docker-compose.prod.yml pull auth-service
docker-compose -f docker-compose.prod.yml up -d auth-service

# Exécuter des commandes dans un conteneur
docker exec -it auth-service bash

# Dump base de données
docker exec postgres-auth pg_dump -U bankadmin auth_db > backup.sql

# Redémarrer avec images reconstruites
docker-compose -f docker-compose.prod.yml down
docker-compose -f docker-compose.prod.yml up -d --build
```

---

## Support & Documentation

- 📖 Architecture complète: [Documentation/architecture.md](../Documentation/architecture.md)
- 🔐 Sécurité & Conformité: [Documentation/exploitation-securite.md](../Documentation/exploitation-securite.md)
- 🔌 API Endpoints: [Documentation/API.md](../Documentation/API.md)

---

**Dernière mise à jour**: 2024
**Version**: 1.0 - Production Ready
