package com.minar.birday.utilities

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.minar.birday.R
import java.time.LocalDate

enum class ZodiacSign(
    @param:StringRes val labelRes: Int,
    @param:DrawableRes val iconRes: Int,
    val legacyIndex: Int
) {
    ARIES(R.string.zodiac_aries, R.drawable.ic_zodiac_aries, 4),
    TAURUS(R.string.zodiac_taurus, R.drawable.ic_zodiac_taurus, 5),
    GEMINI(R.string.zodiac_gemini, R.drawable.ic_zodiac_gemini, 6),
    CANCER(R.string.zodiac_cancer, R.drawable.ic_zodiac_cancer, 7),
    LEO(R.string.zodiac_leo, R.drawable.ic_zodiac_leo, 8),
    VIRGO(R.string.zodiac_virgo, R.drawable.ic_zodiac_virgo, 9),
    LIBRA(R.string.zodiac_libra, R.drawable.ic_zodiac_libra, 10),
    SCORPIO(R.string.zodiac_scorpio, R.drawable.ic_zodiac_scorpio, 11),
    SAGITTARIUS(R.string.zodiac_sagittarius, R.drawable.ic_zodiac_sagittarius, 0),
    CAPRICORN(R.string.zodiac_capricorn, R.drawable.ic_zodiac_capricorn, 1),
    AQUARIUS(R.string.zodiac_aquarius, R.drawable.ic_zodiac_aquarius, 2),
    PISCES(R.string.zodiac_pisces, R.drawable.ic_zodiac_pisces, 3)
}

fun zodiacSignFor(date: LocalDate): ZodiacSign = when (date.monthValue) {
    1 -> if (date.dayOfMonth <= 20) ZodiacSign.CAPRICORN else ZodiacSign.AQUARIUS
    2 -> if (date.dayOfMonth <= 18) ZodiacSign.AQUARIUS else ZodiacSign.PISCES
    3 -> if (date.dayOfMonth <= 20) ZodiacSign.PISCES else ZodiacSign.ARIES
    4 -> if (date.dayOfMonth <= 20) ZodiacSign.ARIES else ZodiacSign.TAURUS
    5 -> if (date.dayOfMonth <= 20) ZodiacSign.TAURUS else ZodiacSign.GEMINI
    6 -> if (date.dayOfMonth <= 21) ZodiacSign.GEMINI else ZodiacSign.CANCER
    7 -> if (date.dayOfMonth <= 22) ZodiacSign.CANCER else ZodiacSign.LEO
    8 -> if (date.dayOfMonth <= 23) ZodiacSign.LEO else ZodiacSign.VIRGO
    9 -> if (date.dayOfMonth <= 22) ZodiacSign.VIRGO else ZodiacSign.LIBRA
    10 -> if (date.dayOfMonth <= 22) ZodiacSign.LIBRA else ZodiacSign.SCORPIO
    11 -> if (date.dayOfMonth <= 22) ZodiacSign.SCORPIO else ZodiacSign.SAGITTARIUS
    12 -> if (date.dayOfMonth <= 21) ZodiacSign.SAGITTARIUS else ZodiacSign.CAPRICORN
    else -> error("Unexpected month: ${date.monthValue}")
}
