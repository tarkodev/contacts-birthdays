[CmdletBinding()]
param(
    [string]$Avd = "Medium_Phone_API_36.0"
)

$ErrorActionPreference = "Stop"
$emulator = Join-Path $env:LOCALAPPDATA "Android\Sdk\emulator\emulator.exe"

if (-not (Test-Path -LiteralPath $emulator)) {
    throw "Android Emulator was not found at $emulator."
}

$availableAvds = & $emulator -list-avds
if ($Avd -notin $availableAvds) {
    throw "AVD '$Avd' does not exist. Available AVDs: $($availableAvds -join ', ')"
}

Start-Process -FilePath $emulator -ArgumentList @("-avd", $Avd)
