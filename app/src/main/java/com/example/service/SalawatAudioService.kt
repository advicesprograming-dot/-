package com.example.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.PrayerApplication
import com.example.data.local.AppSettingsEntity
import com.example.util.AudioPlayerHelper
import com.example.util.NotificationHelper
import com.example.util.ZipExtractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import kotlin.random.Random

class SalawatAudioService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null
    private var mediaPlayer: MediaPlayer? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 1. Acquire WakeLock to keep CPU active during playback on Samsung / Doze mode
        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        wakeLock = try {
            powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "PrayerApp:SalawatServiceWakeLock"
            )?.apply {
                setReferenceCounted(false)
                acquire(45000L) // Safe 45s ceiling
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

        // 2. Build high priority foreground notification
        val appIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, NotificationHelper.CHANNEL_SALAWAT)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("ﷺ صلّ على الحبيب المصطفى ﷺ")
            .setContentText("اللهم صل وسلم وبارك على نبينا محمد وعلى آله وصحبه أجمعين")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NotificationHelper.NOTIFICATION_ID_SALAWAT,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(NotificationHelper.NOTIFICATION_ID_SALAWAT, notification)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 3. Play audio
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = PrayerApplication.instance.database
                val settings = db.settingsDao().getSettingsDirect() ?: AppSettingsEntity()

                if (!settings.salawatEnabled) {
                    finishService()
                    return@launch
                }

                val audioFiles = ZipExtractor.getExtractedFiles(this@SalawatAudioService, "salawat_audios")
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

                    playFile(fileToPlay)
                } else {
                    // Fallback to high quality synthesized sound
                    AudioPlayerHelper.playTimeChime {
                        finishService()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                finishService()
            }
        }

        return START_NOT_STICKY
    }

    private fun playFile(file: File) {
        try {
            mediaPlayer?.release()
            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                val fis = java.io.FileInputStream(file)
                setDataSource(fis.fd)
                fis.close()
                prepare()
                setOnCompletionListener {
                    finishService()
                }
                setOnErrorListener { _, _, _ ->
                    finishService()
                    true
                }
                start()
            }
            mediaPlayer = mp
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback
            AudioPlayerHelper.playTimeChime {
                finishService()
            }
        }
    }

    private fun finishService() {
        try {
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (_: Exception) {}

        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {}

        try {
            stopForeground(true)
            stopSelf()
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        finishService()
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, SalawatAudioService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
