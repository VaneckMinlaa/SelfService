# Portail Bancaire - Construction de tous les services (PowerShell)
# Cree les images Docker pour tous les microservices

$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectDir = Split-Path -Parent $ScriptDir

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  Construction de tous les microservices..." -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""

# Liste des services a construire
$Services = @(
    "auth-service",
    "account-service",
    "transaction-service",
    "notification-service",
    "nginx",
    "frontend"
)

# Se placer dans le repertoire racine du projet
Push-Location $ProjectDir

# Boucle sur chaque service et construire l'image Docker
foreach ($Service in $Services) {
    Write-Host "[*] Construction de $Service..." -ForegroundColor Cyan
    $ServicePath = Join-Path $ProjectDir $Service
    if (Test-Path "$ServicePath/Dockerfile") {
        docker build -t "banking-$Service:latest" -f "$ServicePath/Dockerfile" "$ServicePath/"
        Write-Host "[OK] $Service construit avec succes" -ForegroundColor Green
    } else {
        Write-Host "[!] Ignore $Service (pas de Dockerfile trouve)" -ForegroundColor Yellow
    }
    Write-Host ""
}

Pop-Location

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  [OK] Tous les services ont ete construits avec succes!" -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Cyan
