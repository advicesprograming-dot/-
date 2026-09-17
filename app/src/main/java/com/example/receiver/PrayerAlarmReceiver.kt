package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.PrayerApplication
import com.example.data.local.AppSettingsEntity
import com.example.service.AdhanAudioService
import com.example.util.AlarmScheduler
import com.example.util.AudioPlayerHelper
import com.example.util.NotificationHelper
import com.example.util.PrayerWidgetHelper
import com.example.util.ZipExtractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import kotlin.random.Random

class PrayerAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return

        when (action) {
            AlarmScheduler.ACTION_ADHAN -> {
                val prayerId = intent.getStringExtra("EXTRA_PRAYER_ID") ?: "FAJR"
                val prayerName = intent.getStringExtra("EXTRA_PRAYER_NAME") ?: "الصلاة"

                CoroutineScope(Dispatchers.IO).launch {
                    val db = PrayerApplication.instance.database
                    val settings = db.settingsDao().getSettingsDirect() ?: AppSettingsEntity()

                    NotificationHelper.showAdhanNotification(context, prayerName, prayerId, settings.ramadanCannonEnabled, settings.ramadanCannonVideoUri)
                    if (settings.adhanSoundEnabled) {
                        AdhanAudioService.start(context, prayerId, prayerName)
                    }

                    // Reschedule next prayer times
                    AlarmScheduler.scheduleAll(context)
                    PrayerWidgetHelper.updateAllWidgets(context)
                    NotificationHelper.updateOngoingPrayerNotification(context)
                }
            }

            AlarmScheduler.ACTION_PRE_ALERT -> {
                val prayerName = intent.getStringExtra("EXTRA_PRAYER_NAME") ?: "الصلاة"
                val minutesBefore = intent.getIntExtra("EXTRA_MINUTES_BEFORE", 15)
                val ringtoneUri = intent.getStringExtra("EXTRA_RINGTONE_URI")

                CoroutineScope(Dispatchers.IO).launch {
                    val db = PrayerApplication.instance.database
                    val settings = db.settingsDao().getSettingsDirect() ?: AppSettingsEntity()

                    if (settings.preAdhanAlertsEnabled) {
                        NotificationHelper.showAlertNotification(context, prayerName, minutesBefore, ringtoneUri)
                        AudioPlayerHelper.playAudioUri(context, ringtoneUri)
                    }
                    AlarmScheduler.scheduleAll(context)
                }
            }

            AlarmScheduler.ACTION_SALAWAT -> {
                CoroutineScope(Dispatchers.IO).launch {
                    val db = PrayerApplication.instance.database
                    val settings = db.settingsDao().getSettingsDirect() ?: AppSettingsEntity()

                    if (settings.salawatEnabled) {
                        NotificationHelper.showSalawatNotification(context)

                        // Play audio according to selected mode
                        val audioFiles = ZipExtractor.getExtractedFiles(context, "salawat_audios")
                        if (audioFiles.isNotEmpty()) {
                            val fileToPlay: File = when (settings.salawatSelectionMode) {
                                "RANDOM" -> audioFiles[Random.nextInt(audioFiles.size)]
                                "SPECIFIC" -> {
                                    val idx = settings.salawatSpecificSoundIndex.coerceIn(0, audioFiles.size - 1)
                                    audioFiles[idx]
                                }
                                else -> { // "ORDER"
                                    val nextIdx = (settings.salawatLastPlayedIndex + 1) % audioFiles.size
                                    db.settingsDao().insertOrUpdate(settings.copy(salawatLastPlayedIndex = nextIdx))
                                    audioFiles[nextIdx]
                                }
                            }
                            AudioPlayerHelper.playAudioUri(context, fileToPlay.absolutePath)
                        } else {
                            AudioPlayerHelper.playSynthesizedChime()
                        }

                        // Schedule next recurring salawat alarm
                        AlarmScheduler.scheduleSalawat(context, settings)
                    }
                }
            }

            AlarmScheduler.ACTION_MESAHARATY -> {
                CoroutineScope(Dispatchers.IO).launch {
                    val db = PrayerApplication.instance.database
                    val settings = db.settingsDao().getSettingsDirect() ?: AppSettingsEntity()

                    if (settings.mesaharatyEnabled) {
                        NotificationHelper.showMesaharatyNotification(context, settings.mesaharatyVideoUri)
                        if (settings.adhanSoundEnabled) {
                            AudioPlayerHelper.playAudioUri(context, settings.mesaharatyVideoUri)
                        }
                    }
                    AlarmScheduler.scheduleAll(context)
                }
            }
        }
    }
}
