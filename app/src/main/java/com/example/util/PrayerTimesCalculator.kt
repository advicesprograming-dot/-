package com.example.util

import com.example.data.local.AppSettingsEntity
import java.util.Calendar
import java.util.Date
import java.util.TimeZone
import kotlin.math.*

data class PrayerTime(
    val id: String,
    val arabicName: String,
    val englishName: String,
    val frenchName: String,
    val timestamp: Long,
    val hour24: Int,
    val minute: Int
)

data class PrayerSchedule(
    val date: Date,
    val fajr: PrayerTime,
    val sunrise: PrayerTime,
    val dhuhr: PrayerTime,
    val asr: PrayerTime,
    val maghrib: PrayerTime,
    val isha: PrayerTime,
    val nextPrayer: PrayerTime,
    val timeToNextMillis: Long,
    val isFriday: Boolean
)

object PrayerTimesCalculator {

    enum class CalculationMethod(
        val code: String,
        val arabicName: String,
        val englishName: String,
        val fajrAngle: Double,
        val ishaAngle: Double,
        val ishaMinutesAfterMaghrib: Double? = null,
        val maghribAngle: Double? = null
    ) {
        UMM_AL_QURA("UMM_AL_QURA", "جامعة أم القرى (مكة المكرمة)", "Umm al-Qura University (Makkah)", 18.5, 0.0, ishaMinutesAfterMaghrib = 90.0),
        EGYPT("EGYPT", "الهيئة المصرية العامة للمساحة", "Egyptian General Authority of Survey", 19.5, 17.5),
        MWL("MWL", "رابطة العالم الإسلامي", "Muslim World League (MWL)", 18.0, 17.0),
        KARACHI("KARACHI", "جامعة العلوم الإسلامية بكراتشي", "University of Islamic Sciences (Karachi)", 18.0, 18.0),
        ISNA("ISNA", "الجمعية الإسلامية لأمريكا الشمالية (ISNA)", "Islamic Society of North America (ISNA)", 15.0, 15.0),
        DUBAI("DUBAI", "دبي / الشؤون الإسلامية", "Dubai (Awqaf)", 18.2, 18.2),
        QATAR("QATAR", "وزارة الأوقاف القطرية", "Qatar Awqaf", 18.0, 0.0, ishaMinutesAfterMaghrib = 90.0),
        KUWAIT("KUWAIT", "وزارة الأوقاف الكويتية", "Kuwait Awqaf", 18.0, 17.5),
        DIYANET("DIYANET", "رئاسة الشؤون الدينية التركية (Diyanet)", "Presidency of Religious Affairs (Turkey)", 18.0, 17.0),
        TEHRAN("TEHRAN", "معهد الجيوفيزياء بجامعة طهران", "Institute of Geophysics (Tehran)", 17.7, 14.0, maghribAngle = 4.5),
        FRANCE("FRANCE", "اتحاد المنظمات الإسلامية بفرنسا (UOIF)", "Union of Islamic Organisations of France", 12.0, 12.0),
        RUSSIA("RUSSIA", "مجلس شورى المفتين لروسيا", "Spiritual Board of Muslims of Russia", 16.0, 15.0),
        MUIS("MUIS", "المجلس الإسلامي السنغافوري (MUIS)", "Majlis Ugama Islam Singapura", 20.0, 18.0);

        companion object {
            fun fromCode(code: String): CalculationMethod {
                return entries.firstOrNull { it.code == code } ?: UMM_AL_QURA
            }
        }
    }

    enum class AsrMadhab(val code: String, val arabicName: String, val englishName: String, val shadowFactor: Double) {
        SHAFI("SHAFI", "الشافعي", "Shafi'i", 1.0),
        MALIKI("MALIKI", "المالكي", "Maliki", 1.0),
        HANBALI("HANBALI", "الحنبلي", "Hanbali", 1.0),
        HANAFI("HANAFI", "الحنفي", "Hanafi", 2.0);

        companion object {
            fun fromCode(code: String): AsrMadhab {
                return entries.firstOrNull { it.code == code } ?: SHAFI
            }
        }
    }

    fun calculateTimes(date: Date, settings: AppSettingsEntity): PrayerSchedule {
        val cal = Calendar.getInstance(TimeZone.getTimeZone(settings.timezoneId))
        cal.time = date

        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val dayOfYear = cal.get(Calendar.DAY_OF_YEAR)
        val isFriday = cal.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY

        val lat = settings.latitude
        val lng = settings.longitude
        val tz = getTimeZoneOffsetHours(settings.timezoneId, date, settings.dstMode)

        val method = CalculationMethod.fromCode(settings.calcMethod)
        val madhab = AsrMadhab.fromCode(settings.asrMadhab)

        // NOAA Solar Calculation Algorithm (Extremely High Precision)
        // Fractional year gamma in radians
        val gamma = 2.0 * Math.PI / 365.0 * (dayOfYear - 1)

        // Equation of Time in minutes
        val eqtMinutes = 229.18 * (0.000075 +
                0.001868 * cos(gamma) - 0.032077 * sin(gamma) -
                0.014615 * cos(2.0 * gamma) - 0.040849 * sin(2.0 * gamma))
        val eqtHours = eqtMinutes / 60.0

        // Sun Declination in radians
        val declRad = 0.006918 -
                0.399912 * cos(gamma) + 0.070257 * sin(gamma) -
                0.006758 * cos(2.0 * gamma) + 0.000907 * sin(2.0 * gamma) -
                0.002697 * cos(3.0 * gamma) + 0.000148 * sin(3.0 * gamma)
        val declDeg = r2d(declRad)

        // Dhuhr (Solar Noon) time in hours (local standard time)
        val dhuhrBase = 12.0 + tz - (lng / 15.0) - eqtHours

        // Helper function for sun angle time
        fun sunAngleTime(angleDeg: Double): Double {
            val cosH = (cos(d2r(90.0 + angleDeg)) - sin(d2r(lat)) * sin(d2r(declDeg))) /
                    (cos(d2r(lat)) * cos(d2r(declDeg)))
            if (cosH > 1.0) return 0.0
            if (cosH < -1.0) return 12.0
            return r2d(acos(cosH)) / 15.0
        }

        // Sunrise & Sunset (Standard atmospheric refraction 34' + semi-diameter 16' = 50' = 0.833333°)
        val sunriseAngle = 0.833333
        val semiArc = sunAngleTime(sunriseAngle)
        val sunriseTime = dhuhrBase - semiArc
        val sunsetTime = dhuhrBase + semiArc

        // Fajr
        val fajrTime = dhuhrBase - sunAngleTime(method.fajrAngle)

        // Asr (Shadow factor 1 for Shafi/Maliki/Hanbali, 2 for Hanafi)
        val altitudeAtAsr = r2d(atan(1.0 / (madhab.shadowFactor + tan(d2r(abs(lat - declDeg))))))
        val asrTime = dhuhrBase + sunAngleTime(-altitudeAtAsr)

        // Maghrib
        val maghribTime = if (method.maghribAngle != null) {
            dhuhrBase + sunAngleTime(method.maghribAngle)
        } else {
            sunsetTime
        }

        // Isha
        val ishaTime = if (method.ishaMinutesAfterMaghrib != null) {
            maghribTime + (method.ishaMinutesAfterMaghrib / 60.0)
        } else {
            dhuhrBase + sunAngleTime(method.ishaAngle)
        }

        // Apply Manual Adjustments
        val fajrFinal = adjustTime(fajrTime, settings.adjFajr)
        val sunriseFinal = adjustTime(sunriseTime, settings.adjSunrise)
        val dhuhrFinal = adjustTime(dhuhrBase, settings.adjDhuhr)
        val asrFinal = adjustTime(asrTime, settings.adjAsr)
        val maghribFinal = adjustTime(maghribTime, settings.adjMaghrib)
        val ishaFinal = adjustTime(ishaTime, settings.adjIsha)

        // Build PrayerTime objects with second-level high precision
        val fajrPT = toPrayerTime("FAJR", "الفجر", "Fajr", "Fadjr", cal, fajrFinal)
        val sunrisePT = toPrayerTime("SUNRISE", "الشروق", "Sunrise", "Lever", cal, sunriseFinal)
        val dhuhrPT = if (isFriday) {
            toPrayerTime("JUMUAH", "الجمعة", "Jumu'ah", "Djoumou'a", cal, dhuhrFinal)
        } else {
            toPrayerTime("DHUHR", "الظهر", "Dhuhr", "Dhohr", cal, dhuhrFinal)
        }
        val asrPT = toPrayerTime("ASR", "العصر", "Asr", "Asr", cal, asrFinal)
        val maghribPT = toPrayerTime("MAGHRIB", "المغرب", "Maghrib", "Maghrib", cal, maghribFinal)
        val ishaPT = toPrayerTime("ISHA", "العشاء", "Isha", "Icha", cal, ishaFinal)

        // Determine Next Prayer
        val now = System.currentTimeMillis()
        val allTimes = listOf(fajrPT, dhuhrPT, asrPT, maghribPT, ishaPT)
        var next = allTimes.firstOrNull { it.timestamp > now }
        var diff = 0L

        if (next == null) {
            val tomorrowFajr = fajrPT.timestamp + 24 * 3600 * 1000L
            next = fajrPT.copy(timestamp = tomorrowFajr)
            diff = max(0L, tomorrowFajr - now)
        } else {
            diff = max(0L, next.timestamp - now)
        }

        return PrayerSchedule(
            date = date,
            fajr = fajrPT,
            sunrise = sunrisePT,
            dhuhr = dhuhrPT,
            asr = asrPT,
            maghrib = maghribPT,
            isha = ishaPT,
            nextPrayer = next,
            timeToNextMillis = diff,
            isFriday = isFriday
        )
    }

    private fun adjustTime(baseHour: Double, minutesOffset: Int): Double {
        return baseHour + (minutesOffset / 60.0)
    }

    private fun toPrayerTime(
        id: String,
        ar: String,
        en: String,
        fr: String,
        baseCal: Calendar,
        hourDecimal: Double
    ): PrayerTime {
        val totalSeconds = (hourDecimal * 3600.0).roundToLong()
        val h = (((totalSeconds / 3600) % 24) + 24) % 24
        val m = (((totalSeconds % 3600) / 60) + 60) % 60
        val s = (((totalSeconds % 60)) + 60) % 60

        val cal = baseCal.clone() as Calendar
        cal.set(Calendar.HOUR_OF_DAY, h.toInt())
        cal.set(Calendar.MINUTE, m.toInt())
        cal.set(Calendar.SECOND, s.toInt())
        cal.set(Calendar.MILLISECOND, 0)

        return PrayerTime(
            id = id,
            arabicName = ar,
            englishName = en,
            frenchName = fr,
            timestamp = cal.timeInMillis,
            hour24 = h.toInt(),
            minute = m.toInt()
        )
    }

    private fun getTimeZoneOffsetHours(tzId: String, date: Date, dstMode: Int): Double {
        val tz = TimeZone.getTimeZone(tzId)
        val offsetMillis = tz.getOffset(date.time)
        var offsetHours = offsetMillis / (1000.0 * 3600.0)
        if (dstMode == 1 && !tz.inDaylightTime(date)) {
            offsetHours += 1.0
        } else if (dstMode == 0 && tz.inDaylightTime(date)) {
            offsetHours -= 1.0
        }
        return offsetHours
    }

    private fun d2r(d: Double): Double = d * Math.PI / 180.0
    private fun r2d(r: Double): Double = r * 180.0 / Math.PI

    fun formatTime(hour: Int, minute: Int, is24Hour: Boolean, lang: String): String {
        return if (is24Hour) {
            String.format("%02d:%02d", hour, minute)
        } else {
            val h12 = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
            val suffix = when (lang) {
                "ar" -> if (hour >= 12) "م" else "ص"
                "fr" -> if (hour >= 12) "PM" else "AM"
                else -> if (hour >= 12) "PM" else "AM"
            }
            String.format("%02d:%02d %s", h12, minute, suffix)
        }
    }
}
