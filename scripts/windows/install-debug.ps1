[CmdletBinding()]
param()

$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$androidSdk = Join-Path $env:LOCALAPPDATA "Android\Sdk"
$jdk = Get-ChildItem -LiteralPath "C:\Program Files\Eclipse Adoptium" -Directory -Filter "jdk-17*" |
    Sort-Object Name -Descending |
    Select-Object -First 1

if (-not $jdk) {
    throw "JDK 17 was not found under C:\Program Files\Eclipse Adoptium."
}

if (-not (Test-Path -LiteralPath $androidSdk)) {
    throw "Android SDK was not found at $androidSdk."
}

if (-not (Get-Command adb -ErrorAction SilentlyContinue)) {
    throw "adb is not available on PATH."
}

if (-not (adb devices | Select-String -Pattern "\sdevice$")) {
    throw "No booted Android device or emulator was found."
}

$env:JAVA_HOME = $jdk.FullName
$env:Path = "$($jdk.FullName)\bin;$env:Path"
$env:ANDROID_HOME = $androidSdk
$env:ANDROID_SDK_ROOT = $androidSdk

Push-Location -LiteralPath $projectRoot
try {
    & .\gradlew.bat installDebug
    if ($LASTEXITCODE -ne 0) {
        throw "Debug installation failed with exit code $LASTEXITCODE."
    }
} finally {
    Pop-Location
}
