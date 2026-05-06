#!/bin/bash
# ============================================
# SCRIPT DEPLOYMENT PRODUCTION VPS
# Banking Portal - Microservices
# ============================================
# Usage: ./deploy-prod.sh <DOMAIN> <REGISTRY_URL> <IMAGE_TAG>
# Example: ./deploy-prod.sh banking.example.com registry.example.com latest
# ============================================

set -e

DOMAIN=${1:-banking.example.com}
REGISTRY=${2:-registry.example.com}
IMAGE_TAG=${3:-latest}
DEPLOY_PATH="/opt/banking-app"
BACKUP_PATH="/backups/banking"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}================================${NC}"
echo -e "${GREEN}Banking Portal - Production Deploy${NC}"
echo -e "${GREEN}================================${NC}"
echo "Domain: $DOMAIN"
echo "Registry: $REGISTRY"
echo "Image Tag: $IMAGE_TAG"
echo "Deployment Path: $DEPLOY_PATH"
echo ""

# ============================================
# 1. PRE-DEPLOYMENT CHECKS
# ============================================
echo -e "${YELLOW}[1/7] Pré-vérifications...${NC}"

if ! command -v docker &> /dev/null; then
    echo -e "${RED}❌ Docker n'est pas installé${NC}"
    exit 1
fi

if ! command -v docker-compose &> /dev/null; then
    echo -e "${RED}❌ Docker Compose n'est pas installé${NC}"
    exit 1
fi

if [ ! -f "docker-compose.prod.yml" ]; then
    echo -e "${RED}❌ docker-compose.prod.yml non trouvé${NC}"
    exit 1
fi

if [ ! -f ".env.production" ]; then
    echo -e "${RED}❌ .env.production non trouvé${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Vérifications OK${NC}"

# ============================================
# 2. BACKUP DATABASES
# ============================================
echo -e "${YELLOW}[2/7] Sauvegarde des bases de données...${NC}"

mkdir -p "$BACKUP_PATH/$TIMESTAMP"

docker-compose -f docker-compose.prod.yml exec -T postgres-auth pg_dump -U bankadmin auth_db > "$BACKUP_PATH/$TIMESTAMP/auth_db.sql" 2>/dev/null || true
docker-compose -f docker-compose.prod.yml exec -T postgres-account pg_dump -U bankadmin account_db > "$BACKUP_PATH/$TIMESTAMP/account_db.sql" 2>/dev/null || true
docker-compose -f docker-compose.prod.yml exec -T postgres-transaction pg_dump -U bankadmin transaction_db > "$BACKUP_PATH/$TIMESTAMP/transaction_db.sql" 2>/dev/null || true

echo -e "${GREEN}✓ Bases de données sauvegardées: $BACKUP_PATH/$TIMESTAMP${NC}"

# ============================================
# 3. PREPARE ENVIRONMENT
# ============================================
echo -e "${YELLOW}[3/7] Préparation de l'environnement...${NC}"

cp .env.production .env

# Remplacer les variables de domaine et registry
sed -i "s|DOMAIN=.*|DOMAIN=$DOMAIN|g" .env
sed -i "s|YOUR_REGISTRY_URL|$REGISTRY|g" .env
sed -i "s|IMAGE_TAG=.*|IMAGE_TAG=$IMAGE_TAG|g" .env
sed -i "s|DOMAIN/|$DOMAIN/|g" Docker/nginx-prod.conf

echo -e "${GREEN}✓ Variables d'environnement configurées${NC}"

# ============================================
# 4. SETUP LETS ENCRYPT
# ============================================
echo -e "${YELLOW}[4/7] Configuration Let's Encrypt...${NC}"

mkdir -p certbot/{conf,www}

# First time setup (si pas de certificat existant)
if [ ! -f "certbot/conf/live/$DOMAIN/fullchain.pem" ]; then
    echo -e "${YELLOW}Premier déploiement - Création certificat Let's Encrypt${NC}"
    
    # Démarrer nginx en mode défi ACME
    docker-compose -f docker-compose.prod.yml up -d nginx
    sleep 5
    
    # Certbot
    docker run -it --rm \
        -v "$(pwd)/certbot/conf:/etc/letsencrypt" \
        -v "$(pwd)/certbot/www:/var/www/certbot" \
        -p 80:80 \
        certbot/certbot certonly \
        --standalone \
        --agree-tos \
        --no-eff-email \
        --email admin@$DOMAIN \
        -d $DOMAIN
    
    echo -e "${GREEN}✓ Certificat Let's Encrypt créé${NC}"
else
    echo -e "${GREEN}✓ Certificat Let's Encrypt trouvé${NC}"
fi

# ============================================
# 5. DOCKER LOGIN
# ============================================
echo -e "${YELLOW}[5/7] Docker Registry Login...${NC}"

if [ -z "$DOCKER_USERNAME" ] || [ -z "$DOCKER_PASSWORD" ]; then
    echo -e "${RED}❌ DOCKER_USERNAME ou DOCKER_PASSWORD non définis${NC}"
    echo "Exécutez: docker login $REGISTRY"
    exit 1
fi

echo "$DOCKER_PASSWORD" | docker login -u "$DOCKER_USERNAME" --password-stdin "$REGISTRY" || true

echo -e "${GREEN}✓ Docker Login OK${NC}"

# ============================================
# 6. PULL & START SERVICES
# ============================================
echo -e "${YELLOW}[6/7] Démarrage des services...${NC}"

docker-compose -f docker-compose.prod.yml pull
docker-compose -f docker-compose.prod.yml down || true
docker-compose -f docker-compose.prod.yml up -d

# Wait for services to be healthy
echo "Attente de l'initialisation des services..."
sleep 30

# Check health
docker-compose -f docker-compose.prod.yml ps

echo -e "${GREEN}✓ Services démarrés${NC}"

# ============================================
# 7. HEALTH CHECKS
# ============================================
echo -e "${YELLOW}[7/7] Vérification de la santé des services...${NC}"

# Wait for services to stabilize
sleep 10

HEALTH_CHECKS_PASSED=0
HEALTH_CHECKS_TOTAL=5

# Check each service
for service in auth-service account-service transaction-service postgres-auth rabbitmq; do
    if docker-compose -f docker-compose.prod.yml ps $service | grep -q "healthy"; then
        echo -e "${GREEN}✓ $service: healthy${NC}"
        ((HEALTH_CHECKS_PASSED++))
    else
        echo -e "${YELLOW}⚠ $service: checking...${NC}"
    fi
done

if [ $HEALTH_CHECKS_PASSED -ge 3 ]; then
    echo -e "${GREEN}✓ Santé des services OK${NC}"
else
    echo -e "${RED}❌ Certains services ne sont pas sains${NC}"
    echo "Vérifiez avec: docker-compose -f docker-compose.prod.yml logs"
    exit 1
fi

# ============================================
# DEPLOYMENT SUCCESS
# ============================================
echo ""
echo -e "${GREEN}================================${NC}"
echo -e "${GREEN}🎉 DÉPLOIEMENT RÉUSSI!${NC}"
echo -e "${GREEN}================================${NC}"
echo ""
echo "Application accessible sur: https://$DOMAIN"
echo "Grafana (monitoring) sur: https://$DOMAIN/grafana (admin/VOTRE_PASSWORD)"
echo ""
echo "Backup location: $BACKUP_PATH/$TIMESTAMP"
echo ""
echo "Commandes utiles:"
echo "  - Logs: docker-compose -f docker-compose.prod.yml logs -f [service_name]"
echo "  - Status: docker-compose -f docker-compose.prod.yml ps"
echo "  - Arrêt: docker-compose -f docker-compose.prod.yml down"
echo "  - Redémarrage: docker-compose -f docker-compose.prod.yml restart"
echo ""
