@echo off
REM Banking Portal - Health Check Script
REM Verifies all services are running and healthy

setlocal EnableDelayedExpansion

echo ============================================================
echo   Banking Portal - Health Check
echo ============================================================
echo.

set ALL_HEALTHY=1

call :CHECK_SERVICE "Auth Service"        8081 /api/auth/health
call :CHECK_SERVICE "Account Service"     8082 /api/accounts/health
call :CHECK_SERVICE "Transaction Service" 8083 /api/transactions/health
call :CHECK_SERVICE "RabbitMQ UI"         15672 /
call :CHECK_SERVICE "Frontend"            443 /

echo.
echo ============================================================
if "!ALL_HEALTHY!"=="1" (
    echo   [OK] All services are healthy!
    echo ============================================================
    echo.
    echo Service URLs:
    echo   Frontend           : https://localhost
    echo   Auth Service       : http://localhost:8081
    echo   Account Service    : http://localhost:8082
    echo   Transaction Service: http://localhost:8083
    echo   RabbitMQ UI        : http://localhost:15672  (guest/guest)
    echo.
) else (
    echo   [!] Some services are unhealthy!
    echo ============================================================
    echo.
    echo Check logs with: docker-compose logs ^<service-name^>
    exit /b 1
)
goto :EOF

:CHECK_SERVICE
set SVC_NAME=%~1
set SVC_PORT=%~2
set SVC_PATH=%~3
<nul set /p =Checking %SVC_NAME% (PORT %SVC_PORT%)... 
curl -s -o nul -w "%%{http_code}" "http://localhost:%SVC_PORT%%SVC_PATH%" >"%TEMP%\_hc_status.txt" 2>nul
set /p HTTP_CODE=<"%TEMP%\_hc_status.txt"
del "%TEMP%\_hc_status.txt" >nul 2>&1
if "%HTTP_CODE%"=="200" (
    echo OK
) else if "%HTTP_CODE%"=="301" (
    echo OK (redirect^)
) else if "%HTTP_CODE%"=="302" (
    echo OK (redirect^)
) else (
    echo ERROR (HTTP %HTTP_CODE%^)
    set ALL_HEALTHY=0
)
goto :EOF
