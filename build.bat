@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0"

set "JAVAC_BIN=C:\Program Files\Java\jdk-25.0.4.1\bin\javac.exe"

if not exist "%JAVAC_BIN%" (
    where javac >nul 2>nul
    if %errorlevel% equ 0 (
        set "JAVAC_BIN=javac"
    ) else (
        echo [ERROR] javac was not found in %JAVAC_BIN% or the system PATH.
        exit /b 1
    )
)

if not exist "bin" mkdir "bin"

echo [COMPILING] Compiling Java classes with Java 25 LTS...

powershell -NoProfile -Command "$files = Get-ChildItem -Path src/main/java -Filter '*.java' -Recurse | ForEach-Object { $_.FullName }; & '%JAVAC_BIN%' --release 25 -d bin $files"

if %errorlevel% equ 0 (
    echo [SUCCESS] Compilation finished with zero errors in 'bin/'.
) else (
    echo [ERROR] Compilation failed.
    exit /b %errorlevel%
)
