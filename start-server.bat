@echo off
setlocal

cd /d "%~dp0"

where node >nul 2>nul
if errorlevel 1 (
  echo Node.js is required to run Book Reader.
  echo Install Node.js 18 or newer, then try again.
  pause
  exit /b 1
)

echo Starting Book Reader at http://100.127.36.5:3511
echo Local access is available at http://localhost:3511
echo Press Ctrl+C to stop the server.
echo.

node server.js

if errorlevel 1 (
  echo.
  echo The Book Reader server stopped with an error.
  pause
  exit /b 1
)
