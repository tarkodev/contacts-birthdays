# Fictional contact fixtures

`contacts.vcf` contains synthetic data created exclusively for AnnivApp tests.
The names and dates are fictional and must not be replaced with personal data.

For a repeatable import into a running emulator:

```powershell
.\scripts\windows\seed-test-contacts.ps1
```

For a manual VCF import:

```powershell
adb push .\test-data\contacts.vcf /sdcard/Download/annivapp-test-contacts.vcf
```

Then open Contacts, choose import from file, and select the file from Downloads.
