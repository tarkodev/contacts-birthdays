package com.minar.birday.utils

import com.minar.birday.utilities.ZodiacSign
import com.minar.birday.utilities.zodiacSignFor
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ZodiacSignUnitTest {
    @Test
    fun zodiacSign_respects_every_boundary() {
        val expectedSigns = listOf(
            LocalDate.of(2000, 3, 21) to ZodiacSign.ARIES,
            LocalDate.of(2000, 4, 21) to ZodiacSign.TAURUS,
            LocalDate.of(2000, 5, 21) to ZodiacSign.GEMINI,
            LocalDate.of(2000, 6, 22) to ZodiacSign.CANCER,
            LocalDate.of(2000, 7, 23) to ZodiacSign.LEO,
            LocalDate.of(2000, 8, 24) to ZodiacSign.VIRGO,
            LocalDate.of(2000, 9, 23) to ZodiacSign.LIBRA,
            LocalDate.of(2000, 10, 23) to ZodiacSign.SCORPIO,
            LocalDate.of(2000, 11, 23) to ZodiacSign.SAGITTARIUS,
            LocalDate.of(2000, 12, 22) to ZodiacSign.CAPRICORN,
            LocalDate.of(2000, 1, 21) to ZodiacSign.AQUARIUS,
            LocalDate.of(2000, 2, 19) to ZodiacSign.PISCES
        )

        expectedSigns.forEach { (date, expectedSign) ->
            assertEquals(expectedSign, zodiacSignFor(date))
        }
    }

    @Test
    fun zodiacSign_works_without_a_meaningful_birth_year() {
        assertEquals(ZodiacSign.VIRGO, zodiacSignFor(LocalDate.of(1900, 8, 27)))
    }
}
