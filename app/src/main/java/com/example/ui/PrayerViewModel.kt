package com.example.ui

import android.app.Application
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.PrayerApplication
import com.example.data.local.AppSettingsEntity
import com.example.data.local.PrayerAlertEntity
import com.example.util.AlarmScheduler
import com.example.util.AudioPlayerHelper
import com.example.util.CityPreset
import com.example.util.HijriCalendarHelper
import com.example.util.NotificationHelper
import com.example.util.PrayerSchedule
import com.example.util.PrayerTimesCalculator
import com.example.util.PrayerWidgetHelper
import com.example.util.ZipExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class PrayerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as PrayerApplication).repository

    val settings: StateFlow<AppSettingsEntity> = repository.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppSettingsEntity()
        )

    val alerts: StateFlow<List<PrayerAlertEntity>> = repository.alertsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _currentCalendar = MutableStateFlow(Calendar.getInstance())
    val currentCalendar: StateFlow<Calendar> = _currentCalendar.asStateFlow()

    private val _schedule = MutableStateFlow<PrayerSchedule?>(null)
    val schedule: StateFlow<PrayerSchedule?> = _schedule.asStateFlow()

    private val _isDetectingLocation = MutableStateFlow(false)
    val isDetectingLocation: StateFlow<Boolean> = _isDetectingLocation.asStateFlow()

    private val _locationMessage = MutableStateFlow<String?>(null)
    val locationMessage: StateFlow<String?> = _locationMessage.asStateFlow()

    private val _salawatCountdownMillis = MutableStateFlow(0L)
    val salawatCountdownMillis: StateFlow<Long> = _salawatCountdownMillis.asStateFlow()

    init {
        // 1. Reactive calculation: whenever settings change in Room, recalculate immediately
        viewModelScope.launch {
            repository.settingsFlow.collectLatest { currentSettings ->
                val now = Calendar.getInstance()
                val newSched = PrayerTimesCalculator.calculateTimes(now.time, currentSettings)
                _schedule.value = newSched

                try {
                    AlarmScheduler.scheduleAll(getApplication())
                    PrayerWidgetHelper.updateAllWidgets(getApplication())
                    NotificationHelper.updateOngoingPrayerNotification(getApplication())
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        // 2. Smooth 1-second ticker for clock and countdowns (without jumping prayer times)
        viewModelScope.launch(Dispatchers.Default) {
            var lastDay = -1
            while (true) {
                val now = Calendar.getInstance()
                _currentCalendar.value = now

                val currentDay = now.get(Calendar.DAY_OF_YEAR)
                val currentSettings = settings.value

                // If day changed (crossed midnight), recalculate full schedule for the new day
                if (currentDay != lastDay) {
                    lastDay = currentDay
                    val sched = PrayerTimesCalculator.calculateTimes(now.time, currentSettings)
                    _schedule.value = sched
                } else {
                    _schedule.value?.let { currentSched ->
                        val nowMs = now.timeInMillis
                        val next = currentSched.nextPrayer
                        if (next.timestamp <= nowMs) {
                            val sched = PrayerTimesCalculator.calculateTimes(now.time, currentSettings)
                            _schedule.value = sched
                        } else {
                            val diff = (next.timestamp - nowMs).coerceAtLeast(0L)
                            _schedule.value = currentSched.copy(timeToNextMillis = diff)
                        }
                    }
                }

                // Update Salawat Countdown smoothly
                if (currentSettings.salawatEnabled) {
                    val nowMs = System.currentTimeMillis()
                    val target = currentSettings.nextSalawatTimestamp
                    if (target <= 0L) {
                        val intervalMs = currentSettings.salawatIntervalMinutes.coerceAtLeast(1) * 60 * 1000L
                        _salawatCountdownMillis.value = intervalMs
                    } else {
                        val diff = target - nowMs
                        if (diff > 0) {
                            _salawatCountdownMillis.value = diff
                        } else {
                            _salawatCountdownMillis.value = 0L
                        }
                    }
                } else {
                    _salawatCountdownMillis.value = 0L
                }

                delay(1000)
            }
        }
    }

    fun setLanguage(lang: String) {
        viewModelScope.launch {
            repository.updateSettings(settings.value.copy(language = lang))
        }
    }

    fun selectCityPreset(preset: CityPreset) {
        viewModelScope.launch {
            val name = if (settings.value.language == "ar") preset.nameAr else preset.nameEn
            repository.updateSettings(
                settings.value.copy(
                    locationMode = "MANUAL",
                    cityName = name,
                    latitude = preset.lat,
                    longitude = preset.lng,
                    timezoneId = preset.tz,
                    calcMethod = preset.method,
                    dstMode = -1 // Auto DST
                )
            )
            _locationMessage.value = "تم ضبط المدينة: $name"
        }
    }

    fun updateManualLocation(city: String, lat: Double, lng: Double) {
        viewModelScope.launch {
            val closest = PrayerTimesCalculator.findClosestPreset(lat, lng)
            val tz = closest?.tz ?: settings.value.timezoneId
            val method = closest?.method ?: settings.value.calcMethod
            repository.updateSettings(
                settings.value.copy(
                    locationMode = "MANUAL",
                    cityName = city,
                    latitude = lat,
                    longitude = lng,
                    timezoneId = tz,
                    calcMethod = method
                )
            )
            _locationMessage.value = "تم حفظ الموقع"
        }
    }

    fun detectLocationFromGps() {
        val context = getApplication<Application>()
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (lm == null) {
            _locationMessage.value = "GPS غير متاح"
            return
        }

        _isDetectingLocation.value = true
        _locationMessage.value = null

        try {
            val isGps = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val isNet = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            var lastLoc: Location? = null
            if (isGps) lastLoc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            if (lastLoc == null && isNet) lastLoc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

            val now = System.currentTimeMillis()
            // If we have a fresh location fix (< 15 mins), use it directly
            if (lastLoc != null && (now - lastLoc.time) < 15 * 60 * 1000L) {
                applyDetectedLocation(lastLoc.latitude, lastLoc.longitude)
            } else {
                val listener = object : LocationListener {
                    override fun onLocationChanged(loc: Location) {
                        applyDetectedLocation(loc.latitude, loc.longitude)
                        try { lm.removeUpdates(this) } catch (e: Exception) {}
                    }
                    override fun onProviderEnabled(p: String) {}
                    override fun onProviderDisabled(p: String) {}
                    override fun onStatusChanged(p: String?, s: Int, e: Bundle?) {}
                }
                if (isNet) lm.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, listener, null)
                else if (isGps) lm.requestSingleUpdate(LocationManager.GPS_PROVIDER, listener, null)
                else if (lastLoc != null) {
                    applyDetectedLocation(lastLoc.latitude, lastLoc.longitude)
                } else {
                    _isDetectingLocation.value = false
                    _locationMessage.value = "يرجى تفعيل خدمة الموقع في الهاتف"
                }
            }
        } catch (e: SecurityException) {
            _isDetectingLocation.value = false
            _locationMessage.value = "إذن الموقع غير ممنوح"
        }
    }

    private fun applyDetectedLocation(lat: Double, lng: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            var resolvedCity = ""
            var resolvedMethod = settings.value.calcMethod
            var resolvedTz = TimeZone.getDefault().id

            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses = geocoder.getFromLocation(lat, lng, 1)
                if (!addresses.isNullOrEmpty()) {
                    val addr = addresses[0]
                    val city = addr.locality ?: addr.subAdminArea ?: addr.adminArea
                    val country = addr.countryName
                    if (!city.isNullOrBlank() && !country.isNullOrBlank()) {
                        resolvedCity = "$city، $country"
                    } else if (!city.isNullOrBlank()) {
                        resolvedCity = city
                    } else if (!country.isNullOrBlank()) {
                        resolvedCity = country
                    }

                    val cc = addr.countryCode?.uppercase(Locale.US) ?: ""
                    resolvedMethod = when (cc) {
                        "EG" -> "EGYPT"
                        "SA", "YE", "OM", "BH" -> "UMM_AL_QURA"
                        "AE" -> "DUBAI"
                        "QA" -> "QATAR"
                        "KW" -> "KUWAIT"
                        "TR" -> "DIYANET"
                        "PK", "IN", "BD", "AF" -> "KARACHI"
                        "US", "CA" -> "ISNA"
                        "FR" -> "FRANCE"
                        "RU" -> "RUSSIA"
                        "SG", "MY", "ID" -> "MUIS"
                        "MA", "DZ", "TN", "LY", "JO", "PS", "SY", "LB", "IQ" -> "MWL"
                        else -> settings.value.calcMethod
                    }
                }
            } catch (e: Exception) {
                // Geocoder error, fallback
            }

            // Fallback to closest preset city if Geocoder didn't return a name
            val closest = PrayerTimesCalculator.findClosestPreset(lat, lng)
            if (resolvedCity.isBlank()) {
                if (closest != null) {
                    resolvedCity = closest.nameAr
                    resolvedMethod = closest.method
                    resolvedTz = closest.tz
                } else {
                    resolvedCity = "الموقع الحالي (${String.format(Locale.US, "%.2f", lat)}, ${String.format(Locale.US, "%.2f", lng)})"
                }
            }

            repository.updateSettings(
                settings.value.copy(
                    locationMode = "AUTO",
                    cityName = resolvedCity,
                    latitude = lat,
                    longitude = lng,
                    timezoneId = resolvedTz,
                    calcMethod = resolvedMethod,
                    dstMode = -1 // Auto DST
                )
            )
            _isDetectingLocation.value = false
            _locationMessage.value = "تم تحديد الموقع: $resolvedCity"
        }
    }

    fun setTimezone(tzId: String) {
        viewModelScope.launch {
            repository.updateSettings(settings.value.copy(timezoneId = tzId))
        }
    }

    fun setDstMode(mode: Int) {
        viewModelScope.launch {
            repository.updateSettings(settings.value.copy(dstMode = mode))
        }
    }

    fun setCalculationMethod(method: String) {
        viewModelScope.launch {
            repository.updateSettings(settings.value.copy(calcMethod = method))
        }
    }

    fun setAsrMadhab(madhab: String) {
        viewModelScope.launch {
            repository.updateSettings(settings.value.copy(asrMadhab = madhab))
        }
    }

    fun updateManualAdjustment(prayer: String, deltaMinutes: Int) {
        viewModelScope.launch {
            val s = settings.value
            val newSettings = when (prayer) {
                "FAJR" -> s.copy(adjFajr = (s.adjFajr + deltaMinutes).coerceIn(-60, 60))
                "SUNRISE" -> s.copy(adjSunrise = (s.adjSunrise + deltaMinutes).coerceIn(-60, 60))
                "DHUHR" -> s.copy(adjDhuhr = (s.adjDhuhr + deltaMinutes).coerceIn(-60, 60))
                "ASR" -> s.copy(adjAsr = (s.adjAsr + deltaMinutes).coerceIn(-60, 60))
                "MAGHRIB" -> s.copy(adjMaghrib = (s.adjMaghrib + deltaMinutes).coerceIn(-60, 60))
                "ISHA" -> s.copy(adjIsha = (s.adjIsha + deltaMinutes).coerceIn(-60, 60))
                else -> s
            }
            repository.updateSettings(newSettings)
        }
    }

    fun setTimeFormat24(is24: Boolean) {
        viewModelScope.launch {
            repository.updateSettings(settings.value.copy(timeFormat24 = is24))
        }
    }

    fun adjustHijriDays(delta: Int) {
        viewModelScope.launch {
            val s = settings.value
            repository.updateSettings(s.copy(hijriAdjustmentDays = (s.hijriAdjustmentDays + delta).coerceIn(-5, 5)))
        }
    }

    // Alerts CRUD
    fun addAlert(
        targetPrayer: String,
        minutesBefore: Int,
        repeatDays: String,
        ringtoneUri: String?,
        ringtoneName: String
    ) {
        viewModelScope.launch {
            repository.insertAlert(
                PrayerAlertEntity(
                    prayerTarget = targetPrayer,
                    minutesBefore = minutesBefore,
                    repeatDays = repeatDays,
                    ringtoneUri = ringtoneUri,
                    ringtoneName = ringtoneName,
                    isEnabled = true
                )
            )
        }
    }

    fun toggleAlert(alert: PrayerAlertEntity) {
        viewModelScope.launch {
            repository.updateAlert(alert.copy(isEnabled = !alert.isEnabled))
        }
    }

    fun deleteAlert(alert: PrayerAlertEntity) {
        viewModelScope.launch {
            repository.deleteAlert(alert)
        }
    }

    // Adhan Customization
    fun setPrayerAdhanAudio(prayerId: String, uri: String?) {
        viewModelScope.launch {
            val s = settings.value
            val updated = when (prayerId) {
                "FAJR" -> s.copy(adhanAudioFajr = uri)
                "DHUHR" -> s.copy(adhanAudioDhuhr = uri)
                "ASR" -> s.copy(adhanAudioAsr = uri)
                "MAGHRIB" -> s.copy(adhanAudioMaghrib = uri)
                "ISHA" -> s.copy(adhanAudioIsha = uri)
                "JUMUAH" -> s.copy(adhanAudioJumuah = uri)
                else -> s
            }
            repository.updateSettings(updated)
        }
    }

    fun setPreAdhanSound(prayerId: String, uri: String?) {
        viewModelScope.launch {
            val s = settings.value
            val updated = when (prayerId) {
                "FAJR" -> s.copy(preAdhanSoundFajr = uri)
                "DHUHR" -> s.copy(preAdhanSoundDhuhr = uri)
                "ASR" -> s.copy(preAdhanSoundAsr = uri)
                "MAGHRIB" -> s.copy(preAdhanSoundMaghrib = uri)
                "ISHA" -> s.copy(preAdhanSoundIsha = uri)
                "JUMUAH" -> s.copy(preAdhanSoundJumuah = uri)
                else -> s
            }
            repository.updateSettings(updated)
        }
    }

    fun setDuaVideo(prayerId: String, uri: String?) {
        viewModelScope.launch {
            val s = settings.value
            val updated = when (prayerId) {
                "FAJR" -> s.copy(duaVideoFajr = uri)
                "DHUHR" -> s.copy(duaVideoDhuhr = uri)
                "ASR" -> s.copy(duaVideoAsr = uri)
                "MAGHRIB" -> s.copy(duaVideoMaghrib = uri)
                "ISHA" -> s.copy(duaVideoIsha = uri)
                "JUMUAH" -> s.copy(duaVideoJumuah = uri)
                else -> s
            }
            repository.updateSettings(updated)
        }
    }

    fun setAutoPlayDuaAfterAdhan(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateSettings(settings.value.copy(autoPlayDuaAfterAdhan = enabled))
        }
    }

    fun setDuaVideoFillScreen(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateSettings(settings.value.copy(duaVideoFillScreen = enabled))
        }
    }

    fun importAdhanScreenZip(uri: Uri) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val files = ZipExtractor.extractZipFromUri(context, uri, "adhan_screen_images")
            if (files.isNotEmpty()) {
                repository.updateSettings(settings.value.copy(adhanScreenImagesDir = "adhan_screen_images"))
            }
        }
    }

    fun setAdhanScreenDisplayMode(mode: String, durationSec: Int) {
        viewModelScope.launch {
            repository.updateSettings(
                settings.value.copy(
                    adhanScreenMode = mode,
                    adhanSlideDurationSec = durationSec
                )
            )
        }
    }

    // Ramadan
    fun setRamadanCannonVideo(uri: String?) {
        viewModelScope.launch {
            repository.updateSettings(settings.value.copy(ramadanCannonVideoUri = uri))
        }
    }

    fun setMesaharatyVideo(uri: String?) {
        viewModelScope.launch {
            repository.updateSettings(settings.value.copy(mesaharatyVideoUri = uri))
        }
    }

    fun setMesaharatyConfig(mode: String, fixedTime: String, beforeFajrMinutes: Int) {
        viewModelScope.launch {
            repository.updateSettings(
                settings.value.copy(
                    mesaharatyMode = mode,
                    mesaharatyFixedTime = fixedTime,
                    mesaharatyBeforeFajrMinutes = beforeFajrMinutes
                )
            )
        }
    }

    // Salawat
    fun importSalawatZip(uri: Uri) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val files = ZipExtractor.extractZipFromUri(context, uri, "salawat_audios")
            if (files.isNotEmpty()) {
                repository.updateSettings(settings.value.copy(salawatAudioDir = "salawat_audios"))
            }
        }
    }

    fun updateSalawatSettings(intervalMinutes: Int, mode: String, specificIndex: Int, enabled: Boolean) {
        viewModelScope.launch {
            val intervalChanged = settings.value.salawatIntervalMinutes != intervalMinutes
            val enabledChanged = settings.value.salawatEnabled != enabled
            val forceReset = intervalChanged || enabledChanged

            val s = settings.value.copy(
                salawatIntervalMinutes = intervalMinutes,
                salawatSelectionMode = mode,
                salawatSpecificSoundIndex = specificIndex,
                salawatEnabled = enabled
            )
            repository.updateSettings(s)
            AlarmScheduler.scheduleSalawat(getApplication(), s, forceReset = forceReset)
        }
    }

    fun playSalawatNow() {
        val context = getApplication<Application>()
        com.example.service.SalawatAudioService.start(context)
        AlarmScheduler.scheduleSalawat(context, settings.value, forceReset = true)
    }

    fun playAudioPreview(uri: String?) {
        AudioPlayerHelper.playAudioUri(getApplication(), uri)
    }

    fun stopAudioPreview() {
        AudioPlayerHelper.stopAudio()
    }

    // Notification Bar & Widget Settings
    fun setNotificationBarEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateSettings(settings.value.copy(notificationBarEnabled = enabled))
            val ctx = getApplication<Application>()
            if (enabled) {
                NotificationHelper.updateOngoingPrayerNotification(ctx)
            } else {
                NotificationHelper.cancelOngoingNotification(ctx)
            }
        }
    }

    fun setNotificationBarShowSeconds(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateSettings(settings.value.copy(notificationBarShowSeconds = enabled))
            val ctx = getApplication<Application>()
            NotificationHelper.updateOngoingPrayerNotification(ctx)
            PrayerWidgetHelper.updateAllWidgets(ctx)
        }
    }

    fun setNotificationBarShowSalawat(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateSettings(settings.value.copy(notificationBarShowSalawat = enabled))
            val ctx = getApplication<Application>()
            NotificationHelper.updateOngoingPrayerNotification(ctx)
            PrayerWidgetHelper.updateAllWidgets(ctx)
        }
    }

    fun setWidgetShowSeconds(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateSettings(settings.value.copy(widgetShowSeconds = enabled))
            val ctx = getApplication<Application>()
            PrayerWidgetHelper.updateAllWidgets(ctx)
            NotificationHelper.updateOngoingPrayerNotification(ctx)
        }
    }

    fun setWidgetThemeStyle(themeStyle: String) {
        viewModelScope.launch {
            repository.updateSettings(settings.value.copy(widgetThemeStyle = themeStyle))
            val ctx = getApplication<Application>()
            PrayerWidgetHelper.updateAllWidgets(ctx)
            NotificationHelper.updateOngoingPrayerNotification(ctx)
        }
    }

    fun setDarkMode(isDark: Boolean) {
        viewModelScope.launch {
            repository.updateSettings(settings.value.copy(isDarkMode = isDark))
        }
    }

    fun setPreAdhanAlertsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateSettings(settings.value.copy(preAdhanAlertsEnabled = enabled))
            AlarmScheduler.scheduleAll(getApplication())
        }
    }

    fun setTimeAlertsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateSettings(settings.value.copy(timeAlertsEnabled = enabled))
            val ctx = getApplication<Application>()
            if (enabled) {
                NotificationHelper.updateOngoingPrayerNotification(ctx)
            } else {
                NotificationHelper.cancelOngoingNotification(ctx)
            }
        }
    }

    fun setAdhanSoundEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateSettings(settings.value.copy(adhanSoundEnabled = enabled))
        }
    }

    fun setRamadanCannonEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateSettings(settings.value.copy(ramadanCannonEnabled = enabled))
        }
    }

    fun setMesaharatyEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateSettings(settings.value.copy(mesaharatyEnabled = enabled))
            AlarmScheduler.scheduleAll(getApplication())
        }
    }

    fun updateAlert(alert: PrayerAlertEntity) {
        viewModelScope.launch {
            repository.updateAlert(alert)
            AlarmScheduler.scheduleAll(getApplication())
        }
    }

    fun refreshNotificationAndWidgets() {
        val ctx = getApplication<Application>()
        NotificationHelper.updateOngoingPrayerNotification(ctx)
        PrayerWidgetHelper.updateAllWidgets(ctx)
    }
}
