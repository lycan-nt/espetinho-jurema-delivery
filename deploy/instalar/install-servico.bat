@echo off
chcp 65001 >nul
cd /d "%~dp0"
title Instalar servico Espetinho Jurema

echo.
echo ============================================
echo   Instalar servico "Espetinho Jurema"
echo ============================================
echo.

net session >nul 2>&1
if errorlevel 1 (
  echo ERRO: Execute como ADMINISTRADOR.
  echo Botao direito neste arquivo - Executar como administrador.
  echo.
  pause
  exit /b 1
)

set WINSW=WinSW-x64.exe
if not exist "%WINSW%" set WINSW=WinSW.exe
if not exist "%WINSW%" (
  echo ERRO: WinSW nao encontrado nesta pasta.
  echo.
  echo 1. Baixe em: https://github.com/winsw/winsw/releases
  echo 2. Coloque WinSW-x64.exe nesta pasta:
  echo    %CD%
  echo 3. Execute este script novamente.
  echo.
  pause
  exit /b 1
)

set NOME_BASE=%WINSW:~0,-4%
set XML_DEST=%NOME_BASE%.xml

echo Copiando configuracao para %XML_DEST% ...
copy /y "espetinho-jurema-servico.xml" "%XML_DEST%" >nul
if errorlevel 1 (
  echo ERRO ao copiar o XML.
  pause
  exit /b 1
)

echo Instalando o servico...
echo.
"%WINSW%" install
if errorlevel 1 (
  echo ERRO ao instalar. Verifique Java no PATH: java -version
  pause
  exit /b 1
)

echo.
echo ============================================
echo   Servico instalado.
echo ============================================
echo.
echo Para iniciar agora (Prompt como administrador):
echo   net start EspetinhoJurema
echo.
echo Ou reinicie o PC.
echo services.msc - procure "Espetinho Jurema"
echo.
pause
