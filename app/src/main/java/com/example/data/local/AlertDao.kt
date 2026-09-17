package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AlertDao {
    @Query("SELECT * FROM prayer_alerts ORDER BY id DESC")
    fun getAllAlerts(): Flow<List<PrayerAlertEntity>>

    @Query("SELECT * FROM prayer_alerts WHERE isEnabled = 1")
    suspend fun getEnabledAlerts(): List<PrayerAlertEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: PrayerAlertEntity): Long

    @Update
    suspend fun updateAlert(alert: PrayerAlertEntity)

    @Delete
    suspend fun deleteAlert(alert: PrayerAlertEntity)

    @Query("DELETE FROM prayer_alerts WHERE id = :id")
    suspend fun deleteAlertById(id: Int)
}
