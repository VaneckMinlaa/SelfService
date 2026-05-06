# ⚡ Quick Setup - Commandes Copy-Paste

## 1️⃣ Générer les Clés SSH (Local)

```bash
# Générer une clé ED25519
ssh-keygen -t ed25519 -N "" -f ~/.ssh/id_gitlab

# Afficher la clé publique (à copier sur VPS)
cat ~/.ssh/id_gitlab.pub

# Afficher la clé privée (à copier dans GitLab)
cat ~/.ssh/id_gitlab
```

**💾 Sauvegarder:**
- Copier le contenu de `~/.ssh/id_gitlab` dans un fichier sécurisé
- Copier le contenu de `~/.ssh/id_gitlab.pub` pour l'étape suivante

---

## 2️⃣ Installer la Clé sur le VPS

### SSH vers le VPS:
```bash
ssh ubuntu@YOUR_VPS_IP
```

### Installer la clé publique:
```bash
# En tant qu'utilisateur ubuntu sur le VPS:
mkdir -p ~/.ssh
chmod 700 ~/.ssh

# Ajouter la clé publique
cat >> ~/.ssh/authorized_keys << 'EOF'
PASTE_HERE_THE_CONTENT_OF_id_gitlab.pub
EOF

# Fixer les permissions
chmod 600 ~/.ssh/authorized_keys

# Vérifier
cat ~/.ssh/authorized_keys
```

### Tester la connexion (depuis votre local):
```bash
ssh -i ~/.ssh/id_gitlab ubuntu@YOUR_VPS_IP "echo 'SSH Connection OK'"
```

---

## 3️⃣ Variables GitLab - Copy-Paste Ready

### Accès:
**GitLab** → Settings ⚙️ → **CI/CD** → **Variables** → **Add Variable**

### Pour chaque variable, faire:
1. **Key:** (nom de la variable)
2. **Value:** (copier-coller la valeur)
3. **Protected:** ✅ (pour les secrets)
4. **Save variable**

---

## 📝 STAGING Variables

```
Key: STAGING_HOST
Value: 192.168.1.100
Type: Variable
Protected: ☑

---

Key: STAGING_USER
Value: ubuntu
Type: Variable

---

Key: STAGING_DEPLOY_PATH
Value: /opt/banking-app
Type: Variable

---

Key: STAGING_SSH_PRIVATE_KEY
Value: (PASTE ENTIRE CONTENT OF ~/.ssh/id_gitlab)
Type: Variable
Protected: ☑

---

Key: STAGING_DOMAIN
Value: staging.banking.example.com
Type: Variable
```

---

## 📝 PRODUCTION Variables

```
Key: PRODUCTION_HOST
Value: 192.168.1.200
Type: Variable
Protected: ☑

---

Key: PRODUCTION_USER
Value: ubuntu
Type: Variable

---

Key: PRODUCTION_DEPLOY_PATH
Value: /opt/banking-app
Type: Variable

---

Key: PRODUCTION_SSH_PRIVATE_KEY
Value: (PASTE ENTIRE CONTENT OF ~/.ssh/id_gitlab)
Type: Variable
Protected: ☑

---

Key: PRODUCTION_DOMAIN
Value: banking.example.com
Type: Variable
```

---

## 📢 SLACK Variables (Optionnel)

```
Key: SLACK_WEBHOOK_URL
Value: https://hooks.slack.com/services/T00000000/B00000000/XXXXXXXXXXXXXXXXXXXX
Type: Variable
Protected: ☑
```

---

## 🖥️ Setup VPS - Commandes Complètes

### Sur STAGING et PRODUCTION (en tant que ubuntu):

```bash
# Se connecter
ssh ubuntu@YOUR_VPS_IP

# Créer les répertoires
mkdir -p /opt/banking-app /backups/banking /var/log/banking
cd /opt/banking-app

# Cloner le repo
git clone https://gitlab.com/YOUR_GROUP/banking-portal.git .

# Vérifier la structure
ls -la

# Créer le fichier .env.production
cat > .env.production << 'EOF'
# === Docker Registry ===
DOCKER_REGISTRY=registry.gitlab.com/your-group
IMAGE_TAG=latest

# === Domaine ===
DOMAIN=staging.banking.example.com
LETSENCRYPT_EMAIL=admin@example.com

# === Secrets (⚠️ À GÉNÉRER!) ===
JWT_SECRET=GenerateWith32Chars_openssl_rand_base64_32
DB_PASSWORD=GenerateStrong_openssl_rand_base64_30

# === RabbitMQ ===
RABBITMQ_USERNAME=rabbitmq
RABBITMQ_PASSWORD=GenerateStrong_password

# === PostgreSQL ===
POSTGRES_USER=postgres
POSTGRES_PASSWORD=GenerateStrong_password

# === Grafana ===
GRAFANA_PASSWORD=GenerateStrong_password

# === Application ===
LOG_LEVEL=WARN
APP_ENV=production

# === SMTP (optionnel, pour notifications) ===
SMTP_HOST=smtp-relay.brevo.com
SMTP_PORT=587
SMTP_USERNAME=your-email@example.com
SMTP_PASSWORD=your-api-key
SMTP_FROM=noreply@example.com

# === CORS ===
CORS_ALLOWED_ORIGINS=https://staging.banking.example.com
EOF

# Permissions
chmod 600 .env.production
chmod +x Scripts/*.sh

# Créer logs directory
sudo mkdir -p /var/log/banking
sudo chown ubuntu:ubuntu /var/log/banking

# Tester la connexion Docker
docker ps
```

---

## 🚀 Premier Déploiement - Staging

### Local (git push sur develop):

```bash
# Assurez-vous que le repo est cloné localement
cd /path/to/banking-portal

# Créer une branche ou aller sur develop
git checkout develop

# Faire un changement (optionnel)
git add -A
git commit -m "Deploy: Initial production setup"

# Pousser vers GitLab
git push origin develop

# Aller sur GitLab:
# CI/CD → Pipelines → Regarder le déploiement en direct
```

### Vérifier sur le VPS:

```bash
ssh ubuntu@STAGING_IP

cd /opt/banking-app

# Voir l'état des services
docker-compose -f docker-compose.prod.yml ps

# Voir les logs
docker-compose logs --tail=50

# Vérifier que tout est up
curl -s https://staging.banking.example.com/api/auth/health
```

---

## 📦 Déploiement Production

### Local (tag ou push sur main):

```bash
# Option 1: Push directement sur main
git checkout main
git add -A
git commit -m "Production release: v1.0.0"
git push origin main
# ✅ Déploiement auto en production!

# Option 2: Créer un tag
git tag -a v1.0.0 -m "Release 1.0.0"
git push origin v1.0.0
# ✅ Déploiement auto en production!
```

### Monitoring en direct:

```bash
ssh ubuntu@PRODUCTION_IP

cd /opt/banking-app

# Logs de déploiement (en direct pendant le push)
tail -f /var/log/banking/deploy.log

# État des services
watch -n 2 "docker-compose -f docker-compose.prod.yml ps"

# Logs des services
docker-compose logs -f
```

### ⏰ Temps d'exécution:
- Build: 3-5 min
- Deploy Production: 2-3 min
- **Total: 5-8 minutes** du push origin main à la production live

### 🚀 C'est automatique!
Pas besoin de cliquer sur un bouton dans GitLab. Dès que vous faites `git push origin main`, la pipeline démarre automatiquement et déploie en production!

---

## 🔄 Rollback d'Urgence

### Via GitLab UI:

1. **CI/CD** → **Pipelines**
2. Chercher la pipeline du déploiement échoué
3. **Rollback** → Cliquer ▶️
4. Attendre la restauration

### Via SSH (manuel):

```bash
ssh ubuntu@PRODUCTION_IP

cd /opt/banking-app

# Voir les backups
ls -lah /backups/banking/

# Restaurer la dernière (remplacer par votre timestamp)
./Scripts/restore-db.sh all /backups/banking/2026-05-06_14-30-45

# Redémarrer les services
docker-compose -f docker-compose.prod.yml up -d

# Vérifier
docker-compose -f docker-compose.prod.yml ps
```

---

## ✅ Vérification Final Checklist

```bash
# 1. Tester SSH depuis votre local
ssh -i ~/.ssh/id_gitlab ubuntu@STAGING_IP "echo OK"
ssh -i ~/.ssh/id_gitlab ubuntu@PRODUCTION_IP "echo OK"

# 2. Vérifier Docker sur les VPS
ssh ubuntu@STAGING_IP "docker ps"
ssh ubuntu@PRODUCTION_IP "docker ps"

# 3. Vérifier les fichiers
ssh ubuntu@STAGING_IP "ls -la /opt/banking-app/"
ssh ubuntu@PRODUCTION_IP "ls -la /opt/banking-app/"

# 4. Vérifier .env.production
ssh ubuntu@STAGING_IP "head -n 5 /opt/banking-app/.env.production"

# 5. Test Git clone
ssh ubuntu@STAGING_IP "cd /opt/banking-app && git log --oneline | head -5"
```

---

## 🐛 Commandes Utiles pour Troubleshooting

### Vérifier la connexion SSH:
```bash
ssh -v -i ~/.ssh/id_gitlab ubuntu@VPS_IP

# Si "Permission denied":
ssh ubuntu@VPS_IP "cat ~/.ssh/authorized_keys | grep 'ed25519'"
```

### Vérifier Docker:
```bash
# Sur le VPS
docker ps
docker-compose -f docker-compose.prod.yml ps
docker-compose logs -f auth-service
```

### Vérifier les permissions:
```bash
# Sur le VPS
ls -la /opt/banking-app/Scripts/
chmod +x /opt/banking-app/Scripts/*.sh
```

### Vérifier les backups:
```bash
# Sur le VPS
ls -lah /backups/banking/
du -sh /backups/banking/
```

### Nettoyer les images Docker:
```bash
# Sur le VPS
docker image prune -a --force
docker system prune -a --volumes --force
```

---

## 📊 Monitoring

### Vérifier les services:
```bash
# Sur le VPS
curl -s https://banking.example.com/api/auth/health | jq
curl -s https://banking.example.com/api/accounts/health | jq
curl -s https://banking.example.com/metrics
```

### Voir les logs:
```bash
# Tous les services
docker-compose logs -f

# Un service spécifique
docker-compose logs -f auth-service
docker-compose logs -f nginx

# Historique
docker-compose logs --tail=100 postgres-auth
```

### Vérifier les ressources:
```bash
# CPU et mémoire
docker stats

# Espace disque
df -h
du -sh /backups/banking/
```

---

## 🎯 Prochaines Étapes Recommandées

1. ✅ Générer SSH: `ssh-keygen -t ed25519 -N "" -f ~/.ssh/id_gitlab`
2. ✅ Installer sur VPS: Copier la clé publique
3. ✅ Configurer Variables GitLab: 11 variables (voir sections ci-dessus)
4. ✅ Setup VPS: Créer /opt/banking-app, clone repo, .env
5. ✅ Tester Staging: Push sur develop
6. ✅ Tester Production: Tag et déploiement manuel
7. ✅ Activer Slack: (optionnel) pour notifications

**Durée estimée:** 20-30 minutes pour tout setup

**Questions?** Regardez les logs:
- GitLab: CI/CD → Pipelines → Job Log
- VPS: tail -f /var/log/banking/deploy.log
- Docker: docker-compose logs
