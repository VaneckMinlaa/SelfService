#!/bin/bash
# Banking Portal - Database Backup Script
# Backs up all PostgreSQL databases

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
BACKUP_DIR="$PROJECT_DIR/database/backups"

mkdir -p "$BACKUP_DIR"

TIMESTAMP=$(date +"%Y%m%d_%H%M%S")

echo "════════════════════════════════════════════════════════════"
echo "  Backing up all databases..."
echo "════════════════════════════════════════════════════════════"
echo ""

databases=(
    "auth_db:postgres-auth"
    "account_db:postgres-account"
    "transaction_db:postgres-transaction"
)

for db_info in "${databases[@]}"; do
    IFS=':' read -r db_name container_name <<< "$db_info"
    backup_file="$BACKUP_DIR/backup_${db_name}_${TIMESTAMP}.sql"

    echo "📦 Backing up $db_name..."

    docker exec "$container_name" pg_dump -U bankadmin "$db_name" > "$backup_file"

    echo "   ✅ Saved to: $backup_file"
done

echo ""
echo "════════════════════════════════════════════════════════════"
echo "  ✅ Backup completed!"
echo "════════════════════════════════════════════════════════════"
