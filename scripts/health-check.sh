#!/bin/bash
# Banking Portal - Health Check Script
# Verifies all services are running and healthy

set -e

echo "════════════════════════════════════════════════════════════"
echo "  Banking Portal - Health Check"
echo "════════════════════════════════════════════════════════════"
echo ""

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

check_service() {
    local service=$1
    local port=$2
    local endpoint=$3

    echo -n "Checking $service (PORT $port)... "

    if curl -s -k "http://localhost:$port$endpoint" > /dev/null 2>&1; then
        echo -e "${GREEN}✅ HEALTHY${NC}"
        return 0
    else
        echo -e "${RED}❌ UNHEALTHY${NC}"
        return 1
    fi
}

# Check all services
services=(
    "Auth Service:8081:/api/auth/health"
    "Account Service:8082:/api/accounts/health"
    "Transaction Service:8083:/api/transactions/health"
    "RabbitMQ:15672:/"
    "Frontend:443:/"
)

all_healthy=true

for service_info in "${services[@]}"; do
    IFS=':' read -r service port endpoint <<< "$service_info"
    if ! check_service "$service" "$port" "$endpoint"; then
        all_healthy=false
    fi
done

echo ""
echo "════════════════════════════════════════════════════════════"

if [ "$all_healthy" = true ]; then
    echo -e "  ${GREEN}✅ All services are healthy!${NC}"
    echo "════════════════════════════════════════════════════════════"
    echo ""
    echo "Service URLs:"
    echo "  Frontend:          https://localhost"
    echo "  Auth Service:      http://localhost:8081"
    echo "  Account Service:   http://localhost:8082"
    echo "  Transaction Service: http://localhost:8083"
    echo "  RabbitMQ UI:       http://localhost:15672 (guest/guest)"
    echo ""
else
    echo -e "  ${RED}❌ Some services are unhealthy!${NC}"
    echo "════════════════════════════════════════════════════════════"
    echo ""
    echo "Check logs with:"
    echo "  docker-compose logs <service-name>"
    exit 1
fi
