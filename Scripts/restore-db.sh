#!/bin/bash
# ============================================
# RESTORE SCRIPT - PostgreSQL from Backup
# Restaure 1 ou toutes les bases de données
# ============================================
# Usage:
#   ./restore-db.sh                         # Menu interactif
#   ./restore-db.sh all <backup_dir>       # Tout restaurer
#   ./restore-db.sh auth <backup_file>     # Restaurer 1 base
# ============================================

set -e

BACKUP_DIR="/backups/banking"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# ============================================
# FONCTION RESTORE
# ============================================
restore_database() {
    local container=$1
    local database=$2
    local user=$3
    local backup_file=$4
    
    echo -e "${YELLOW}Restauration de $database...${NC}"
    
    # Vérifier que le backup existe
    if [ ! -f "$backup_file" ]; then
        echo -e "${RED}✗ Fichier de backup non trouvé: $backup_file${NC}"
        return 1
    fi
    
    # Décompresser et restaurer
    if [[ "$backup_file" == *.gz ]]; then
        gunzip -c "$backup_file" | docker exec -i "$container" psql -U "$user" "$database" > /dev/null
    else
        cat "$backup_file" | docker exec -i "$container" psql -U "$user" "$database" > /dev/null
    fi
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✓ $database restaurée avec succès${NC}"
        return 0
    else
        echo -e "${RED}✗ Erreur lors de la restauration de $database${NC}"
        return 1
    fi
}

# ============================================
# MENU INTERACTIF
# ============================================
show_menu() {
    echo ""
    echo -e "${GREEN}================================${NC}"
    echo "   Restauration de Backup"
    echo -e "${GREEN}================================${NC}"
    echo "Sauvegardes disponibles:"
    ls -1d "$BACKUP_DIR"/*/ 2>/dev/null | tail -10
    echo ""
    echo "1) Restaurer toutes les bases"
    echo "2) Restaurer auth_db uniquement"
    echo "3) Restaurer account_db uniquement"
    echo "4) Restaurer transaction_db uniquement"
    echo "5) Quitter"
    echo ""
    read -p "Choisir une option (1-5): " choice
}

# ============================================
# MAIN
# ============================================

if [ $# -eq 0 ]; then
    # Mode interactif
    show_menu
    
    case $choice in
        1)
            read -p "Entrer le répertoire backup (ex: 2024-01-15_02-30-45): " backup_folder
            BACKUP_PATH="$BACKUP_DIR/$backup_folder"
            
            if [ ! -d "$BACKUP_PATH" ]; then
                echo -e "${RED}✗ Répertoire non trouvé: $BACKUP_PATH${NC}"
                exit 1
            fi
            
            echo -e "${YELLOW}⚠ ATTENTION: Ceci restaurera TOUTES les bases de données!${NC}"
            read -p "Confirmer (yes/no): " confirm
            
            if [ "$confirm" != "yes" ]; then
                echo "Annulé."
                exit 0
            fi
            
            restore_database "postgres-auth" "auth_db" "bankadmin" "$BACKUP_PATH/auth_db_*.sql.gz"
            restore_database "postgres-account" "account_db" "bankadmin" "$BACKUP_PATH/account_db_*.sql.gz"
            restore_database "postgres-transaction" "transaction_db" "bankadmin" "$BACKUP_PATH/transaction_db_*.sql.gz"
            ;;
        2)
            read -p "Entrer le fichier backup pour auth_db: " backup_file
            restore_database "postgres-auth" "auth_db" "bankadmin" "$backup_file"
            ;;
        3)
            read -p "Entrer le fichier backup pour account_db: " backup_file
            restore_database "postgres-account" "account_db" "bankadmin" "$backup_file"
            ;;
        4)
            read -p "Entrer le fichier backup pour transaction_db: " backup_file
            restore_database "postgres-transaction" "transaction_db" "bankadmin" "$backup_file"
            ;;
        5)
            exit 0
            ;;
        *)
            echo -e "${RED}Option invalide${NC}"
            exit 1
            ;;
    esac
elif [ "$1" == "all" ] && [ -n "$2" ]; then
    # Mode restauration complète
    BACKUP_PATH="$2"
    
    if [ ! -d "$BACKUP_PATH" ]; then
        echo -e "${RED}✗ Répertoire non trouvé: $BACKUP_PATH${NC}"
        exit 1
    fi
    
    echo -e "${YELLOW}Restauration depuis: $BACKUP_PATH${NC}"
    
    restore_database "postgres-auth" "auth_db" "bankadmin" "$BACKUP_PATH/auth_db_*.sql.gz"
    restore_database "postgres-account" "account_db" "bankadmin" "$BACKUP_PATH/account_db_*.sql.gz"
    restore_database "postgres-transaction" "transaction_db" "bankadmin" "$BACKUP_PATH/transaction_db_*.sql.gz"
fi

echo -e "${GREEN}================================${NC}"
echo -e "${GREEN}Restauration complétée!${NC}"
echo -e "${GREEN}================================${NC}"
