package com.example.ui.tabs

import android.content.Context
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
import com.example.util.FileStorageHelper
import com.example.util.HijriCalendarHelper
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RamadanTab(
    settings: AppSettingsEntity,
    ramadanCannonEnabled: Boolean,
    mesaharatyEnabled: Boolean,
    onSetRamadanCannonEnabled: (Boolean) -> Unit,
    onSetMesaharatyEnabled: (Boolean) -> Unit,
    onSetCannonVideo: (String?) -> Unit,
    onSetMesaharatyVideo: (String?) -> Unit,
    onSetMesaharatyConfig: (String, String, Int) -> Unit
) {
    val context = LocalContext.current
    val lang = settings.language
    val scrollState = rememberScrollState()

    val isRamadanNow = remember {
        HijriCalendarHelper.isRamadan(Date(), settings.hijriAdjustmentDays)
    }

    val cannonPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val savedPath = FileStorageHelper.saveUriToInternalStorage(context, it, "ramadan_videos", "cannon")
            onSetCannonVideo(savedPath)
        }
    }

    val mesaharatyPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val savedPath = FileStorageHelper.saveUriToInternalStorage(context, it, "ramadan_videos", "mesaharaty")
            onSetMesaharatyVideo(savedPath)
        }
    }

    var beforeFajrMinutes by remember(settings.mesaharatyBeforeFajrMinutes) {
        mutableStateOf(settings.mesaharatyBeforeFajrMinutes.toString())
    }
    var fixedTimeText by remember(settings.mesaharatyFixedTime) {
        mutableStateOf(settings.mesaharatyFixedTime)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Ramadan status banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.NightsStay, contentDescription = null, tint = IslamicGold, modifier = Modifier.size(32.dp))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (isRamadanNow) "شهر رمضان المبارك (مفعّل)" else "قسم مميزات وإعدادات شهر رمضان المبارك",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = IslamicGold
                    )
                    Text(
                        text = "يعمل مدفع الإفطار وتنبيه المسحراتي بدقة عالية ومزامنة كاملة مع الصلوات",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Toggle Cards for Cannon and Mesaharaty
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "تفعيل مدفع الإفطار", fontWeight = FontWeight.Bold, color = IslamicGold)
                        Text(text = "تشغيل أو إيقاف عرض مدفع الإفطار عند أذان المغرب", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = ramadanCannonEnabled,
                        onCheckedChange = onSetRamadanCannonEnabled,
                        colors = SwitchDefaults.colors(checkedThumbColor = DigitalGreenLed)
                    )
                }

                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "تفعيل تنبيه المسحراتي والسحور", fontWeight = FontWeight.Bold, color = IslamicGold)
                        Text(text = "تشغيل أو إيقاف تنبيه وفيديو المسحراتي وقت السحور", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = mesaharatyEnabled,
                        onCheckedChange = onSetMesaharatyEnabled,
                        colors = SwitchDefaults.colors(checkedThumbColor = DigitalGreenLed)
                    )
                }
            }
        }

        // Section 1: Iftar Cannon Video
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Celebration, contentDescription = null, tint = IslamicGold)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = AppStrings.get("ramadan_cannon_title", lang),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = IslamicGold
                    )
                }

                Text(
                    text = AppStrings.get("ramadan_cannon_desc", lang),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "فيديو المدفع المختار: ${settings.ramadanCannonVideoUri ?: "فيديو مدفع الإفطار المدمج"}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { cannonPicker.launch("video/*") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.VideoFile, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text(AppStrings.get("pick_cannon_video", lang))
                    }

                    OutlinedButton(
                        onClick = {
                            val intent = Intent(context, DuaVideoActivity::class.java).apply {
                                putExtra("EXTRA_PRAYER_ID", "MAGHRIB")
                                putExtra("EXTRA_PRAYER_NAME", "مدفع الإفطار")
                                putExtra("EXTRA_VIDEO_URI", settings.ramadanCannonVideoUri)
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PlayCircle, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text(AppStrings.get("preview_cannon_video", lang))
                    }
                }
            }
        }

        // Section 2: Mesaharaty Video & Timing
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AlarmOn, contentDescription = null, tint = IslamicGold)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = AppStrings.get("mesaharaty_title", lang),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = IslamicGold
                    )
                }

                Text(
                    text = AppStrings.get("mesaharaty_desc", lang),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "فيديو المسحراتي المختار: ${settings.mesaharatyVideoUri ?: "فيديو المسحراتي التراثي المدمج"}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { mesaharatyPicker.launch("video/*") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.VideoFile, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text(AppStrings.get("pick_mesaharaty_video", lang))
                    }

                    OutlinedButton(
                        onClick = {
                            val intent = Intent(context, DuaVideoActivity::class.java).apply {
                                putExtra("EXTRA_PRAYER_ID", "MESAHARATY")
                                putExtra("EXTRA_PRAYER_NAME", "المسحراتي (تنبيه السحور)")
                                putExtra("EXTRA_VIDEO_URI", settings.mesaharatyVideoUri)
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PlayCircle, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text(AppStrings.get("preview_mesaharaty_video", lang))
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                Text(
                    text = AppStrings.get("mesaharaty_timing", lang),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = settings.mesaharatyMode == "BEFORE_FAJR",
                        onClick = {
                            onSetMesaharatyConfig("BEFORE_FAJR", settings.mesaharatyFixedTime, settings.mesaharatyBeforeFajrMinutes)
                        },
                        label = { Text(AppStrings.get("timing_before_fajr", lang)) }
                    )
                    FilterChip(
                        selected = settings.mesaharatyMode == "FIXED_TIME",
                        onClick = {
                            onSetMesaharatyConfig("FIXED_TIME", settings.mesaharatyFixedTime, settings.mesaharatyBeforeFajrMinutes)
                        },
                        label = { Text(AppStrings.get("timing_fixed", lang)) }
                    )
                }

                if (settings.mesaharatyMode == "BEFORE_FAJR") {
                    OutlinedTextField(
                        value = beforeFajrMinutes,
                        onValueChange = {
                            beforeFajrMinutes = it
                            it.toIntOrNull()?.let { min ->
                                onSetMesaharatyConfig("BEFORE_FAJR", settings.mesaharatyFixedTime, min)
                            }
                        },
                        label = { Text(AppStrings.get("before_fajr_val", lang)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    OutlinedTextField(
                        value = fixedTimeText,
                        onValueChange = {
                            fixedTimeText = it
                            onSetMesaharatyConfig("FIXED_TIME", it, settings.mesaharatyBeforeFajrMinutes)
                        },
                        label = { Text("الساعة الثابتة (HH:mm)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
