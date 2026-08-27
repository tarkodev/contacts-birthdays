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

$env:JAVA_HOME = $jdk.FullName
$env:Path = "$($jdk.FullName)\bin;$env:Path"
$env:ANDROID_HOME = $androidSdk
$env:ANDROID_SDK_ROOT = $androidSdk

Push-Location -LiteralPath $projectRoot
try {
    & .\gradlew.bat testDebugUnitTest lintDebug assembleDebug
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle validation failed with exit code $LASTEXITCODE."
    }
} finally {
    Pop-Location
}
