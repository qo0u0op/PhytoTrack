@echo off
REM prod only
start "" "%~dp0\phytotrack.exe" --spring.profiles.active=prod %*
