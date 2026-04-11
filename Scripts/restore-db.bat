@echo off
REM Banking Portal - Database Restore Script
REM Usage: restore-db.bat <fichier_backup.sql> <db_name>
REM Requires: DB_USERNAME, DB_PASSWORD, DB_HOST env vars (or set below)

setlocal

if "%~1"=="" (
    echo Usage: restore-db.bat ^<fichier_backup.sql^> ^<db_name^>
    exit /b 1
)
if "%~2"=="" (
    echo Usage: restore-db.bat ^<fichier_backup.sql^> ^<db_name^>
    exit /b 1
)

set BACKUP_FILE=%~1
set PGDB=%~2

if not exist "%BACKUP_FILE%" (
    echo [ERROR] Fichier introuvable: %BACKUP_FILE%
    exit /b 1
)

REM Variables de connexion (peuvent etre surchargees via l'env)
if "%DB_HOST%"=="" set DB_HOST=localhost
if "%DB_PORT%"=="" set DB_PORT=5432
if "%DB_USERNAME%"=="" set DB_USERNAME=bankadmin

echo.
echo ============================================================
echo   Banking Portal - Database Restore
echo ============================================================
echo.
echo Fichier : %BACKUP_FILE%
echo Base    : %PGDB%
echo Hote    : %DB_HOST%:%DB_PORT%
echo.
echo ATTENTION: Les donnees existantes seront ecrasees.
set /p CONFIRM=Continuer ? (o/N) : 
if /i not "%CONFIRM%"=="o" (
    echo Annule.
    exit /b 0
)

echo.
echo [*] Restauration en cours...
set PGPASSWORD=%DB_PASSWORD%
psql -h %DB_HOST% -p %DB_PORT% -U %DB_USERNAME% -d %PGDB% -f "%BACKUP_FILE%"
if errorlevel 1 (
    echo [ERROR] La restauration a echoue.
    exit /b 1
)
set PGPASSWORD=

echo.
echo [OK] Restauration terminee.
