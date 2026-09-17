package com.example.util

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.PrayerApplication
import com.example.R
import com.example.data.local.AppSettingsEntity
import com.example.receiver.WidgetActionReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object PrayerWidgetHelper {

    fun formatDuration(millis: Long, includeSeconds: Boolean = true): String {
        val totalSec = (millis.coerceAtLeast(0L) / 1000).toInt()
        val hours = totalSec / 3600
        val minutes = (totalSec % 3600) / 60
        val seconds = totalSec % 60

        return if (includeSeconds) {
            String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", hours, minutes)
        }
    }

    fun formatSalawatTimer(millis: Long): String {
        val totalSec = (millis.coerceAtLeast(0L) / 1000).toInt()
        val minutes = totalSec / 60
        val seconds = totalSec % 60
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }

    fun formatClockTime(hour24: Int, minute: Int, is24h: Boolean): String {
        return if (is24h) {
            String.format(Locale.getDefault(), "%02d:%02d", hour24, minute)
        } else {
            val h = if (hour24 == 0) 12 else if (hour24 > 12) hour24 - 12 else hour24
            val ampm = if (hour24 >= 12) "م" else "ص"
            String.format(Locale.getDefault(), "%02d:%02d %s", h, minute, ampm)
        }
    }

    fun getCurrentAndNextPrayer(schedule: PrayerSchedule, nowMillis: Long): Pair<PrayerTime, PrayerTime> {
        val times = listOf(
            schedule.fajr,
            schedule.dhuhr,
            schedule.asr,
            schedule.maghrib,
            schedule.isha
        )

        // Find current (most recently passed) prayer
        var current = schedule.isha
        var next = schedule.fajr

        for (i in times.indices) {
            if (nowMillis >= times[i].timestamp) {
                current = times[i]
                next = if (i + 1 < times.size) {
                    times[i + 1]
                } else {
                    schedule.fajr.copy(timestamp = schedule.fajr.timestamp + 24 * 3600 * 1000L)
                }
            }
        }

        // If before Fajr today, current is yesterday's Isha, next is Fajr
        if (nowMillis < schedule.fajr.timestamp) {
            current = schedule.isha
            next = schedule.fajr
        }

        return Pair(current, next)
    }

    fun getSafeNextSalawatTime(settings: AppSettingsEntity): Long {
        val now = System.currentTimeMillis()
        val intervalMs = settings.salawatIntervalMinutes.coerceAtLeast(1) * 60 * 1000L
        var nextTime = settings.nextSalawatTimestamp
        if (nextTime <= now) {
            nextTime = now + intervalMs
        }
        return nextTime
    }

    fun buildWidgetRemoteViews(context: Context, settings: AppSettingsEntity): RemoteViews {
        val layoutId = when (settings.widgetThemeStyle) {
            "BURGUNDY" -> R.layout.widget_prayer_times_burgundy
            "CLASSIC" -> R.layout.widget_prayer_times
            else -> R.layout.widget_prayer_times_emerald
        }
        val views = RemoteViews(context.packageName, layoutId)
        val now = Calendar.getInstance()
        val schedule = PrayerTimesCalculator.calculateTimes(now.time, settings)
        val (currentPrayer, nextPrayer) = getCurrentAndNextPrayer(schedule, now.timeInMillis)

        // City
        views.setTextViewText(R.id.tv_widget_city, settings.cityName)
        views.setTextViewText(R.id.tv_widget_title, "مواقيت الصلاة 🕌")

        // Countdown to next prayer
        val diffMs = (nextPrayer.timestamp - now.timeInMillis).coerceAtLeast(0L)
        val baseRealtime = SystemClock.elapsedRealtime() + diffMs
        if (settings.widgetShowSeconds) {
            views.setViewVisibility(R.id.layout_widget_prayer_countdown, View.VISIBLE)
            views.setViewVisibility(R.id.tv_widget_countdown_label, View.GONE)
            views.setChronometer(R.id.chronometer_widget_prayer, baseRealtime, null, true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                views.setChronometerCountDown(R.id.chronometer_widget_prayer, true)
            }
        } else {
            views.setViewVisibility(R.id.layout_widget_prayer_countdown, View.GONE)
            views.setViewVisibility(R.id.tv_widget_countdown_label, View.VISIBLE)
            val countdownStr = formatDuration(diffMs, includeSeconds = false)
            views.setTextViewText(R.id.tv_widget_countdown_label, "يتبقى $countdownStr على")
        }
        views.setTextViewText(R.id.tv_widget_next_prayer_name, nextPrayer.arabicName)

        // Dates
        val hijriDate = HijriCalendarHelper.getHijriDate(now.time, settings.hijriAdjustmentDays)
        val dayOfWeekAr = SimpleDateFormat("EEEE", Locale("ar")).format(now.time)
        val hijriStr = "$dayOfWeekAr ${hijriDate.formattedAr}"
        val gregFormat = SimpleDateFormat("yyyy-M-d", Locale.ENGLISH)
        views.setTextViewText(R.id.tv_widget_hijri, hijriStr)
        views.setTextViewText(R.id.tv_widget_gregorian, gregFormat.format(now.time))

        // Prayer Columns
        val is24 = settings.timeFormat24
        views.setTextViewText(R.id.tv_col_fajr_time, formatClockTime(schedule.fajr.hour24, schedule.fajr.minute, is24))
        views.setTextViewText(R.id.tv_col_dhuhr_name, schedule.dhuhr.arabicName)
        views.setTextViewText(R.id.tv_col_dhuhr_time, formatClockTime(schedule.dhuhr.hour24, schedule.dhuhr.minute, is24))
        views.setTextViewText(R.id.tv_col_asr_time, formatClockTime(schedule.asr.hour24, schedule.asr.minute, is24))
        views.setTextViewText(R.id.tv_col_maghrib_time, formatClockTime(schedule.maghrib.hour24, schedule.maghrib.minute, is24))
        views.setTextViewText(R.id.tv_col_isha_time, formatClockTime(schedule.isha.hour24, schedule.isha.minute, is24))

        // Badges: "الحالية" and "القادمة"
        views.setViewVisibility(R.id.tv_col_fajr_badge, if (schedule.fajr.id == currentPrayer.id) View.VISIBLE else if (schedule.fajr.id == nextPrayer.id) View.VISIBLE else View.INVISIBLE)
        if (schedule.fajr.id == currentPrayer.id) views.setTextViewText(R.id.tv_col_fajr_badge, "الحالية")
        else if (schedule.fajr.id == nextPrayer.id) views.setTextViewText(R.id.tv_col_fajr_badge, "القادمة")

        views.setViewVisibility(R.id.tv_col_dhuhr_badge, if (schedule.dhuhr.id == currentPrayer.id) View.VISIBLE else if (schedule.dhuhr.id == nextPrayer.id) View.VISIBLE else View.INVISIBLE)
        if (schedule.dhuhr.id == currentPrayer.id) views.setTextViewText(R.id.tv_col_dhuhr_badge, "الحالية")
        else if (schedule.dhuhr.id == nextPrayer.id) views.setTextViewText(R.id.tv_col_dhuhr_badge, "القادمة")

        views.setViewVisibility(R.id.tv_col_asr_badge, if (schedule.asr.id == currentPrayer.id) View.VISIBLE else if (schedule.asr.id == nextPrayer.id) View.VISIBLE else View.INVISIBLE)
        if (schedule.asr.id == currentPrayer.id) views.setTextViewText(R.id.tv_col_asr_badge, "الحالية")
        else if (schedule.asr.id == nextPrayer.id) views.setTextViewText(R.id.tv_col_asr_badge, "القادمة")

        views.setViewVisibility(R.id.tv_col_maghrib_badge, if (schedule.maghrib.id == currentPrayer.id) View.VISIBLE else if (schedule.maghrib.id == nextPrayer.id) View.VISIBLE else View.INVISIBLE)
        if (schedule.maghrib.id == currentPrayer.id) views.setTextViewText(R.id.tv_col_maghrib_badge, "الحالية")
        else if (schedule.maghrib.id == nextPrayer.id) views.setTextViewText(R.id.tv_col_maghrib_badge, "القادمة")

        views.setViewVisibility(R.id.tv_col_isha_badge, if (schedule.isha.id == currentPrayer.id) View.VISIBLE else if (schedule.isha.id == nextPrayer.id) View.VISIBLE else View.INVISIBLE)
        if (schedule.isha.id == currentPrayer.id) views.setTextViewText(R.id.tv_col_isha_badge, "الحالية")
        else if (schedule.isha.id == nextPrayer.id) views.setTextViewText(R.id.tv_col_isha_badge, "القادمة")

        // Salawat bottom bar
        if (settings.salawatEnabled) {
            views.setViewVisibility(R.id.layout_widget_salawat, View.VISIBLE)
            val now = System.currentTimeMillis()
            val nextSalawat = getSafeNextSalawatTime(settings)
            val salawatRemaining = (nextSalawat - now).coerceAtLeast(0L)
            val salawatBaseRealtime = SystemClock.elapsedRealtime() + salawatRemaining

            if (settings.widgetShowSeconds) {
                views.setViewVisibility(R.id.layout_widget_salawat_timer, View.VISIBLE)
                views.setViewVisibility(R.id.tv_widget_salawat_countdown, View.GONE)
                views.setChronometer(R.id.chronometer_widget_salawat, salawatBaseRealtime, null, true)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    views.setChronometerCountDown(R.id.chronometer_widget_salawat, true)
                }
            } else {
                views.setViewVisibility(R.id.layout_widget_salawat_timer, View.GONE)
                views.setViewVisibility(R.id.tv_widget_salawat_countdown, View.VISIBLE)
                views.setTextViewText(R.id.tv_widget_salawat_countdown, "الصلاة على النبي ﷺ: يتبقى ${formatSalawatTimer(salawatRemaining)}")
            }

            // Click listener on "صلّ الآن ﷺ"
            val playIntent = Intent(context, WidgetActionReceiver::class.java).apply {
                action = WidgetActionReceiver.ACTION_PLAY_SALAWAT
            }
            val playPendingIntent = PendingIntent.getBroadcast(
                context,
                201,
                playIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_widget_salawat_action, playPendingIntent)
        } else {
            views.setViewVisibility(R.id.layout_widget_salawat, View.GONE)
        }

        // Open App when clicking main widget area
        val appIntent = Intent(context, MainActivity::class.java)
        val appPendingIntent = PendingIntent.getActivity(
            context,
            100,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.banner_next_prayer, appPendingIntent)

        return views
    }

    fun buildSalawatWidgetRemoteViews(context: Context, settings: AppSettingsEntity): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_salawat)

        val now = System.currentTimeMillis()
        val nextTime = getSafeNextSalawatTime(settings)
        val diffMs = (nextTime - now).coerceAtLeast(0L)
        val baseTime = android.os.SystemClock.elapsedRealtime() + diffMs

        views.setChronometer(R.id.chronometer_standalone_salawat, baseTime, null, true)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            views.setChronometerCountDown(R.id.chronometer_standalone_salawat, true)
        }

        val actionIntent = Intent(context, com.example.receiver.WidgetActionReceiver::class.java).apply {
            action = com.example.receiver.WidgetActionReceiver.ACTION_PLAY_SALAWAT
        }
        val actionPendingIntent = PendingIntent.getBroadcast(
            context,
            200,
            actionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btn_standalone_salawat_action, actionPendingIntent)

        val appIntent = Intent(context, com.example.MainActivity::class.java)
        val appPendingIntent = PendingIntent.getActivity(
            context,
            201,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_salawat_root, appPendingIntent)

        return views
    }

    fun updateAllWidgets(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = PrayerApplication.instance.database
                val settings = db.settingsDao().getSettingsDirect() ?: AppSettingsEntity()
                val appWidgetManager = AppWidgetManager.getInstance(context)

                // 1. Prayer Times Main Widget
                val prayerWidgetComponent = ComponentName(context, "com.example.receiver.PrayerTimesWidgetProvider")
                val prayerWidgetIds = appWidgetManager.getAppWidgetIds(prayerWidgetComponent)
                if (prayerWidgetIds.isNotEmpty()) {
                    val views = buildWidgetRemoteViews(context, settings)
                    appWidgetManager.updateAppWidget(prayerWidgetIds, views)
                }

                // 2. Standalone Salawat upon the Prophet Widget
                val salawatWidgetComponent = ComponentName(context, "com.example.receiver.SalawatWidgetProvider")
                val salawatWidgetIds = appWidgetManager.getAppWidgetIds(salawatWidgetComponent)
                if (salawatWidgetIds.isNotEmpty()) {
                    val salawatViews = buildSalawatWidgetRemoteViews(context, settings)
                    appWidgetManager.updateAppWidget(salawatWidgetIds, salawatViews)
                }

                // 3. Dedicated Mosque Clock Strip Widget (Images 1 & 2)
                val mosqueStripComponent = ComponentName(context, "com.example.receiver.MosqueStripWidgetProvider")
                val mosqueStripIds = appWidgetManager.getAppWidgetIds(mosqueStripComponent)
                if (mosqueStripIds.isNotEmpty()) {
                    val stripViews = buildMosqueStripRemoteViews(context, settings)
                    appWidgetManager.updateAppWidget(mosqueStripIds, stripViews)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Dedicated wide Mosque Display Board Strip widget matching Images 1 and 2
     * with Mosque Emblem, Digital Clock with Live Seconds, Circular Countdown Dial,
     * Quranic Verse, and Dome Prayer Columns.
     */
    fun buildMosqueStripRemoteViews(context: Context, settings: AppSettingsEntity): RemoteViews {
        val layoutId = if (settings.widgetThemeStyle == "BURGUNDY") {
            R.layout.widget_mosque_display_burgundy
        } else {
            R.layout.widget_mosque_display_emerald
        }
        val views = RemoteViews(context.packageName, layoutId)
        val now = Calendar.getInstance()
        val schedule = PrayerTimesCalculator.calculateTimes(now.time, settings)
        val (_, nextPrayer) = getCurrentAndNextPrayer(schedule, now.timeInMillis)

        // Mosque / City Name
        views.setTextViewText(R.id.tv_strip_mosque_name, "مسجد الهدى • ${settings.cityName}")

        // Dates (Hijri & Gregorian)
        val hijriDate = HijriCalendarHelper.getHijriDate(now.time, settings.hijriAdjustmentDays)
        val dayOfWeekAr = SimpleDateFormat("EEEE", Locale("ar")).format(now.time)
        val gregFormat = SimpleDateFormat("yyyy/MM/dd", Locale.ENGLISH)
        val datesStr = "$dayOfWeekAr ${hijriDate.formattedAr} • ${gregFormat.format(now.time)}"
        views.setTextViewText(R.id.tv_strip_date, datesStr)

        // Circular Countdown Badge to next prayer with live seconds
        val diffMs = (nextPrayer.timestamp - now.timeInMillis).coerceAtLeast(0L)
        val baseRealtime = SystemClock.elapsedRealtime() + diffMs
        views.setChronometer(R.id.chronometer_strip_prayer, baseRealtime, null, true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            views.setChronometerCountDown(R.id.chronometer_strip_prayer, true)
        }
        views.setTextViewText(R.id.tv_strip_countdown_prayer_name, "المتبقي لـ ${nextPrayer.arabicName}")

        // 6 Columns (Fajr, Sunrise, Dhuhr, Asr, Maghrib, Isha)
        val is24 = settings.timeFormat24
        views.setTextViewText(R.id.tv_strip_time_fajr, formatClockTime(schedule.fajr.hour24, schedule.fajr.minute, is24))
        views.setTextViewText(R.id.tv_strip_time_sunrise, formatClockTime(schedule.sunrise.hour24, schedule.sunrise.minute, is24))
        views.setTextViewText(R.id.tv_strip_time_dhuhr, formatClockTime(schedule.dhuhr.hour24, schedule.dhuhr.minute, is24))
        views.setTextViewText(R.id.tv_strip_time_asr, formatClockTime(schedule.asr.hour24, schedule.asr.minute, is24))
        views.setTextViewText(R.id.tv_strip_time_maghrib, formatClockTime(schedule.maghrib.hour24, schedule.maghrib.minute, is24))
        views.setTextViewText(R.id.tv_strip_time_isha, formatClockTime(schedule.isha.hour24, schedule.isha.minute, is24))

        // Salawat Mini Bar
        if (settings.salawatEnabled) {
            views.setViewVisibility(R.id.layout_strip_salawat, View.VISIBLE)
            val now = System.currentTimeMillis()
            val nextSalawat = getSafeNextSalawatTime(settings)
            val salRemaining = (nextSalawat - now).coerceAtLeast(0L)
            val salBase = SystemClock.elapsedRealtime() + salRemaining
            views.setChronometer(R.id.chronometer_strip_salawat, salBase, null, true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                views.setChronometerCountDown(R.id.chronometer_strip_salawat, true)
            }
            val playIntent = Intent(context, WidgetActionReceiver::class.java).apply {
                action = WidgetActionReceiver.ACTION_PLAY_SALAWAT
            }
            val playPendingIntent = PendingIntent.getBroadcast(
                context,
                205,
                playIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_strip_salawat_action, playPendingIntent)
        } else {
            views.setViewVisibility(R.id.layout_strip_salawat, View.GONE)
        }

        // Open app on click
        val appIntent = Intent(context, MainActivity::class.java)
        val appPendingIntent = PendingIntent.getActivity(
            context,
            104,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.card_mosque_display, appPendingIntent)

        return views
    }

    /**
     * Builds a custom, fully visible compact remote view specifically tailored
     * for Android notification bars so that all 5 prayer times, countdown, city,
     * and Salawat timer with seconds fit without vertical clipping or partial truncation.
     */
    fun buildNotificationRemoteViews(context: Context, settings: AppSettingsEntity): RemoteViews {
        val layoutId = when (settings.widgetThemeStyle) {
            "BURGUNDY" -> R.layout.notification_prayer_bar_burgundy
            "CLASSIC" -> R.layout.notification_prayer_bar
            else -> R.layout.notification_prayer_bar_emerald
        }
        val views = RemoteViews(context.packageName, layoutId)
        val now = Calendar.getInstance()
        val schedule = PrayerTimesCalculator.calculateTimes(now.time, settings)
        val (_, nextPrayer) = getCurrentAndNextPrayer(schedule, now.timeInMillis)

        // Countdown to next prayer with live seconds Chronometer
        val diffMs = (nextPrayer.timestamp - now.timeInMillis).coerceAtLeast(0L)
        val baseRealtime = SystemClock.elapsedRealtime() + diffMs

        if (settings.notificationBarShowSeconds) {
            views.setViewVisibility(R.id.layout_notif_prayer_countdown, View.VISIBLE)
            views.setViewVisibility(R.id.tv_notif_countdown, View.GONE)
            views.setTextViewText(
                R.id.tv_notif_prayer_label,
                "${nextPrayer.arabicName} ${formatClockTime(nextPrayer.hour24, nextPrayer.minute, settings.timeFormat24)} (يتبقى "
            )
            views.setChronometer(R.id.chronometer_notif_prayer, baseRealtime, null, true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                views.setChronometerCountDown(R.id.chronometer_notif_prayer, true)
            }
        } else {
            views.setViewVisibility(R.id.layout_notif_prayer_countdown, View.GONE)
            views.setViewVisibility(R.id.tv_notif_countdown, View.VISIBLE)
            val countdownStr = formatDuration(diffMs, includeSeconds = false)
            views.setTextViewText(
                R.id.tv_notif_countdown,
                "${nextPrayer.arabicName} ${formatClockTime(nextPrayer.hour24, nextPrayer.minute, settings.timeFormat24)} (يتبقى $countdownStr)"
            )
        }
        views.setTextViewText(R.id.tv_notif_city, "${settings.cityName} • ﷺ")

        // 5 Prayers
        val is24 = settings.timeFormat24
        views.setTextViewText(R.id.tv_notif_time_fajr, formatClockTime(schedule.fajr.hour24, schedule.fajr.minute, is24))
        views.setTextViewText(R.id.tv_notif_name_dhuhr, schedule.dhuhr.arabicName)
        views.setTextViewText(R.id.tv_notif_time_dhuhr, formatClockTime(schedule.dhuhr.hour24, schedule.dhuhr.minute, is24))
        views.setTextViewText(R.id.tv_notif_time_asr, formatClockTime(schedule.asr.hour24, schedule.asr.minute, is24))
        views.setTextViewText(R.id.tv_notif_time_maghrib, formatClockTime(schedule.maghrib.hour24, schedule.maghrib.minute, is24))
        views.setTextViewText(R.id.tv_notif_time_isha, formatClockTime(schedule.isha.hour24, schedule.isha.minute, is24))

        // Highlight next prayer cell
        val activeBgColor = 0xFF143B2E.toInt()
        val defaultBgColor = 0x00000000

        views.setInt(R.id.cell_notif_fajr, "setBackgroundColor", if (nextPrayer.id == schedule.fajr.id) activeBgColor else defaultBgColor)
        views.setInt(R.id.cell_notif_dhuhr, "setBackgroundColor", if (nextPrayer.id == schedule.dhuhr.id) activeBgColor else defaultBgColor)
        views.setInt(R.id.cell_notif_asr, "setBackgroundColor", if (nextPrayer.id == schedule.asr.id) activeBgColor else defaultBgColor)
        views.setInt(R.id.cell_notif_maghrib, "setBackgroundColor", if (nextPrayer.id == schedule.maghrib.id) activeBgColor else defaultBgColor)
        views.setInt(R.id.cell_notif_isha, "setBackgroundColor", if (nextPrayer.id == schedule.isha.id) activeBgColor else defaultBgColor)

        val goldColor = 0xFFE5A93C.toInt()
        val whiteColor = 0xFFFFFFFF.toInt()
        val mutedColor = 0xFFA0B2C6.toInt()

        views.setTextColor(R.id.tv_notif_name_fajr, if (nextPrayer.id == schedule.fajr.id) goldColor else mutedColor)
        views.setTextColor(R.id.tv_notif_time_fajr, if (nextPrayer.id == schedule.fajr.id) goldColor else whiteColor)

        views.setTextColor(R.id.tv_notif_name_dhuhr, if (nextPrayer.id == schedule.dhuhr.id) goldColor else mutedColor)
        views.setTextColor(R.id.tv_notif_time_dhuhr, if (nextPrayer.id == schedule.dhuhr.id) goldColor else whiteColor)

        views.setTextColor(R.id.tv_notif_name_asr, if (nextPrayer.id == schedule.asr.id) goldColor else mutedColor)
        views.setTextColor(R.id.tv_notif_time_asr, if (nextPrayer.id == schedule.asr.id) goldColor else whiteColor)

        views.setTextColor(R.id.tv_notif_name_maghrib, if (nextPrayer.id == schedule.maghrib.id) goldColor else mutedColor)
        views.setTextColor(R.id.tv_notif_time_maghrib, if (nextPrayer.id == schedule.maghrib.id) goldColor else whiteColor)

        views.setTextColor(R.id.tv_notif_name_isha, if (nextPrayer.id == schedule.isha.id) goldColor else mutedColor)
        views.setTextColor(R.id.tv_notif_time_isha, if (nextPrayer.id == schedule.isha.id) goldColor else whiteColor)

        // Salawat Row in Notification Bar
        if (settings.salawatEnabled && settings.notificationBarShowSalawat) {
            views.setViewVisibility(R.id.layout_notif_salawat, View.VISIBLE)
            val now = System.currentTimeMillis()
            val nextSalawat = getSafeNextSalawatTime(settings)
            val salawatRemaining = (nextSalawat - now).coerceAtLeast(0L)
            val salawatBaseRealtime = SystemClock.elapsedRealtime() + salawatRemaining

            if (settings.notificationBarShowSeconds) {
                views.setViewVisibility(R.id.layout_notif_salawat_timer, View.VISIBLE)
                views.setViewVisibility(R.id.tv_notif_salawat_countdown, View.GONE)
                views.setChronometer(R.id.chronometer_notif_salawat, salawatBaseRealtime, null, true)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    views.setChronometerCountDown(R.id.chronometer_notif_salawat, true)
                }
            } else {
                views.setViewVisibility(R.id.layout_notif_salawat_timer, View.GONE)
                views.setViewVisibility(R.id.tv_notif_salawat_countdown, View.VISIBLE)
                views.setTextViewText(
                    R.id.tv_notif_salawat_countdown,
                    "الصلاة على النبي ﷺ: يتبقى ${formatSalawatTimer(salawatRemaining)}"
                )
            }

            // Click listener on "صلّ الآن ﷺ" in notification
            val playIntent = Intent(context, WidgetActionReceiver::class.java).apply {
                action = WidgetActionReceiver.ACTION_PLAY_SALAWAT
            }
            val playPendingIntent = PendingIntent.getBroadcast(
                context,
                203,
                playIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_notif_salawat_action, playPendingIntent)
        } else {
            views.setViewVisibility(R.id.layout_notif_salawat, View.GONE)
        }

        return views
    }

    fun playSalawatDirectly(context: Context) {
        com.example.service.SalawatAudioService.start(context)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = PrayerApplication.instance.database
                val s = db.settingsDao().getSettingsDirect() ?: AppSettingsEntity()
                AlarmScheduler.scheduleSalawat(context, s, forceReset = true)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
