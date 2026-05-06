# 🏗️ Architecture CI/CD - Vue d'ensemble

## 📦 Fichiers de Configuration Créés

### Docker Compose
- **docker-compose.prod.yml** (357 lignes)
  - 5 microservices Spring Boot
  - PostgreSQL (3 instances)
  - RabbitMQ, Nginx, Certbot, Prometheus, Grafana
  - Volumes persistants et health checks

### Configuration Nginx
- **Docker/nginx-prod.conf** (280 lignes)
  - HTTPS/TLS 1.2+ obligatoire
  - Let's Encrypt Certbot integration
  - Rate limiting et security headers
  - Proxy reverses pour API

### Monitoring
- **Docker/prometheus/prometheus-prod.yml** (120 lignes)
  - Scrape interval: 30 secondes
  - Rétention: 30 jours
  - 8 jobs (services + bases de données)

### Environment
- **.env.production** (70 lignes)
  - Secrets (JWT, DB passwords)
  - Domaine et certificats
  - Configuration SMTP et logging

### Scripts d'Automatisation
- **Scripts/deploy-prod.sh** (170 lignes) - Déploiement Linux/Mac
- **Scripts/deploy-prod.ps1** (100 lignes) - Déploiement Windows
- **Scripts/backup-prod.sh** (110 lignes) - Sauvegarde automatisée
- **Scripts/health-check-prod.sh** (130 lignes) - Monitoring continu
- **Scripts/restore-db.sh** (MODIFIÉ) - Restauration interactive

### GitLab CI/CD
- **.gitlab-ci.yml** (320+ lignes)
  - 6 stages: lint, build-push, deploy-staging, deploy-production, rollback, notify
  - 5 builds en parallèle (services + frontend)
  - Déploiement staging automatique, production manuel
  - Rollback avec restauration BD
  - Notifications Slack

### Documentation
- **GITLAB_CI_CD_SETUP.md** - Guide complet de configuration
- **GITLAB_QUICK_SETUP.md** - Commandes copy-paste
- **PRODUCTION_DEPLOYMENT_GUIDE.md** - Déploiement détaillé
- **PRODUCTION_READY.md** - Quick start
- **VPS_DEPLOYMENT_CHECKLIST.md** - Checklist pré-déploiement

---

## 🔄 Pipeline CI/CD Workflow

```
GIT PUSH → GITLAB PIPELINE
     ↓
┌─ LINT STAGE ──────────────────────────────┐
│ • Valider docker-compose.prod.yml         │
└─────────────────────────────────────────────┘
     ↓ (succès)
┌─ BUILD STAGE (parallèle) ─────────────────┐
│ • build:auth-service                      │
│ • build:account-service                   │
│ • build:transaction-service               │
│ • build:notification-service              │
│ • build:frontend                          │
│                                            │
│ Chaque job:                                │
│ - docker build avec tags                  │
│ - push vers Docker Registry               │
│ - retry sur erreur                        │
└─────────────────────────────────────────────┘
     ↓ (succès)
┌─ DEPLOY STAGING (AUTO) ───────────────────┐
│ Déclenché automatiquement si:             │
│ - Branch: develop                         │
│ - Build jobs réussis                      │
│                                            │
│ Actions:                                   │
│ 1. SSH vers STAGING_HOST                  │
│ 2. Backup BD                              │
│ 3. Update .env (IMAGE_TAG)                │
│ 4. docker-compose pull                    │
│ 5. docker-compose up -d                   │
│ 6. Health checks (30s)                    │
└─────────────────────────────────────────────┘
     ↓
┌─ DEPLOY PRODUCTION (MANUEL) ──────────────┐
│ Déclenché manuellement si:                │
│ - Branch: main ou tag                     │
│ - Build jobs réussis                      │
│ - User clique ▶️ bouton dans GitLab       │
│                                            │
│ Actions:                                   │
│ 1. SSH vers PRODUCTION_HOST               │
│ 2. Backup BD (CRITIQUE)                   │
│ 3. Update .env (IMAGE_TAG)                │
│ 4. docker-compose pull                    │
│ 5. docker-compose up -d (rolling)         │
│ 6. Health checks (5 tentatives × 5s)      │
│ 7. Rollback auto si échec                 │
└─────────────────────────────────────────────┘
     ↓
┌─ ROLLBACK (MANUEL) ───────────────────────┐
│ Déclenché manuellement en urgence         │
│                                            │
│ Actions:                                   │
│ 1. Trouver le backup récent               │
│ 2. docker-compose down                    │
│ 3. restore-db.sh (restore complète)       │
│ 4. docker-compose up -d                   │
│ 5. Vérifier avec docker-compose ps        │
└─────────────────────────────────────────────┘
     ↓
┌─ NOTIFY SLACK ────────────────────────────┐
│ notify:slack:success (si deploy OK)       │
│ notify:slack:failure (si deploy échoue)   │
│                                            │
│ Message avec:                              │
│ - Status (✅ ou ❌)                       │
│ - Commit SHA                              │
│ - Branch                                  │
│ - Author                                  │
│ - Pipeline URL                            │
└─────────────────────────────────────────────┘
```

---

## 📊 Statuts des Stages

| Stage | Déclenché | Condition | Action |
|-------|-----------|-----------|--------|
| **lint** | Toujours | Tous les branches | Valide configs |
| **build-push** | Toujours | main, develop, tags | Build 5 services |
| **deploy-staging** | Auto | develop + build OK | Déploie staging |
| **deploy-production** | Auto | main/tags + build OK | Déploie production |
| **rollback** | Manuel | main + build OK | Restaure backup |
| **notify** | Après deploy | main seulement | Slack webhook |

---

## 🔐 Variables Requises (11 total)

### Staging (5 variables)
1. `STAGING_HOST` - IP du VPS staging
2. `STAGING_USER` - Utilisateur SSH
3. `STAGING_DEPLOY_PATH` - Chemin déploiement
4. `STAGING_SSH_PRIVATE_KEY` - Clé SSH (Protected)
5. `STAGING_DOMAIN` - URL staging

### Production (5 variables)
6. `PRODUCTION_HOST` - IP du VPS production
7. `PRODUCTION_USER` - Utilisateur SSH
8. `PRODUCTION_DEPLOY_PATH` - Chemin déploiement
9. `PRODUCTION_SSH_PRIVATE_KEY` - Clé SSH (Protected)
10. `PRODUCTION_DOMAIN` - URL production

### Notifications (1 variable)
11. `SLACK_WEBHOOK_URL` - Webhook Slack (optionnel)

---

## 🚀 Flux de Déploiement Complet

### Scenario 1: Développement → Staging

```
1. Developer fait un commit sur develop:
   $ git commit -m "Feature: Add account transfer"
   $ git push origin develop

2. GitLab détecte le push:
   - Crée une nouvelle pipeline
   - Déclenche LINT → BUILD

3. Après BUILD réussi:
   - DEPLOY STAGING se déclenche automatiquement
   - SSH vers STAGING_HOST
   - Sauvegarde BD
   - Pull nouvelles images
   - docker-compose up -d
   - Health checks

4. Si Health checks OK:
   - 🟢 Pipeline réussie
   - 🔔 Notification Slack (succès)
   - Team peut tester sur staging

5. Si Health checks échouent:
   - 🔴 Pipeline échouée
   - 🔔 Notification Slack (erreur)
   - Logs disponibles dans GitLab
```

### Scenario 2: Production Release (Automatique)

```
1. Developer push sur main ou crée un tag:
   $ git push origin main
   ou
   $ git tag -a v1.0.0 -m "Release 1.0.0"
   $ git push origin v1.0.0

2. GitLab détecte le push/tag:
   - Crée une nouvelle pipeline
   - Déclenche LINT → BUILD → DEPLOY PRODUCTION (AUTO)

3. DEPLOY PRODUCTION démarre automatiquement:
   - Pas besoin de cliquer sur un bouton!
   - SSH vers PRODUCTION_HOST
   - Sauvegarde BD (IMPORTANT!)
   - Update .env
   - docker-compose pull
   - docker-compose up -d
   - Health checks (5 tentatives)

4. Si succès:
   - 🟢 Production updated
   - 🔔 Slack notification
   - Logs dans /var/log/banking/deploy.log

5. Si échec:
   - 🔴 Deployment failed
   - ⚠️ Rollback disponible manuellement
   - 🔔 Slack notification
```

### Scenario 3: Rollback d'Urgence

```
1. Production a un problème:
   $ ssh ubuntu@PROD_IP tail -f /var/log/banking/health.log
   (Services crashent)

2. Options de rollback:

   A. Via GitLab UI (recommandé):
      - Pipelines → Stage "rollback"
      - Cliquer ▶️ "Play"
      - Auto: find backup → docker-compose down
      - Auto: restore-db.sh → docker-compose up -d

   B. Via SSH (manuel):
      $ ssh ubuntu@PROD_IP
      $ cd /opt/banking-app
      $ ls -la /backups/banking/
      $ ./Scripts/restore-db.sh all /backups/banking/2026-05-06_14-30-45
      $ docker-compose -f docker-compose.prod.yml up -d
      $ docker-compose -f docker-compose.prod.yml ps

3. Vérifier:
   $ curl -s https://banking.example.com/api/auth/health
   (Doit retourner 200 OK)
```

---

## 📈 Statistiques de la Pipeline

| Metric | Valeur |
|--------|--------|
| Nombre de stages | 6 |
| Nombre de jobs de build | 5 (parallèle) |
| Timeout build | ~5 min |
| Timeout déploiement | ~3 min |
| Health check retries (prod) | 5 × 5s = 25s max |
| Taille du backup | ~50-100 MB (gzip) |
| Rétention backups | 30 jours |

---

## 🔧 Configuration de Base Requise

### VPS Minimum
- OS: Ubuntu 22.04 LTS
- CPU: 4 cores
- RAM: 8 GB
- Disk: 50 GB SSD
- Ports: 22 (SSH), 80 (HTTP), 443 (HTTPS)

### Logiciels Requis
- Docker 24+
- Docker Compose v2+
- Git
- PostgreSQL client (optionnel)
- OpenSSH server

### Connectivité
- Accès SSH public (port 22)
- Accès outbound vers Docker Registry
- Accès outbound vers api.github.com (certificats)
- Accès optionnel vers Slack webhooks

---

## ✅ Validation de Setup

### Test locale:
```bash
# 1. Pipeline syntax
gitlab-runner verify

# 2. SSH keys
ssh -T git@gitlab.com
ssh -i ~/.ssh/id_gitlab ubuntu@STAGING_IP echo OK
```

### Test sur GitLab:
```
1. Push vers develop
2. Observer CI/CD → Pipelines
3. Vérifier que LINT → BUILD → DEPLOY STAGING réussissent
4. SSH vers VPS pour vérifier deployment
```

### Test sur VPS:
```bash
# Services running:
docker-compose -f docker-compose.prod.yml ps

# Health endpoints:
curl -s https://banking.example.com/api/auth/health
curl -s https://banking.example.com/api/accounts/health
curl -s https://banking.example.com/api/transactions/health

# Monitoring:
curl -s https://banking.example.com/metrics | head -20

# Logs:
tail -100 /var/log/banking/deploy.log
```

---

## 🎯 Résumé pour l'Équipe

**La pipeline fait automatiquement:**
1. ✅ Valide la configuration
2. ✅ Build les 5 services en parallèle
3. ✅ Pousse vers le registry Docker
4. ✅ Déploie en staging (automatique)
5. ✅ Vérifie la santé des services
6. ✅ Permet le déploiement production (manuel)
7. ✅ Permet le rollback d'urgence (manuel)
8. ✅ Notifie l'équipe sur Slack

**L'équipe fait manuellement:**
1. Pousser du code sur develop (staging auto)
2. Créer un tag sur main pour production
3. Cliquer ▶️ dans GitLab pour déployer
4. Monitorer les logs et l'application

**Temps moyen:**
- Build: 3-5 minutes
- Deploy: 2-3 minutes
- Total: 5-8 minutes du push à la production

---

## 📞 Support & Documentation

**Si vous avez des questions:**
1. 📖 Lire GITLAB_CI_CD_SETUP.md
2. ⚡ Utiliser les commandes de GITLAB_QUICK_SETUP.md
3. 🔍 Vérifier les logs:
   - GitLab: CI/CD → Pipelines → Job Log
   - VPS: tail -f /var/log/banking/deploy.log
4. 🐛 Troubleshooting: voir PRODUCTION_DEPLOYMENT_GUIDE.md

**Fichiers de référence:**
- .gitlab-ci.yml - Pipeline définition
- docker-compose.prod.yml - Services production
- Docker/nginx-prod.conf - Web server config
- Scripts/deploy-prod.sh - Deployment automation
- .env.production - Variables d'environnement
