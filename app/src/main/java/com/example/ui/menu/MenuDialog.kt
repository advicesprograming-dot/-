package com.example.ui.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.AppSettingsEntity
import com.example.data.local.PrayerAlertEntity
import com.example.ui.PrayerViewModel
import com.example.ui.tabs.*
import com.example.ui.theme.IslamicGold
import com.example.ui.theme.MosqueEmeraldDark
import com.example.util.AppStrings

data class MenuTabItem(
    val id: Int,
    val titleKey: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuDialog(
    viewModel: PrayerViewModel,
    settings: AppSettingsEntity,
    alerts: List<PrayerAlertEntity>,
    salawatCountdownMillis: Long,
    isDetectingLocation: Boolean,
    locationMessage: String?,
    onDismiss: () -> Unit
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val lang = settings.language

    val tabs = remember {
        listOf(
            MenuTabItem(0, "tab_language", Icons.Default.Language),
            MenuTabItem(1, "tab_location_calc", Icons.Default.Explore),
            MenuTabItem(2, "tab_alerts", Icons.Default.NotificationsActive),
            MenuTabItem(3, "tab_adhan", Icons.Default.VolumeUp),
            MenuTabItem(4, "tab_ramadan", Icons.Default.NightsStay),
            MenuTabItem(5, "tab_salawat", Icons.Default.Favorite),
            MenuTabItem(6, "tab_notification_widget", Icons.Default.Widgets)
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Menu, contentDescription = null, tint = IslamicGold, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = AppStrings.get("menu_button", lang) + " • " + AppStrings.get("app_title", lang),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = IslamicGold
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("btn_close_menu")) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }

                // Scrollable Navigation Tabs
                ScrollableTabRow(
                    selectedTabIndex = selectedTabIndex,
                    edgePadding = 12.dp,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    contentColor = IslamicGold
                ) {
                    tabs.forEachIndexed { index, tab ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            icon = { Icon(tab.icon, contentDescription = null, modifier = Modifier.size(20.dp)) },
                            text = {
                                Text(
                                    text = AppStrings.get(tab.titleKey, lang),
                                    fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp
                                )
                            },
                            modifier = Modifier.testTag("tab_${tab.id}")
                        )
                    }
                }

                // Tab Content
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    when (selectedTabIndex) {
                        0 -> LanguageTab(
                            currentLanguage = settings.language,
                            onLanguageSelected = { viewModel.setLanguage(it) },
                            isDarkMode = settings.isDarkMode,
                            onThemeToggle = { viewModel.setDarkMode(it) }
                        )

                        1 -> LocationCalculationTab(
                            settings = settings,
                            isDetectingLocation = isDetectingLocation,
                            locationMessage = locationMessage,
                            onDetectLocation = { viewModel.detectLocationFromGps() },
                            onSelectCityPreset = { viewModel.selectCityPreset(it) },
                            onSaveManualLocation = { city, lat, lng ->
                                viewModel.updateManualLocation(city, lat, lng)
                            },
                            onSetTimezone = { viewModel.setTimezone(it) },
                            onSetDstMode = { viewModel.setDstMode(it) },
                            onSetCalcMethod = { viewModel.setCalculationMethod(it) },
                            onSetAsrMadhab = { viewModel.setAsrMadhab(it) },
                            onUpdateAdjustment = { prayer, delta ->
                                viewModel.updateManualAdjustment(prayer, delta)
                            },
                            onSetTimeFormat24 = { viewModel.setTimeFormat24(it) },
                            onAdjustHijriDays = { viewModel.adjustHijriDays(it) }
                        )

                        2 -> AlertsTab(
                            alerts = alerts,
                            language = settings.language,
                            preAdhanAlertsEnabled = settings.preAdhanAlertsEnabled,
                            onSetPreAdhanAlertsEnabled = { viewModel.setPreAdhanAlertsEnabled(it) },
                            onAddAlert = { target, min, days, uri, name ->
                                viewModel.addAlert(target, min, days, uri, name)
                            },
                            onUpdateAlert = { viewModel.updateAlert(it) },
                            onToggleAlert = { viewModel.toggleAlert(it) },
                            onDeleteAlert = { viewModel.deleteAlert(it) }
                        )

                        3 -> AdhanTab(
                            settings = settings,
                            adhanSoundEnabled = settings.adhanSoundEnabled,
                            onSetAdhanSoundEnabled = { viewModel.setAdhanSoundEnabled(it) },
                            onSetAdhanAudio = { prayer, uri -> viewModel.setPrayerAdhanAudio(prayer, uri) },
                            onSetPreAdhanSound = { prayer, uri -> viewModel.setPreAdhanSound(prayer, uri) },
                            onSetDuaVideo = { prayer, uri -> viewModel.setDuaVideo(prayer, uri) },
                            onImportAdhanZip = { uri -> viewModel.importAdhanScreenZip(uri) },
                            onSetAdhanDisplayMode = { mode, dur -> viewModel.setAdhanScreenDisplayMode(mode, dur) },
                            onPreviewAudio = { uri -> viewModel.playAudioPreview(uri) },
                            onStopAudio = { viewModel.stopAudioPreview() },
                            onSetAutoPlayDua = { viewModel.setAutoPlayDuaAfterAdhan(it) },
                            onSetDuaVideoFillScreen = { viewModel.setDuaVideoFillScreen(it) }
                        )

                        4 -> RamadanTab(
                            settings = settings,
                            ramadanCannonEnabled = settings.ramadanCannonEnabled,
                            mesaharatyEnabled = settings.mesaharatyEnabled,
                            onSetRamadanCannonEnabled = { viewModel.setRamadanCannonEnabled(it) },
                            onSetMesaharatyEnabled = { viewModel.setMesaharatyEnabled(it) },
                            onSetCannonVideo = { viewModel.setRamadanCannonVideo(it) },
                            onSetMesaharatyVideo = { viewModel.setMesaharatyVideo(it) },
                            onSetMesaharatyConfig = { mode, fixed, min ->
                                viewModel.setMesaharatyConfig(mode, fixed, min)
                            }
                        )

                        5 -> SalawatTab(
                            settings = settings,
                            salawatCountdownMillis = salawatCountdownMillis,
                            onImportSalawatZip = { viewModel.importSalawatZip(it) },
                            onUpdateSalawatSettings = { interval, mode, idx, enabled ->
                                viewModel.updateSalawatSettings(interval, mode, idx, enabled)
                            },
                            onPlaySalawatNow = { viewModel.playSalawatNow() }
                        )

                        6 -> NotificationWidgetTab(
                            viewModel = viewModel,
                            settings = settings,
                            lang = lang
                        )
                    }
                }
            }
        }
    }
}
