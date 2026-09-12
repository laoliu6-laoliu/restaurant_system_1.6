@echo off
set /p MYSQL_USER=MySQL user [root]: 
if "%MYSQL_USER%"=="" set MYSQL_USER=root
set /p MYSQL_PASSWORD=MySQL password: 
mysql --default-character-set=utf8mb4 -u%MYSQL_USER% -p%MYSQL_PASSWORD% < "%~dp0resjk_seed.sql"
if errorlevel 1 (
  echo.
  echo Import failed. Please check MySQL username, password, and whether database resjk exists.
  pause
  exit /b 1
)
echo.
echo Seed data imported into database resjk.
pause
