#!/bin/bash
# ============================================
# HEALTH CHECK SCRIPT - Production Services
# Vérifie la santé de tous les services
# ============================================
# Usage: ./health-check.sh
# Cron:  */5 * * * * /opt/banking-app/Scripts/health-check.sh
# ============================================

set -e

DOMAIN=${1:-banking.example.com}
LOG_FILE="/var/log/banking/health-check.log"
ALERT_EMAIL="admin@example.com"

# Colors (pour logs)
RED='[ERROR]'
GREEN='[OK]'
YELLOW='[WARN]'

# Créer le répertoire de logs
mkdir -p "$(dirname "$LOG_FILE")"

# ============================================
# LOGGING
# ============================================
log_message() {
    local level=$1
    local message=$2
    local timestamp=$(date +'%Y-%m-%d %H:%M:%S')
    echo "[$timestamp] $level $message" >> "$LOG_FILE"
}

# ============================================
# HEALTH CHECKS
# ============================================

echo "Démarrage des health checks à $(date)" >> "$LOG_FILE"

FAILED_SERVICES=0

# Check 1: API Gateway (Nginx)
if curl -sf https://$DOMAIN/health > /dev/null 2>&1; then
    log_message "$GREEN" "Nginx Gateway: OK"
else
    log_message "$RED" "Nginx Gateway: DOWN"
    ((FAILED_SERVICES++))
fi

# Check 2: Auth Service
if curl -sf https://$DOMAIN/api/auth/health > /dev/null 2>&1; then
    log_message "$GREEN" "Auth Service: OK"
else
    log_message "$RED" "Auth Service: DOWN"
    ((FAILED_SERVICES++))
fi

# Check 3: Account Service
if curl -sf https://$DOMAIN/api/accounts/health > /dev/null 2>&1; then
    log_message "$GREEN" "Account Service: OK"
else
    log_message "$RED" "Account Service: DOWN"
    ((FAILED_SERVICES++))
fi

# Check 4: Transaction Service
if curl -sf https://$DOMAIN/api/transactions/health > /dev/null 2>&1; then
    log_message "$GREEN" "Transaction Service: OK"
else
    log_message "$RED" "Transaction Service: DOWN"
    ((FAILED_SERVICES++))
fi

# Check 5: Database Connections
if [ "$(docker-compose -f /opt/banking-app/docker-compose.prod.yml ps postgres-auth | grep -c Up)" -gt 0 ]; then
    log_message "$GREEN" "PostgreSQL Auth: OK"
else
    log_message "$RED" "PostgreSQL Auth: DOWN"
    ((FAILED_SERVICES++))
fi

# Check 6: RabbitMQ
if docker-compose -f /opt/banking-app/docker-compose.prod.yml exec -T rabbitmq rabbitmq-diagnostics -q ping > /dev/null 2>&1; then
    log_message "$GREEN" "RabbitMQ: OK"
else
    log_message "$RED" "RabbitMQ: DOWN"
    ((FAILED_SERVICES++))
fi

# Check 7: Disk Space
DISK_USAGE=$(df / | awk 'NR==2 {print $5}' | sed 's/%//')
if [ "$DISK_USAGE" -gt 90 ]; then
    log_message "$YELLOW" "Disk Space: WARNING ($DISK_USAGE%)"
elif [ "$DISK_USAGE" -gt 95 ]; then
    log_message "$RED" "Disk Space: CRITICAL ($DISK_USAGE%)"
    ((FAILED_SERVICES++))
else
    log_message "$GREEN" "Disk Space: OK ($DISK_USAGE%)"
fi

# Check 8: SSL Certificate Expiration
CERT_EXPIRY=$(docker exec certbot openssl x509 -in /etc/letsencrypt/live/DOMAIN/cert.pem -noout -dates 2>/dev/null | grep notAfter | cut -d= -f2)
CERT_DAYS=$(($(date -d "$CERT_EXPIRY" +%s) - $(date +%s) / 86400))
if [ "$CERT_DAYS" -lt 7 ]; then
    log_message "$RED" "SSL Certificate: EXPIRING SOON ($CERT_DAYS days)"
    ((FAILED_SERVICES++))
elif [ "$CERT_DAYS" -lt 30 ]; then
    log_message "$YELLOW" "SSL Certificate: WARNING ($CERT_DAYS days)"
else
    log_message "$GREEN" "SSL Certificate: OK ($CERT_DAYS days)"
fi

# ============================================
# ALERTES
# ============================================

if [ "$FAILED_SERVICES" -gt 0 ]; then
    log_message "$RED" "FAILED: $FAILED_SERVICES service(s)"
    
    # Envoyer une alerte email
    SUBJECT="🚨 Banking Portal Health Check Failed"
    BODY="Les health checks ont détecté $FAILED_SERVICES service(s) défaillant(s). Vérifiez les logs."
    
    # echo "$BODY" | mail -s "$SUBJECT" "$ALERT_EMAIL"
    
    exit 1
else
    log_message "$GREEN" "All checks passed!"
    exit 0
fi
