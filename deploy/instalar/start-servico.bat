@echo off
cd /d "%~dp0"
if not exist "data" mkdir data
if not exist "logs" mkdir logs
set JAVA_OPTS=-Xmx512m -Dfile.encoding=UTF-8
set SPRING_OPTS=
if exist "config\application.properties" set SPRING_OPTS=--spring.config.additional-location=file:./config/
java %JAVA_OPTS% %SPRING_OPTS% -jar "app\espetinho-app.jar"
