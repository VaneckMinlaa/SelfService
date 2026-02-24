#!/bin/bash
# Banking Portal - Setup Script
# Initializes the project with .env and generates JWT secret

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

echo "════════════════════════════════════════════════════════════"
echo "  Banking Portal - Microservices Setup"
echo "════════════════════════════════════════════════════════════"
echo ""

# Check if .env already exists
if [ -f "$PROJECT_DIR/.env" ]; then
    echo "⚠️  .env file already exists. Skipping creation."
    echo ""
else
    echo "📝 Creating .env file from .env.example..."
    cp "$PROJECT_DIR/.env.example" "$PROJECT_DIR/.env"

    # Generate JWT secret
    JWT_SECRET=$(openssl rand -base64 32)
    sed -i "s|your-super-secret-jwt-key-minimum-32-characters-here|$JWT_SECRET|g" "$PROJECT_DIR/.env"

    # Generate DB password if needed
    DB_PASSWORD=$(openssl rand -base64 16)
    sed -i "s|your-secure-database-password-here|$DB_PASSWORD|g" "$PROJECT_DIR/.env"

    echo "✅ .env file created successfully"
    echo "   JWT_SECRET: $(echo $JWT_SECRET | cut -c 1-20)..."
    echo "   DB_PASSWORD: $(echo $DB_PASSWORD | cut -c 1-16)..."
    echo ""
fi

# Check Docker
echo "🐳 Checking Docker installation..."
if ! command -v docker &> /dev/null; then
    echo "❌ Docker is not installed. Please install Docker first."
    exit 1
fi
echo "✅ Docker found: $(docker --version)"
echo ""

# Check Docker Compose
echo "🐳 Checking Docker Compose installation..."
if ! command -v docker-compose &> /dev/null; then
    # Try newer syntax
    if docker compose version &> /dev/null; then
        echo "✅ Docker Compose found (via docker compose)"
    else
        echo "❌ Docker Compose is not installed."
        exit 1
    fi
else
    echo "✅ Docker Compose found: $(docker-compose --version)"
fi
echo ""

echo "════════════════════════════════════════════════════════════"
echo "  ✅ Setup completed successfully!"
echo "════════════════════════════════════════════════════════════"
echo ""
echo "Next steps:"
echo "  1. Review .env file and update SMTP credentials (if needed)"
echo "  2. Run: ./scripts/build-all.sh"
echo "  3. Run: docker-compose up -d"
echo "  4. Run: ./scripts/health-check.sh"
echo ""
