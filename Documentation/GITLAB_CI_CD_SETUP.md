# 🚀 Configuration GitLab CI/CD - Guide Complet

## 📋 Vue d'ensemble

Ce guide vous aide à configurer la pipeline CI/CD pour automatiser:
- ✅ Build des 5 microservices
- ✅ Push vers Docker Registry
- ✅ Déploiement sur Staging (automatique sur `develop`)
- ✅ Déploiement Production (automatique sur `main` ou tags)
- ✅ Rollback manuel avec restauration DB
- ✅ Notifications Slack

---

## 1️⃣ Générer les Clés SSH (Très Important!)

### Sur votre machine locale:

```bash
# Générer une clé ED25519 (moderne et sécurisée)
ssh-keygen -t ed25519 -N "" -f ~/.ssh/id_gitlab

# Résultat:
# - ~/.ssh/id_gitlab (clé privée - À GARDER SECRÈTE)
# - ~/.ssh/id_gitlab.pub (clé publique - À installer sur VPS)
```

### Copier la clé publique sur le VPS:

```bash
# Sur le VPS (en tant que ubuntu ou root)
mkdir -p ~/.ssh
chmod 700 ~/.ssh

# Ajouter la clé publique
cat >> ~/.ssh/authorized_keys << 'EOF'
ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIK... (contenu de ~/.ssh/id_gitlab.pub)
EOF

chmod 600 ~/.ssh/authorized_keys
```

### Obtenir le contenu de la clé privée:

```bash
# Pour copier dans les variables GitLab:
cat ~/.ssh/id_gitlab
```

**Copier TOUT le contenu, incluant `-----BEGIN` et `-----END`**

---

## 2️⃣ Configurer les Variables GitLab CI/CD

### Accès:
1. **GitLab UI** → Votre Projet
2. **Settings** (⚙️) → **CI/CD**
3. **Variables** → **Add Variable**

### Variables à configurer:

#### 🔐 STAGING - Accès VPS

| Variable | Type | Exemple | Notes |
|----------|------|---------|-------|
| `STAGING_HOST` | String | `192.168.1.100` | IP publique du VPS staging |
| `STAGING_USER` | String | `ubuntu` | Utilisateur SSH du VPS |
| `STAGING_DEPLOY_PATH` | String | `/opt/banking-app` | Répertoire de déploiement |
| `STAGING_SSH_PRIVATE_KEY` | Variable (Protected) | `-----BEGIN ...` | Contenu de ~/.ssh/id_gitlab |
| `STAGING_DOMAIN` | String | `staging.banking.example.com` | URL du staging |

#### 🔐 PRODUCTION - Accès VPS

| Variable | Type | Exemple | Notes |
|----------|------|---------|-------|
| `PRODUCTION_HOST` | String | `192.168.1.200` | IP publique du VPS prod |
| `PRODUCTION_USER` | String | `ubuntu` | Utilisateur SSH du VPS |
| `PRODUCTION_DEPLOY_PATH` | String | `/opt/banking-app` | Répertoire de déploiement |
| `PRODUCTION_SSH_PRIVATE_KEY` | Variable (Protected) | `-----BEGIN ...` | Contenu de ~/.ssh/id_gitlab |
| `PRODUCTION_DOMAIN` | String | `banking.example.com` | URL de production |

#### 📢 Notifications

| Variable | Type | Exemple |
|----------|------|---------|
| `SLACK_WEBHOOK_URL` | Variable (Protected) | `https://hooks.slack.com/services/T00...` |

---

## 3️⃣ Configuration Slack (Optionnel mais Recommandé)

### Créer un Webhook Slack:

1. Aller sur [api.slack.com](https://api.slack.com/apps)
2. **Create New App** → **From scratch**
3. **App Name:** Banking-Deployments
4. **Workspace:** Sélectionner
5. **Features** (à gauche) → **Incoming Webhooks**
6. **Add New Webhook to Workspace**
7. Sélectionner le canal `#deployments` (ou créer)
8. **Copier le Webhook URL**
9. **Ajouter dans GitLab** comme `SLACK_WEBHOOK_URL` (Protected)

---

## 4️⃣ Setup Préalable sur le VPS

### Sur le STAGING et PRODUCTION VPS:

```bash
# Se connecter au VPS
ssh ubuntu@VPS_IP

# Créer la structure
mkdir -p /opt/banking-app /backups/banking
cd /opt/banking-app

# Cloner le repo
git clone https://gitlab.com/YOUR_GROUP/banking-portal.git .

# Copier les fichiers
cp -r Scripts/ .
cp docker-compose.prod.yml .
cp .env.production .

# Éditer les secrets
nano .env.production
# Remplacer:
# - JWT_SECRET
# - DB_PASSWORD
# - RABBITMQ_USERNAME/PASSWORD
# - DOMAIN
# - DOCKER_REGISTRY
# - SMTP config

# Permissions
chmod +x Scripts/*.sh

# Créer les répertoires de logs
mkdir -p /var/log/banking
chmod 755 /var/log/banking
```

### Exemple .env.production complet:

```env
# === Docker Registry ===
DOCKER_REGISTRY=registry.gitlab.com/your-group
IMAGE_TAG=latest

# === Domaine ===
DOMAIN=banking.example.com
LETSENCRYPT_EMAIL=admin@example.com

# === Secrets (GÉNÉRER!) ===
JWT_SECRET=S3cureJWTSecretKeyWith32CharactersMinimum1234567890
DB_PASSWORD=SuperSecureDBPassword123456789012345

# === RabbitMQ ===
RABBITMQ_USERNAME=rabbitmq_user
RABBITMQ_PASSWORD=RabbitMQSecurePassword123456

# === PostgreSQL ===
POSTGRES_USER=postgres
POSTGRES_PASSWORD=PostgresSecurePassword123456

# === Monitoring ===
GRAFANA_PASSWORD=GrafanaSecurePassword123456
LOG_LEVEL=WARN

# === SMTP (pour notifications) ===
SMTP_HOST=smtp-relay.brevo.com
SMTP_PORT=587
SMTP_USERNAME=your-brevo-email@example.com
SMTP_PASSWORD=your-brevo-smtp-key
SMTP_FROM=noreply@banking.example.com

# === Application ===
APP_ENV=production
CORS_ALLOWED_ORIGINS=https://banking.example.com
```

---

## 5️⃣ Premier Déploiement - Staging

### Étape 1: Tester la pipeline avec `develop`

```bash
# Sur votre machine locale
git checkout develop
git add -A
git commit -m "Deploy: Production configuration ready"
git push origin develop

# Regarder dans GitLab: Pipelines → En cours
```

### Étape 2: Vérifier le déploiement

```bash
# La pipeline devrait:
1. ✅ Lint: Valider docker-compose.prod.yml
2. ✅ Build: 5 services en parallèle
3. ✅ Deploy Staging: Auto-déclenché sur develop
4. 🔔 Slack: Notification de succès/échec
```

---

## 6️⃣ Déploiement Production

### Étape 1: Pousser sur `main`

```bash
git checkout main
git add -A
git commit -m "Production release: v1.0.0"
git push origin main

# ✅ Déploiement automatique déclenché!
```

### Ou créer un tag:

```bash
git tag -a v1.0.0 -m "Release 1.0.0"
git push origin v1.0.0

# ✅ Déploiement automatique déclenché!
```

### Étape 2: Monitoring en temps réel

```bash
# Sur le VPS de production
ssh ubuntu@PRODUCTION_IP

cd /opt/banking-app

# Logs de déploiement (en direct)
tail -f /var/log/banking/deploy.log

# État des services
watch -n 2 "docker-compose -f docker-compose.prod.yml ps"

# Vérifier la santé
docker-compose logs -f nginx
```

### 🎯 Workflow automatique:
1. ✅ Code pushed → GitLab pipeline démarre
2. ✅ Lint valide la config
3. ✅ Build 5 services en parallèle (~5 min)
4. ✅ **Deploy Production automatique** (~2-3 min)
5. ✅ Health checks et notifications Slack

---

## 7️⃣ Rollback en Cas de Problème

### Manuel via GitLab:

1. **CI/CD** → **Pipelines** → Votre pipeline
2. **Rollback** → Bouton ▶️ **Play**

### Manuel via SSH:

```bash
ssh ubuntu@PROD_VPS

cd /opt/banking-app

# Voir les backups
ls -la /backups/banking/

# Restaurer (exemple)
./Scripts/restore-db.sh all /backups/banking/2026-05-06_14-30-45

# Relancer les services
docker-compose -f docker-compose.prod.yml up -d
```

---

## 8️⃣ Vérification Final

### Checklist avant premier déploiement:

- [ ] Clés SSH générées et installées sur VPS
- [ ] Variables GitLab configurées (voir section 2)
- [ ] `.env.production` créé et édité sur VPS
- [ ] Répertoires `/opt/banking-app` créés
- [ ] Permissions correctes (`chmod +x Scripts/*.sh`)
- [ ] Docker & Docker Compose installés sur VPS
- [ ] Tests de connexion SSH: `ssh ubuntu@VPS_IP docker ps`
- [ ] Slack webhook configuré (optionnel)

### Test de connexion:

```bash
# Depuis votre local:
ssh -i ~/.ssh/id_gitlab ubuntu@STAGING_VPS_IP "cd /opt/banking-app && docker-compose -f docker-compose.prod.yml ps"
```

---

## 🔥 Troubleshooting

### ❌ "SSH: Connection refused"
```bash
# VPS firewall bloque le port 22
# Ouvrir SSH:
sudo ufw allow 22/tcp
sudo ufw enable

# Ou vérifier si sshd tourne:
sudo systemctl status ssh
```

### ❌ "docker: not found"
```bash
# Docker pas installé sur le VPS
wget https://get.docker.com -O - | sh
sudo usermod -aG docker $USER
newgrp docker
```

### ❌ "Permission denied (publickey)"
```bash
# Vérifier authorized_keys
ssh ubuntu@VPS_IP "cat ~/.ssh/authorized_keys"

# Réinstaller la clé publique:
ssh-copy-id -i ~/.ssh/id_gitlab.pub ubuntu@VPS_IP
```

### ❌ "Health checks failed"
```bash
# Sur le VPS, vérifier les logs:
docker-compose logs --tail=100 auth-service
docker-compose logs --tail=100 nginx

# Vérifier si PostgreSQL est accessible:
docker-compose exec postgres-auth pg_isready
```

---

## 📖 Commandes Utiles

```bash
# Voir les logs de la pipeline GitLab
# GitLab UI → CI/CD → Pipelines → Job name → Job log

# Déclencher manuellement une build
# GitLab UI → CI/CD → Run Pipeline → Create Pipeline

# Voir les logs du déploiement sur VPS
tail -f /var/log/banking/deploy.log
tail -f /var/log/banking/health.log

# Vérifier les images Docker
docker images | grep banking

# Vérifier les backups
ls -la /backups/banking/

# Nettoyer les images anciennes
docker image prune -a --force
```

---

## 🎯 Prochaines Étapes

1. ✅ Générer les clés SSH
2. ✅ Configurer les variables GitLab
3. ✅ Setup les VPS (Staging & Production)
4. ✅ Tester avec une poussée sur `develop`
5. ✅ Valider le déploiement Staging
6. ✅ Créer un tag et déployer en Production
7. ✅ Configurer les alertes et monitoring

**Besoin d'aide?** Vérifiez les logs dans:
- **GitLab:** CI/CD → Pipelines → Job Log
- **VPS:** `/var/log/banking/deploy.log`
- **Docker:** `docker-compose logs -f`
