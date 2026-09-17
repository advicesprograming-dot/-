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

data class CityPreset(
    val nameAr: String,
    val nameEn: String,
    val lat: Double,
    val lng: Double,
    val tz: String,
    val method: String
)

object PrayerTimesCalculator {

    val PRESET_CITIES = listOf(
        CityPreset("مكة المكرمة", "Makkah", 21.4225, 39.8262, "Asia/Riyadh", "UMM_AL_QURA"),
        CityPreset("المدينة المنورة", "Madinah", 24.4672, 39.6111, "Asia/Riyadh", "UMM_AL_QURA"),
        CityPreset("الرياض", "Riyadh", 24.7136, 46.6753, "Asia/Riyadh", "UMM_AL_QURA"),
        CityPreset("جدة", "Jeddah", 21.5433, 39.1728, "Asia/Riyadh", "UMM_AL_QURA"),
        CityPreset("القاهرة", "Cairo", 30.0444, 31.2357, "Africa/Cairo", "EGYPT"),
        CityPreset("الإسكندرية", "Alexandria", 31.2001, 29.9187, "Africa/Cairo", "EGYPT"),
        CityPreset("الجيزة", "Giza", 30.0131, 31.2089, "Africa/Cairo", "EGYPT"),
        CityPreset("المنصورة", "Mansoura", 31.0409, 31.3785, "Africa/Cairo", "EGYPT"),
        CityPreset("طنطا", "Tanta", 30.7865, 31.0004, "Africa/Cairo", "EGYPT"),
        CityPreset("أسيوط", "Asyut", 27.1801, 31.1837, "Africa/Cairo", "EGYPT"),
        CityPreset("أسوان", "Aswan", 24.0889, 32.8998, "Africa/Cairo", "EGYPT"),
        CityPreset("دبي", "Dubai", 25.2048, 55.2708, "Asia/Dubai", "DUBAI"),
        CityPreset("أبوظبي", "Abu Dhabi", 24.4539, 54.3773, "Asia/Dubai", "DUBAI"),
        CityPreset("الدوحة", "Doha", 25.2854, 51.5310, "Asia/Qatar", "QATAR"),
        CityPreset("الكويت", "Kuwait City", 29.3759, 47.9774, "Asia/Kuwait", "KUWAIT"),
        CityPreset("المنامة", "Manama", 26.2285, 50.5860, "Asia/Bahrain", "UMM_AL_QURA"),
        CityPreset("مسقط", "Muscat", 23.5880, 58.3829, "Asia/Muscat", "MWL"),
        CityPreset("القدس الشريف", "Jerusalem", 31.7683, 35.2137, "Asia/Jerusalem", "MWL"),
        CityPreset("عمان", "Amman", 31.9454, 35.9284, "Asia/Amman", "MWL"),
        CityPreset("بغداد", "Baghdad", 33.3152, 44.3661, "Asia/Baghdad", "MWL"),
        CityPreset("دمشق", "Damascus", 33.5138, 36.2765, "Asia/Damascus", "MWL"),
        CityPreset("بيروت", "Beirut", 33.8938, 35.5018, "Asia/Beirut", "MWL"),
        CityPreset("صنعاء", "Sanaa", 15.3694, 44.1910, "Asia/Aden", "UMM_AL_QURA"),
        CityPreset("طرابلس", "Tripoli", 32.8872, 13.1913, "Africa/Tripoli", "MWL"),
        CityPreset("تونس", "Tunis", 36.8065, 10.1815, "Africa/Tunis", "MWL"),
        CityPreset("الجزائر", "Algiers", 36.7538, 3.0588, "Africa/Algiers", "MWL"),
        CityPreset("الرباط", "Rabat", 34.0209, -6.8416, "Africa/Casablanca", "MWL"),
        CityPreset("الدار البيضاء", "Casablanca", 33.5731, -7.5898, "Africa/Casablanca", "MWL"),
        CityPreset("الخرطوم", "Khartoum", 15.5007, 32.5599, "Africa/Khartoum", "EGYPT"),
        CityPreset("إسطنبول", "Istanbul", 41.0082, 28.9784, "Europe/Istanbul", "DIYANET"),
        CityPreset("أنقرة", "Ankara", 39.9334, 32.8597, "Europe/Istanbul", "DIYANET"),
        CityPreset("كراتشي", "Karachi", 24.8607, 67.0011, "Asia/Karachi", "KARACHI"),
        CityPreset("لاهور", "Lahore", 31.5204, 74.3587, "Asia/Karachi", "KARACHI"),
        CityPreset("إسلام آباد", "Islamabad", 33.6844, 73.0479, "Asia/Karachi", "KARACHI"),
        CityPreset("جاكرتا", "Jakarta", -6.2088, 106.8456, "Asia/Jakarta", "MWL"),
        CityPreset("كوالالمبور", "Kuala Lumpur", 3.1390, 101.6869, "Asia/Kuala_Lumpur", "MWL"),
        CityPreset("لندن", "London", 51.5074, -0.1278, "Europe/London", "MWL"),
        CityPreset("باريس", "Paris", 48.8566, 2.3522, "Europe/Paris", "FRANCE"),
        CityPreset("برلين", "Berlin", 52.5200, 13.4050, "Europe/Berlin", "MWL"),
        CityPreset("موسكو", "Moscow", 55.7558, 37.6173, "Europe/Moscow", "RUSSIA"),
        CityPreset("نيويورك", "New York", 40.7128, -74.0060, "America/New_York", "ISNA"),
        CityPreset("تورونتو", "Toronto", 43.6532, -79.3832, "America/Toronto", "ISNA"),
        CityPreset("سيدني", "Sydney", -33.8688, 151.2093, "Australia/Sydney", "MWL")
    )

    fun findClosestPreset(lat: Double, lng: Double): CityPreset? {
        return PRESET_CITIES.minByOrNull { preset ->
            val dLat = preset.lat - lat
            val dLng = preset.lng - lng
            dLat * dLat + dLng * dLng
        }
    }

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
        val tzId = if (settings.timezoneId.isNotBlank()) settings.timezoneId else TimeZone.getDefault().id
        val cal = Calendar.getInstance(TimeZone.getTimeZone(tzId))
        cal.time = date

        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val isFriday = cal.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY

        val lat = settings.latitude
        val lng = settings.longitude
        val tz = getTimeZoneOffsetHours(tzId, date, settings.dstMode)

        val method = CalculationMethod.fromCode(settings.calcMethod)
        val madhab = AsrMadhab.fromCode(settings.asrMadhab)

        // Astronomical High-Precision Ephemeris (Jean Meeus algorithm)
        // 1. Julian Date
        val jd = julianDate(year, month, day)
        val d = jd - 2451545.0

        // 2. Solar Position
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * dsin(g) + 0.020 * dsin(2.0 * g))

        val e = 23.439 - 0.00000036 * d
        val declDeg = darcsin(dsin(e) * dsin(l))
        val raHours = fixAngle(darctan2(dcos(e) * dsin(l), dcos(l))) / 15.0

        // 3. Equation of Time in hours
        var eqt = (q / 15.0) - raHours
        if (eqt > 12.0) eqt -= 24.0
        if (eqt < -12.0) eqt += 24.0

        // 4. Solar Noon / Zawal
        val noon = 12.0 + tz - (lng / 15.0) - eqt
        val dhuhrBase = noon + (1.0 / 60.0) // +1 minute precaution after solar noon for sun to pass meridian

        fun sunAngleTime(angleDeg: Double): Double {
            val cosH = (-dsin(angleDeg) - dsin(lat) * dsin(declDeg)) / (dcos(lat) * dcos(declDeg))
            if (cosH > 1.0) return 0.0
            if (cosH < -1.0) return 12.0
            return darccos(cosH) / 15.0
        }

        // Sunrise & Sunset (atmospheric refraction 34' + sun semi-diameter 16' = 50' = 0.8333°)
        val sunriseAngle = 0.833333
        val semiArc = sunAngleTime(sunriseAngle)
        val sunriseTime = noon - semiArc
        val sunsetTime = noon + semiArc

        // Fajr
        val fajrTime = noon - sunAngleTime(method.fajrAngle)

        // Asr (Shadow factor 1 for Shafi/Maliki/Hanbali, 2 for Hanafi)
        val altAsr = r2d(atan(1.0 / (madhab.shadowFactor + tan(d2r(abs(lat - declDeg))))))
        val cosHAsr = (dsin(altAsr) - dsin(lat) * dsin(declDeg)) / (dcos(lat) * dcos(declDeg))
        val asrTime = noon + (darccos(cosHAsr) / 15.0)

        // Maghrib
        val maghribTime = if (method.maghribAngle != null) {
            noon + sunAngleTime(method.maghribAngle)
        } else {
            sunsetTime + (1.0 / 60.0) // +1 minute precaution for sunset
        }

        // Isha
        val ishaTime = if (method.ishaMinutesAfterMaghrib != null) {
            maghribTime + (method.ishaMinutesAfterMaghrib / 60.0)
        } else {
            noon + sunAngleTime(method.ishaAngle)
        }

        // Apply Manual Adjustments
        val fajrFinal = adjustTime(fajrTime, settings.adjFajr)
        val sunriseFinal = adjustTime(sunriseTime, settings.adjSunrise)
        val dhuhrFinal = adjustTime(dhuhrBase, settings.adjDhuhr)
        val asrFinal = adjustTime(asrTime, settings.adjAsr)
        val maghribFinal = adjustTime(maghribTime, settings.adjMaghrib)
        val ishaFinal = adjustTime(ishaTime, settings.adjIsha)

        // Build PrayerTime objects with minute-accurate synchronization for mosque clocks
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

    private fun julianDate(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2.0 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    private fun fixAngle(a: Double): Double {
        var res = a - 360.0 * floor(a / 360.0)
        if (res < 0.0) res += 360.0
        return res
    }

    private fun d2r(d: Double): Double = d * Math.PI / 180.0
    private fun r2d(r: Double): Double = r * 180.0 / Math.PI
    private fun dsin(d: Double): Double = sin(d2r(d))
    private fun dcos(d: Double): Double = cos(d2r(d))
    private fun darcsin(x: Double): Double = r2d(asin(x.coerceIn(-1.0, 1.0)))
    private fun darccos(x: Double): Double = r2d(acos(x.coerceIn(-1.0, 1.0)))
    private fun darctan2(y: Double, x: Double): Double = r2d(atan2(y, x))

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
        var fixedHour = hourDecimal
        while (fixedHour < 0.0) fixedHour += 24.0
        while (fixedHour >= 24.0) fixedHour -= 24.0

        val totalMinutes = floor(fixedHour * 60.0 + 0.5).toInt()
        val h = (totalMinutes / 60) % 24
        val m = totalMinutes % 60

        val cal = baseCal.clone() as Calendar
        cal.set(Calendar.HOUR_OF_DAY, h)
        cal.set(Calendar.MINUTE, m)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        return PrayerTime(
            id = id,
            arabicName = ar,
            englishName = en,
            frenchName = fr,
            timestamp = cal.timeInMillis,
            hour24 = h,
            minute = m
        )
    }

    private fun getTimeZoneOffsetHours(tzId: String, date: Date, dstMode: Int): Double {
        val tz = if (tzId.isNotBlank()) TimeZone.getTimeZone(tzId) else TimeZone.getDefault()
        val offsetMillis = tz.getOffset(date.time)
        var offsetHours = offsetMillis / (1000.0 * 3600.0)

        // dstMode: -1=Auto (device/system default), 1=Force ON, 0=Force OFF
        if (dstMode == 1 && !tz.inDaylightTime(date)) {
            offsetHours += 1.0
        } else if (dstMode == 0 && tz.inDaylightTime(date)) {
            val savings = if (tz.useDaylightTime()) tz.dstSavings else 3600000
            offsetHours -= (savings / (1000.0 * 3600.0))
        }
        return offsetHours
    }

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
