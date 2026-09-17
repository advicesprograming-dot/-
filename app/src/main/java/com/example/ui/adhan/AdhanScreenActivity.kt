package com.example.ui.adhan

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.lifecycleScope
import coil.compose.AsyncImage
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.example.util.HijriCalendarHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import com.example.PrayerApplication
import com.example.data.local.AppSettingsEntity
import com.example.service.AdhanAudioService
import com.example.ui.theme.DigitalAmberLed
import com.example.ui.theme.DigitalGreenLed
import com.example.ui.theme.IslamicGold
import com.example.ui.theme.MosqueEmeraldDark
import com.example.ui.theme.MyApplicationTheme
import com.example.util.AudioPlayerHelper
import com.example.util.NotificationHelper
import com.example.util.ZipExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class AdhanScreenActivity : ComponentActivity() {

    private fun closeAndTurnOffScreen() {
        try {
            AdhanAudioService.stop(this)
            AudioPlayerHelper.stopAudio()
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(false)
                setTurnScreenOn(false)
            }
            val lp = window.attributes
            lp.screenBrightness = 0.001f
            window.attributes = lp
        } catch (e: Exception) {
            e.printStackTrace()
        }
        finishAndRemoveTask()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Force hardware display to wake up immediately from deep sleep on Samsung AMOLED & all devices
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
            val wakeLock = pm?.newWakeLock(
                @Suppress("DEPRECATION")
                android.os.PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                        android.os.PowerManager.ACQUIRE_CAUSES_WAKEUP or
                        android.os.PowerManager.ON_AFTER_RELEASE,
                "PrayerApp:AdhanScreenWake"
            )
            wakeLock?.acquire(3000L)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Cutout handling for Samsung Infinity-U display / notches
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        // 3. Ensure activity displays specifically over lock screen and turns screen on without dismissing the lock screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
        )

        val rawPrayerId = intent.getStringExtra("EXTRA_PRAYER_ID") ?: "FAJR"
        val rawPrayerName = intent.getStringExtra("EXTRA_PRAYER_NAME") ?: "الصلاة"
        val isAlert = intent.getBooleanExtra("EXTRA_IS_ALERT", false)
        val minutesRemaining = intent.getIntExtra("EXTRA_MINUTES_REMAINING", 15)
        val directVideoUri = intent.getStringExtra("EXTRA_VIDEO_URI")

        val formattedPrayerName = NotificationHelper.getFullPrayerName(rawPrayerName)

        fun launchDuaVideo() {
            lifecycleScope.launch(Dispatchers.Main) {
                val s = withContext(Dispatchers.IO) {
                    PrayerApplication.instance.database.settingsDao().getSettingsDirect()
                }
                val videoUri = when {
                    !directVideoUri.isNullOrBlank() -> directVideoUri
                    rawPrayerId == "MAGHRIB" && s?.ramadanCannonEnabled == true && !s.ramadanCannonVideoUri.isNullOrBlank() -> s.ramadanCannonVideoUri
                    rawPrayerId == "MESAHARATY" -> s?.mesaharatyVideoUri
                    else -> when (rawPrayerId) {
                        "FAJR" -> s?.duaVideoFajr
                        "DHUHR" -> s?.duaVideoDhuhr
                        "ASR" -> s?.duaVideoAsr
                        "MAGHRIB" -> s?.duaVideoMaghrib
                        "ISHA" -> s?.duaVideoIsha
                        "JUMUAH" -> s?.duaVideoJumuah
                        else -> null
                    }
                }
                val intent = Intent(this@AdhanScreenActivity, DuaVideoActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
                    putExtra("EXTRA_PRAYER_ID", rawPrayerId)
                    putExtra("EXTRA_PRAYER_NAME", formattedPrayerName)
                    putExtra("EXTRA_VIDEO_URI", videoUri)
                }
                try {
                    startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                finish()
            }
        }

        // If pre-alert, allow sufficient time for user to read alert after sound finishes
        if (isAlert) {
            AudioPlayerHelper.onAlertCompletedListener = {
                runOnUiThread {
                    window.decorView.postDelayed({
                        if (!isFinishing && !isDestroyed) {
                            closeAndTurnOffScreen()
                        }
                    }, 8000L)
                }
            }
        } else {
            // When Adhan finishes, transition automatically to horizontal fullscreen Dua video
            AudioPlayerHelper.onAlertCompletedListener = {
                runOnUiThread {
                    launchDuaVideo()
                }
            }
        }

        setContent {
            MyApplicationTheme(darkTheme = true) {
                AdhanScreenContent(
                    prayerId = rawPrayerId,
                    prayerName = formattedPrayerName,
                    isAlert = isAlert,
                    minutesRemaining = minutesRemaining,
                    onPlayDuaVideo = {
                        launchDuaVideo()
                    },
                    onDismiss = {
                        closeAndTurnOffScreen()
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        AudioPlayerHelper.onAlertCompletedListener = null
        // AudioPlayerHelper.stopAudio() // Removed to prevent interrupting Dua audio
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AdhanScreenContent(
    prayerId: String,
    prayerName: String,
    isAlert: Boolean,
    minutesRemaining: Int,
    onPlayDuaVideo: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var settings by remember { mutableStateOf(AppSettingsEntity()) }

    LaunchedEffect(Unit) {
        val s = PrayerApplication.instance.database.settingsDao().getSettingsDirect()
        if (s != null) settings = s
    }

    val images = remember(settings.adhanScreenImagesDir) {
        ZipExtractor.getExtractedFiles(context, "adhan_screen_images")
    }

    var currentImageIndex by remember { mutableStateOf(0) }

    // Smooth slideshow rotation between zip images every 3.5 seconds
    LaunchedEffect(images) {
        if (images.size > 1) {
            while (true) {
                delay(3500L)
                currentImageIndex = (currentImageIndex + 1) % images.size
            }
        }
    }

    // Auto-detect when Adhan audio finishes to automatically transition to Dua video
    LaunchedEffect(isAlert) {
        if (!isAlert) {
            var waitCount = 0
            while (!AudioPlayerHelper.isPlaying() && waitCount < 10) {
                delay(100)
                waitCount++
            }
            if (AudioPlayerHelper.isPlaying()) {
                while (true) {
                    if (!AudioPlayerHelper.isPlaying()) {
                        // Check again after 1.5 seconds to ensure it's not just transitioning
                        delay(1500)
                        if (!AudioPlayerHelper.isPlaying()) {
                            break
                        }
                    }
                    delay(250)
                }
                // Adhan audio ended -> launch post-Adhan Dua video automatically
                onPlayDuaVideo()
            } else {
                // If phone is muted or no audio playing, display images for 15s then transition to Dua video
                delay(15000L)
                onPlayDuaVideo()
            }
        } else {
            var waitCount = 0
            while (!AudioPlayerHelper.isPlaying() && waitCount < 10) {
                delay(100)
                waitCount++
            }
            if (AudioPlayerHelper.isPlaying()) {
                while (true) {
                    if (!AudioPlayerHelper.isPlaying()) {
                        delay(6000L) // Stay visible for 6 seconds after sound finishes
                        if (!AudioPlayerHelper.isPlaying()) {
                            break
                        }
                    }
                    delay(250)
                }
                onDismiss()
            } else {
                delay(20000L) // Stay visible for 20 seconds if muted or until user dismisses
                onDismiss()
            }
        }
    }

    // Clean up prayer name so it displays "أذان صلاة الظهر" or "أذان الظهر"
    val cleanPrayerName = remember(prayerName) {
        prayerName.removePrefix("أذان").trim()
    }
    val titleText = remember(cleanPrayerName, isAlert) {
        if (isAlert) "تنبيه $cleanPrayerName" else "أذان $cleanPrayerName"
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // 1. FULLSCREEN ZIP IMAGES
            if (images.isNotEmpty()) {
                val activeImage = images[currentImageIndex.coerceIn(0, images.size - 1)]
                AnimatedContent(
                    targetState = activeImage,
                    transitionSpec = { fadeIn(tween(800)) with fadeOut(tween(800)) },
                    label = "zip_image_crossfade",
                    modifier = Modifier.fillMaxSize()
                ) { imgFile ->
                    AsyncImage(
                        model = imgFile,
                        contentDescription = "صورة الأذان",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                // Deep atmospheric dark canvas if no zip images imported yet
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFF021B14),
                                    Color(0xFF042B20),
                                    Color(0xFF010E0A)
                                )
                            )
                        )
                )
            }

            // Top soft gradient scrim for high contrast readability of the top label
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.75f), Color.Transparent)
                        )
                    )
            )

            // 3. CENTERED ALERT MESSAGE WHEN isAlert == true
            if (isAlert) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = Color.Black.copy(alpha = 0.8f),
                        border = androidx.compose.foundation.BorderStroke(2.dp, IslamicGold),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 32.dp, horizontal = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = IslamicGold,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = "يتبقى $minutesRemaining دقيقة على أذان $cleanPrayerName",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                lineHeight = 38.sp
                            )
                        }
                    }
                }
            }

            // 2. TOP LEFT: "أذان + اسم الصلاة" & TOP RIGHT: Discreet Close Button
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(top = 24.dp, start = 18.dp, end = 18.dp)
                ) {
                    // Physical TOP LEFT: "أذان + اسم الصلاة"
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.Black.copy(alpha = 0.65f),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, IslamicGold),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .testTag("adhan_top_left_title")
                    ) {
                        Text(
                            text = titleText,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = IslamicGold,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                        )
                    }

                    // Physical TOP RIGHT: Discreet close button
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                            .size(42.dp)
                            .testTag("btn_close_adhan")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق",
                            tint = Color.White
                        )
                    }

                    // Bottom Center: Discreet skip pill to directly play post-Adhan Dua video
                    if (!isAlert) {
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = Color.Black.copy(alpha = 0.65f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, IslamicGold.copy(alpha = 0.7f)),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .navigationBarsPadding()
                                .padding(bottom = 28.dp)
                                .clickable { onPlayDuaVideo() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayCircle,
                                    contentDescription = null,
                                    tint = IslamicGold,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "دعاء بعد الأذان",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
