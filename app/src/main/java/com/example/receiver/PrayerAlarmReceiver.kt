package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import com.example.PrayerApplication
import com.example.data.local.AppSettingsEntity
import com.example.service.AdhanAudioService
import com.example.ui.adhan.AdhanScreenActivity
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

        // 1. Keep CPU awake immediately so Samsung Exynos / One UI does not sleep
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = try {
            powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "PrayerApp:AlarmWakeLock"
            )?.apply {
                setReferenceCounted(false)
                acquire(15000L) // 15 seconds safe ceiling
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

        val pendingResult = goAsync()

        when (action) {
            AlarmScheduler.ACTION_ADHAN -> {
                val prayerId = intent.getStringExtra("EXTRA_PRAYER_ID") ?: "FAJR"
                val prayerName = intent.getStringExtra("EXTRA_PRAYER_NAME") ?: "الصلاة"

                // Launch Adhan Screen immediately over lockscreen in its own isolated task
                try {
                    val screenIntent = Intent(context, AdhanScreenActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                        putExtra("EXTRA_PRAYER_ID", prayerId)
                        putExtra("EXTRA_PRAYER_NAME", prayerName)
                        putExtra("EXTRA_IS_ALERT", false)
                    }
                    context.startActivity(screenIntent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = PrayerApplication.instance.database
                        val settings = db.settingsDao().getSettingsDirect() ?: AppSettingsEntity()

                        NotificationHelper.showAdhanNotification(context, prayerName, prayerId, settings.ramadanCannonEnabled, settings.ramadanCannonVideoUri)
                        AdhanAudioService.start(context, prayerId, prayerName)

                        // Reschedule next prayer times
                        AlarmScheduler.scheduleAll(context)
                        PrayerWidgetHelper.updateAllWidgets(context)
                        NotificationHelper.updateOngoingPrayerNotification(context)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        try {
                            if (wakeLock?.isHeld == true) wakeLock.release()
                        } catch (_: Exception) {}
                        pendingResult.finish()
                    }
                }
            }

            AlarmScheduler.ACTION_PRE_ALERT -> {
                val prayerName = intent.getStringExtra("EXTRA_PRAYER_NAME") ?: "الصلاة"
                val minutesBefore = intent.getIntExtra("EXTRA_MINUTES_BEFORE", 15)
                val ringtoneUri = intent.getStringExtra("EXTRA_RINGTONE_URI")

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = PrayerApplication.instance.database
                        val settings = db.settingsDao().getSettingsDirect() ?: AppSettingsEntity()

                        if (settings.preAdhanAlertsEnabled) {
                            // Launch Alert Screen immediately over lockscreen in its own isolated task
                            try {
                                val screenIntent = Intent(context, AdhanScreenActivity::class.java).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                                    putExtra("EXTRA_PRAYER_NAME", prayerName)
                                    putExtra("EXTRA_MINUTES_REMAINING", minutesBefore)
                                    putExtra("EXTRA_IS_ALERT", true)
                                    putExtra("EXTRA_RINGTONE_URI", ringtoneUri)
                                }
                                context.startActivity(screenIntent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }

                            NotificationHelper.showAlertNotification(context, prayerName, minutesBefore, ringtoneUri)
                            AudioPlayerHelper.playAudioUri(context, ringtoneUri)
                        }
                        AlarmScheduler.scheduleAll(context)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        try {
                            if (wakeLock?.isHeld == true) wakeLock.release()
                        } catch (_: Exception) {}
                        pendingResult.finish()
                    }
                }
            }

            AlarmScheduler.ACTION_SALAWAT -> {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
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
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        try {
                            if (wakeLock?.isHeld == true) wakeLock.release()
                        } catch (_: Exception) {}
                        pendingResult.finish()
                    }
                }
            }

            AlarmScheduler.ACTION_MESAHARATY -> {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = PrayerApplication.instance.database
                        val settings = db.settingsDao().getSettingsDirect() ?: AppSettingsEntity()

                        if (settings.mesaharatyEnabled) {
                            NotificationHelper.showMesaharatyNotification(context, settings.mesaharatyVideoUri)
                            if (settings.adhanSoundEnabled) {
                                AudioPlayerHelper.playAudioUri(context, settings.mesaharatyVideoUri)
                            }
                        }
                        AlarmScheduler.scheduleAll(context)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        try {
                            if (wakeLock?.isHeld == true) wakeLock.release()
                        } catch (_: Exception) {}
                        pendingResult.finish()
                    }
                }
            }
        }
    }
}
