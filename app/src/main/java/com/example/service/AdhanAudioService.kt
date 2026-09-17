package com.example.service

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.PrayerApplication
import com.example.data.local.AppSettingsEntity
import com.example.ui.adhan.AdhanScreenActivity
import com.example.util.AudioPlayerHelper
import com.example.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AdhanAudioService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val prayerId = intent?.getStringExtra("PRAYER_ID") ?: "FAJR"
        val prayerName = intent?.getStringExtra("PRAYER_NAME") ?: "الصلاة"

        val notification: Notification = NotificationCompat.Builder(this, NotificationHelper.CHANNEL_SERVICE)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("انطلاق أذان $prayerName")
            .setContentText("حي على الصلاة • استمع للأذان")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NotificationHelper.NOTIFICATION_ID_SERVICE,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(NotificationHelper.NOTIFICATION_ID_SERVICE, notification)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        CoroutineScope(Dispatchers.IO).launch {
            val db = PrayerApplication.instance.database
            val settings = db.settingsDao().getSettingsDirect() ?: AppSettingsEntity()

            if (!settings.adhanSoundEnabled) {
                kotlinx.coroutines.delay(15000L) // Wait 15 seconds then transition
                AudioPlayerHelper.onAlertCompletedListener?.invoke()
                stopSelf()
                return@launch
            }

            // 1. Time Alert Sound (First)
            val preSoundUri = when (prayerId) {
                "FAJR" -> settings.preAdhanSoundFajr
                "DHUHR" -> settings.preAdhanSoundDhuhr
                "ASR" -> settings.preAdhanSoundAsr
                "MAGHRIB" -> settings.preAdhanSoundMaghrib
                "ISHA" -> settings.preAdhanSoundIsha
                "JUMUAH" -> settings.preAdhanSoundJumuah
                else -> null
            }

            val adhanSoundUri = when (prayerId) {
                "FAJR" -> settings.adhanAudioFajr
                "DHUHR" -> settings.adhanAudioDhuhr
                "ASR" -> settings.adhanAudioAsr
                "MAGHRIB" -> settings.adhanAudioMaghrib
                "ISHA" -> settings.adhanAudioIsha
                "JUMUAH" -> settings.adhanAudioJumuah
                else -> null
            }

            // Play Pre-Chime first
            val onAdhanFinished = {
                onAdhanCompletedListener?.invoke()
                stopSelf()
            }

            if (!preSoundUri.isNullOrBlank()) {
                AudioPlayerHelper.playAudioUri(this@AdhanAudioService, preSoundUri) {
                    // Directly followed by Adhan without delay
                    AudioPlayerHelper.playAudioUri(this@AdhanAudioService, adhanSoundUri) {
                        onAdhanFinished()
                    }
                }
            } else {
                AudioPlayerHelper.playTimeChime {
                    // Directly followed by Adhan
                    AudioPlayerHelper.playAudioUri(this@AdhanAudioService, adhanSoundUri) {
                        onAdhanFinished()
                    }
                }
            }
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        onAdhanCompletedListener = null
    }

    companion object {
        var onAdhanCompletedListener: (() -> Unit)? = null
        fun start(context: Context, prayerId: String, prayerName: String) {
            val intent = Intent(context, AdhanAudioService::class.java).apply {
                putExtra("PRAYER_ID", prayerId)
                putExtra("PRAYER_NAME", prayerName)
            }
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun stop(context: Context) {
            AudioPlayerHelper.stopAudio()
            val intent = Intent(context, AdhanAudioService::class.java)
            context.stopService(intent)
        }
    }
}
