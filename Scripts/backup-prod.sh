#!/bin/bash
# ============================================
# BACKUP SCRIPT - Production PostgreSQL
# Sauvegarde complète des 3 bases de données
# ============================================
# Utilisation: ./backup-db.sh
# Cron:        0 2 * * * /opt/banking-app/Scripts/backup-db.sh
# ============================================

set -e

BACKUP_DIR="/backups/banking"
TIMESTAMP=$(date +%Y-%m-%d_%H-%M-%S)
RETENTION_DAYS=30

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${GREEN}[$(date +'%Y-%m-%d %H:%M:%S')] Démarrage de la sauvegarde${NC}"

# Créer le répertoire
mkdir -p "$BACKUP_DIR/$TIMESTAMP"

# ============================================
# FONCTION BACKUP
# ============================================
backup_database() {
    local container=$1
    local database=$2
    local user=$3
    local backup_file="$BACKUP_DIR/$TIMESTAMP/${database}_${TIMESTAMP}.sql"
    
    echo -e "${YELLOW}  → Sauvegarde $database depuis $container...${NC}"
    
    if docker exec -T "$container" pg_dump -U "$user" "$database" | gzip > "${backup_file}.gz" 2>/dev/null; then
        local size=$(du -h "${backup_file}.gz" | cut -f1)
        echo -e "${GREEN}    ✓ $database sauvegardé ($size)${NC}"
    else
        echo -e "${RED}    ✗ Erreur lors de la sauvegarde de $database${NC}"
        return 1
    fi
}

# ============================================
# SAUVEGARDES
# ============================================

echo -e "${YELLOW}1. Sauvegarde des bases de données${NC}"
backup_database "postgres-auth" "auth_db" "bankadmin"
backup_database "postgres-account" "account_db" "bankadmin"
backup_database "postgres-transaction" "transaction_db" "bankadmin"

echo -e "${GREEN}✓ Sauvegardes complétées${NC}"

# ============================================
# SAUVEGARDE DES CONFIGURATIONS
# ============================================

echo -e "${YELLOW}2. Sauvegarde des configurations${NC}"

# Copier les configs
cp .env "$BACKUP_DIR/$TIMESTAMP/.env.backup" 2>/dev/null || true
cp docker-compose.prod.yml "$BACKUP_DIR/$TIMESTAMP/" 2>/dev/null || true
cp Docker/nginx-prod.conf "$BACKUP_DIR/$TIMESTAMP/" 2>/dev/null || true

# Certificats SSL (uniquement si présents)
if [ -d "certbot/conf" ]; then
    tar -czf "$BACKUP_DIR/$TIMESTAMP/ssl-certs_${TIMESTAMP}.tar.gz" certbot/conf 2>/dev/null || true
fi

echo -e "${GREEN}✓ Configurations sauvegardées${NC}"

# ============================================
# NETTOYAGE ANCIEN BACKUPS
# ============================================

echo -e "${YELLOW}3. Nettoyage des anciennes sauvegardes (>$RETENTION_DAYS jours)${NC}"

find "$BACKUP_DIR" -maxdepth 1 -type d -mtime +$RETENTION_DAYS -exec rm -rf {} \; 2>/dev/null || true

echo -e "${GREEN}✓ Nettoyage complété${NC}"

# ============================================
# STATISTIQUES
# ============================================

echo ""
echo -e "${GREEN}================================${NC}"
echo -e "${GREEN}Sauvegarde réussie!${NC}"
echo -e "${GREEN}================================${NC}"
echo "Date: $TIMESTAMP"
echo "Répertoire: $BACKUP_DIR/$TIMESTAMP"
echo "Fichiers:"
ls -lh "$BACKUP_DIR/$TIMESTAMP/"
echo ""
echo "Espace disque utilisé:"
du -sh "$BACKUP_DIR"
echo ""

# ============================================
# UPLOAD CLOUD (Optionnel)
# ============================================

# Décommenter pour activer sync S3
# aws s3 sync "$BACKUP_DIR/$TIMESTAMP" "s3://my-backup-bucket/banking/$TIMESTAMP" --sse AES256

# Ou rsync
# rsync -avz --delete "$BACKUP_DIR/$TIMESTAMP/" backup-server:/backups/banking/"

echo -e "${GREEN}[$(date +'%Y-%m-%d %H:%M:%S')] Sauvegarde terminée${NC}"
