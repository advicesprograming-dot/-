package com.example.util

import java.util.Calendar
import java.util.Date
import kotlin.math.floor

data class HijriDate(
    val day: Int,
    val month: Int,
    val monthNameAr: String,
    val monthNameEn: String,
    val monthNameFr: String,
    val year: Int,
    val formattedAr: String,
    val formattedEn: String,
    val formattedFr: String
)

object HijriCalendarHelper {

    private val HIJRI_MONTHS_AR = listOf(
        "محرم", "صفر", "ربيع الأول", "ربيع الآخر",
        "جمادى الأولى", "جمادى الآخرة", "رجب", "شعبان",
        "رمضان", "شوال", "ذو القعدة", "ذو الحجة"
    )

    private val HIJRI_MONTHS_EN = listOf(
        "Muharram", "Safar", "Rabi' al-Awwal", "Rabi' al-Thani",
        "Jumada al-Ula", "Jumada al-Akhirah", "Rajab", "Sha'ban",
        "Ramadan", "Shawwal", "Dhu al-Qi'dah", "Dhu al-Hijjah"
    )

    private val HIJRI_MONTHS_FR = listOf(
        "Mouharram", "Safar", "Rabi al-Awwal", "Rabi ath-Thani",
        "Joumada al-Oula", "Joumada ath-Thaniya", "Rajab", "Cha'bane",
        "Ramadan", "Chawwal", "Dhou al-Qi'da", "Dhou al-Hijja"
    )

    fun getHijriDate(gregorianDate: Date, adjustmentDays: Int = 0): HijriDate {
        val cal = Calendar.getInstance()
        cal.time = gregorianDate
        if (adjustmentDays != 0) {
            cal.add(Calendar.DAY_OF_YEAR, adjustmentDays)
        }

        var day = cal.get(Calendar.DAY_OF_MONTH)
        var month = cal.get(Calendar.MONTH) // 0-11
        var year = cal.get(Calendar.YEAR)

        var m = month + 1
        var y = year
        if (m < 3) {
            y -= 1
            m += 12
        }

        var a = floor(y / 100.0)
        var b = 2 - a + floor(a / 4.0)
        if (y < 1583) b = 0.0
        if (y == 1582) {
            if (m > 10) b = -10.0
            if (m == 10) {
                b = 0.0
                if (day > 4) b = -10.0
            }
        }

        val jd = floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524

        b = 0.0
        if (jd > 2299160) {
            a = floor((jd - 1867216.25) / 36524.25)
            b = 1 + a - floor(a / 4.0)
        }
        val bb = jd + b + 1524
        var cc = floor((bb - 122.1) / 365.25)
        val dd = floor(365.25 * cc)
        val ee = floor((bb - dd) / 30.6001)

        val epoch = 1948439.5
        val z = jd - epoch
        val cyc = floor(z / 10631.0)
        val rem = z - 10631.0 * cyc
        val j = floor((rem - 0.1335) / 354.366)
        val hYear = (30 * cyc + j + 1).toInt()
        val remYear = rem - floor(j * 354.366)
        val hMonth = minOf(12, maxOf(1, floor((remYear + 28.5001) / 29.5).toInt()))
        val hDay = minOf(30, maxOf(1, (remYear - floor(29.5 * (hMonth - 1)) + 1).toInt()))

        val monthIdx = (hMonth - 1).coerceIn(0, 11)
        val monthAr = HIJRI_MONTHS_AR[monthIdx]
        val monthEn = HIJRI_MONTHS_EN[monthIdx]
        val monthFr = HIJRI_MONTHS_FR[monthIdx]

        val formattedAr = "$hDay $monthAr $hYear هـ"
        val formattedEn = "$hDay $monthEn $hYear AH"
        val formattedFr = "$hDay $monthFr $hYear H"

        return HijriDate(
            day = hDay,
            month = hMonth,
            monthNameAr = monthAr,
            monthNameEn = monthEn,
            monthNameFr = monthFr,
            year = hYear,
            formattedAr = formattedAr,
            formattedEn = formattedEn,
            formattedFr = formattedFr
        )
    }

    fun isRamadan(gregorianDate: Date, adjustmentDays: Int = 0): Boolean {
        val hijri = getHijriDate(gregorianDate, adjustmentDays)
        return hijri.month == 9 // Ramadan is the 9th month
    }
}
