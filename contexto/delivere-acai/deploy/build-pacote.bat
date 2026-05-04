@echo off
chcp 65001 >nul
setlocal
echo ========================================
echo  Build do pacote Delivere Acai (Windows)
echo ========================================
echo.

set ROOT=%~dp0..
set FRONTEND=%ROOT%\frontend
set BACKEND=%ROOT%\backend
set STATIC=%BACKEND%\src\main\resources\static

:: 1) Build do frontend
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

:: 2) Copiar saida do frontend para backend/static
echo [2/4] Copiando frontend para backend (static)...
if exist "%STATIC%" rmdir /s /q "%STATIC%"
mkdir "%STATIC%"
:: Angular 18+ (application builder) gera dist/frontend/browser/*
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

:: 3) Build do backend (JAR)
echo [3/4] Build do backend (Maven)...
cd /d "%BACKEND%"
if exist mvnw.cmd (
  call mvnw.cmd -q package -DskipTests
) else (
  call mvn -q package -DskipTests
)
if errorlevel 1 ( echo ERRO no build do backend. & exit /b 1 )
if not exist "%BACKEND%\target\acai-backend-1.0.0.jar" (
  echo ERRO: JAR nao gerado.
  exit /b 1
)
echo OK.
echo.

:: 4) Copiar pacote para deploy/instalar
echo [4/4] Montando pasta instalar...
set INSTALAR=%ROOT%\deploy\instalar
if exist "%INSTALAR%\app" rmdir /s /q "%INSTALAR%\app"
mkdir "%INSTALAR%\app"
copy "%BACKEND%\target\acai-backend-1.0.0.jar" "%INSTALAR%\app\acai-app.jar" >nul
copy "%ROOT%\deploy\restaurar-backup.bat" "%INSTALAR%\app\restaurar-backup.bat" >nul
copy "%ROOT%\deploy\COMO-RESTAURAR-BACKUP.txt" "%INSTALAR%\app\COMO-RESTAURAR-BACKUP.txt" >nul
mkdir "%INSTALAR%\app\data"
(
echo Pasta do banco H2. O arquivo acaidb.mv.db sera criado na primeira execucao.
echo.
echo BACKUP: uma vez ao dia o banco e salvo no caminho definido em app.backup.path no application.properties. Nome do arquivo: acaidb-backup-AAAA-MM-DD.zip
echo.
echo RESTAURAR: quando for reinstalar e quiser voltar os dados, leia o arquivo COMO-RESTAURAR-BACKUP.txt que esta na pasta app. Em resumo: feche a aplicacao, execute na pasta app: restaurar-backup.bat "caminho\do\acaidb-backup-AAAA-MM-DD.zip", depois inicie a aplicacao.
) > "%INSTALAR%\app\data\LEIA-ME.txt"
echo OK.
echo.
echo ========================================
echo  Pacote pronto em: deploy\instalar
echo  Copie a pasta instalar para o cliente. Porta 8080.
echo ========================================

endlocal
exit /b 0
