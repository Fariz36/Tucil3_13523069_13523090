@echo off
REM Rush Hour Compilation Script for Windows

echo Compiling Rush Hour Project...

REM Create bin directory if it doesn't exist
if not exist bin mkdir bin

REM Check if JavaFX is available
set JAVAFX_PATH=
if defined JAVAFX_HOME (
    set JAVAFX_PATH=%JAVAFX_HOME%\lib
) else if exist "C:\Program Files\JavaFX\lib" (
    set JAVAFX_PATH=C:\Program Files\JavaFX\lib
) else if exist "C:\javafx\lib" (
    set JAVAFX_PATH=C:\javafx\lib
) else if exist "%USERPROFILE%\javafx\lib" (
    set JAVAFX_PATH=%USERPROFILE%\javafx\lib
)

REM Try to compile with JavaFX first (for GUI)
if defined JAVAFX_PATH (
    echo Found JavaFX at: %JAVAFX_PATH%
    echo Compiling with JavaFX support...
    
    javac --module-path "%JAVAFX_PATH%" ^
          --add-modules javafx.controls,javafx.fxml ^
          -d bin ^
          -cp src ^
          src\cli\*.java ^
          src\gui\*.java ^
          src\gui\controllers\*.java
) else (
    echo JavaFX not found. Compiling CLI version only...
    
    REM Compile only CLI files
    javac -d bin ^
          -cp src ^
          src\cli\*.java
)

if %ERRORLEVEL% equ 0 (
    echo Compilation successful!
    
    REM Copy resources
    echo Copying resources...
    if not exist bin\resources mkdir bin\resources
    if exist src\resources\* (
        xcopy /Y /Q src\resources\* bin\resources\
    ) else (
        echo No resources to copy
    )
    
    REM Copy FXML files
    if exist src\gui\fxml (
        if not exist bin\gui\fxml mkdir bin\gui\fxml
        if exist src\gui\fxml\*.fxml (
            xcopy /Y /Q src\gui\fxml\*.fxml bin\gui\fxml\
        ) else (
            echo No FXML files to copy
        )
    )
    
    echo.
    echo Build complete! You can now run:
    echo   - CLI version: java -cp bin cli.Main
    
    if defined JAVAFX_PATH (
        echo   - GUI version: java --module-path "%JAVAFX_PATH%" --add-modules javafx.controls,javafx.fxml -cp bin gui.GuiMain
    )
) else (
    echo Compilation failed. Please check the error messages above.
    exit /b 1
)