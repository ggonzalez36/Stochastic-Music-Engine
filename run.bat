@echo off
setlocal
cd /d "%~dp0"

set "JAVA_BIN=C:\Program Files\Java\jdk-25.0.4.1\bin\java.exe"

if not exist "%JAVA_BIN%" (
    where java >nul 2>nul
    if %errorlevel% equ 0 (
        set "JAVA_BIN=java"
    ) else (
        echo [ERROR] Java 25 was not found in %JAVA_BIN% or the system PATH.
        exit /b 1
    )
)

if not exist "bin\com\algorithmic\music\Main.class" (
    echo [INFO] Binaries not found. Compiling project first...
    call "%~dp0build.bat"
)

echo =====================================================================
echo  STARTING STOCHASTIC TECHNO ENGINE (JAVA 25 LTS)
echo  Press Ctrl + C to stop playback at any time.
echo =====================================================================
echo.

"%JAVA_BIN%" -cp bin com.algorithmic.music.Main %*
