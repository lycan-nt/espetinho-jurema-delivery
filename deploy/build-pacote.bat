@echo off
chcp 65001 >nul
setlocal
echo ========================================
echo  Build do pacote Espetinho Jurema (Windows)
echo ========================================
echo.

set ROOT=%~dp0..
set FRONTEND=%ROOT%\frontend
set BACKEND=%ROOT%\backend
set STATIC=%BACKEND%\src\main\resources\static
set JAR_NAME=espetinho-jurema-api-2.2.1-SNAPSHOT.jar
set DEPLOY_JAR=espetinho-app.jar

echo [1/4] Build do frontend (Angular)...
cd /d "%FRONTEND%"
set AJV=%FRONTEND%\node_modules\ajv\dist\vocabularies\applicator\index.js
if not exist "node_modules" goto install_fe
if not exist "%AJV%" (
  echo node_modules incompleto. Removendo e reinstalando...
  rmdir /s /q node_modules
  goto install_fe
)
goto build_fe
:install_fe
if exist package-lock.json ( call npm ci ) else ( call npm install )
if errorlevel 1 ( echo ERRO no npm install/ci. & exit /b 1 )
:build_fe
call npm run build -- --configuration=production
if errorlevel 1 ( echo ERRO no build do frontend. & exit /b 1 )
echo OK.
echo.

echo [2/4] Copiando frontend para backend (static)...
if exist "%STATIC%" rmdir /s /q "%STATIC%"
mkdir "%STATIC%"
if exist "%FRONTEND%\dist\frontend\browser" (
  xcopy "%FRONTEND%\dist\frontend\browser\*" "%STATIC%\" /e /i /y >nul
) else if exist "%FRONTEND%\dist\frontend" (
  xcopy "%FRONTEND%\dist\frontend\*" "%STATIC%\" /e /i /y >nul
) else (
  echo ERRO: pasta dist/frontend nao encontrada apos o build.
  exit /b 1
)
echo OK.
echo.

echo [3/4] Build do backend (Maven)...
echo Verificando Java 21+...
set "JAVA_BIN="
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" set "JAVA_BIN=%JAVA_HOME%\bin\java.exe"
if not defined JAVA_BIN for /f "delims=" %%J in ('where java 2^>nul') do ( set "JAVA_BIN=%%J" & goto :java_found )
:java_found
if not defined JAVA_BIN (
  echo ERRO: Java nao encontrado. Instale JDK 21 e defina JAVA_HOME.
  echo   winget install Microsoft.OpenJDK.21
  exit /b 1
)
for /f "tokens=3 delims= " %%V in ('"%JAVA_BIN%" -version 2^>^&1 ^| findstr /i version') do set "JAVA_VER=%%V"
set "JAVA_VER=%JAVA_VER:"=%"
for /f "delims=. tokens=1" %%M in ("%JAVA_VER%") do set "JAVA_MAJOR=%%M"
if %JAVA_MAJOR% LSS 21 (
  echo ERRO: backend exige JDK 21+. Detectado: Java %JAVA_MAJOR% ^(%JAVA_BIN%^)
  echo JAVA_HOME atual: %JAVA_HOME%
  echo Instale JDK 21, aponte JAVA_HOME e rode o build de novo.
  exit /b 1
)
"%JAVA_BIN%" -version 2>&1 | findstr /i version
cd /d "%BACKEND%"
if exist mvnw.cmd (
  call mvnw.cmd -q package -DskipTests
) else (
  call mvn -q package -DskipTests
)
if errorlevel 1 ( echo ERRO no build do backend. & exit /b 1 )
if not exist "%BACKEND%\target\%JAR_NAME%" (
  echo ERRO: JAR nao gerado: target\%JAR_NAME%
  exit /b 1
)
echo OK.
echo.

echo [4/4] Montando pasta deploy\instalar...
set INSTALAR=%ROOT%\deploy\instalar
if exist "%INSTALAR%\app" rmdir /s /q "%INSTALAR%\app"
mkdir "%INSTALAR%\app"
mkdir "%INSTALAR%\app\data"
copy "%BACKEND%\target\%JAR_NAME%" "%INSTALAR%\app\%DEPLOY_JAR%" >nul
copy "%ROOT%\deploy\restaurar-backup.bat" "%INSTALAR%\restaurar-backup.bat" >nul 2>nul
copy "%ROOT%\deploy\COMO-RESTAURAR-BACKUP.txt" "%INSTALAR%\COMO-RESTAURAR-BACKUP.txt" >nul 2>nul
(
echo Pasta app: apenas o JAR gerado pelo build.
echo Os dados do H2 ficam em ..\data na mesma pasta do start.bat.
) > "%INSTALAR%\app\data\LEIA-ME.txt"
echo OK.
echo.
echo ========================================
echo  Pacote pronto em: deploy\instalar
echo  Copie a pasta instalar para o cliente. Porta 9090.
echo ========================================

endlocal
exit /b 0
