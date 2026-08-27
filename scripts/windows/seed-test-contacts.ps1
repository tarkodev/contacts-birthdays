[CmdletBinding()]
param()

$ErrorActionPreference = "Stop"
$accountName = "AnnivAppTest"
$accountType = "io.github.tarkodev.annivapp.test"
$contacts = @(
    @{ Name = "Aline Test"; Birthday = "1990-08-27" },
    @{ Name = "Benoit Exemple"; Birthday = "1985-08-28" },
    @{ Name = "Chloe Demo"; Birthday = "2000-09-03" },
    @{ Name = "Dorian Essai"; Birthday = "1979-09-10" },
    @{ Name = "Eva Sansannee"; Birthday = "--10-15" },
    @{ Name = "Felix Bissextile"; Birthday = "2000-02-29" },
    @{ Name = "Gabriel Homonyme"; Birthday = "1992-03-14" },
    @{ Name = "Gabriel Homonyme"; Birthday = "1998-11-05" },
    @{ Name = "Ines Unicode"; Birthday = "1995-12-31" },
    @{ Name = "Jean-Baptiste De La Demonstration Au Nom Volontairement Tres Long"; Birthday = "1988-01-01" },
    @{ Name = "Lea Test"; Birthday = "2004-08-27" },
    @{ Name = "Malo Test"; Birthday = "2002-08-27" }
)

if (-not (Get-Command adb -ErrorAction SilentlyContinue)) {
    throw "adb is not available on PATH."
}

if (-not (adb devices | Select-String -Pattern "\sdevice$")) {
    throw "No booted Android device or emulator was found."
}

# Remove active fixtures created by a previous run. Deleting a raw contact marks
# it as deleted, so the query explicitly ignores already-deleted rows.
$rawContacts = adb shell content query `
    --uri content://com.android.contacts/raw_contacts `
    --projection _id:account_name:deleted

$fixtureIds = $rawContacts |
    Select-String -Pattern "_id=(\d+), account_name=$accountName, deleted=0" |
    ForEach-Object { [int]$_.Matches[0].Groups[1].Value }

foreach ($fixtureId in $fixtureIds) {
    adb shell content delete --uri "content://com.android.contacts/raw_contacts/$fixtureId" | Out-Null
}

foreach ($contact in $contacts) {
    adb shell content insert `
        --uri content://com.android.contacts/raw_contacts `
        --bind "account_name:s:$accountName" `
        --bind "account_type:s:$accountType" | Out-Null

    $rawContacts = adb shell content query `
        --uri content://com.android.contacts/raw_contacts `
        --projection _id:account_name:deleted

    $rawContactId = $rawContacts |
        Select-String -Pattern "_id=(\d+), account_name=$accountName, deleted=0" |
        ForEach-Object { [int]$_.Matches[0].Groups[1].Value } |
        Sort-Object -Descending |
        Select-Object -First 1

    if (-not $rawContactId) {
        throw "Failed to create a raw contact for $($contact.Name)."
    }

    $nameCommand = "content insert --uri content://com.android.contacts/data " +
        "--bind raw_contact_id:i:$rawContactId " +
        "--bind mimetype:s:vnd.android.cursor.item/name " +
        "--bind data1:s:'$($contact.Name)'"
    adb shell $nameCommand | Out-Null

    adb shell content insert `
        --uri content://com.android.contacts/data `
        --bind "raw_contact_id:i:$rawContactId" `
        --bind mimetype:s:vnd.android.cursor.item/contact_event `
        --bind "data1:s:$($contact.Birthday)" `
        --bind data2:i:3 | Out-Null
}

$activeFixtures = adb shell content query `
    --uri content://com.android.contacts/raw_contacts `
    --projection _id:account_name:deleted |
    Select-String -Pattern "account_name=$accountName, deleted=0"

if ($activeFixtures.Count -ne $contacts.Count) {
    throw "Expected $($contacts.Count) active fixtures, found $($activeFixtures.Count)."
}

Write-Output "Seeded $($contacts.Count) fictional contacts into the emulator."
