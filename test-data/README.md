# Fictional contact fixtures

`contacts.vcf` contains synthetic data created exclusively for AnnivApp tests.
The names and dates are fictional and must not be replaced with personal data.

`profile-photos` contains 15 AI-generated fictional adult portraits. They do
not depict known or intentionally identifiable people. The automated seeder
assigns one photo to each contact and imports the same image into the app's
debug database.

Four fixtures intentionally omit the birth year (`--MM-DD`) to exercise the
unknown-age path: Nina, Omar, Salome, and Eva.

For a repeatable import into a running emulator:

```powershell
.\scripts\windows\seed-test-contacts.ps1
```

The seeder compiles and runs a debug-only Android instrumentation helper. It
grants contact-write access only to the debug package; release builds do not
request this permission.

For a manual VCF import:

```powershell
adb push .\test-data\contacts.vcf /sdcard/Download/annivapp-test-contacts.vcf
```

Then open Contacts, choose import from file, and select the file from Downloads.
