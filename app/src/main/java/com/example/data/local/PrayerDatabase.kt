package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [PrayerAlertEntity::class, AppSettingsEntity::class],
    version = 5,
    exportSchema = false
)
abstract class PrayerDatabase : RoomDatabase() {
    abstract fun alertDao(): AlertDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        @Volatile
        private var INSTANCE: PrayerDatabase? = null
        
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val cursor = db.query("PRAGMA table_info(app_settings)")
                val existingColumns = mutableSetOf<String>()
                while (cursor.moveToNext()) {
                    existingColumns.add(cursor.getString(1))
                }
                cursor.close()

                val columnsToAdd = mapOf(
                    "language" to "TEXT NOT NULL DEFAULT 'ar'",
                    "locationMode" to "TEXT NOT NULL DEFAULT 'MANUAL'",
                    "cityName" to "TEXT NOT NULL DEFAULT 'مكة المكرمة'",
                    "latitude" to "REAL NOT NULL DEFAULT 21.4225",
                    "longitude" to "REAL NOT NULL DEFAULT 39.8262",
                    "timezoneId" to "TEXT NOT NULL DEFAULT 'Asia/Riyadh'",
                    "dstMode" to "INTEGER NOT NULL DEFAULT 0",
                    "calcMethod" to "TEXT NOT NULL DEFAULT 'UMM_AL_QURA'",
                    "asrMadhab" to "TEXT NOT NULL DEFAULT 'SHAFI'",
                    "adjFajr" to "INTEGER NOT NULL DEFAULT 0",
                    "adjSunrise" to "INTEGER NOT NULL DEFAULT 0",
                    "adjDhuhr" to "INTEGER NOT NULL DEFAULT 0",
                    "adjAsr" to "INTEGER NOT NULL DEFAULT 0",
                    "adjMaghrib" to "INTEGER NOT NULL DEFAULT 0",
                    "adjIsha" to "INTEGER NOT NULL DEFAULT 0",
                    "timeFormat24" to "INTEGER NOT NULL DEFAULT 0",
                    "hijriAdjustmentDays" to "INTEGER NOT NULL DEFAULT 0",
                    "adhanAudioFajr" to "TEXT",
                    "adhanAudioDhuhr" to "TEXT",
                    "adhanAudioAsr" to "TEXT",
                    "adhanAudioMaghrib" to "TEXT",
                    "adhanAudioIsha" to "TEXT",
                    "adhanAudioJumuah" to "TEXT",
                    "preAdhanSoundFajr" to "TEXT",
                    "preAdhanSoundDhuhr" to "TEXT",
                    "preAdhanSoundAsr" to "TEXT",
                    "preAdhanSoundMaghrib" to "TEXT",
                    "preAdhanSoundIsha" to "TEXT",
                    "preAdhanSoundJumuah" to "TEXT",
                    "duaVideoFajr" to "TEXT",
                    "duaVideoDhuhr" to "TEXT",
                    "duaVideoAsr" to "TEXT",
                    "duaVideoMaghrib" to "TEXT",
                    "duaVideoIsha" to "TEXT",
                    "duaVideoJumuah" to "TEXT",
                    "adhanScreenImagesDir" to "TEXT",
                    "adhanScreenMode" to "TEXT NOT NULL DEFAULT 'SLIDESHOW'",
                    "adhanSlideDurationSec" to "INTEGER NOT NULL DEFAULT 5",
                    "ramadanCannonVideoUri" to "TEXT",
                    "mesaharatyVideoUri" to "TEXT",
                    "mesaharatyMode" to "TEXT NOT NULL DEFAULT 'BEFORE_FAJR'",
                    "mesaharatyFixedTime" to "TEXT NOT NULL DEFAULT '02:30'",
                    "mesaharatyBeforeFajrMinutes" to "INTEGER NOT NULL DEFAULT 45",
                    "salawatAudioDir" to "TEXT",
                    "salawatIntervalMinutes" to "INTEGER NOT NULL DEFAULT 15",
                    "salawatSelectionMode" to "TEXT NOT NULL DEFAULT 'ORDER'",
                    "salawatSpecificSoundIndex" to "INTEGER NOT NULL DEFAULT 0",
                    "salawatLastPlayedIndex" to "INTEGER NOT NULL DEFAULT 0",
                    "salawatEnabled" to "INTEGER NOT NULL DEFAULT 1",
                    "nextSalawatTimestamp" to "INTEGER NOT NULL DEFAULT 0",
                    "notificationBarEnabled" to "INTEGER NOT NULL DEFAULT 1",
                    "notificationBarShowSeconds" to "INTEGER NOT NULL DEFAULT 1",
                    "notificationBarShowSalawat" to "INTEGER NOT NULL DEFAULT 1",
                    "widgetShowSeconds" to "INTEGER NOT NULL DEFAULT 1",
                    "widgetThemeStyle" to "TEXT NOT NULL DEFAULT 'EMERALD'",
                    "autoPlayDuaAfterAdhan" to "INTEGER NOT NULL DEFAULT 1",
                    "duaVideoFillScreen" to "INTEGER NOT NULL DEFAULT 1",
                    "isDarkMode" to "INTEGER NOT NULL DEFAULT 0",
                    "preAdhanAlertsEnabled" to "INTEGER NOT NULL DEFAULT 1",
                    "timeAlertsEnabled" to "INTEGER NOT NULL DEFAULT 1",
                    "adhanSoundEnabled" to "INTEGER NOT NULL DEFAULT 1",
                    "ramadanCannonEnabled" to "INTEGER NOT NULL DEFAULT 1",
                    "mesaharatyEnabled" to "INTEGER NOT NULL DEFAULT 1"
                )

                for ((col, def) in columnsToAdd) {
                    if (!existingColumns.contains(col)) {
                        db.execSQL("ALTER TABLE app_settings ADD COLUMN $col $def")
                    }
                }
            }
        }

        fun getDatabase(context: Context): PrayerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PrayerDatabase::class.java,
                    "prayer_database.db"
                )
                    .addMigrations(MIGRATION_4_5)
                    .fallbackToDestructiveMigration()
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
