@echo off
REM Banking Portal - Backup Database Script

setlocal

set SCRIPT_DIR=%~dp0
set PROJECT_DIR=%SCRIPT_DIR%..
set BACKUP_DIR=%PROJECT_DIR%\backups

if not exist "%BACKUP_DIR%" mkdir "%BACKUP_DIR%"

for /f "tokens=1-5 delims=/ " %%a in ('date /t') do set TODAY=%%c-%%b-%%a
for /f "tokens=1-2 delims=: " %%a in ('time /t') do set NOW=%%a%%b
set TIMESTAMP=%TODAY%_%NOW%

echo ============================================================
echo   Banking Portal - Database Backup
echo ============================================================
echo.
echo [*] Sauvegarde des bases de donnees...

for %%D in (auth_db account_db transaction_db audit_db) do (
    echo [*] Sauvegarde de %%D...
    docker-compose exec -T postgres pg_dump -U bankadmin %%D > "%BACKUP_DIR%\%%D_%TIMESTAMP%.sql"
    if errorlevel 1 (
        echo [ERROR] Echec de la sauvegarde de %%D
    ) else (
        echo [OK] %%D sauvegarde dans %BACKUP_DIR%\%%D_%TIMESTAMP%.sql
    )
)

echo.
echo ============================================================
echo   [OK] Sauvegarde completee!
echo ============================================================
