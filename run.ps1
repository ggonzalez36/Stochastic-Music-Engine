param (
    [int]$Duration = 0
)

$javaBin = "C:\Program Files\Java\jdk-25.0.4.1\bin\java.exe"
if (-not (Test-Path $javaBin)) {
    $cmd = Get-Command java -ErrorAction SilentlyContinue
    if ($cmd) {
        $javaBin = "java"
    } else {
        Write-Error "No se encontro Java 25 en $javaBin ni en el PATH del sistema."
        exit 1
    }
}

$root = $PSScriptRoot
if (-not (Test-Path "$root\bin\com\algorithmic\music\Main.class")) {
    Write-Host "[INFO] Binarios no encontrados. Compilando primero..." -ForegroundColor Yellow
    $javacBin = "C:\Program Files\Java\jdk-25.0.4.1\bin\javac.exe"
    $files = Get-ChildItem -Path "$root\src\main\java" -Filter "*.java" -Recurse | ForEach-Object { $_.FullName }
    & $javacBin --release 25 -d "$root\bin" $files
}

Write-Host "=====================================================================" -ForegroundColor Cyan
Write-Host " INICIANDO MOTOR DE MUSICA ESTOCASTICA (JAVA 25 LTS)" -ForegroundColor Green
Write-Host " Pulsa Ctrl + C para detener la reproduccion en cualquier momento." -ForegroundColor Yellow
Write-Host "=====================================================================" -ForegroundColor Cyan

if ($Duration -gt 0) {
    & $javaBin -cp "$root\bin" com.algorithmic.music.Main $Duration
} else {
    & $javaBin -cp "$root\bin" com.algorithmic.music.Main
}
