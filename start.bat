@echo off
title Italiano - Italian Vocabulary App

echo ============================================
echo   Italiano - Italian Vocabulary App
echo ============================================
echo.

REM Start backend (Spring Boot, port 8080)
echo [1/2] Starting backend...
start "Italiano-Backend" cmd /k "cd /d "%~dp0backend" && mvn spring-boot:run"

REM Wait for backend to initialize
timeout /t 5 /nobreak >nul

REM Start frontend (Vite, port 5173)
echo [2/2] Starting frontend...
start "Italiano-Frontend" cmd /k "cd /d "%~dp0frontend" && npm run dev"

REM Wait for frontend then open browser
timeout /t 5 /nobreak >nul
start http://localhost:5173

echo.
echo Started:
echo   Backend  http://localhost:8080  (window: Italiano-Backend)
echo   Frontend http://localhost:5173  (window: Italiano-Frontend)
echo.
echo Close the two cmd windows to stop the services.
pause
