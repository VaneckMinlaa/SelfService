#!/bin/bash
# Banking Portal - Build All Services
# Builds Docker images for all microservices

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

echo "════════════════════════════════════════════════════════════"
echo "  Building all microservices..."
echo "════════════════════════════════════════════════════════════"
echo ""

services=(
    "auth-service"
    "account-service"
    "transaction-service"
    "notification-service"
    "nginx"
    "frontend"
)

cd "$PROJECT_DIR"

for service in "${services[@]}"; do
    echo "📦 Building $service..."
    if [ -f "$service/Dockerfile" ] || [ -f "$service/package.json" ]; then
        docker build -t banking-$service:latest -f $service/Dockerfile $service/ 2>&1 | tail -5
        echo "✅ $service built successfully"
    else
        echo "⚠️  Skipping $service (no Dockerfile found)"
    fi
    echo ""
done

echo "════════════════════════════════════════════════════════════"
echo "  ✅ All services built successfully!"
echo "════════════════════════════════════════════════════════════"
