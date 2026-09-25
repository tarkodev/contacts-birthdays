# Birthdays

Birthdays is an Android application for managing birthdays and other personal
events. The project is currently in its bootstrap phase: the imported Birday
baseline is kept working while the application identity, user experience, and
feature set are rebuilt.

## Project status

- Independent Git repository; it is not connected to an upstream remote.
- Android API 26 minimum, API 36 compile and target SDK.
- Kotlin, Android Views/ViewBinding, Room, and WorkManager.
- The release application id is `fr.exoti.bday`; debug builds use
  `fr.exoti.bday.debug`. The Kotlin namespace still reflects the Birday origin.

## Local development on Windows

The detailed setup, build, emulator, and test commands are documented in
[`DEVELOPMENT.md`](DEVELOPMENT.md).

Quick validation:

```powershell
.\scripts\windows\build-and-test.ps1
```

## Origin and license

The initial codebase was imported from Birday v4.7.2. Birthdays is distributed
under the GNU General Public License v3.0. See [`NOTICE.md`](NOTICE.md) for the
origin and attribution details and [`LICENSE.md`](LICENSE.md) for the complete
license.
