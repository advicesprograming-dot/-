package com.example.ui.tabs

import android.content.Context
import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AppSettingsEntity
import com.example.ui.adhan.AdhanScreenActivity
import com.example.ui.adhan.DuaVideoActivity
import com.example.ui.theme.IslamicGold
import com.example.ui.theme.DigitalGreenLed
import com.example.util.AppStrings
import com.example.util.AudioPlayerHelper
import com.example.util.FileStorageHelper
import com.example.util.ZipExtractor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdhanTab(
    settings: AppSettingsEntity,
    adhanSoundEnabled: Boolean,
    onSetAdhanSoundEnabled: (Boolean) -> Unit,
    onSetAdhanAudio: (String, String?) -> Unit,
    onSetPreAdhanSound: (String, String?) -> Unit,
    onSetDuaVideo: (String, String?) -> Unit,
    onImportAdhanZip: (Uri) -> Unit,
    onSetAdhanDisplayMode: (String, Int) -> Unit,
    onPreviewAudio: (String?) -> Unit,
    onStopAudio: () -> Unit,
    onSetAutoPlayDua: (Boolean) -> Unit = {},
    onSetDuaVideoFillScreen: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val lang = settings.language
    val scrollState = rememberScrollState()

    var activePrayerTab by remember { mutableStateOf("FAJR") }

    val prayers = listOf(
        Pair("FAJR", AppStrings.get("fajr", lang)),
        Pair("DHUHR", AppStrings.get("dhuhr", lang)),
        Pair("ASR", AppStrings.get("asr", lang)),
        Pair("MAGHRIB", AppStrings.get("maghrib", lang)),
        Pair("ISHA", AppStrings.get("isha", lang)),
        Pair("JUMUAH", AppStrings.get("jumuah", lang))
    )

    val currentAdhanAudioUri = when (activePrayerTab) {
        "FAJR" -> settings.adhanAudioFajr
        "DHUHR" -> settings.adhanAudioDhuhr
        "ASR" -> settings.adhanAudioAsr
        "MAGHRIB" -> settings.adhanAudioMaghrib
        "ISHA" -> settings.adhanAudioIsha
        "JUMUAH" -> settings.adhanAudioJumuah
        else -> null
    }

    val currentPreAdhanUri = when (activePrayerTab) {
        "FAJR" -> settings.preAdhanSoundFajr
        "DHUHR" -> settings.preAdhanSoundDhuhr
        "ASR" -> settings.preAdhanSoundAsr
        "MAGHRIB" -> settings.preAdhanSoundMaghrib
        "ISHA" -> settings.preAdhanSoundIsha
        "JUMUAH" -> settings.preAdhanSoundJumuah
        else -> null
    }

    val currentDuaVideoUri = when (activePrayerTab) {
        "FAJR" -> settings.duaVideoFajr
        "DHUHR" -> settings.duaVideoDhuhr
        "ASR" -> settings.duaVideoAsr
        "MAGHRIB" -> settings.duaVideoMaghrib
        "ISHA" -> settings.duaVideoIsha
        "JUMUAH" -> settings.duaVideoJumuah
        else -> null
    }

    val adhanAudioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val savedPath = FileStorageHelper.saveUriToInternalStorage(context, it, "adhan_audio", "adhan_${activePrayerTab.lowercase()}")
            onSetAdhanAudio(activePrayerTab, savedPath)
        }
    }

    val preSoundPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val savedPath = FileStorageHelper.saveUriToInternalStorage(context, it, "pre_sound", "pre_${activePrayerTab.lowercase()}")
            onSetPreAdhanSound(activePrayerTab, savedPath)
        }
    }

    val preSoundRingtonePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.getParcelableExtra<Uri>(android.media.RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            if (uri != null) {
                onSetPreAdhanSound(activePrayerTab, uri.toString())
            }
        }
    }

    val duaVideoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val savedPath = FileStorageHelper.saveUriToInternalStorage(context, it, "dua_videos", "dua_${activePrayerTab.lowercase()}")
            onSetDuaVideo(activePrayerTab, savedPath)
        }
    }

    val zipPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onImportAdhanZip(it) }
    }

    val extractedImages = remember(settings.adhanScreenImagesDir) {
        ZipExtractor.getExtractedFiles(context, "adhan_screen_images")
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Master Toggle Card for Adhan Sound
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "تفعيل صوت الأذان",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = IslamicGold
                    )
                    Text(
                        text = "تشغيل أو إيقاف صوت الأذان عند حلول وقت الصلاة",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = adhanSoundEnabled,
                    onCheckedChange = onSetAdhanSoundEnabled,
                    colors = SwitchDefaults.colors(checkedThumbColor = DigitalGreenLed)
                )
            }
        }
        // Prayer Selector Tabs
        ScrollableTabRow(
            selectedTabIndex = prayers.indexOfFirst { it.first == activePrayerTab }.coerceAtLeast(0),
            edgePadding = 8.dp,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            prayers.forEach { (code, name) ->
                Tab(
                    selected = activePrayerTab == code,
                    onClick = { activePrayerTab = code },
                    text = { Text(name, fontWeight = FontWeight.Bold) }
                )
            }
        }

        // Section 1: Adhan Audio
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VolumeUp, contentDescription = null, tint = IslamicGold)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "${AppStrings.get("adhan_audio_section", lang)} (${prayers.firstOrNull { it.first == activePrayerTab }?.second})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = IslamicGold
                    )
                }

                Text(
                    text = AppStrings.get("adhan_audio_desc", lang),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "الملف الحالي: ${currentAdhanAudioUri ?: "الأذان المدمج الافتراضي"}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { adhanAudioPicker.launch("audio/*") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text(AppStrings.get("pick_audio_file", lang))
                    }

                    OutlinedButton(
                        onClick = { onPreviewAudio(currentAdhanAudioUri) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text(AppStrings.get("preview_audio", lang))
                    }
                }
            }
        }

        // Section 2: Time Alert Sound (Pre-chime)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = IslamicGold)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = AppStrings.get("time_alert_section", lang),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = IslamicGold
                    )
                }

                Text(
                    text = AppStrings.get("time_alert_desc", lang),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "صوت التنبيه بالوقت: ${currentPreAdhanUri ?: "نغمة المسجد الإلكتروني الافتراضية"}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(android.media.RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_TYPE, android.media.RingtoneManager.TYPE_ALARM or android.media.RingtoneManager.TYPE_NOTIFICATION)
                                putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                            }
                            preSoundRingtonePicker.launch(intent)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("نغمة الهاتف", fontSize = 11.sp)
                    }

                    Button(
                        onClick = { preSoundPicker.launch("audio/*") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("ملف صوتي", fontSize = 11.sp)
                    }
                }
                
                OutlinedButton(
                    onClick = { 
                        if (!currentPreAdhanUri.isNullOrBlank()) {
                            AudioPlayerHelper.playAudioUri(context, currentPreAdhanUri)
                        } else {
                            AudioPlayerHelper.playTimeChime() 
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text(AppStrings.get("preview_audio", lang))
                }
            }
        }

        // Section 3: Du'a Video After Adhan
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = IslamicGold)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = AppStrings.get("dua_section", lang),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = IslamicGold
                    )
                }

                Text(
                    text = AppStrings.get("dua_desc", lang),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "فيديو الدعاء: ${currentDuaVideoUri ?: "دعاء الوسيلة بعد الأذان (مدمج)"}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )

                // 1. Primary Action: Play & Preview in Horizontal Fullscreen
                Button(
                    onClick = {
                        val intent = Intent(context, DuaVideoActivity::class.java).apply {
                            putExtra("EXTRA_PRAYER_ID", activePrayerTab)
                            putExtra("EXTRA_PRAYER_NAME", prayers.firstOrNull { it.first == activePrayerTab }?.second ?: "الصلاة")
                            putExtra("EXTRA_VIDEO_URI", currentDuaVideoUri)
                        }
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = IslamicGold,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_preview_dua_video")
                ) {
                    Icon(Icons.Default.PlayCircle, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("تشغيل ومعاينة فيديو الدعاء (أفقي ملء الشاشة)", fontWeight = FontWeight.Bold)
                }

                // 2. Pick custom video from device
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { duaVideoPicker.launch("video/*") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.VideoFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("اختيار فيديو من الهاتف", fontSize = 12.sp)
                    }

                    if (currentDuaVideoUri != null) {
                        OutlinedButton(
                            onClick = { onSetDuaVideo(activePrayerTab, null) },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // 3. Settings: Auto-play after Adhan & Fullscreen Mode
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = "تشغيل فيديو الدعاء تلقائياً بعد الأذان",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "يعمل بالوضع الأفقي بكامل الشاشة فور انتهاء الأذان",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.autoPlayDuaAfterAdhan,
                        onCheckedChange = { onSetAutoPlayDua(it) },
                        modifier = Modifier.testTag("switch_autoplay_dua")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = "ملء الشاشة بالكامل أفقياً (بدون حواف)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "تمديد الفيديو على الشاشة بأكملها (Edge-to-Edge)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.duaVideoFillScreen,
                        onCheckedChange = { onSetDuaVideoFillScreen(it) },
                        modifier = Modifier.testTag("switch_fill_screen_dua")
                    )
                }
            }
        }

        // Section 4: Adhan Screen (Slideshow / Static from ZIP)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Wallpaper, contentDescription = null, tint = IslamicGold)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = AppStrings.get("adhan_screen_section", lang),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = IslamicGold
                    )
                }

                Text(
                    text = AppStrings.get("adhan_screen_desc", lang),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = { zipPicker.launch("application/zip") },
                    modifier = Modifier.fillMaxWidth().testTag("btn_pick_zip")
                ) {
                    Icon(Icons.Default.FolderZip, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(AppStrings.get("zip_images_picker", lang))
                }

                if (extractedImages.isNotEmpty()) {
                    Text(
                        text = String.format(AppStrings.get("zip_images_loaded", lang), extractedImages.size),
                        style = MaterialTheme.typography.bodyMedium,
                        color = IslamicGold,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = AppStrings.get("display_mode", lang),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = settings.adhanScreenMode == "SLIDESHOW",
                        onClick = { onSetAdhanDisplayMode("SLIDESHOW", settings.adhanSlideDurationSec) },
                        label = { Text(AppStrings.get("mode_slideshow", lang)) }
                    )
                    FilterChip(
                        selected = settings.adhanScreenMode == "STATIC",
                        onClick = { onSetAdhanDisplayMode("STATIC", settings.adhanSlideDurationSec) },
                        label = { Text(AppStrings.get("mode_static", lang)) }
                    )
                }

                if (settings.adhanScreenMode == "SLIDESHOW") {
                    Text(
                        text = AppStrings.get("slide_duration", lang),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )

                    val durations = listOf(3, 5, 10, 15)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        durations.forEach { sec ->
                            FilterChip(
                                selected = settings.adhanSlideDurationSec == sec,
                                onClick = { onSetAdhanDisplayMode("SLIDESHOW", sec) },
                                label = { Text("$sec ثوانٍ") }
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        val intent = Intent(context, AdhanScreenActivity::class.java).apply {
                            putExtra("EXTRA_PRAYER_ID", activePrayerTab)
                            putExtra("EXTRA_PRAYER_NAME", prayers.firstOrNull { it.first == activePrayerTab }?.second ?: "الصلاة")
                            putExtra("EXTRA_IS_ALERT", false)
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth().testTag("btn_preview_adhan_screen"),
                    colors = ButtonDefaults.buttonColors(containerColor = IslamicGold, contentColor = Color.Black)
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(AppStrings.get("preview_screen", lang), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
