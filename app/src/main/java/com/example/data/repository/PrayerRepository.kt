package com.example.data.repository

import android.content.Context
import com.example.data.local.AppSettingsEntity
import com.example.data.local.PrayerAlertEntity
import com.example.data.local.PrayerDatabase
import com.example.util.AlarmScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PrayerRepository(
    private val context: Context,
    private val database: PrayerDatabase
) {
    val settingsFlow: Flow<AppSettingsEntity> = database.settingsDao().getSettingsFlow().map {
        it ?: AppSettingsEntity()
    }

    val alertsFlow: Flow<List<PrayerAlertEntity>> = database.alertDao().getAllAlerts()

    suspend fun getSettings(): AppSettingsEntity {
        return database.settingsDao().getSettingsDirect() ?: AppSettingsEntity()
    }

    suspend fun updateSettings(settings: AppSettingsEntity) {
        database.settingsDao().insertOrUpdate(settings)
        AlarmScheduler.scheduleAll(context)
    }

    suspend fun insertAlert(alert: PrayerAlertEntity): Long {
        val id = database.alertDao().insertAlert(alert)
        AlarmScheduler.scheduleAll(context)
        return id
    }

    suspend fun updateAlert(alert: PrayerAlertEntity) {
        database.alertDao().updateAlert(alert)
        AlarmScheduler.scheduleAll(context)
    }

    suspend fun deleteAlert(alert: PrayerAlertEntity) {
        database.alertDao().deleteAlert(alert)
        AlarmScheduler.scheduleAll(context)
    }
}
