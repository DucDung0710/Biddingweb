@echo off
cd /d "%~dp0"
set MAVEN_PATH=%USERPROFILE%\.maven\maven-3.9.16\apache-maven-3.9.16\bin\mvn.cmd
if exist "%MAVEN_PATH%" (
    echo Running JavaFX GUI via local Maven...
    "%MAVEN_PATH%" javafx:run
) else (
    echo Local Maven not found: %MAVEN_PATH%
    echo Please install Maven or update this script with the correct path.
    pause
)
