package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "prayer_alerts")
data class PrayerAlertEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val prayerTarget: String = "ALL", // "ALL", "FAJR", "DHUHR", "ASR", "MAGHRIB", "ISHA", "JUMUAH"
    val minutesBefore: Int = 15,
    val repeatDays: String = "1,2,3,4,5,6,7", // 1=Sun, 2=Mon... 7=Sat
    val ringtoneUri: String? = null,
    val ringtoneName: String = "افتراضي",
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
