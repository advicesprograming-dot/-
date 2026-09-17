package com.example.ui.tabs

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AppSettingsEntity
import com.example.ui.theme.DigitalAmberLed
import com.example.ui.theme.DigitalGreenLed
import com.example.ui.theme.IslamicGold
import com.example.util.AppStrings
import com.example.util.ZipExtractor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalawatTab(
    settings: AppSettingsEntity,
    salawatCountdownMillis: Long,
    onImportSalawatZip: (Uri) -> Unit,
    onUpdateSalawatSettings: (Int, String, Int, Boolean) -> Unit,
    onPlaySalawatNow: () -> Unit
) {
    val context = LocalContext.current
    val lang = settings.language
    val scrollState = rememberScrollState()

    val extractedAudios = remember(settings.salawatAudioDir) {
        ZipExtractor.getExtractedFiles(context, "salawat_audios")
    }

    val zipPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onImportSalawatZip(it) }
    }

    val intervalOptions = listOf(1, 5, 10, 15, 20, 30, 45, 60)

    val remainingSec = (salawatCountdownMillis / 1000) % 60
    val remainingMin = (salawatCountdownMillis / (1000 * 60)) % 60
    val remainingHours = salawatCountdownMillis / (1000 * 60 * 60)
    val formattedCountdown = String.format("%02d:%02d:%02d", remainingHours, remainingMin, remainingSec)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Hero / Status & Live Countdown Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, IslamicGold.copy(alpha = 0.6f), RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "ﷺ إِنَّ اللَّهَ وَمَلائِكَتَهُ يُصَلُّونَ عَلَى النَّبِيِّ ﷺ",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = IslamicGold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (settings.salawatEnabled) AppStrings.get("salawat_active", lang) else AppStrings.get("salawat_disabled", lang),
                        fontWeight = FontWeight.Bold,
                        color = if (settings.salawatEnabled) DigitalGreenLed else MaterialTheme.colorScheme.error
                    )
                    Switch(
                        checked = settings.salawatEnabled,
                        onCheckedChange = {
                            onUpdateSalawatSettings(
                                settings.salawatIntervalMinutes,
                                settings.salawatSelectionMode,
                                settings.salawatSpecificSoundIndex,
                                it
                            )
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = DigitalGreenLed)
                    )
                }

                if (settings.salawatEnabled) {
                    Text(
                        text = AppStrings.get("countdown_label", lang),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formattedCountdown,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black,
                        color = DigitalAmberLed,
                        letterSpacing = 2.sp
                    )
                }

                Button(
                    onClick = onPlaySalawatNow,
                    modifier = Modifier.fillMaxWidth().testTag("btn_test_salawat"),
                    colors = ButtonDefaults.buttonColors(containerColor = IslamicGold, contentColor = Color.Black)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(AppStrings.get("test_salawat_now", lang), fontWeight = FontWeight.Bold)
                }
            }
        }

        // Section 1: Audio ZIP files
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FolderZip, contentDescription = null, tint = IslamicGold)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = AppStrings.get("salawat_zip_picker", lang),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = IslamicGold
                    )
                }

                Button(
                    onClick = { zipPicker.launch("application/zip") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.UploadFile, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("اختيار ملف ZIP يحتوي على أصوات التذكير")
                }

                if (extractedAudios.isNotEmpty()) {
                    Text(
                        text = String.format(AppStrings.get("salawat_zip_loaded", lang), extractedAudios.size),
                        style = MaterialTheme.typography.bodyMedium,
                        color = IslamicGold,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = "يعمل النظام حاليًا بالأصوات والتنبيهات المدمجة عالية الجودة، ويمكنك استبدالها بأي ملف ZIP.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Section 2: Intervals (Minutes)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = AppStrings.get("salawat_interval", lang),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = IslamicGold
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    intervalOptions.forEach { m ->
                        FilterChip(
                            selected = settings.salawatIntervalMinutes == m,
                            onClick = {
                                onUpdateSalawatSettings(
                                    m,
                                    settings.salawatSelectionMode,
                                    settings.salawatSpecificSoundIndex,
                                    settings.salawatEnabled
                                )
                            },
                            label = { Text("كل $m ${AppStrings.get("minutes_suffix", lang)}") }
                        )
                    }
                }
            }
        }

        // Section 3: Mode (In Order, Random, Specific)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = AppStrings.get("salawat_mode", lang),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = IslamicGold
                )

                val modes = listOf(
                    Pair("ORDER", AppStrings.get("mode_order", lang)),
                    Pair("RANDOM", AppStrings.get("mode_random", lang)),
                    Pair("SPECIFIC", AppStrings.get("mode_specific", lang))
                )

                modes.forEach { (modeCode, title) ->
                    val isSelected = settings.salawatSelectionMode == modeCode
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                1.dp,
                                if (isSelected) IslamicGold else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                RoundedCornerShape(12.dp)
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = title, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    onUpdateSalawatSettings(
                                        settings.salawatIntervalMinutes,
                                        modeCode,
                                        settings.salawatSpecificSoundIndex,
                                        settings.salawatEnabled
                                    )
                                }
                            )
                        }
                    }
                }

                if (settings.salawatSelectionMode == "SPECIFIC" && extractedAudios.isNotEmpty()) {
                    Text(
                        text = "اختر الصوت المحدد من ملف ZIP:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        extractedAudios.forEachIndexed { index, file ->
                            FilterChip(
                                selected = settings.salawatSpecificSoundIndex == index,
                                onClick = {
                                    onUpdateSalawatSettings(
                                        settings.salawatIntervalMinutes,
                                        "SPECIFIC",
                                        index,
                                        settings.salawatEnabled
                                    )
                                },
                                label = { Text("صوت ${index + 1}: ${file.name}") }
                            )
                        }
                    }
                }
            }
        }
    }
}
