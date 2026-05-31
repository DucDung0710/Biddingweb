@echo off
setlocal
rem Update these paths if your JDK or Maven locations differ.
set "JDK=C:\Users\ADMIN\.vscode\extensions\redhat.java-1.54.0-win32-x64\jre\21.0.10-win32-x86_64"
set "MAVEN=%USERPROFILE%\.maven\maven-3.9.16\apache-maven-3.9.16\bin\mvn.cmd"
set "WORKDIR=%~dp0"
cd /d "%WORKDIR%"

if not exist "%JDK%\bin\java.exe" (
  echo ERROR: JDK not found at %JDK%
  echo Please update the JDK path in run-app.bat.
  pause
  exit /b 1
)

if not exist "%MAVEN%" (
  echo WARNING: Maven not found at %MAVEN%
  echo Skipping Maven build. Ensure target/classes is up to date.
) else (
  echo Building project...
  "%MAVEN%" -q compile
)

echo Starting ServerMain in a separate window...
start "ServerMain" "%JDK%\bin\java.exe" -cp "%WORKDIR%target\classes;%WORKDIR%target\dependency\*" com.bidding.server.ServerMain

echo Waiting 2 seconds for server startup...
timeout /t 2 /nobreak >nul

echo Starting JavaFX client...
"%JDK%\bin\java.exe" --module-path "%USERPROFILE%\.m2\repository\org\openjfx\javafx-controls\21.0.6\javafx-controls-21.0.6-win.jar;%USERPROFILE%\.m2\repository\org\openjfx\javafx-fxml\21.0.6\javafx-fxml-21.0.6-win.jar;%USERPROFILE%\.m2\repository\org\openjfx\javafx-graphics\21.0.6\javafx-graphics-21.0.6-win.jar;%USERPROFILE%\.m2\repository\org\openjfx\javafx-base\21.0.6\javafx-base-21.0.6-win.jar" --add-modules javafx.controls,javafx.fxml,javafx.graphics --enable-native-access=javafx.graphics -cp "%WORKDIR%target\classes;%WORKDIR%target\dependency\*" com.bidding.app.BiddingApplication
