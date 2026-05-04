@echo off
chcp 65001 >nul
title Delivere Acai - Servidor
cd /d "%~dp0"

set APP_JAR=app\acai-app.jar

:: Criar pastas se nao existirem (dados e logs ficam na mesma pasta do start.bat)
if not exist "data" mkdir data
if not exist "logs" mkdir logs

:: Opcoes de JVM para maquinas com pouca memoria (ajuste se precisar)
set JAVA_OPTS=-Xmx512m -Dfile.encoding=UTF-8
:: Reduz uso em maquinas fracas (descomente se necessario):
:: set JAVA_OPTS=-Xmx256m -Xms128m -Dfile.encoding=UTF-8 -Dspring.jmx.enabled=false

:: Config opcional por loja (coloque application.properties em config\)
set SPRING_OPTS=
if exist "config\application.properties" set SPRING_OPTS=--spring.config.additional-location=file:./config/

echo Iniciando aplicacao...
echo Dados em: %CD%\data
echo Logs em: %CD%\logs
echo.
echo Acesse: http://localhost:8080
echo Para parar: feche esta janela ou use stop.bat (se instalou como servico)
echo.

java %JAVA_OPTS% %SPRING_OPTS% -jar "%APP_JAR%"

pause
