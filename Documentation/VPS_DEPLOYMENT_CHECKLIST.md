# Checklist Production VPS - Guide Étape par Étape

## ✅ PHASE 0: PRÉ-DÉPLOIEMENT (À faire AVANT le déploiement)

### 1. Infrastructure VPS
- [ ] VPS provisionné (Ubuntu 22.04 LTS, 4 CPU, 8GB RAM, 50GB SSD)
- [ ] SSH accessible (port 22)
- [ ] IP publique attribuée
- [ ] Domaine DNS pointe vers IP VPS

### 2. Prérequis Logiciels
- [ ] Docker installé: `docker --version` ≥ 24.0
- [ ] Docker Compose installé: `docker-compose --version` ≥ 2.0
- [ ] Git installé: `git --version`
- [ ] OpenSSL disponible: `openssl version`

### 3. Configuration Sécurité
- [ ] SSH key-based auth configuré (no password)
- [ ] Firewall UFW activé avec règles minimales
- [ ] Seuls ports 22, 80, 443 ouverts
- [ ] Fail2ban ou rate limiting SSH configuré

### 4. Domaine & DNS
- [ ] Domaine enregistré (ex: banking.example.com)
- [ ] Enregistrement DNS A pointe vers IP VPS
- [ ] TTL du DNS = 300s (5 min)
- [ ] Vérifier DNS: `nslookup banking.example.com`

---

## ✅ PHASE 1: PRÉPARATION PROJET (2h)

### 1. Cloner le Projet
```bash
mkdir -p /opt/banking-app
cd /opt/banking-app
git clone YOUR_GIT_REPO .
ls -la  # Vérifier présence des fichiers
```

### 2. Vérifier les Fichiers de Configuration
```bash
# Vérifier fichiers NEW (créés pour vous)
ls -la docker-compose.prod.yml
ls -la .env.production
ls -la Docker/nginx-prod.conf
ls -la Docker/prometheus/prometheus-prod.yml
ls -la PRODUCTION_DEPLOYMENT_GUIDE.md
ls -la Scripts/deploy-prod.sh
```

### 3. Créer & Sécuriser `.env`
```bash
# Copier template
cp .env.production .env

# Sécuriser (root only)
chmod 600 .env

# Éditer
nano .env
```

**SECRETS À GÉNÉRER (remplacer les placeholders):**

```bash
# 1. JWT_SECRET (32+ caractères, base64)
JWT_SECRET=$(openssl rand -base64 32)
echo "JWT_SECRET=$JWT_SECRET"

# 2. DB_PASSWORD (30+ caractères forts)
DB_PASSWORD=$(openssl rand -base64 30)
echo "DB_PASSWORD=$DB_PASSWORD"

# 3. RABBITMQ_PASSWORD
RABBITMQ_PASSWORD=$(openssl rand -base64 24)
echo "RABBITMQ_PASSWORD=$RABBITMQ_PASSWORD"

# 4. GRAFANA_PASSWORD
GRAFANA_PASSWORD=$(openssl rand -base64 20)
echo "GRAFANA_PASSWORD=$GRAFANA_PASSWORD"
```

**Puis éditer `.env`:**
```bash
nano .env

# Remplacer ces valeurs:
DOMAIN=banking.example.com              # ← VOTRE DOMAINE
JWT_SECRET=<valeur générée ci-dessus>
DB_PASSWORD=<valeur générée ci-dessus>
RABBITMQ_PASSWORD=<valeur générée ci-dessus>
GRAFANA_PASSWORD=<valeur générée ci-dessus>
DOCKER_REGISTRY=registry.example.com    # ← VOTRE REGISTRY
IMAGE_TAG=latest
LOG_LEVEL=WARN                          # ← Production = WARN/ERROR
SMTP_HOST=smtp-relay.brevo.com
SMTP_USERNAME=<votre-email-brevo>
SMTP_PASSWORD=<votre-brevo-smtp-pass>
BREVO_API_KEY=<votre-api-key>
```

### 4. Vérifier les Scripts
```bash
chmod +x Scripts/deploy-prod.sh
chmod +x Scripts/backup-prod.sh
chmod +x Scripts/restore-db.sh
chmod +x Scripts/health-check-prod.sh

# Vérifier syntaxe bash
bash -n Scripts/deploy-prod.sh
```

---

## ✅ PHASE 2: DÉPLOIEMENT INITIAL (30 min)

### 1. Docker Registry Login
```bash
# Vérifier accès au registry
docker login registry.example.com
# Entrer username/password

# Vérifier images disponibles
docker-compose -f docker-compose.prod.yml pull
```

### 2. Let's Encrypt Setup
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

# Vérifier certificat créé
ls -la certbot/conf/live/banking.example.com/
openssl x509 -in certbot/conf/live/banking.example.com/cert.pem -text -noout
```

### 3. Lancer Déploiement Automatisé
```bash
# OPTION 1: Script automatisé (RECOMMANDÉ)
./Scripts/deploy-prod.sh banking.example.com registry.example.com latest

# OPTION 2: Manuel (étape par étape)
docker-compose -f docker-compose.prod.yml pull
docker-compose -f docker-compose.prod.yml up -d
```

### 4. Attendre Initialisation
```bash
# Attendre 30-60 secondes
sleep 60

# Vérifier tous les services
docker-compose -f docker-compose.prod.yml ps

# ✓ Tous doivent être "Up" ou "healthy"
```

---

## ✅ PHASE 3: VÉRIFICATION POST-DÉPLOIEMENT (15 min)

### 1. Tests Connectivité
```bash
# Test health endpoint
curl -k https://banking.example.com/health
# ✓ Expected: "OK"

# Test SSL certificate
openssl s_client -connect banking.example.com:443 < /dev/null
# ✓ Verify: cert is valid

# Test DNS resolution
nslookup banking.example.com
# ✓ Should resolve to VPS IP
```

### 2. Tests Services
```bash
# Test Auth API
curl -k -X POST https://banking.example.com/api/auth/health
# ✓ Expected: {"status":"UP"}

# Test Account API
curl -k https://banking.example.com/api/accounts/health
# ✓ Expected: {"status":"UP"}

# Test Transaction API
curl -k https://banking.example.com/api/transactions/health
# ✓ Expected: {"status":"UP"}
```

### 3. Vérifier Logs
```bash
# Voir les logs de déploiement
docker-compose -f docker-compose.prod.yml logs --tail=50 auth-service

# Vérifier pas d'erreurs critiques
docker-compose -f docker-compose.prod.yml logs | grep -i "error\|exception"
```

### 4. Vérifier Bases de Données
```bash
# Test connexion PostgreSQL Auth
docker exec postgres-auth pg_isready -U bankadmin
# ✓ Expected: "accepting connections"

docker exec postgres-account pg_isready -U bankadmin
docker exec postgres-transaction pg_isready -U bankadmin
```

### 5. Vérifier RabbitMQ
```bash
# Test RabbitMQ
docker exec rabbitmq rabbitmq-diagnostics -q ping
# ✓ Expected: (no output = OK)

# Voir la console management (optionnel)
# http://VPS_IP:15672
# User: RABBITMQ_USERNAME (depuis .env)
# Pass: RABBITMQ_PASSWORD
```

---

## ✅ PHASE 4: CONFIGURATION MONITORING (15 min)

### 1. Accéder à Grafana
```
https://banking.example.com/grafana

Username: admin
Password: (votre GRAFANA_PASSWORD de .env)
```

### 2. Ajouter Prometheus Data Source
- Configuration → Data Sources → Add
- Prometheus server URL: `http://prometheus:9090`
- Save & test → ✓ OK

### 3. Importer Dashboards
Grafana ID 1 à 10 (Kubernetes, Docker, etc.)
- Grafana → Dashboards → Import
- Entrer ID: `1860` (Node Exporter)
- Load → Select Prometheus → Import

### 4. Configurer Alertes (Optionnel)
- Alerting → Create Alert Rule
- Configurer pour high CPU, high memory, SSL expiring

---

## ✅ PHASE 5: BACKUP & DISASTER RECOVERY (30 min)

### 1. Test Backup Manuel
```bash
./Scripts/backup-prod.sh

# Vérifier backup créé
ls -lah /backups/banking/
du -sh /backups/banking/
```

### 2. Test Restauration (dans un ENV de TEST!)
```bash
# Créer nouvelle DB de test
# ... créer un backup de test ...

# Test restore
./Scripts/restore-db.sh
# Sélectionner option interactive

# ✓ Verify restore réussi
```

### 3. Configurer Cron Automatisé
```bash
# Éditer crontab
crontab -e

# Ajouter:
# Backup quotidien à 2h du matin
0 2 * * * /opt/banking-app/Scripts/backup-prod.sh >> /var/log/banking/backup.log 2>&1

# Health check toutes les 5 min
*/5 * * * * /opt/banking-app/Scripts/health-check-prod.sh >> /var/log/banking/health.log 2>&1

# Vérifier
crontab -l
```

### 4. Setup Backup Cloud (Optionnel mais Recommandé)
```bash
# S3 backup
aws s3 sync /backups/banking s3://my-backup-bucket/banking

# Ou rsync
rsync -avz /backups/banking/ backup-server:/backups/banking/
```

---

## ✅ PHASE 6: SÉCURISATION SUPPLÉMENTAIRE (30 min)

### 1. Renforcer SSH
```bash
# Éditer config SSH
nano /etc/ssh/sshd_config

# Vérifier/modifier:
Port 22
PermitRootLogin no                  # ✓ Désactiver root
PasswordAuthentication no            # ✓ Key-based only
PubkeyAuthentication yes
AllowUsers ubuntu appuser           # ✓ Utilisateurs spécifiques

# Redémarrer
sudo systemctl restart sshd
```

### 2. Configurer Firewall UFW
```bash
sudo ufw status
sudo ufw default deny incoming
sudo ufw default allow outgoing

# Ouvrir ports essentiels
sudo ufw allow 22/tcp     # SSH
sudo ufw allow 80/tcp     # HTTP (Let's Encrypt renewal)
sudo ufw allow 443/tcp    # HTTPS

# Activer (si pas encore)
sudo ufw enable

# Vérifier
sudo ufw status numbered
```

### 3. Installer Fail2ban
```bash
sudo apt install fail2ban

# Config SSH protection
sudo nano /etc/fail2ban/jail.local

[sshd]
enabled = true
port = ssh
logpath = /var/log/auth.log
maxretry = 3
findtime = 600
bantime = 3600

# Redémarrer
sudo systemctl restart fail2ban
```

### 4. Setup Monitoring d'Alertes
```bash
# Email alerts (via Sentry ou Grafana)
# Configurer destinataire email
# Tester notification

# Ou webhook Discord/Slack
# Ajouter webhook URL
```

---

## ✅ PHASE 7: VÉRIFICATIONS FINALES

### 1. Checklist SSL/TLS
- [ ] HTTPS marche (curl -k)
- [ ] Certificat valide (openssl s_client)
- [ ] HSTS header présent
- [ ] CSP header présent
- [ ] Redirection HTTP → HTTPS OK
- [ ] Certificat auto-renewal configuré

### 2. Checklist Données
- [ ] Backup créé et compressé
- [ ] Restore testé (sur environnement de test)
- [ ] Backup cron job actif (crontab -l)
- [ ] Rétention 30 jours en place

### 3. Checklist Monitoring
- [ ] Prometheus scrape OK
- [ ] Grafana accès OK
- [ ] Health checks tournent
- [ ] Logs structurés (JSON)

### 4. Checklist Sécurité
- [ ] `.env` sécurisé (600 permissions)
- [ ] Secrets forts générés
- [ ] Firewall restrictif
- [ ] SSH hardened
- [ ] Fail2ban actif
- [ ] Non-root containers

---

## 🎯 Checklist Quotidienne

Une fois en production:

### Chaque jour
- [ ] Vérifier espace disque: `df -h`
- [ ] Vérifier logs erreurs: `docker-compose -f docker-compose.prod.yml logs | grep -i error`
- [ ] Vérifier backup du jour: `ls -la /backups/banking/`

### Chaque semaine
- [ ] Tester restauration de backup
- [ ] Vérifier certificat SSL (30+ jours restants)
- [ ] Analyser Grafana metrics

### Chaque mois
- [ ] Mettre à jour images Docker: `docker-compose -f docker-compose.prod.yml pull`
- [ ] Analyser logs pour patterns
- [ ] Vérifier backup cloud sync

---

## 🆘 En Cas de Problème

### Service Down?
```bash
# Voir logs
docker-compose -f docker-compose.prod.yml logs -f <service_name>

# Redémarrer
docker-compose -f docker-compose.prod.yml restart <service_name>

# Vérifier santé
docker-compose -f docker-compose.prod.yml ps
```

### Base Inaccessible?
```bash
docker exec postgres-auth pg_isready -U bankadmin
docker-compose -f docker-compose.prod.yml logs postgres-auth
```

### SSL Certificat Problème?
```bash
openssl x509 -in certbot/conf/live/DOMAIN/cert.pem -noout -dates
docker exec certbot certbot renew --force-renewal
```

Voir `PRODUCTION_DEPLOYMENT_GUIDE.md` pour troubleshooting complet.

---

**🎉 Bonne chance avec votre déploiement production!**
