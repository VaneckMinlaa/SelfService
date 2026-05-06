# Configuration Production - Résumé Complet
# Banking Portal - Microservices
# Date: 2024 | Version: 1.0

## 📋 Fichiers Créés / Modifiés

### 1. Docker Compose Production
- **Fichier**: `docker-compose.prod.yml`
- **Changements principaux**:
  - Images pré-construites depuis registry (au lieu de build local)
  - Restart policies `always` pour haute disponibilité
  - Logging JSON limité (10m, 3 fichiers max)
  - Volumes persistants nommés pour données
  - Health checks optimisés
  - Certbot pour Let's Encrypt auto-renewal
  - Prometheus + Grafana pour monitoring
  - Variables d'environnement externalisées

### 2. Configuration Environnement
- **Fichier**: `.env.production`
- **Éléments**:
  - JWT_SECRET (à générer: `openssl rand -base64 32`)
  - DB_PASSWORD fort (minimum 30 caractères)
  - Credentials RABBITMQ modifiés
  - SMTP/Brevo pour notifications
  - Variables SSL/TLS
  - Grafana admin password
  - Sentry DSN pour error tracking

### 3. Nginx Production
- **Fichier**: `Docker/nginx-prod.conf`
- **Sécurité**:
  - HTTP → HTTPS redirect
  - Let's Encrypt SSL/TLS (TLS 1.2 + 1.3)
  - OCSP Stapling
  - Security headers complets (HSTS, CSP, X-Frame-Options, etc)
  - Rate limiting (API: 100 req/s, Auth: 5 req/s)
  - GZIP compression
  - Protections contre chemins sensibles

### 4. Prometheus Production
- **Fichier**: `Docker/prometheus/prometheus-prod.yml`
- **Métriques**:
  - Scrape interval: 30s (optimisé pour prod)
  - Retention: 30 jours
  - Tous les services instrumentés
  - PostgreSQL metrics
  - RabbitMQ metrics
  - Node exporter pour système

### 5. Scripts Déploiement
- **Fichiers**:
  - `Scripts/deploy-prod.sh` (Linux/Mac)
  - `Scripts/deploy-prod.ps1` (Windows PowerShell)

**Fonctionnalités**:
1. Pré-vérifications (Docker, configs)
2. Backup complet bases de données
3. Configuration Let's Encrypt
4. Docker login au registry
5. Pull + démarrage services
6. Health checks

### 6. Scripts Sauvegarde
- **Fichier**: `Scripts/backup-prod.sh`

**Fonctionnalités**:
- Dump compressé (gzip) 3 bases de données
- Backup configurations (.env, docker-compose.yml)
- Backup certificats SSL
- Nettoyage automatique (retention: 30j)
- Préparé pour sync S3/rsync cloud

### 7. Scripts Restauration
- **Fichier**: `Scripts/restore-db.sh` (mis à jour)

**Fonctionnalités**:
- Menu interactif
- Restauration base unique ou complète
- Vérification fichiers
- Décompression automatique

### 8. Health Checks
- **Fichier**: `Scripts/health-check-prod.sh`

**Vérifie**:
- Nginx + APIs (GET /health)
- Bases de données (connexions)
- RabbitMQ
- Espace disque (>90% warning, >95% critical)
- Expiration certificat SSL

### 9. Documentation Complète
- **Fichier**: `PRODUCTION_DEPLOYMENT_GUIDE.md`

**Contient**:
- Architecture détaillée
- Prérequis système (CPU, RAM, disque)
- Installation Docker
- Configuration firewall
- Instructions déploiement (auto + manuel)
- Post-déploiement
- Monitoring avec Grafana
- Backup & Disaster Recovery
- Troubleshooting
- Checkliste sécurité

---

## 🚀 Guide Déploiement Rapide

### Prérequis
```bash
# Sur VPS (Ubuntu 22.04 LTS)
sudo apt update && sudo apt upgrade -y
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker $USER
```

### 1. Cloner & Configurer
```bash
cd /opt/banking-app
git clone <votre-repo> .

# Éditer configuration
cp .env.production .env
nano .env  # Modifier JWT_SECRET, DB_PASSWORD, DOMAIN, etc
```

### 2. Déployer (Option: Automatisé)
```bash
chmod +x Scripts/deploy-prod.sh
./Scripts/deploy-prod.sh banking.example.com registry.example.com latest
```

### 3. Vérifier
```bash
docker-compose -f docker-compose.prod.yml ps
curl -k https://banking.example.com/health
```

### 4. Configurer Monitoring
```
https://banking.example.com/grafana
User: admin
Password: (voir .env GRAFANA_PASSWORD)
```

---

## 🔒 Sécurité - Points Clés

✅ **Configuration**:
- [x] Secrets externalisés (.env)
- [x] JWT_SECRET > 32 caractères
- [x] DB_PASSWORD > 30 caractères
- [x] RabbitMQ credentials modifiés
- [x] Non-root users dans Dockerfiles
- [x] Health checks (readiness + liveness)

✅ **Réseau**:
- [x] Firewall (UFW) - ports 22, 80, 443 seulement
- [x] HTTPS/TLS obligatoire (redirect HTTP)
- [x] HSTS header (1 an)
- [x] CSP header
- [x] Rate limiting API

✅ **Données**:
- [x] Backup automatisé (quotidien)
- [x] Retention 30 jours
- [x] Volumes persistants Docker
- [x] Database-per-service isolation

✅ **Monitoring**:
- [x] Prometheus scrape tout
- [x] Grafana dashboards
- [x] Health checks (5 min)
- [x] Alertes SSL expiration
- [x] Logs structurés (JSON)

---

## 📊 Sizing Recommandé VPS

| Taille | CPU | RAM | Disque | Coût | Cas d'Usage |
|--------|-----|-----|--------|------|------------|
| Small  | 2   | 4GB | 20GB   | $10  | Dev/Test |
| Medium | 4   | 8GB | 50GB   | $20  | **Production** |
| Large  | 8   | 16GB| 100GB  | $50  | High Traffic |

**Recommandation**: Medium (4 CPU, 8GB RAM, 50GB SSD)

---

## 🔄 Processus Déploiement Continu (CI/CD)

Votre `.gitlab-ci.yml` existant déjà:
1. Build images Docker (services)
2. Push vers registry privé
3. Tag latest + commit SHA

Pour production:
```bash
# Sur VPS, cron job
0 2 * * * cd /opt/banking-app && ./Scripts/deploy-prod.sh banking.example.com registry.example.com latest
```

---

## 📝 Commandes Utiles

```bash
# Logs
docker-compose -f docker-compose.prod.yml logs -f [service]

# Status
docker-compose -f docker-compose.prod.yml ps

# Redémarrer service
docker-compose -f docker-compose.prod.yml restart auth-service

# Exécuter commande
docker exec -it auth-service bash

# Backup manuel
./Scripts/backup-prod.sh

# Health check
./Scripts/health-check-prod.sh

# Renouveler certificat SSL
docker exec certbot certbot renew --force-renewal
```

---

## 🆘 Troubleshooting

**Service ne démarre pas**
```bash
docker-compose -f docker-compose.prod.yml logs auth-service
# Vérifier: DB_PASSWORD, JWT_SECRET, image disponible
```

**Base de données inaccessible**
```bash
docker exec postgres-auth pg_isready -U bankadmin
# Vérifier volumes, permissions
```

**SSL certificate invalide**
```bash
openssl x509 -in certbot/conf/live/DOMAIN/cert.pem -text -noout
docker exec certbot certbot renew
```

**Mémoire insuffisante**
```bash
docker stats  # Voir utilisation
# Augmenter dans docker-compose.prod.yml: mem_limit
```

---

## ✅ Checkliste Pré-Production

- [ ] Domaine DNS pointe vers VPS
- [ ] Firewall configuré (22, 80, 443)
- [ ] Docker + Compose installés
- [ ] `.env.production` créé avec secrets
- [ ] Registry privée accessible
- [ ] Let's Encrypt OK (test certificat)
- [ ] Backups configurés (test restore)
- [ ] Monitoring Grafana configuré
- [ ] Email notifications testées
- [ ] Health checks OK

---

## 📞 Support

- **Documentation complète**: `PRODUCTION_DEPLOYMENT_GUIDE.md`
- **Architecture**: `Documentation/architecture.md`
- **API**: `Documentation/API.md`
- **Sécurité**: `Documentation/exploitation-securite.md`

---

**Prêt pour production! 🎉**
Vérifier tous les fichiers créés dans le dossier de votre projet.
