@echo off
REM Portail Bancaire - Construction de tous les services
REM Cree les images Docker pour tous les microservices

setlocal

set SCRIPT_DIR=%~dp0
set PROJECT_DIR=%SCRIPT_DIR%..

echo ============================================================
echo   Construction de tous les microservices...
echo ============================================================
echo.

cd /d "%PROJECT_DIR%"

for %%S in (auth-service account-service transaction-service notification-service nginx frontend) do (
    echo [*] Construction de %%S...
    if exist "%%S\Dockerfile" (
        docker build -t banking-%%S:latest -f %%S\Dockerfile %%S\
        if errorlevel 1 (
            echo [ERROR] Echec de la construction de %%S
        ) else (
            echo [OK] %%S construit avec succes
        )
    ) else (
        echo [!] Ignore %%S - pas de Dockerfile trouve
    )
    echo.
)

echo ============================================================
echo   [OK] Tous les services ont ete traites!
echo ============================================================
