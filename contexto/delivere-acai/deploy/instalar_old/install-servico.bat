@echo off
chcp 65001 >nul
cd /d "%~dp0"
title Instalar servico Delivere Acai

echo.
echo ============================================
echo   Instalar servico "Delivere Acai"
echo   (iniciar junto com o Windows)
echo ============================================
echo.

:: Obrigar execucao como Administrador
net session >nul 2>&1
if errorlevel 1 (
  echo ERRO: E preciso executar como ADMINISTRADOR.
  echo.
  echo Clique com o botao direito neste arquivo
  echo e escolha "Executar como administrador".
  echo.
  pause
  exit /b 1
)

:: Encontrar o WinSW (WinSW-x64.exe ou WinSW.exe)
set WINSW=WinSW-x64.exe
if not exist "%WINSW%" set WINSW=WinSW.exe
if not exist "%WINSW%" (
  echo ERRO: WinSW nao encontrado nesta pasta.
  echo.
  echo 1. Baixe em: https://github.com/winsw/winsw/releases
  echo 2. Baixe "WinSW-x64.exe" ^(Windows 64 bits^)
  echo 3. Coloque o arquivo nesta pasta:
  echo    %CD%
  echo 4. Execute este script novamente.
  echo.
  pause
  exit /b 1
)

:: O WinSW exige um XML com o MESMO NOME do .exe na mesma pasta
:: Ex.: WinSW-x64.exe procura WinSW-x64.xml
set NOME_BASE=%WINSW:~0,-4%
set XML_DEST=%NOME_BASE%.xml

echo Copiando configuracao para %XML_DEST% ...
copy /y "acai-servico.xml" "%XML_DEST%" >nul
if errorlevel 1 (
  echo ERRO ao copiar o XML.
  pause
  exit /b 1
)

echo Instalando o servico...
echo.
"%WINSW%" install
if errorlevel 1 (
  echo.
  echo ERRO ao instalar. Verifique se o Java esta instalado e no PATH.
  echo Teste no Prompt: java -version
  echo.
  pause
  exit /b 1
)

echo.
echo ============================================
echo   Servico instalado com sucesso.
echo ============================================
echo.
echo Para iniciar AGORA (sem reiniciar o PC):
echo   Abra o Prompt como administrador e digite:
echo   net start DelivereAcai
echo.
echo Ou reinicie o computador - o servico sobe automaticamente.
echo.
echo Para verificar: services.msc e procure "Delivere Acai"
echo.
pause
