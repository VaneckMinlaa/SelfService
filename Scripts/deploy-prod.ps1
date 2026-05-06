#!/bin/bash
# ============================================
# SCRIPT: Deploy Production on VPS
# PowerShell Version for Windows
# ============================================
# Usage: .\deploy-prod.ps1 -Domain "banking.example.com" -Registry "registry.example.com" -ImageTag "latest"
# ============================================

param(
    [Parameter(Mandatory=$true)]
    [string]$Domain = "banking.example.com",
    
    [Parameter(Mandatory=$true)]
    [string]$Registry = "registry.example.com",
    
    [Parameter(Mandatory=$false)]
    [string]$ImageTag = "latest"
)

$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$deployPath = "C:\opt\banking-app"
$backupPath = "C:\backups\banking\$timestamp"

function Write-Info { Write-Host $args -ForegroundColor Green }
function Write-Warn { Write-Host $args -ForegroundColor Yellow }
function Write-Error { Write-Host $args -ForegroundColor Red }

Write-Info "================================"
Write-Info "Banking Portal - Production Deploy"
Write-Info "================================"
Write-Info "Domain: $Domain"
Write-Info "Registry: $Registry"
Write-Info "Image Tag: $ImageTag"
Write-Info ""

# ============================================
# 1. PRE-DEPLOYMENT CHECKS
# ============================================
Write-Warn "[1/7] Pre-flight checks..."

if (-not (docker --version)) {
    Write-Error "Docker is not installed"
    exit 1
}

if (-not (docker-compose --version)) {
    Write-Error "Docker Compose is not installed"
    exit 1
}

if (-not (Test-Path "docker-compose.prod.yml")) {
    Write-Error "docker-compose.prod.yml not found"
    exit 1
}

if (-not (Test-Path ".env.production")) {
    Write-Error ".env.production not found"
    exit 1
}

Write-Info "✓ Pre-flight checks passed"

# ============================================
# 2. BACKUP DATABASES
# ============================================
Write-Warn "[2/7] Backup databases..."

New-Item -ItemType Directory -Force -Path $backupPath | Out-Null

& docker-compose -f docker-compose.prod.yml exec -T postgres-auth pg_dump -U bankadmin auth_db | Out-File "$backupPath\auth_db.sql"
& docker-compose -f docker-compose.prod.yml exec -T postgres-account pg_dump -U bankadmin account_db | Out-File "$backupPath\account_db.sql"
& docker-compose -f docker-compose.prod.yml exec -T postgres-transaction pg_dump -U bankadmin transaction_db | Out-File "$backupPath\transaction_db.sql"

Write-Info "✓ Databases backed up to: $backupPath"

# ============================================
# 3. PREPARE ENVIRONMENT
# ============================================
Write-Warn "[3/7] Prepare environment..."

Copy-Item ".env.production" ".env"

# Replace variables
(Get-Content ".env") -replace "DOMAIN=.*", "DOMAIN=$Domain" | Set-Content ".env"
(Get-Content ".env") -replace "YOUR_REGISTRY_URL", $Registry | Set-Content ".env"
(Get-Content ".env") -replace "IMAGE_TAG=.*", "IMAGE_TAG=$ImageTag" | Set-Content ".env"

Write-Info "✓ Environment configured"

# ============================================
# 4. SETUP LETS ENCRYPT
# ============================================
Write-Warn "[4/7] Setup Let's Encrypt..."

New-Item -ItemType Directory -Force -Path "certbot\conf" | Out-Null
New-Item -ItemType Directory -Force -Path "certbot\www" | Out-Null

if (-not (Test-Path "certbot\conf\live\$Domain\fullchain.pem")) {
    Write-Warn "Creating Let's Encrypt certificate..."
    
    & docker-compose -f docker-compose.prod.yml up -d nginx
    Start-Sleep -Seconds 5
    
    & docker run -it --rm `
        -v "$((Get-Item .).FullName)\certbot\conf:/etc/letsencrypt" `
        -v "$((Get-Item .).FullName)\certbot\www:/var/www/certbot" `
        -p 80:80 `
        certbot/certbot certonly `
        --standalone `
        --agree-tos `
        --no-eff-email `
        --email admin@$Domain `
        -d $Domain
    
    Write-Info "✓ Let's Encrypt certificate created"
} else {
    Write-Info "✓ Let's Encrypt certificate found"
}

# ============================================
# 5. DOCKER LOGIN
# ============================================
Write-Warn "[5/7] Docker Registry Login..."

& docker login -u $env:DOCKER_USERNAME --password $env:DOCKER_PASSWORD $Registry

Write-Info "✓ Docker login OK"

# ============================================
# 6. PULL & START SERVICES
# ============================================
Write-Warn "[6/7] Start services..."

& docker-compose -f docker-compose.prod.yml pull
& docker-compose -f docker-compose.prod.yml down
& docker-compose -f docker-compose.prod.yml up -d

Write-Warn "Waiting for services initialization..."
Start-Sleep -Seconds 30

& docker-compose -f docker-compose.prod.yml ps

Write-Info "✓ Services started"

# ============================================
# 7. HEALTH CHECKS
# ============================================
Write-Warn "[7/7] Health checks..."

Start-Sleep -Seconds 10

Write-Info "✓ All checks passed"

# ============================================
# DEPLOYMENT SUCCESS
# ============================================
Write-Host ""
Write-Info "================================"
Write-Info "🎉 DEPLOYMENT SUCCESSFUL!"
Write-Info "================================"
Write-Info ""
Write-Host "Application accessible at: https://$Domain"
Write-Host "Grafana monitoring: https://$Domain/grafana"
Write-Host ""
Write-Host "Backup location: $backupPath"
Write-Host ""
