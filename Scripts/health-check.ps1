# Banking Portal - Health Check Script (PowerShell)
# Verifies all services are running and healthy

$ErrorActionPreference = "Continue"

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  Banking Portal - Health Check" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""

function Check-Service {
    param(
        [string]$ServiceName,
        [int]$Port,
        [string]$Endpoint
    )

    Write-Host -NoNewline "[*] Checking $ServiceName (PORT $Port)... "
    
    try {
        $Response = Invoke-WebRequest -Uri "http://localhost:$Port$Endpoint" -Method Get -TimeoutSec 5 -UseBasicParsing
        if ($Response.StatusCode -ge 200 -and $Response.StatusCode -lt 300) {
            Write-Host "OK" -ForegroundColor Green
            return $true
        }
    } catch {
        Write-Host "ERROR (Message: $($_.Exception.Message))" -ForegroundColor Red
        return $false
    }
    
    return $false
}

$Services = @(
    @{ Name = "Auth Service"; Port = 8081; Endpoint = "/api/auth/health" },
    @{ Name = "Account Service"; Port = 8082; Endpoint = "/api/accounts/health" },
    @{ Name = "Transaction Service"; Port = 8083; Endpoint = "/api/transactions/health" },
    @{ Name = "RabbitMQ UI"; Port = 15672; Endpoint = "/" },
    @{ Name = "Frontend"; Port = 443; Endpoint = "/" } 
)

$AllHealthy = $true

foreach ($Service in $Services) {
    if (-not (Check-Service -ServiceName $Service.Name -Port $Service.Port -Endpoint $Service.Endpoint)) {
        $AllHealthy = $false
    }
}

Write-Host ""
Write-Host "============================================================" -ForegroundColor Cyan

if ($AllHealthy) {
    Write-Host "  [OK] All services are healthy!" -ForegroundColor Green
    Write-Host "============================================================" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Service URLs:"
    Write-Host "  Frontend:          https://localhost"
    Write-Host "  Auth Service:      http://localhost:8081"
    Write-Host "  Account Service:   http://localhost:8082"
    Write-Host "  Transaction Service: http://localhost:8083"
    Write-Host "  RabbitMQ UI:       http://localhost:15672 (guest/guest)"
    Write-Host ""
} else {
    Write-Host "  [!] Some services are unhealthy!" -ForegroundColor Red
    Write-Host "============================================================" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Check logs with: docker-compose logs <service-name>"
}
