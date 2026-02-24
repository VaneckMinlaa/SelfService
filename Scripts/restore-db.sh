#!/usr/bin/env bash
# Restauration PostgreSQL (pour démo ou récupération).
# Usage: ./restore-db.sh <fichier_backup.sql>
# Variables d'environnement : DB_NAME, DB_USERNAME, DB_PASSWORD, DB_HOST (optionnel).

set -euo pipefail

BACKUP_FILE="${1:?Usage: $0 <fichier_backup.sql>}"
[ -f "$BACKUP_FILE" ] || { echo "Fichier introuvable: $BACKUP_FILE"; exit 1; }

export PGPASSWORD="${DB_PASSWORD:?DB_PASSWORD requis}"
PGHOST="${DB_HOST:-localhost}"
PGPORT="${DB_PORT:-5432}"
PGUSER="${DB_USERNAME:?DB_USERNAME requis}"
PGDB="${DB_NAME:?DB_NAME requis}"

if [ -n "${DOCKER:-}" ]; then
  PGHOST="${DB_HOST:-db}"
fi

echo "Restauration de $BACKUP_FILE vers $PGDB (les données existantes peuvent être écrasées)."
read -p "Continuer ? (y/N) " -n 1 -r
echo
[[ $REPLY =~ ^[yY]$ ]] || exit 0

psql -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$PGDB" -f "$BACKUP_FILE"
unset PGPASSWORD
echo "Restauration terminée."
