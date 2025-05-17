@echo off
REM KessokuNoOwari - Batch script with additional CLI commands
REM Usage:
REM   kessoku.bat build       # Build the JAR and run Gradle build
REM   kessoku.bat run         # Run the GUI application using runGui task
REM   kessoku.bat cli         # Run the CLI application using runCli task
REM   kessoku.bat clean       # Clean .jar and .class files from bin folder

setlocal enabledelayedexpansion

set SCRIPT_DIR=%~dp0
set SCRIPT_DIR=%SCRIPT_DIR:~0,-1%

if "%1"=="" goto usage
if "%1"=="build" goto build
if "%1"=="gui" goto gui
if "%1"=="cli" goto cli
if "%1"=="clean" goto clean
goto usage

:build
echo Building KessokuNoOwari.jar...

REM Check for Java
where java >nul 2>&1
if %ERRORLEVEL% neq 0 (
  echo Error: Java not found. Please install Java.
  exit /b 1
)

REM Check for JDK
where javac >nul 2>&1
if %ERRORLEVEL% neq 0 (
  echo Error: Java compiler not found. Please install JDK.
  exit /b 1
)

REM Create bin directory
if not exist "%SCRIPT_DIR%\bin" mkdir "%SCRIPT_DIR%\bin"

REM Compile Java source
javac -d "%SCRIPT_DIR%\bin" "%SCRIPT_DIR%\src\KessokuNoOwari.java"
if %ERRORLEVEL% neq 0 (
  echo Error: Compilation failed.
  exit /b 1
)

REM Create JAR
cd "%SCRIPT_DIR%\bin"
jar cvfe KessokuNoOwari.jar KessokuNoOwari KessokuNoOwari.class
if %ERRORLEVEL% neq 0 (
  echo Error: JAR creation failed.
  exit /b 1
)
cd "%SCRIPT_DIR%"

echo Success! JAR created at: %SCRIPT_DIR%\bin\KessokuNoOwari.jar
echo.
echo Now running Gradle build...

REM Run Gradle build using the JAR
cd "%SCRIPT_DIR%"
java -jar "%SCRIPT_DIR%\bin\KessokuNoOwari.jar" build
exit /b %ERRORLEVEL%

:gui
if not exist "%SCRIPT_DIR%\bin\KessokuNoOwari.jar" (
  echo Error: KessokuNoOwari.jar not found! Please build it first with:
  echo   kessoku.bat build
  exit /b 1
)

echo Running the GUI application...
cd "%SCRIPT_DIR%"
REM Run the GUI application using Gradle runGui task
java -jar "%SCRIPT_DIR%\bin\KessokuNoOwari.jar" runGui
exit /b %ERRORLEVEL%

:cli
if not exist "%SCRIPT_DIR%\bin\KessokuNoOwari.jar" (
  echo Error: KessokuNoOwari.jar not found! Please build it first with:
  echo   kessoku.bat build
  exit /b 1
)

echo Running the CLI application...
cd "%SCRIPT_DIR%"
REM Run the CLI application using Gradle runCli task
java -jar "%SCRIPT_DIR%\bin\KessokuNoOwari.jar" runCli
exit /b %ERRORLEVEL%

:clean
echo Cleaning bin folder...
set bin_dir=%SCRIPT_DIR%\bin

REM Check if bin directory exists
if not exist "%bin_dir%" (
  echo Bin directory does not exist. Nothing to clean.
  exit /b 0
)

REM Remove JAR files first
echo Removing JAR files...
del /s /q "%bin_dir%\*.jar" 2>nul
if errorlevel 0 (
  echo JAR files removed successfully.
) else (
  echo No JAR files found or unable to delete them.
)

REM Remove class files next
echo Removing class files...
del /s /q "%bin_dir%\*.class" 2>nul
if errorlevel 0 (
  echo Class files removed successfully.
) else (
  echo No class files found or unable to delete them.
)

echo Clean completed.
exit /b 0

:usage
echo KessokuNoOwari - Gradle Launcher
echo Usage:
echo   kessoku.bat build       # Build the JAR and run Gradle build
echo   kessoku.bat gui         # Run the GUI application
echo   kessoku.bat cli         # Run the CLI application
echo   kessoku.bat clean       # Clean .jar and .class files from bin folder
echo.
echo Examples:
echo   kessoku.bat build
echo   kessoku.bat gui
echo   kessoku.bat cli
echo   kessoku.bat clean
exit /b 1