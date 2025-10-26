@echo off
where gradle >nul 2>nul
if %errorlevel%==0 (
  gradle %*
) else (
  echo Gradle is not installed. Please install Gradle or use Android Studio to build this project.
  exit /b 1
)
