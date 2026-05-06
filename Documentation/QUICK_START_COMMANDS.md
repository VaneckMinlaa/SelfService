# 🎯 Quick Start - Commandes à Copier-Coller

## 1️⃣ PHASE SETUP INITIALE (à faire UNE FOIS)

### Préparer le VPS
```bash
# SSH au VPS
ssh root@YOUR_VPS_IP

# Installer Docker (ubuntu)
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker $USER
newgrp docker

# Créer répertoires
mkdir -p /opt/banking-app /backups/banking
cd /opt/banking-app

# Cloner le projet
git clone YOUR_REPOSITORY .
```

### Préparer .env Secrets
```bash
# Copier template
cp .env.production .env
chmod 600 .env

# Générer secrets (copier/coller dans .env)
openssl rand -base64 32    # JWT_SECRET
openssl rand -base64 30    # DB_PASSWORD
openssl rand -base64 24    # RABBITMQ_PASSWORD
openssl rand -base64 20    # GRAFANA_PASSWORD
```

### Configuration DNS
```bash
# Vérifier domaine pointe vers VPS
nslookup banking.example.com
# ✓ Should show VPS IP address
```

---

## 2️⃣ DÉPLOYER EN PRODUCTION (5 min)

### OPTION A: Déploiement Automatisé (RECOMMANDÉ)
```bash
chmod +x Scripts/deploy-prod.sh
./Scripts/deploy-prod.sh banking.example.com registry.example.com latest
```

### OPTION B: Déploiement Manuel Étape par Étape

**Étape 1: Let's Encrypt Certificat**
```bash
mkdir -p certbot/{conf,www}

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

**Étape 2: Démarrer Services**
```bash
docker-compose -f docker-compose.prod.yml pull
docker-compose -f docker-compose.prod.yml up -d
sleep 30
docker-compose -f docker-compose.prod.yml ps
```

**Étape 3: Vérifier**
```bash
curl -k https://banking.example.com/health
# ✓ Should return OK
```

---

## 3️⃣ APRÈS DÉPLOIEMENT (Configuration)

### Accéder à Grafana
```bash
# Browser vers:
https://banking.example.com/grafana

# Credentials:
User: admin
Password: (votre GRAFANA_PASSWORD de .env)
```

### Configurer Cronjobs (Automatisation)
```bash
crontab -e

# Ajouter ces lignes:
# Backup quotidien à 2h du matin
0 2 * * * /opt/banking-app/Scripts/backup-prod.sh >> /var/log/banking/backup.log 2>&1

# Health check toutes les 5 minutes
*/5 * * * * /opt/banking-app/Scripts/health-check-prod.sh >> /var/log/banking/health.log 2>&1

# Vérifier ajoutées
crontab -l
```

---

## 4️⃣ VÉRIFICATIONS POST-DÉPLOIEMENT

### Tests Basiques
```bash
# Test health général
curl -k https://banking.example.com/health

# Test Auth API
curl -k https://banking.example.com/api/auth/health

# Test Account API
curl -k https://banking.example.com/api/accounts/health

# Test Transaction API
curl -k https://banking.example.com/api/transactions/health

# Vérifier certificat SSL
openssl s_client -connect banking.example.com:443 < /dev/null

# Voir tous les services
docker-compose -f docker-compose.prod.yml ps
```

### Vérifier Bases de Données
```bash
docker exec postgres-auth pg_isready -U bankadmin
docker exec postgres-account pg_isready -U bankadmin
docker exec postgres-transaction pg_isready -U bankadmin
```

### Vérifier RabbitMQ
```bash
docker exec rabbitmq rabbitmq-diagnostics -q ping
```

---

## 5️⃣ OPÉRATIONS QUOTIDIENNES

### Voir Logs (Troubleshooting)
```bash
# Logs service spécifique
docker-compose -f docker-compose.prod.yml logs -f auth-service

# Tous les logs
docker-compose -f docker-compose.prod.yml logs -f

# Erreurs seulement
docker-compose -f docker-compose.prod.yml logs | grep -i error
```

### Santé Système
```bash
# Espace disque
df -h
du -sh /var/lib/docker
du -sh /backups/banking

# Utilisation ressources
docker stats

# Services running
docker-compose -f docker-compose.prod.yml ps
```

### Redémarrer Services
```bash
# Redémarrer UN service
docker-compose -f docker-compose.prod.yml restart auth-service

# Redémarrer TOUS
docker-compose -f docker-compose.prod.yml restart

# Arrêter (sans supprimer données)
docker-compose -f docker-compose.prod.yml down

# Redémarrer complètement
docker-compose -f docker-compose.prod.yml down
docker-compose -f docker-compose.prod.yml up -d
```

---

## 6️⃣ SAUVEGARDES & RESTAURATION

### Backup Manuel
```bash
./Scripts/backup-prod.sh

# Vérifier backup créé
ls -lah /backups/banking/
```

### Restauration Interactive
```bash
./Scripts/restore-db.sh
# Suivre le menu
```

### Restauration Spécifique (Auth DB)
```bash
docker exec -i postgres-auth psql -U bankadmin auth_db < /backups/banking/2024-01-15_02-30-45/auth_db_*.sql.gz
```

---

## 7️⃣ CERTIFICATION SSL (Let's Encrypt)

### Renouveler Manuellement
```bash
docker exec certbot certbot renew --force-renewal
```

### Vérifier Date Expiration
```bash
openssl x509 -in certbot/conf/live/banking.example.com/cert.pem -noout -dates
```

### Auto-renewal Status
```bash
docker-compose -f docker-compose.prod.yml logs certbot
```

---

## 8️⃣ MONITORING & ALERTES

### Prometheus Metrics
```bash
# Accès direct (pour admin seulement)
curl http://localhost:9090/api/v1/query?query=up

# Voir les targets scrape
curl http://localhost:9090/api/v1/targets
```

### Grafana Dashboards
```
https://banking.example.com/grafana/d/...
```

### Health Check Manual
```bash
./Scripts/health-check-prod.sh
cat /var/log/banking/health.log
```

---

## 9️⃣ MISE À JOUR SERVICES

### Mettre à Jour Images
```bash
# Pull les dernières images
docker-compose -f docker-compose.prod.yml pull

# Redémarrer avec nouvelles images
docker-compose -f docker-compose.prod.yml up -d

# Vérifier version
docker-compose -f docker-compose.prod.yml ps
```

### Mettre à Jour Code
```bash
# Pull latest code
git pull origin main

# Si docker-compose.prod.yml modifié
docker-compose -f docker-compose.prod.yml config > /tmp/config-check

# Redéployer
./Scripts/deploy-prod.sh banking.example.com registry.example.com latest
```

---

## 🆘 TROUBLESHOOTING RAPIDE

### Service Down?
```bash
# 1. Voir les logs
docker-compose -f docker-compose.prod.yml logs -f <service_name>

# 2. Redémarrer
docker-compose -f docker-compose.prod.yml restart <service_name>

# 3. Vérifier santé
docker-compose -f docker-compose.prod.yml ps
```

### "Connection Refused"
```bash
# Vérifier service écoute
docker exec <service> netstat -tlnp

# Vérifier ports Nginx
docker logs nginx-gateway
```

### Certificat SSL Expiré?
```bash
openssl x509 -in certbot/conf/live/DOMAIN/cert.pem -noout -dates
docker exec certbot certbot renew --force-renewal
docker-compose -f docker-compose.prod.yml restart nginx
```

### Disque Plein?
```bash
# Voir utilisation
du -sh /var/lib/docker/*

# Nettoyer images inutilisées
docker image prune -a

# Archiver anciens logs
find /var/log/banking -mtime +30 -delete
```

### Mémoire Insuffisante?
```bash
# Voir utilisation
free -h
docker stats

# Limiter un service (dans docker-compose.prod.yml)
services:
  postgres-auth:
    mem_limit: 2g
```

---

## 🔒 SÉCURITÉ

### Renforcer SSH
```bash
# Éditer config
sudo nano /etc/ssh/sshd_config

# Ajouter/modifier:
PermitRootLogin no
PasswordAuthentication no
AllowUsers ubuntu

# Redémarrer
sudo systemctl restart sshd
```

### Activier Firewall
```bash
sudo ufw default deny incoming
sudo ufw default allow outgoing
sudo ufw allow 22/tcp
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw enable
sudo ufw status numbered
```

### Installer Fail2ban
```bash
sudo apt install fail2ban
sudo systemctl start fail2ban
sudo systemctl status fail2ban
```

---

## 📊 MONITORING DASHBOARD

### Browser vers:
```
Frontend:           https://banking.example.com
Grafana:           https://banking.example.com/grafana
Prometheus:        https://banking.example.com/metrics (admin only)
Health Endpoint:   https://banking.example.com/health
```

---

## 🎯 ONE-LINER DÉPLOIEMENT COMPLET

```bash
# Tout-en-un (remplacer DOMAIN + REGISTRY)
mkdir -p /opt/banking-app && cd /opt/banking-app && \
git clone YOUR_REPO . && \
cp .env.production .env && \
# Éditer .env manuellement \
chmod +x Scripts/deploy-prod.sh && \
./Scripts/deploy-prod.sh banking.example.com registry.example.com latest
```

---

## ✅ CHECKLIST FINAL

- [ ] `docker-compose -f docker-compose.prod.yml ps` → Tous "Up"
- [ ] `curl -k https://banking.example.com/health` → "OK"
- [ ] Grafana accessible → login OK
- [ ] `crontab -l` → 2 cronjobs visibles
- [ ] `ls /backups/banking/` → Au moins 1 backup
- [ ] `./Scripts/health-check-prod.sh` → PASS

---

**🚀 Vous êtes prêt pour la production!**

Pour plus de détails → Lire:
- `PRODUCTION_READY.md`
- `PRODUCTION_DEPLOYMENT_GUIDE.md`
- `VPS_DEPLOYMENT_CHECKLIST.md`
