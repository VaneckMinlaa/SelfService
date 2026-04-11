# Portail Bancaire - Script d'Initialisation (PowerShell)
# Initialise le projet avec le fichier .env et genere le secret JWT

$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectDir = Split-Path -Parent $ScriptDir

# Use ASCII/Simple characters for better compatibility
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  Portail Bancaire - Configuration Microservices" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""

# Verifier si le fichier .env existe deja
$EnvFile = Join-Path $ProjectDir ".env"
$ExampleFile = Join-Path $ProjectDir ".env.example"

if (Test-Path $EnvFile) {
    Write-Host "[!] Le fichier .env existe deja. Creation ignoree." -ForegroundColor Yellow
} else {
    Write-Host "[*] Creation du fichier .env a partir de .env.example..."
    Copy-Item $ExampleFile $EnvFile

    # Genere un secret JWT (using simpler method)
    $chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789+/="
    $JwtSecret = ""
    for ($i=0; $i -lt 44; $i++) { $JwtSecret += $chars[(Get-Random -Maximum $chars.Length)] }
    
    # Genere un mot de passe pour la BD
    $DbPassword = ""
    for ($i=0; $i -lt 24; $i++) { $DbPassword += $chars[(Get-Random -Maximum $chars.Length)] }

    # Remplacer dans le fichier .env
    $Content = Get-Content $EnvFile
    $Content = $Content -replace "your-super-secret-jwt-key-minimum-32-characters-here", $JwtSecret
    $Content = $Content -replace "your-secure-database-password-here", $DbPassword
    $Content | Set-Content $EnvFile

    Write-Host "[OK] Fichier .env cree avec succes" -ForegroundColor Green
    Write-Host "   JWT_SECRET: $($JwtSecret.Substring(0, 20))..."
    Write-Host "   DB_PASSWORD: $($DbPassword.Substring(0, 16))..."
}
Write-Host ""

# Verifier Docker
Write-Host "[*] Verification de l'installation Docker..."
try {
    $DockerVer = docker --version -ErrorAction Stop
    Write-Host "[OK] Docker trouve: $DockerVer" -ForegroundColor Green
} catch {
    Write-Host "[ERROR] Docker n'est pas installe. Veuillez installer Docker Desktop." -ForegroundColor Red
    exit 1
}
Write-Host ""

# Verifier Docker Compose
Write-Host "[*] Verification de l'installation Docker Compose..."
$ComposeFound = $false
try {
    docker-compose --version -ErrorAction Stop
    $ComposeFound = $true
    Write-Host "[OK] Docker Compose trouve" -ForegroundColor Green
} catch {
    try {
        docker compose version -ErrorAction Stop
        $ComposeFound = $true
        Write-Host "[OK] Docker Compose trouve (via 'docker compose')" -ForegroundColor Green
    } catch {
        Write-Host "[ERROR] Docker Compose n'est pas installe." -ForegroundColor Red
        exit 1
    }
}
Write-Host ""

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  [OK] Configuration completee avec succes!" -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Prochaines etapes:"
Write-Host "  1. Verifier le fichier .env"
Write-Host "  2. Executer: .\scripts\build-all.ps1"
Write-Host "  3. Executer: docker-compose up -d"
Write-Host "  4. Executer: .\scripts\health-check.ps1"
Write-Host ""
