@echo off
chcp 65001 >nul
setlocal
if "%~1"=="" (
  echo Uso: restaurar-backup.bat "C:\caminho\para\espetinho-backup-....zip"
  echo.
  echo Execute na pasta INSTALAR (mesma pasta do start.bat e da pasta data).
  echo Feche a aplicacao antes de restaurar.
  exit /b 1
)

set ZIP=%~1
set DATA=%~dp0data

if not exist "%ZIP%" (
  echo Arquivo nao encontrado: %ZIP%
  exit /b 1
)

echo Restaurando backup: %ZIP%
echo Destino: %DATA%
echo.

if exist "%DATA%\espetinho.mv.db" (
  echo Fazendo copia de seguranca da pasta data atual em data.old ...
  if exist "%DATA%.old" rmdir /s /q "%DATA%.old"
  ren "%DATA%" "data.old"
  mkdir "%DATA%"
)

powershell -NoProfile -Command "Expand-Archive -Path '%ZIP%' -DestinationPath '%DATA%' -Force"
if errorlevel 1 (
  echo Erro ao descompactar. Verifique se o arquivo e um zip valido do backup H2.
  exit /b 1
)

echo.
echo Restauracao concluida. Pode iniciar a aplicacao.
endlocal
exit /b 0
