package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.content.ContextCompat
import com.example.ui.PrayerViewModel
import com.example.ui.menu.MenuDialog
import com.example.ui.mosque_clock.MosqueClockScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: PrayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val settings by viewModel.settings.collectAsState()
            val alerts by viewModel.alerts.collectAsState()
            val currentCal by viewModel.currentCalendar.collectAsState()
            val schedule by viewModel.schedule.collectAsState()
            val isDetectingLoc by viewModel.isDetectingLocation.collectAsState()
            val locMsg by viewModel.locationMessage.collectAsState()
            val salawatCountdown by viewModel.salawatCountdownMillis.collectAsState()

            var showMenuDialog by remember { mutableStateOf(false) }

            // Dynamic Layout Direction based on selected language
            val layoutDirection = if (settings.language == "ar") {
                LayoutDirection.Rtl
            } else {
                LayoutDirection.Ltr
            }

            // Notification permission request for Android 13+
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { /* granted or denied */ }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
                viewModel.refreshNotificationAndWidgets()
            }

            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                MyApplicationTheme(darkTheme = settings.isDarkMode) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        MosqueClockScreen(
                            settings = settings,
                            schedule = schedule,
                            currentCalendar = currentCal,
                            onOpenMenu = { showMenuDialog = true },
                            onTestSalawat = { viewModel.playSalawatNow() }
                        )

                        if (showMenuDialog) {
                            MenuDialog(
                                viewModel = viewModel,
                                settings = settings,
                                alerts = alerts,
                                salawatCountdownMillis = salawatCountdown,
                                isDetectingLocation = isDetectingLoc,
                                locationMessage = locMsg,
                                onDismiss = { showMenuDialog = false }
                            )
                        }
                    }
                }
            }
        }
    }
}
