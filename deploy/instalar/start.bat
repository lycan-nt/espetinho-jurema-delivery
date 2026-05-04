@echo off
chcp 65001 >nul
title Espetinho Jurema - Servidor
cd /d "%~dp0"

set APP_JAR=app\espetinho-app.jar

if not exist "data" mkdir data
if not exist "logs" mkdir logs

set JAVA_OPTS=-Xmx512m -Dfile.encoding=UTF-8

set SPRING_OPTS=
if exist "config\application.properties" set SPRING_OPTS=--spring.config.additional-location=file:./config/

echo Iniciando aplicacao...
echo Dados em: %CD%\data
echo Logs em: %CD%\logs
echo.
echo Acesse: http://localhost:9090
echo Para parar: feche esta janela ou pare o servico Windows se instalou como servico.
echo.

java %JAVA_OPTS% %SPRING_OPTS% -jar "%APP_JAR%"

pause
