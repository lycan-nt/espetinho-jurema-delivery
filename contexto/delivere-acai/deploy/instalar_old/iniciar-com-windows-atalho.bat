@echo off
chcp 65001 >nul
cd /d "%~dp0"

:: Cria um atalho na pasta Inicializar do Windows (app abre quando o usuario fizer login)
set STARTUP=%APPDATA%\Microsoft\Windows\Start Menu\Programs\Startup
set TARGET=%~dp0start.bat
set LINK=%STARTUP%\Delivere Acai - Iniciar servidor.lnk

powershell -NoProfile -Command "$ws = New-Object -ComObject WScript.Shell; $s = $ws.CreateShortcut('%LINK%'); $s.TargetPath = '%TARGET%'; $s.WorkingDirectory = '%~dp0'; $s.Save()"
echo.
echo Atalho criado em: Iniciar com o Windows
echo O servidor vai abrir quando voce fizer login neste PC.
echo.
echo Para remover: delete o atalho em:
echo %STARTUP%
echo.
pause
