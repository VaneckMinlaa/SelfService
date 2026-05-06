# 🚀 Configuration Production - VPS Ready

Votre projet Banking Portal a été **complètement configuré pour la production** sur un VPS.

## 📂 Fichiers Créés

### Configuration & Déploiement
1. ✅ **`docker-compose.prod.yml`** - Docker Compose optimisé production
2. ✅ **`.env.production`** - Template des variables d'environnement
3. ✅ **`Docker/nginx-prod.conf`** - Configuration Nginx sécurisée (HTTPS, CSP, rate limiting)
4. ✅ **`Docker/prometheus/prometheus-prod.yml`** - Monitoring (30j retention)

### Scripts d'Automatisation
5. ✅ **`Scripts/deploy-prod.sh`** - Déploiement automatisé (Linux/Mac)
6. ✅ **`Scripts/deploy-prod.ps1`** - Déploiement automatisé (Windows PowerShell)
7. ✅ **`Scripts/backup-prod.sh`** - Sauvegarde quotidienne bases de données
8. ✅ **`Scripts/restore-db.sh`** - Restauration depuis backup
9. ✅ **`Scripts/health-check-prod.sh`** - Vérification santé services (à cronner)

### Documentation
10. ✅ **`PRODUCTION_DEPLOYMENT_GUIDE.md`** - Guide complet 1500+ lignes
11. ✅ **`PRODUCTION_CONFIG_SUMMARY.md`** - Résumé des changements

---

## ⚡ Démarrage Rapide (5 minutes)

### Étape 1: Préparer le VPS
```bash
# SSH vers VPS
ssh root@YOUR_VPS_IP

# Installer Docker
curl -fsSL https://get.docker.com | sudo sh

# Créer répertoires
mkdir -p /opt/banking-app /backups/banking
cd /opt/banking-app
```

### Étape 2: Cloner le Projet
```bash
git clone YOUR_REPOSITORY .
```

### Étape 3: Configurer Secrets
```bash
# Copier template
cp .env.production .env

# Modifier avec vos valeurs
nano .env
```

**Variables CRITIQUES à configurer:**
```bash
DOMAIN=banking.example.com          # Votre domaine
JWT_SECRET=$(openssl rand -base64 32)
DB_PASSWORD=$(openssl rand -base64 24)
RABBITMQ_PASSWORD=$(openssl rand -base64 24)
DOCKER_REGISTRY=registry.example.com
GRAFANA_PASSWORD=$(openssl rand -base64 24)
```

### Étape 4: Lancer le Déploiement
```bash
chmod +x Scripts/deploy-prod.sh
./Scripts/deploy-prod.sh banking.example.com registry.example.com latest
```

**Le script automatiquement:**
- ✓ Sauvegarde les BDD
- ✓ Configure Let's Encrypt
- ✓ Démarre tous les services
- ✓ Vérifie la santé

### Étape 5: Vérifier
```bash
# Voir tous les services
docker-compose -f docker-compose.prod.yml ps

# Tester l'API
curl https://banking.example.com/health

# Accéder à Grafana
https://banking.example.com/grafana
# User: admin
# Pass: (votre GRAFANA_PASSWORD)
```

---

## 🔐 Sécurité - Ce qui a été Fait

✅ **Authentification & Encryption**
- JWT avec HMAC-SHA512
- HTTPS/TLS 1.2 + 1.3 (Let's Encrypt + auto-renewal)
- Database-per-service isolation
- Non-root containers

✅ **Réseau & Firewall**
- HTTP → HTTPS redirect
- Rate limiting (API: 100 req/s, Auth: 5 req/s)
- Security headers (HSTS, CSP, X-Frame-Options)
- CORS configuré
- OCSP Stapling

✅ **Données & Backup**
- Backup quotidien automatisé (gzip)
- Retention 30 jours
- Volumes Docker persistants
- Scripts restore inclus

✅ **Observabilité**
- Prometheus (scrape 30s)
- Grafana dashboards
- Health checks (5 min)
- Logging JSON

---

## 📊 Architecture Production

```
Internet (HTTPS 443)
    ↓
Nginx Gateway
  (Reverse Proxy + Let's Encrypt)
    ↓
┌─────────────────────────────────┐
│  Microservices (Docker Network) │
├──────────┬──────────┬───────────┤
│ Auth     │ Account  │ Trans     │
│ Service  │ Service  │ Service   │
│ (8081)   │ (8082)   │ (8083)    │
└──────────┼──────────┼───────────┘
│          │
├──────────┴──────────┐
│                     │
PostgreSQL    RabbitMQ
(x3 instances) (async events)
    ↓
Prometheus + Grafana (Monitoring)
```

---

## 📋 Checklist Avant Production

- [ ] **Domaine DNS** pointe vers IP du VPS
- [ ] **Firewall** ouvert pour 22 (SSH), 80 (HTTP), 443 (HTTPS)
- [ ] **Docker installé** (vérifier: `docker --version`)
- [ ] **`.env.production`** créé et sécurisé (`chmod 600`)
- [ ] **Registry Docker** accessible (credentials OK)
- [ ] **Let's Encrypt** certificat généré (test: `openssl x509 -in certbot/conf/live/DOMAIN/cert.pem`)
- [ ] **Backup script** testé (`./Scripts/backup-prod.sh`)
- [ ] **Health check** fonctionnel (`./Scripts/health-check-prod.sh`)
- [ ] **Email SMTP** configuré (Brevo/Gmail)
- [ ] **Monitoring** Grafana accessible

---

## 🔄 Maintenance Quotidienne

### Sauvegardes (Automatisé via Cron)
```bash
# Ajouter au crontab
crontab -e
# Ajouter cette ligne (backup à 2h du matin)
0 2 * * * /opt/banking-app/Scripts/backup-prod.sh >> /var/log/banking/backup.log 2>&1
```

### Health Checks (Automatisé via Cron)
```bash
# Health check toutes les 5 minutes
*/5 * * * * /opt/banking-app/Scripts/health-check-prod.sh >> /var/log/banking/health.log 2>&1
```

### Certificat SSL (Automatisé via Certbot)
```bash
# Renouvellement auto via conteneur certbot (déjà inclus)
docker-compose -f docker-compose.prod.yml logs certbot
```

---

## 📖 Lectures Essentielles

1. **`PRODUCTION_DEPLOYMENT_GUIDE.md`** (en français, 1500+ lignes)
   - Architecture complète
   - Troubleshooting
   - Disaster Recovery
   - Checkliste sécurité

2. **`PRODUCTION_CONFIG_SUMMARY.md`** (résumé complet)
   - Tous les fichiers créés
   - Commandes utiles
   - Quick troubleshooting

3. **`Documentation/architecture.md`**
   - Vue d'ensemble microservices
   - Diagrammes
   - Détail des services

4. **`Documentation/API.md`**
   - Endpoints REST
   - Authentification JWT
   - Exemples curl

---

## 🆘 Troubleshooting Rapide

### Service ne démarre pas?
```bash
docker-compose -f docker-compose.prod.yml logs auth-service
```

### Base de données inaccessible?
```bash
docker exec postgres-auth pg_isready -U bankadmin
```

### Certificat SSL expirant?
```bash
openssl x509 -in certbot/conf/live/DOMAIN/cert.pem -noout -dates
docker exec certbot certbot renew --force-renewal
```

### Espace disque plein?
```bash
docker stats  # voir utilisation
du -sh /var/lib/docker  # vérifier Docker
```

Plus de détails → Voir `PRODUCTION_DEPLOYMENT_GUIDE.md` (section Troubleshooting)

---

## 🎯 Prochaines Étapes

### Phase 1: Test (24h)
```bash
# Lancer le déploiement
./Scripts/deploy-prod.sh

# Tester tous les endpoints
curl https://banking.example.com/health
curl -X POST https://banking.example.com/api/auth/register -d '...'

# Vérifier monitoring
https://banking.example.com/grafana

# Tester backup/restore
./Scripts/backup-prod.sh
./Scripts/restore-db.sh
```

### Phase 2: Optimisation (Semaine 1)
```bash
# Ajuster les limites de ressources
# - mem_limit (docker-compose.prod.yml)
# - Nginx worker_processes
# - Database connection pools

# Configurer alerts Grafana
# - High CPU usage
# - High memory usage
# - SSL certificate expiring
```

### Phase 3: Monitoring Long-terme (Quotidien)
```bash
# Cronjobs en place
# - Backups: 2h du matin
# - Health checks: toutes les 5 min
# - SSL renewal: auto via Certbot

# Emails alerts (à configurer dans Sentry/Grafana)
# - Service down
# - High error rate
# - Certificate expiration
```

---

## 💡 Tips Importants

1. **Ne jamais commitez `.env`** - Utilisez `.env.production` template seulement
2. **Backups réguliers** - Tester restore 1x par mois
3. **Certificat SSL** - Vérifier 30 jours avant expiration
4. **Logs** - Archiver après 30 jours (logs docker limités à 10m)
5. **Updates** - Mettre à jour images Docker mensuellement

---

## 📞 Support

- 💬 Architecture questions → `Documentation/architecture.md`
- 🔐 Security questions → `Documentation/exploitation-securite.md`
- 🚀 Deploy questions → `PRODUCTION_DEPLOYMENT_GUIDE.md`
- 🐛 Bug/Error → Vérifier les logs: `docker-compose -f docker-compose.prod.yml logs -f`

---

## ✨ Summary

**Vous avez maintenant:**
- ✅ Docker Compose optimisé production
- ✅ HTTPS/TLS automatisé (Let's Encrypt)
- ✅ Scripts déploiement automatisés
- ✅ Backup & disaster recovery
- ✅ Monitoring avec Prometheus + Grafana
- ✅ Health checks automatisés
- ✅ Documentation complète (1500+ lignes)

**Prêt pour production! 🎉**

Bonne chance avec votre déploiement! 🚀
