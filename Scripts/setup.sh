#!/bin/bash
# Portail Bancaire - Script d'Initialisation
# Initialise le projet avec le fichier .env et génère le secret JWT

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

echo "════════════════════════════════════════════════════════════"
echo "  Portail Bancaire - Configuration Microservices"
echo "════════════════════════════════════════════════════════════"
echo ""

# Vérifier si le fichier .env existe déjà
if [ -f "$PROJECT_DIR/.env" ]; then
    echo "⚠️  Le fichier .env existe déjà. Création ignorée."
    echo ""
else
    echo "📝 Création du fichier .env à partir de .env.example..."
    cp "$PROJECT_DIR/.env.example" "$PROJECT_DIR/.env"

    # Générer un secret JWT
    JWT_SECRET=$(openssl rand -base64 32)
    sed -i "s|your-super-secret-jwt-key-minimum-32-characters-here|$JWT_SECRET|g" "$PROJECT_DIR/.env"

    # Générer un mot de passe pour la BD si nécessaire
    DB_PASSWORD=$(openssl rand -base64 16)
    sed -i "s|your-secure-database-password-here|$DB_PASSWORD|g" "$PROJECT_DIR/.env"

    echo "✅ Fichier .env créé avec succès"
    echo "   JWT_SECRET: $(echo $JWT_SECRET | cut -c 1-20)..."
    echo "   DB_PASSWORD: $(echo $DB_PASSWORD | cut -c 1-16)..."
    echo ""
fi

# Vérifier Docker
echo "🐳 Vérification de l'installation Docker..."
if ! command -v docker &> /dev/null; then
    echo "❌ Docker n'est pas installé. Veuillez installer Docker d'abord."
    exit 1
fi
echo "✅ Docker trouvé: $(docker --version)"
echo ""

# Vérifier Docker Compose
echo "🐳 Vérification de l'installation Docker Compose..."
if ! command -v docker-compose &> /dev/null; then
    # Essayer la nouvelle syntaxe
    if docker compose version &> /dev/null; then
        echo "✅ Docker Compose trouvé (via docker compose)"
    else
        echo "❌ Docker Compose n'est pas installé."
        exit 1
    fi
else
    echo "✅ Docker Compose trouvé: $(docker-compose --version)"
fi
echo ""

echo "════════════════════════════════════════════════════════════"
echo "  ✅ Configuration complétée avec succès!"
echo "════════════════════════════════════════════════════════════"
echo ""
echo "Prochaines étapes:"
echo "  1. Vérifier le fichier .env et mettre à jour les identifiants SMTP si nécessaire"
echo "  2. Exécuter: ./scripts/build-all.sh"
echo "  3. Exécuter: docker-compose up -d"
echo "  4. Exécuter: ./scripts/health-check.sh"
echo ""
