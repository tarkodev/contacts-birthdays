[CmdletBinding()]
param()

$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$androidSdk = Join-Path $env:LOCALAPPDATA "Android\Sdk"
$jdk = Get-ChildItem -LiteralPath "C:\Program Files\Eclipse Adoptium" -Directory -Filter "jdk-17*" |
    Sort-Object Name -Descending |
    Select-Object -First 1
$packageName = "fr.exoti.bday.debug"
$testRunner = "$packageName.test/androidx.test.runner.AndroidJUnitRunner"
$testClass = "com.minar.birday.testing.TestContactSeeder"
$photosDirectory = Join-Path $projectRoot "test-data\profile-photos"

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

$photos = Get-ChildItem -LiteralPath $photosDirectory -Filter "*.jpg"
if ($photos.Count -ne 15) {
    throw "Expected 15 fictional profile photos in $photosDirectory, found $($photos.Count)."
}

$env:JAVA_HOME = $jdk.FullName
$env:Path = "$($jdk.FullName)\bin;$env:Path"
$env:ANDROID_HOME = $androidSdk
$env:ANDROID_SDK_ROOT = $androidSdk

Push-Location -LiteralPath $projectRoot
try {
    & .\gradlew.bat installDebug installDebugAndroidTest
    if ($LASTEXITCODE -ne 0) {
        throw "Fixture test installation failed with exit code $LASTEXITCODE."
    }

    adb shell pm grant $packageName android.permission.READ_CONTACTS
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to grant READ_CONTACTS to $packageName."
    }

    adb shell pm grant $packageName android.permission.WRITE_CONTACTS
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to grant WRITE_CONTACTS to $packageName."
    }

    $instrumentationOutput = adb shell am instrument -w -e class $testClass $testRunner
    $instrumentationExitCode = $LASTEXITCODE
    $instrumentationOutput | Write-Output
    $instrumentationText = $instrumentationOutput -join "`n"
    if ($instrumentationExitCode -ne 0 -or $instrumentationText -notmatch "OK \(1 test\)") {
        throw "Contact fixture instrumentation failed."
    }
} finally {
    Pop-Location
}

Write-Output "Seeded 15 fictional contacts with synthetic profile photos into the emulator and app."
