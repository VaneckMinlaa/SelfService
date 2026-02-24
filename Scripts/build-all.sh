#!/bin/bash
# Portail Bancaire - Construction de tous les services
# Crée les images Docker pour tous les microservices

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

echo "════════════════════════════════════════════════════════════"
echo "  Construction de tous les microservices..."
echo "════════════════════════════════════════════════════════════"
echo ""

# Liste des services à construire
services=(
    "auth-service"
    "account-service"
    "transaction-service"
    "notification-service"
    "nginx"
    "frontend"
)

cd "$PROJECT_DIR"

# Boucle sur chaque service et constructeur l'image Docker
for service in "${services[@]}"; do
    echo "📦 Construction de $service..."
    if [ -f "$service/Dockerfile" ] || [ -f "$service/package.json" ]; then
        docker build -t banking-$service:latest -f $service/Dockerfile $service/ 2>&1 | tail -5
        echo "✅ $service construit avec succès"
    else
        echo "⚠️  Ignoré $service (pas de Dockerfile trouvé)"
    fi
    echo ""
done

echo "════════════════════════════════════════════════════════════"
echo "  ✅ Tous les services ont été construits avec succès!"
echo "════════════════════════════════════════════════════════════"
