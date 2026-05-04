@echo off
chcp 65001 >nul
cd /d "%~dp0"

net session >nul 2>&1
if errorlevel 1 (
  echo Execute como Administrador para desinstalar o servico.
  pause
  exit /b 1
)

echo ============================================
echo   Desinstalar servico Espetinho Jurema
echo ============================================
echo.

echo Parando o servico...
net stop EspetinhoJurema 2>nul
if errorlevel 1 (
  echo Servico ja estava parado ou nao existe.
) else (
  timeout /t 3 /nobreak >nul
)

set WINSW=WinSW-x64.exe
if not exist "%WINSW%" set WINSW=WinSW.exe
if exist "%WINSW%" (
  "%WINSW%" stop 2>nul
  timeout /t 2 /nobreak >nul
  "%WINSW%" uninstall
  if errorlevel 1 (
    sc delete EspetinhoJurema
  )
) else (
  sc delete EspetinhoJurema
)

echo.
echo Pronto.
pause
