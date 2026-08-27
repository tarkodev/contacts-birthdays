package com.minar.birday.testing

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.minar.birday.model.Event
import com.minar.birday.model.EventCode
import com.minar.birday.persistence.EventDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class TestContactSeeder {

    private data class Fixture(
        val givenName: String,
        val familyName: String,
        val birthday: String,
        val photo: String,
    )

    private val fixtures = listOf(
        Fixture("Aline", "Test", "1990-08-27", "01-aline-test.jpg"),
        Fixture("Benoit", "Exemple", "1985-08-28", "02-benoit-exemple.jpg"),
        Fixture("Chloe", "Demo", "2000-09-03", "03-chloe-demo.jpg"),
        Fixture("Dorian", "Essai", "1979-09-10", "04-dorian-essai.jpg"),
        Fixture("Eva", "Sansannee", "--10-15", "05-eva-sansannee.jpg"),
        Fixture("Felix", "Bissextile", "2000-02-29", "06-felix-bissextile.jpg"),
        Fixture("Gabriel", "Homonyme", "1992-03-14", "07-gabriel-homonyme-a.jpg"),
        Fixture("Gabriel", "Homonyme", "1998-11-05", "08-gabriel-homonyme-b.jpg"),
        Fixture("Ines", "Unicode", "1995-12-31", "09-ines-unicode.jpg"),
        Fixture(
            "Jean-Baptiste",
            "De La Demonstration Au Nom Volontairement Tres Long",
            "1988-01-01",
            "10-jean-baptiste-long.jpg",
        ),
        Fixture("Lea", "Test", "2004-08-27", "11-lea-test.jpg"),
        Fixture("Malo", "Test", "2002-08-27", "12-malo-test.jpg"),
        Fixture("Nina", "Sansannee", "--08-27", "13-nina-sansannee.jpg"),
        Fixture("Omar", "Sansannee", "--08-28", "14-omar-sansannee.jpg"),
        Fixture("Salome", "Sansannee", "--09-01", "15-salome-sansannee.jpg"),
    )

    @Test
    fun seedFictionalContactsWithPhotos() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val testAssets = instrumentation.context.assets
        assertEquals(
            PackageManager.PERMISSION_GRANTED,
            context.checkSelfPermission(Manifest.permission.READ_CONTACTS),
        )
        assertEquals(
            PackageManager.PERMISSION_GRANTED,
            context.checkSelfPermission(Manifest.permission.WRITE_CONTACTS),
        )

        val resolver = context.contentResolver
        resolver.delete(
            ContactsContract.RawContacts.CONTENT_URI,
            "${ContactsContract.RawContacts.ACCOUNT_NAME} = ? AND " +
                "${ContactsContract.RawContacts.ACCOUNT_TYPE} = ? AND " +
                "${ContactsContract.RawContacts.DELETED} = 0",
            arrayOf(ACCOUNT_NAME, ACCOUNT_TYPE),
        )

        val rawContactIds = mutableListOf<Long>()
        val fixtureImages = mutableMapOf<Fixture, ByteArray>()

        fixtures.forEach { fixture ->
            val image = testAssets.open(fixture.photo).use { it.readBytes() }
            fixtureImages[fixture] = image

            val rawContactUri = resolver.insert(
                ContactsContract.RawContacts.CONTENT_URI,
                ContentValues().apply {
                    put(ContactsContract.RawContacts.ACCOUNT_NAME, ACCOUNT_NAME)
                    put(ContactsContract.RawContacts.ACCOUNT_TYPE, ACCOUNT_TYPE)
                },
            ) ?: error("Failed to create ${fixture.givenName} ${fixture.familyName}")

            val rawContactId = ContentUris.parseId(rawContactUri)
            rawContactIds += rawContactId

            resolver.insert(
                ContactsContract.Data.CONTENT_URI,
                ContentValues().apply {
                    put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                    put(
                        ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE,
                    )
                    put(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, fixture.givenName)
                    put(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, fixture.familyName)
                    put(
                        ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME,
                        "${fixture.givenName} ${fixture.familyName}",
                    )
                },
            )

            resolver.insert(
                ContactsContract.Data.CONTENT_URI,
                ContentValues().apply {
                    put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                    put(
                        ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE,
                    )
                    put(ContactsContract.CommonDataKinds.Event.START_DATE, fixture.birthday)
                    put(
                        ContactsContract.CommonDataKinds.Event.TYPE,
                        ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY,
                    )
                },
            )

            resolver.insert(
                ContactsContract.Data.CONTENT_URI,
                ContentValues().apply {
                    put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                    put(
                        ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE,
                    )
                    put(ContactsContract.CommonDataKinds.Photo.PHOTO, image)
                    put(ContactsContract.Data.IS_PRIMARY, 1)
                    put(ContactsContract.Data.IS_SUPER_PRIMARY, 1)
                },
            )
        }

        val activeFixtureCount = resolver.query(
            ContactsContract.RawContacts.CONTENT_URI,
            arrayOf(ContactsContract.RawContacts._ID),
            "${ContactsContract.RawContacts.ACCOUNT_NAME} = ? AND " +
                "${ContactsContract.RawContacts.ACCOUNT_TYPE} = ? AND " +
                "${ContactsContract.RawContacts.DELETED} = 0",
            arrayOf(ACCOUNT_NAME, ACCOUNT_TYPE),
            null,
        )?.use { it.count } ?: 0
        assertEquals(fixtures.size, activeFixtureCount)

        val photoCount = resolver.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(ContactsContract.Data._ID),
            "${ContactsContract.Data.MIMETYPE} = ? AND " +
                "${ContactsContract.Data.RAW_CONTACT_ID} IN (${rawContactIds.joinToString()})",
            arrayOf(ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE),
            null,
        )?.use { it.count } ?: 0
        assertEquals(fixtures.size, photoCount)

        val events = fixtures.map { fixture ->
            val yearMatters = !fixture.birthday.startsWith("--")
            val date = LocalDate.parse(
                if (yearMatters) fixture.birthday else fixture.birthday.replaceFirst("-", "1970"),
            )
            Event(
                id = 0,
                type = EventCode.BIRTHDAY.name,
                name = fixture.givenName,
                surname = fixture.familyName,
                yearMatter = yearMatters,
                originalDate = date,
                image = fixtureImages.getValue(fixture),
            )
        }

        val dao = EventDatabase.getBirdayDatabase(context).eventDao()
        dao.insertAllEventReplace(events)
        val storedEvents = dao.getOrderedEventsStatic()
        events.forEach { expected ->
            assertTrue(
                storedEvents.any { actual ->
                    actual.name == expected.name &&
                        actual.surname == expected.surname &&
                        actual.originalDate == expected.originalDate &&
                        actual.yearMatter == expected.yearMatter &&
                        actual.image != null && actual.image.isNotEmpty()
                },
            )
        }
    }

    private companion object {
        const val ACCOUNT_NAME = "AnnivAppTest"
        const val ACCOUNT_TYPE = "io.github.tarkodev.annivapp.test"
    }
}
