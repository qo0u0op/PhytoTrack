@echo off
REM PhytoTrack launcher - pure ASCII, no Big5
REM Default prod (only admin); for dev with staff/viewer: PhytoTrack.bat --spring.profiles.active=dev
setlocal
set "ARGS=%*"
echo %ARGS% | findstr /C:"spring.profiles.active" >nul
if %errorlevel%==0 (
  cmd /c "%~dp0\phytotrack\bin\phytotrack.exe" %*
) else (
  cmd /c "%~dp0\phytotrack\bin\phytotrack.exe" --spring.profiles.active=prod %*
)
endlocal
