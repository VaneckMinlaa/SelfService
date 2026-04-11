@echo off
REM Portail Bancaire - Script d'Initialisation
REM Initialise le projet avec le fichier .env et genere le secret JWT

setlocal EnableDelayedExpansion

set SCRIPT_DIR=%~dp0
set PROJECT_DIR=%SCRIPT_DIR%..

echo ============================================================
echo   Portail Bancaire - Configuration Microservices
echo ============================================================
echo.

REM Verifier si le fichier .env existe deja
if exist "%PROJECT_DIR%\.env" (
    echo [!] Le fichier .env existe deja. Creation ignoree.
    echo.
    goto CHECK_DOCKER
)

echo [*] Creation du fichier .env a partir de .env.example...
copy "%PROJECT_DIR%\.env.example" "%PROJECT_DIR%\.env" >nul

REM Generer un secret JWT aleatoire (base alphanum)
set JWT_SECRET=
for /l %%i in (1,1,44) do call :RAND_CHAR JWT_SECRET
set DB_PASSWORD=
for /l %%i in (1,1,24) do call :RAND_CHAR DB_PASSWORD

REM Remplacer les placeholders dans .env
set TMPFILE=%TEMP%\env_tmp.txt
type nul > "%TMPFILE%"
for /f "usebackq delims=" %%L in ("%PROJECT_DIR%\.env") do (
    set LINE=%%L
    set LINE=!LINE:your-super-secret-jwt-key-minimum-32-characters-here=!JWT_SECRET!!
    set LINE=!LINE:your-secure-database-password-here=!DB_PASSWORD!!
    echo !LINE! >> "%TMPFILE%"
)
copy /y "%TMPFILE%" "%PROJECT_DIR%\.env" >nul
del "%TMPFILE%"

echo [OK] Fichier .env cree avec succes
echo     JWT_SECRET : !JWT_SECRET:~0,20!...
echo     DB_PASSWORD: !DB_PASSWORD:~0,16!...
echo.

:CHECK_DOCKER
REM Verifier Docker
echo [*] Verification de l'installation Docker...
docker --version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Docker n'est pas installe. Veuillez installer Docker Desktop.
    exit /b 1
)
for /f "delims=" %%V in ('docker --version') do echo [OK] Docker trouve: %%V
echo.

REM Verifier Docker Compose
echo [*] Verification de l'installation Docker Compose...
docker-compose --version >nul 2>&1
if not errorlevel 1 (
    for /f "delims=" %%V in ('docker-compose --version') do echo [OK] Docker Compose trouve: %%V
    goto SUCCESS
)
docker compose version >nul 2>&1
if not errorlevel 1 (
    echo [OK] Docker Compose trouve (via "docker compose")
    goto SUCCESS
)
echo [ERROR] Docker Compose n'est pas installe.
exit /b 1

:SUCCESS
echo.
echo ============================================================
echo   [OK] Configuration completee avec succes!
echo ============================================================
echo.
echo Prochaines etapes:
echo   1. Verifier le fichier .env et mettre a jour les identifiants SMTP
echo   2. Executer: scripts\build-all.bat
echo   3. Executer: docker-compose up -d
echo   4. Executer: scripts\health-check.bat
echo.
goto :EOF

:RAND_CHAR
set CHARS=abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789
set /a IDX=!RANDOM! %% 62
set CH=!CHARS:~%IDX%,1!
set %1=!%1!!CH!
goto :EOF
