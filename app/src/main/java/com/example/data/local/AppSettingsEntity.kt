package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.TimeZone

private fun getDefaultCityName(): String {
    val tz = TimeZone.getDefault().id
    return when {
        tz.contains("Cairo") || tz.contains("Egypt") -> "القاهرة"
        tz.contains("Dubai") || tz.contains("Abu_Dhabi") -> "دبي"
        tz.contains("Riyadh") || tz.contains("Makkah") -> "مكة المكرمة"
        tz.contains("Baghdad") -> "بغداد"
        tz.contains("Amman") -> "عمان"
        tz.contains("Casablanca") || tz.contains("Rabat") -> "الرباط"
        else -> "القاهرة"
    }
}

private fun getDefaultLatitude(): Double {
    val tz = TimeZone.getDefault().id
    return when {
        tz.contains("Cairo") || tz.contains("Egypt") -> 30.0444
        tz.contains("Dubai") || tz.contains("Abu_Dhabi") -> 25.2048
        tz.contains("Riyadh") || tz.contains("Makkah") -> 21.4225
        tz.contains("Baghdad") -> 33.3152
        tz.contains("Amman") -> 31.9454
        tz.contains("Casablanca") || tz.contains("Rabat") -> 34.0209
        else -> 30.0444
    }
}

private fun getDefaultLongitude(): Double {
    val tz = TimeZone.getDefault().id
    return when {
        tz.contains("Cairo") || tz.contains("Egypt") -> 31.2357
        tz.contains("Dubai") || tz.contains("Abu_Dhabi") -> 55.2708
        tz.contains("Riyadh") || tz.contains("Makkah") -> 39.8262
        tz.contains("Baghdad") -> 44.3661
        tz.contains("Amman") -> 35.9284
        tz.contains("Casablanca") || tz.contains("Rabat") -> -6.8416
        else -> 31.2357
    }
}

private fun getDefaultCalcMethod(): String {
    val tz = TimeZone.getDefault().id
    return when {
        tz.contains("Cairo") || tz.contains("Egypt") -> "EGYPT"
        tz.contains("Dubai") || tz.contains("Abu_Dhabi") -> "DUBAI"
        tz.contains("Riyadh") || tz.contains("Makkah") -> "UMM_AL_QURA"
        tz.contains("Baghdad") -> "MWL"
        tz.contains("Amman") -> "MWL"
        tz.contains("Casablanca") || tz.contains("Rabat") -> "FRANCE"
        else -> "EGYPT"
    }
}

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey
    val id: Int = 1,
    // Language
    val language: String = "ar", // "ar", "en", "fr"
    
    // Location & Calculation
    val locationMode: String = "MANUAL", // "MANUAL", "AUTO"
    val cityName: String = getDefaultCityName(),
    val latitude: Double = getDefaultLatitude(),
    val longitude: Double = getDefaultLongitude(),
    val timezoneId: String = TimeZone.getDefault().id,
    val dstMode: Int = 0, // 0=off, 1=on, -1=auto
    val calcMethod: String = getDefaultCalcMethod(),
    val asrMadhab: String = "SHAFI", // "SHAFI", "MALIKI", "HANBALI", "HANAFI"
    
    // Manual Prayer Time Adjustments (Minutes)
    val adjFajr: Int = 0,
    val adjSunrise: Int = 0,
    val adjDhuhr: Int = 0,
    val adjAsr: Int = 0,
    val adjMaghrib: Int = 0,
    val adjIsha: Int = 0,
    
    // Formatting & Calendar
    val timeFormat24: Boolean = false,
    val hijriAdjustmentDays: Int = 0,
    
    // Adhan Audio per prayer (URI string or null for built-in)
    val adhanAudioFajr: String? = null,
    val adhanAudioDhuhr: String? = null,
    val adhanAudioAsr: String? = null,
    val adhanAudioMaghrib: String? = null,
    val adhanAudioIsha: String? = null,
    val adhanAudioJumuah: String? = null,
    
    // Pre-Adhan Time Announcement / Chime per prayer
    val preAdhanSoundFajr: String? = null,
    val preAdhanSoundDhuhr: String? = null,
    val preAdhanSoundAsr: String? = null,
    val preAdhanSoundMaghrib: String? = null,
    val preAdhanSoundIsha: String? = null,
    val preAdhanSoundJumuah: String? = null,
    
    // Du'a Video after Adhan per prayer
    val duaVideoFajr: String? = null,
    val duaVideoDhuhr: String? = null,
    val duaVideoAsr: String? = null,
    val duaVideoMaghrib: String? = null,
    val duaVideoIsha: String? = null,
    val duaVideoJumuah: String? = null,
    
    // Adhan Screen (Slideshow/Static from ZIP)
    val adhanScreenImagesDir: String? = null,
    val adhanScreenMode: String = "SLIDESHOW", // "SLIDESHOW" or "STATIC"
    val adhanSlideDurationSec: Int = 5, // 3, 5, 10, 15
    
    // Ramadan Tab
    val ramadanCannonVideoUri: String? = null,
    val mesaharatyVideoUri: String? = null,
    val mesaharatyMode: String = "BEFORE_FAJR", // "FIXED_TIME", "BEFORE_FAJR"
    val mesaharatyFixedTime: String = "02:30",
    val mesaharatyBeforeFajrMinutes: Int = 45,
    
    // Salawat upon the Prophet ﷺ Tab
    val salawatAudioDir: String? = null,
    val salawatIntervalMinutes: Int = 15,
    val salawatSelectionMode: String = "ORDER", // "ORDER", "RANDOM", "SPECIFIC"
    val salawatSpecificSoundIndex: Int = 0,
    val salawatLastPlayedIndex: Int = 0,
    val salawatEnabled: Boolean = true,
    val nextSalawatTimestamp: Long = 0L,

    // Notification Bar & Widget Settings
    val notificationBarEnabled: Boolean = true,
    val notificationBarShowSeconds: Boolean = true,
    val notificationBarShowSalawat: Boolean = true,
    val widgetShowSeconds: Boolean = true,
    val widgetThemeStyle: String = "EMERALD", // "EMERALD", "BURGUNDY", "CLASSIC"

    // Post-Adhan Dua Video settings
    val autoPlayDuaAfterAdhan: Boolean = true,
    val duaVideoFillScreen: Boolean = true,

    // App Theme (Light by default)
    val isDarkMode: Boolean = false,

    // User requested toggles
    val preAdhanAlertsEnabled: Boolean = true,
    val timeAlertsEnabled: Boolean = true,
    val adhanSoundEnabled: Boolean = true,
    val ramadanCannonEnabled: Boolean = true,
    val mesaharatyEnabled: Boolean = true
)
