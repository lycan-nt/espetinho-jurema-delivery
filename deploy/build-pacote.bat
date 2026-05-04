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
set JAR_NAME=espetinho-jurema-api-1.0.0-SNAPSHOT.jar
set DEPLOY_JAR=espetinho-app.jar

echo [1/4] Build do frontend (Angular)...
cd /d "%FRONTEND%"
if not exist "node_modules" (
  echo Instalando dependencias do frontend (npm install)...
  call npm install
  if errorlevel 1 ( echo ERRO no npm install. & exit /b 1 )
)
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
