package com.example.ui.tabs

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import com.example.data.local.AppSettingsEntity
import com.example.ui.theme.IslamicGold
import com.example.util.AppStrings
import com.example.util.HijriCalendarHelper
import com.example.util.PrayerTimesCalculator
import java.util.Date
import java.util.TimeZone

data class CityPreset(val nameAr: String, val nameEn: String, val lat: Double, val lng: Double, val tz: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationCalculationTab(
    settings: AppSettingsEntity,
    isDetectingLocation: Boolean,
    locationMessage: String?,
    onDetectLocation: () -> Unit,
    onSaveManualLocation: (String, Double, Double) -> Unit,
    onSetTimezone: (String) -> Unit,
    onSetDstMode: (Int) -> Unit,
    onSetCalcMethod: (String) -> Unit,
    onSetAsrMadhab: (String) -> Unit,
    onUpdateAdjustment: (String, Int) -> Unit,
    onSetTimeFormat24: (Boolean) -> Unit,
    onAdjustHijriDays: (Int) -> Unit
) {
    val lang = settings.language
    val scrollState = rememberScrollState()

    var inputCityName by remember(settings.cityName) { mutableStateOf(settings.cityName) }
    var inputLat by remember(settings.latitude) { mutableStateOf(settings.latitude.toString()) }
    var inputLng by remember(settings.longitude) { mutableStateOf(settings.longitude.toString()) }

    val presets = remember {
        listOf(
            CityPreset("مكة المكرمة", "Makkah", 21.4225, 39.8262, "Asia/Riyadh"),
            CityPreset("المدينة المنورة", "Madinah", 24.4672, 39.6111, "Asia/Riyadh"),
            CityPreset("الرياض", "Riyadh", 24.7136, 46.6753, "Asia/Riyadh"),
            CityPreset("القاهرة", "Cairo", 30.0444, 31.2357, "Africa/Cairo"),
            CityPreset("الإسكندرية", "Alexandria", 31.2001, 29.9187, "Africa/Cairo"),
            CityPreset("دبي", "Dubai", 25.2048, 55.2708, "Asia/Dubai"),
            CityPreset("الدوحة", "Doha", 25.2854, 51.5310, "Asia/Qatar"),
            CityPreset("الكويت", "Kuwait City", 29.3759, 47.9774, "Asia/Kuwait"),
            CityPreset("القدس الشريف", "Jerusalem", 31.7683, 35.2137, "Asia/Jerusalem"),
            CityPreset("عمان", "Amman", 31.9454, 35.9284, "Asia/Amman"),
            CityPreset("بغداد", "Baghdad", 33.3152, 44.3661, "Asia/Baghdad"),
            CityPreset("دمشق", "Damascus", 33.5138, 36.2765, "Asia/Damascus"),
            CityPreset("إسطنبول", "Istanbul", 41.0082, 28.9784, "Europe/Istanbul"),
            CityPreset("الرباط", "Rabat", 34.0209, -6.8416, "Africa/Casablanca"),
            CityPreset("الجزائر", "Algiers", 36.7538, 3.0588, "Africa/Algiers"),
            CityPreset("تونس", "Tunis", 36.8065, 10.1815, "Africa/Tunis"),
            CityPreset("لندن", "London", 51.5074, -0.1278, "Europe/London"),
            CityPreset("باريس", "Paris", 48.8566, 2.3522, "Europe/Paris"),
            CityPreset("نيويورك", "New York", 40.7128, -74.0060, "America/New_York")
        )
    }

    val currentHijri = remember(settings.hijriAdjustmentDays) {
        HijriCalendarHelper.getHijriDate(Date(), settings.hijriAdjustmentDays)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. Location Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Place, contentDescription = null, tint = IslamicGold, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = AppStrings.get("location_header", lang),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = IslamicGold
                    )
                }

                // Auto GPS Button
                val locationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->
                    val granted = permissions.getOrDefault(android.Manifest.permission.ACCESS_FINE_LOCATION, false) ||
                            permissions.getOrDefault(android.Manifest.permission.ACCESS_COARSE_LOCATION, false)
                    if (granted) {
                        onDetectLocation()
                    }
                }

                Button(
                    onClick = {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                android.Manifest.permission.ACCESS_FINE_LOCATION,
                                android.Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth().testTag("btn_auto_location"),
                    enabled = !isDetectingLocation,
                    colors = ButtonDefaults.buttonColors(containerColor = IslamicGold, contentColor = Color.Black)
                ) {
                    if (isDetectingLocation) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black, strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(AppStrings.get("loc_detecting", lang))
                    } else {
                        Icon(Icons.Default.MyLocation, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(AppStrings.get("auto_loc_btn", lang), fontWeight = FontWeight.Bold)
                    }
                }

                locationMessage?.let {
                    Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }

                Divider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                // Presets list
                Text(
                    text = AppStrings.get("presets_label", lang),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.forEach { preset ->
                        FilterChip(
                            selected = settings.cityName.contains(preset.nameAr) || settings.cityName.contains(preset.nameEn),
                            onClick = {
                                inputCityName = if (lang == "ar") preset.nameAr else preset.nameEn
                                inputLat = preset.lat.toString()
                                inputLng = preset.lng.toString()
                                onSaveManualLocation(inputCityName, preset.lat, preset.lng)
                                onSetTimezone(preset.tz)
                            },
                            label = { Text(if (lang == "ar") preset.nameAr else preset.nameEn) }
                        )
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                // Manual Input
                Text(
                    text = AppStrings.get("location_manual", lang),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )

                OutlinedTextField(
                    value = inputCityName,
                    onValueChange = { inputCityName = it },
                    label = { Text(AppStrings.get("city_name", lang)) },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = inputLat,
                        onValueChange = { inputLat = it },
                        label = { Text(AppStrings.get("latitude", lang)) },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = inputLng,
                        onValueChange = { inputLng = it },
                        label = { Text(AppStrings.get("longitude", lang)) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Button(
                    onClick = {
                        val lat = inputLat.toDoubleOrNull() ?: settings.latitude
                        val lng = inputLng.toDoubleOrNull() ?: settings.longitude
                        onSaveManualLocation(inputCityName, lat, lng)
                    },
                    modifier = Modifier.align(Alignment.End).testTag("btn_save_manual_loc")
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(AppStrings.get("save_location", lang))
                }
            }
        }

        // 2. Timezone & DST
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, contentDescription = null, tint = IslamicGold, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = AppStrings.get("timezone_header", lang) + " & " + AppStrings.get("dst_header", lang),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = IslamicGold
                    )
                }

                Text(
                    text = "${AppStrings.get("timezone_header", lang)}: ${settings.timezoneId}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )

                // Timezone quick presets
                val tzList = listOf("Asia/Riyadh", "Africa/Cairo", "Asia/Dubai", "Asia/Jerusalem", "Europe/Istanbul", "Europe/Paris", "GMT")
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tzList.forEach { tz ->
                        FilterChip(
                            selected = settings.timezoneId == tz,
                            onClick = { onSetTimezone(tz) },
                            label = { Text(tz) }
                        )
                    }
                }

                Text(
                    text = AppStrings.get("dst_header", lang),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = settings.dstMode == 0,
                        onClick = { onSetDstMode(0) },
                        label = { Text(AppStrings.get("dst_off", lang)) }
                    )
                    FilterChip(
                        selected = settings.dstMode == 1,
                        onClick = { onSetDstMode(1) },
                        label = { Text(AppStrings.get("dst_on", lang)) }
                    )
                    FilterChip(
                        selected = settings.dstMode == -1,
                        onClick = { onSetDstMode(-1) },
                        label = { Text(AppStrings.get("dst_auto", lang)) }
                    )
                }
            }
        }

        // 3. Calculation Methods (Global recognized conventions)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Calculate, contentDescription = null, tint = IslamicGold, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = AppStrings.get("calc_method_header", lang),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = IslamicGold
                    )
                }

                Text(
                    text = AppStrings.get("calc_method_desc", lang),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                PrayerTimesCalculator.CalculationMethod.entries.forEach { method ->
                    val isSelected = settings.calcMethod == method.code
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) IslamicGold else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onSetCalcMethod(method.code) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (lang == "ar") method.arabicName else method.englishName,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    color = if (isSelected) IslamicGold else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Fajr: ${method.fajrAngle}° | Isha: ${if (method.ishaMinutesAfterMaghrib != null) "+${method.ishaMinutesAfterMaghrib.toInt()}m" else "${method.ishaAngle}°"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isSelected) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = IslamicGold)
                            }
                        }
                    }
                }
            }
        }

        // 4. Asr Madhab
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = IslamicGold, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = AppStrings.get("madhab_header", lang),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = IslamicGold
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PrayerTimesCalculator.AsrMadhab.entries.forEach { madhab ->
                        FilterChip(
                            selected = settings.asrMadhab == madhab.code,
                            onClick = { onSetAsrMadhab(madhab.code) },
                            label = { Text(if (lang == "ar") madhab.arabicName else madhab.englishName) }
                        )
                    }
                }
            }
        }

        // 5. Manual Time Adjustments
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = IslamicGold, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = AppStrings.get("manual_adj_header", lang),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = IslamicGold
                    )
                }

                Text(
                    text = AppStrings.get("manual_adj_desc", lang),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val prayerAdjustments = listOf(
                    Triple("FAJR", AppStrings.get("fajr", lang), settings.adjFajr),
                    Triple("SUNRISE", AppStrings.get("sunrise", lang), settings.adjSunrise),
                    Triple("DHUHR", AppStrings.get("dhuhr", lang), settings.adjDhuhr),
                    Triple("ASR", AppStrings.get("asr", lang), settings.adjAsr),
                    Triple("MAGHRIB", AppStrings.get("maghrib", lang), settings.adjMaghrib),
                    Triple("ISHA", AppStrings.get("isha", lang), settings.adjIsha)
                )

                prayerAdjustments.forEach { (key, name, currentVal) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = name, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            IconButton(onClick = { onUpdateAdjustment(key, -1) }) {
                                Icon(Icons.Default.RemoveCircleOutline, contentDescription = "-1")
                            }
                            Text(
                                text = "${if (currentVal > 0) "+" else ""}$currentVal ${AppStrings.get("minutes_suffix", lang)}",
                                fontWeight = FontWeight.Bold,
                                color = if (currentVal != 0) IslamicGold else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.widthIn(min = 60.dp)
                            )
                            IconButton(onClick = { onUpdateAdjustment(key, 1) }) {
                                Icon(Icons.Default.AddCircleOutline, contentDescription = "+1")
                            }
                        }
                    }
                }
            }
        }

        // 6. Time Format (12h vs 24h)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = AppStrings.get("time_format_header", lang),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = IslamicGold
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilterChip(
                        selected = !settings.timeFormat24,
                        onClick = { onSetTimeFormat24(false) },
                        label = { Text(AppStrings.get("format_12h", lang)) }
                    )
                    FilterChip(
                        selected = settings.timeFormat24,
                        onClick = { onSetTimeFormat24(true) },
                        label = { Text(AppStrings.get("format_24h", lang)) }
                    )
                }
            }
        }

        // 7. Hijri Calendar & Adjustment
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = IslamicGold, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = AppStrings.get("hijri_calendar_header", lang),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = IslamicGold
                    )
                }

                Text(
                    text = when (lang) {
                        "fr" -> currentHijri.formattedFr
                        "en" -> currentHijri.formattedEn
                        else -> currentHijri.formattedAr
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = IslamicGold
                )

                Text(
                    text = AppStrings.get("hijri_adjustment", lang),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onAdjustHijriDays(-1) }) {
                        Icon(Icons.Default.Remove, contentDescription = "-1")
                        Spacer(Modifier.width(4.dp))
                        Text("1 ${AppStrings.get("days_suffix", lang)}")
                    }

                    Text(
                        text = "${if (settings.hijriAdjustmentDays > 0) "+" else ""}${settings.hijriAdjustmentDays} ${AppStrings.get("days_suffix", lang)}",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    Button(onClick = { onAdjustHijriDays(1) }) {
                        Icon(Icons.Default.Add, contentDescription = "+1")
                        Spacer(Modifier.width(4.dp))
                        Text("1 ${AppStrings.get("days_suffix", lang)}")
                    }
                }
            }
        }
    }
}
