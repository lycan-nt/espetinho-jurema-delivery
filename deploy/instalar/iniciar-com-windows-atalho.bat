@echo off
chcp 65001 >nul
cd /d "%~dp0"

set STARTUP=%APPDATA%\Microsoft\Windows\Start Menu\Programs\Startup
set TARGET=%~dp0start.bat
set LINK=%STARTUP%\Espetinho Jurema - Servidor.lnk

powershell -NoProfile -Command "$ws = New-Object -ComObject WScript.Shell; $s = $ws.CreateShortcut('%LINK%'); $s.TargetPath = '%TARGET%'; $s.WorkingDirectory = '%~dp0'; $s.Save()"
echo Atalho criado na pasta Inicializar do usuario.
echo Para remover: apague o atalho em:
echo %STARTUP%
pause
