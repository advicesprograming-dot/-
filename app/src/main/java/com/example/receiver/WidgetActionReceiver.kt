package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.util.NotificationHelper
import com.example.util.PrayerWidgetHelper

class WidgetActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            ACTION_PLAY_SALAWAT -> {
                PrayerWidgetHelper.playSalawatDirectly(context)
            }
            ACTION_UPDATE_ALL -> {
                PrayerWidgetHelper.updateAllWidgets(context)
                NotificationHelper.updateOngoingPrayerNotification(context)
            }
        }
    }

    companion object {
        const val ACTION_PLAY_SALAWAT = "com.example.action.PLAY_SALAWAT"
        const val ACTION_UPDATE_ALL = "com.example.action.UPDATE_PRAYER_WIDGET"
    }
}
