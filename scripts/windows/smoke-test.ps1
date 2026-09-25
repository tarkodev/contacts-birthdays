[CmdletBinding()]
param(
    [string]$Package = "fr.exoti.bday.debug"
)

$ErrorActionPreference = "Stop"

if (-not (Get-Command adb -ErrorAction SilentlyContinue)) {
    throw "adb is not available on PATH."
}

if (-not (adb devices | Select-String -Pattern "\sdevice$")) {
    throw "No booted Android device or emulator was found."
}

$installedPackages = adb shell pm list packages $Package
if ($installedPackages -notmatch [regex]::Escape($Package)) {
    throw "Package '$Package' is not installed. Run install-debug.ps1 first."
}

adb shell monkey -p $Package -c android.intent.category.LAUNCHER 1 | Out-Null
Start-Sleep -Seconds 2

$pidValue = (adb shell pidof $Package).Trim()
if (-not $pidValue) {
    throw "Package '$Package' did not remain running after launch."
}

adb shell uiautomator dump /sdcard/annivapp-smoke.xml | Out-Null
$ui = adb shell cat /sdcard/annivapp-smoke.xml

foreach ($expectedText in "Aline Test", "Benoit Exemple") {
    if ($ui -notmatch [regex]::Escape($expectedText)) {
        throw "Expected UI text '$expectedText' was not found. Complete onboarding and seed contacts first."
    }
}

$contactsWithPhotos = adb shell content query `
    --uri content://com.android.contacts/contacts `
    --projection _id:display_name:photo_uri |
    Select-String -Pattern "photo_uri=content://"

if ($contactsWithPhotos.Count -lt 12) {
    throw "Expected at least 12 contact photos, found $($contactsWithPhotos.Count). Run seed-test-contacts.ps1 first."
}

$fatalLogs = adb logcat -d -t 300 AndroidRuntime:E "*:S"

if ($fatalLogs -match "Process: $([regex]::Escape($Package))") {
    throw "A fatal Android log entry was detected for '$Package'."
}

Write-Output "Smoke test passed for $Package (PID $pidValue)."
