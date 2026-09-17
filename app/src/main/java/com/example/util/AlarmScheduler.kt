package com.example.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.PrayerApplication
import com.example.data.local.AppSettingsEntity
import com.example.receiver.PrayerAlarmReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

object AlarmScheduler {

    const val ACTION_ADHAN = "com.example.ACTION_ADHAN"
    const val ACTION_PRE_ALERT = "com.example.ACTION_PRE_ALERT"
    const val ACTION_SALAWAT = "com.example.ACTION_SALAWAT"
    const val ACTION_MESAHARATY = "com.example.ACTION_MESAHARATY"

    private const val REQUEST_CODE_SALAWAT = 9999
    private const val REQUEST_CODE_MESAHARATY = 9998

    fun scheduleAll(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = PrayerApplication.instance.database
            val settings = db.settingsDao().getSettingsDirect() ?: AppSettingsEntity()
            val alerts = db.alertDao().getEnabledAlerts()

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return@launch

            val now = System.currentTimeMillis()

            // 1. Calculate today's and tomorrow's prayers (for Mesaharaty and general fallback)
            val todaySchedule = PrayerTimesCalculator.calculateTimes(Date(), settings)
            val calTomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
            val tomorrowSchedule = PrayerTimesCalculator.calculateTimes(calTomorrow.time, settings)

            // 2. Schedule Adhans and Pre-Adhan Alerts for the next 2 days (Today, Tomorrow, Day After)
            // Rescheduled automatically on each alarm, keeping system responsive and light
            for (dayOffset in 0..2) {
                val dayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, dayOffset) }
                val schedule = PrayerTimesCalculator.calculateTimes(dayCal.time, settings)
                val dayOfWeek = dayCal.get(Calendar.DAY_OF_WEEK)

                val prayers = listOf(
                    schedule.fajr,
                    schedule.dhuhr,
                    schedule.asr,
                    schedule.maghrib,
                    schedule.isha
                )

                // Schedule Adhans
                for (prayer in prayers) {
                    if (prayer.timestamp > now) {
                        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
                            action = ACTION_ADHAN
                            putExtra("EXTRA_PRAYER_ID", prayer.id)
                            putExtra("EXTRA_PRAYER_NAME", prayer.arabicName)
                        }
                        val pi = PendingIntent.getBroadcast(
                            context,
                            (prayer.id.hashCode() + prayer.timestamp).hashCode(),
                            intent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        setExactAlarm(alarmManager, prayer.timestamp, pi)
                    }
                }

                // Schedule Pre-Adhan User Alerts
                if (settings.preAdhanAlertsEnabled) {
                    for (alert in alerts) {
                        val repeatDaysList = alert.repeatDays.split(",").mapNotNull { it.trim().toIntOrNull() }
                        val isDayActive = alert.repeatDays == "ALL" || repeatDaysList.contains(dayOfWeek)

                        if (isDayActive) {
                            for (prayer in prayers) {
                                val matchesPrayer = alert.prayerTarget == "ALL" ||
                                        alert.prayerTarget == prayer.id ||
                                        (alert.prayerTarget == "JUMUAH" && (prayer.id == "JUMUAH" || prayer.id == "DHUHR")) ||
                                        (alert.prayerTarget == "DHUHR" && (prayer.id == "DHUHR" || prayer.id == "JUMUAH"))

                                if (matchesPrayer) {
                                    val alertTime = prayer.timestamp - (alert.minutesBefore * 60 * 1000L)
                                    if (alertTime > now) {
                                        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
                                            action = ACTION_PRE_ALERT
                                            putExtra("EXTRA_PRAYER_NAME", prayer.arabicName)
                                            putExtra("EXTRA_MINUTES_BEFORE", alert.minutesBefore)
                                            putExtra("EXTRA_RINGTONE_URI", alert.ringtoneUri)
                                        }
                                        val pi = PendingIntent.getBroadcast(
                                            context,
                                            (alert.id * 100000 + prayer.id.hashCode() + dayOffset * 1000).hashCode(),
                                            intent,
                                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                                        )
                                        setExactAlarm(alarmManager, alertTime, pi)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. Schedule Salawat
            if (settings.salawatEnabled) {
                scheduleSalawat(context, settings, forceReset = false)
            } else {
                scheduleSalawat(context, settings, forceReset = true)
            }

            // 5. Schedule Mesaharaty / Suhoor Alert
            if (settings.mesaharatyEnabled) {
                val mesaharatyTime = when (settings.mesaharatyMode) {
                    "FIXED_TIME" -> {
                        val parts = settings.mesaharatyFixedTime.split(":")
                        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 2
                        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 30
                        Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, hour)
                            set(Calendar.MINUTE, minute)
                            set(Calendar.SECOND, 0)
                            if (timeInMillis <= now) {
                                add(Calendar.DAY_OF_YEAR, 1)
                            }
                        }.timeInMillis
                    }
                    else -> { // "BEFORE_FAJR"
                        val t1 = todaySchedule.fajr.timestamp - (settings.mesaharatyBeforeFajrMinutes * 60 * 1000L)
                        if (t1 > now) t1 else {
                            tomorrowSchedule.fajr.timestamp - (settings.mesaharatyBeforeFajrMinutes * 60 * 1000L)
                        }
                    }
                }

                if (mesaharatyTime > now) {
                    val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
                        action = ACTION_MESAHARATY
                    }
                    val pi = PendingIntent.getBroadcast(
                        context,
                        REQUEST_CODE_MESAHARATY,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    setExactAlarm(alarmManager, mesaharatyTime, pi)
                }
            }
        }
    }

    fun scheduleSalawat(context: Context, settings: AppSettingsEntity, forceReset: Boolean = false) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = ACTION_SALAWAT
        }
        val pi = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_SALAWAT,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (!settings.salawatEnabled) {
            try {
                alarmManager.cancel(pi)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            if (settings.nextSalawatTimestamp != 0L) {
                CoroutineScope(Dispatchers.IO).launch {
                    PrayerApplication.instance.database.settingsDao().insertOrUpdate(
                        settings.copy(nextSalawatTimestamp = 0L)
                    )
                }
            }
            return
        }

        val intervalMillis = settings.salawatIntervalMinutes.coerceAtLeast(1) * 60 * 1000L
        val now = System.currentTimeMillis()

        // If an alarm is already scheduled in the future and we are not forcing a reset, keep it!
        val nextTime = if (!forceReset && settings.nextSalawatTimestamp > now) {
            settings.nextSalawatTimestamp
        } else {
            now + intervalMillis
        }

        setExactAlarm(alarmManager, nextTime, pi)

        // Only persist if the timestamp has actually changed to prevent recursive database triggers
        if (settings.nextSalawatTimestamp != nextTime) {
            CoroutineScope(Dispatchers.IO).launch {
                PrayerApplication.instance.database.settingsDao().insertOrUpdate(
                    settings.copy(nextSalawatTimestamp = nextTime)
                )
            }
        }
    }

    private fun setExactAlarm(alarmManager: AlarmManager, triggerAtMillis: Long, operation: PendingIntent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerAtMillis, operation)
                alarmManager.setAlarmClock(alarmClockInfo, operation)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, operation)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, operation)
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, operation)
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, operation)
                }
            } catch (ex: Exception) {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, operation)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, operation)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }
}
