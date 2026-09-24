@echo off
chcp 65001 > nul
echo ===================================================
echo   Запуск приложения "Два Ангела" на компьютере
echo ===================================================
echo.

where python >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    echo Запуск локального сервера через Python...
    start http://localhost:8000/web/index.html
    python -m http.server 8000
    exit /b
)

where npx >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    echo Запуск локального сервера через Node.js...
    npx --yes serve web -l 8000
    exit /b
)

echo Открытие веб-приложения в браузере...
start "" "%~dp0web\index.html"
