# Birthdays development guide

## Prerequisites

- Windows 11 with hardware virtualization enabled
- Android Studio
- Android SDK Platform 36 and Build Tools 35.0.0 or newer
- JDK 17
- Git and Android Debug Bridge (`adb`)

The Gradle wrapper is the only supported way to invoke Gradle. Do not use a
system-installed Gradle distribution.

## Java on this workstation

The Windows helper scripts automatically select the newest JDK 17 installed
under `C:\Program Files\Eclipse Adoptium`. In Android Studio, select the same
JDK in:

`Settings > Build, Execution, Deployment > Build Tools > Gradle > Gradle JDK`

The command-line default JDK may be newer and incompatible with this project's
Gradle version, so use the helper scripts or set `JAVA_HOME` to JDK 17.

## Build and static validation

```powershell
Set-Location 'C:\path\to\contacts-birthdays'
.\scripts\windows\build-and-test.ps1
```

This runs unit tests, Android lint, and a debug APK build. The APK is written to
`app\build\outputs\apk\debug\app-debug.apk`.

## Emulator

The default local AVD is `Medium_Phone_API_36.0`.

```powershell
.\scripts\windows\start-emulator.ps1
```

After Android finishes booting:

```powershell
.\scripts\windows\install-debug.ps1
.\scripts\windows\seed-test-contacts.ps1
.\scripts\windows\smoke-test.ps1
```

The app can also be run from Android Studio by selecting the same AVD and using
the Run action.

## Test contacts

`test-data\contacts.vcf` contains fictional contacts covering ordinary dates,
missing years, duplicate names, long names, and February 29. Twelve synthetic
profile portraits are stored in `test-data\profile-photos`. Never use personal
contacts for automated testing. The repeatable local setup is:

```powershell
.\scripts\windows\seed-test-contacts.ps1
```

The script replaces only contacts stored under its dedicated `AnnivAppTest`
test account. It does not delete personal or unrelated emulator contacts. A
debug-only instrumentation helper attaches the portraits and updates Birthdays'
test events immediately; the release manifest does not request contact-write
access.

Alternatively, copy the VCF file to the emulator and open it with Contacts:

```powershell
adb push .\test-data\contacts.vcf /sdcard/Download/annivapp-test-contacts.vcf
```

## Test layers

- `testDebugUnitTest`: date, age, recurrence, sorting, and data conversion logic
- `lintDebug`: Android correctness, resource, permission, and accessibility lint
- `assembleDebugAndroidTest`: compiles the device helpers and instrumentation tests
- `TestContactSeeder`: writes and verifies the 12 contacts, portraits, and app events
- `smoke-test.ps1`: installed-process, fixture visibility, and fatal-log check
- Manual device pass: notification delivery, battery restrictions, and widgets

The current imported project has limited automated coverage. New behavior must
be introduced behind testable interfaces and accompanied by tests.

## Git workflow

- `main` must remain buildable.
- Use `feature/<name>` and `fix/<name>` branches.
- Do not add an upstream remote. The imported version is fixed in `NOTICE.md`.
- Never commit signing keys, passwords, tokens, or real contact data.
