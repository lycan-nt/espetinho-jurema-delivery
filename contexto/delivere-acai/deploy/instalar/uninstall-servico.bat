@echo off
chcp 65001 >nul
cd /d "%~dp0"

net session >nul 2>&1
if errorlevel 1 (
  echo Execute como Administrador para desinstalar o servico.
  echo Botao direito no arquivo - Executar como administrador.
  pause
  exit /b 1
)

echo ============================================
echo   Desinstalar servico Delivere Acai
echo ============================================
echo.

echo Parando o servico...
net stop DelivereAcai 2>nul
if errorlevel 1 (
  echo Servico ja estava parado ou nao existe.
) else (
  echo Servico parado. Aguardando 3 segundos...
  timeout /t 3 /nobreak >nul
)

:: Tentar desinstalar via WinSW (mesmo jeito que foi instalado)
set WINSW=WinSW-x64.exe
if not exist "%WINSW%" set WINSW=WinSW.exe
if exist "%WINSW%" (
  echo Removendo servico com WinSW...
  "%WINSW%" stop 2>nul
  timeout /t 2 /nobreak >nul
  "%WINSW%" uninstall
  if errorlevel 1 (
    echo WinSW uninstall falhou. Tentando sc delete...
    sc delete DelivereAcai
  ) else (
    echo Servico removido com WinSW.
    goto fim
  )
) else (
  echo WinSW nao encontrado nesta pasta. Removendo servico com sc delete...
  sc delete DelivereAcai
)

if errorlevel 1 (
  echo.
  echo Se der erro, abra o Prompt como Administrador nesta pasta e execute:
  echo   sc delete DelivereAcai
  echo Ou em services.msc clique com botao direito no servico e Desinstalar.
) else (
  echo Servico removido.
)

:fim
echo.
echo Pronto. Pode reinstalar com install-servico.bat.
echo.
pause
