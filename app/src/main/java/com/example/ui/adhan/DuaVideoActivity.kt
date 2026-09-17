package com.example.ui.adhan

import android.content.Context
import android.content.pm.ActivityInfo
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.PrayerApplication
import com.example.data.local.AppSettingsEntity
import com.example.ui.theme.DigitalGreenLed
import com.example.ui.theme.IslamicGold
import com.example.ui.theme.MyApplicationTheme
import com.example.util.AudioPlayerHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.io.File

class DuaVideoActivity : ComponentActivity() {

    private fun enableImmersiveLandscape() {
        // Enforce Landscape Orientation safely
        try {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Hardware Screen Wake Lock for AMOLED displays
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
            val wakeLock = pm?.newWakeLock(
                @Suppress("DEPRECATION")
                android.os.PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                        android.os.PowerManager.ACQUIRE_CAUSES_WAKEUP or
                        android.os.PowerManager.ON_AFTER_RELEASE,
                "PrayerApp:DuaVideoWake"
            )
            wakeLock?.acquire(3000L)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Show when locked
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

        // Cutout support for edge-to-edge on Samsung A30s & Infinity displays
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        // Hide system bars completely (Immersive Sticky Fullscreen)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
    }

    private fun closeAndTurnOffScreen() {
        try {
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
        enableImmersiveLandscape()

        val rawPrayerId = intent.getStringExtra("EXTRA_PRAYER_ID") ?: "FAJR"
        val prayerName = intent.getStringExtra("EXTRA_PRAYER_NAME") ?: "الصلاة"
        val explicitVideoUri = intent.getStringExtra("EXTRA_VIDEO_URI")

        setContent {
            MyApplicationTheme(darkTheme = true) {
                DuaVideoScreen(
                    prayerId = rawPrayerId,
                    prayerName = prayerName,
                    explicitVideoUri = explicitVideoUri,
                    onClose = {
                        closeAndTurnOffScreen()
                    }
                )
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            enableImmersiveLandscape()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        AudioPlayerHelper.stopAudio()
    }
}

@Composable
fun DuaVideoScreen(
    prayerId: String,
    prayerName: String,
    explicitVideoUri: String?,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var settings by remember { mutableStateOf(AppSettingsEntity()) }

    LaunchedEffect(Unit) {
        val s = PrayerApplication.instance.database.settingsDao().getSettingsDirect()
        if (s != null) settings = s
    }

    // Determine video URI from explicit argument or database settings
    val resolvedUriString = remember(explicitVideoUri, settings) {
        if (!explicitVideoUri.isNullOrBlank()) {
            explicitVideoUri
        } else {
            when {
                prayerId == "MAGHRIB" && settings.ramadanCannonEnabled && !settings.ramadanCannonVideoUri.isNullOrBlank() -> settings.ramadanCannonVideoUri
                prayerId == "MESAHARATY" -> settings.mesaharatyVideoUri
                else -> when (prayerId) {
                    "FAJR" -> settings.duaVideoFajr
                    "DHUHR" -> settings.duaVideoDhuhr
                    "ASR" -> settings.duaVideoAsr
                    "MAGHRIB" -> settings.duaVideoMaghrib
                    "ISHA" -> settings.duaVideoIsha
                    "JUMUAH" -> settings.duaVideoJumuah
                    else -> null
                }
            }
        }
    }

    // Determine video URI and validate file existence
    val videoUri = remember(resolvedUriString) {
        if (!resolvedUriString.isNullOrBlank()) {
            if (resolvedUriString.startsWith("content://") || resolvedUriString.startsWith("file://")) {
                resolvedUriString
            } else {
                val f = File(resolvedUriString)
                if (f.exists()) resolvedUriString else null
            }
        } else {
            null
        }
    }

    // Toggle for fill screen (100% stretched to edge-to-edge without black bars)
    var isFillScreen by remember { mutableStateOf(settings.duaVideoFillScreen) }
    var isPlaying by remember { mutableStateOf(true) }
    var isControlsVisible by remember { mutableStateOf(true) }
    var videoEnded by remember { mutableStateOf(false) }

    // Reference to video view for controls
    var fullScreenVideoView by remember { mutableStateOf<FullScreenVideoView?>(null) }

    // Auto-hide controls overlay after 3.5 seconds
    LaunchedEffect(isControlsVisible, isPlaying) {
        if (isControlsVisible && isPlaying && !videoEnded) {
            delay(3500L)
            isControlsVisible = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                isControlsVisible = !isControlsVisible
            }
    ) {
        // -------------------------------------------------------------
        // 1. VIDEO LAYER: Custom Video OR Built-in Landscape Cinematic Dua
        // -------------------------------------------------------------
        if (!videoUri.isNullOrBlank()) {
            // Real Video Playback in Fullscreen Landscape
            AndroidView(
                factory = { ctx ->
                    FullScreenVideoView(ctx).apply {
                        this.fillScreen = isFillScreen
                        fullScreenVideoView = this
                        try {
                            val uri = if (videoUri.startsWith("content://") || videoUri.startsWith("file://")) {
                                Uri.parse(videoUri)
                            } else {
                                Uri.fromFile(File(videoUri))
                            }
                            setVideoURI(uri)
                            setOnPreparedListener { mp ->
                                mp.isLooping = false
                                start()
                                isPlaying = true
                            }
                            setOnCompletionListener {
                                isPlaying = false
                                videoEnded = true
                                isControlsVisible = false
                                // Automatically close and turn off screen when video ends
                                postDelayed({
                                    onClose()
                                }, 500)
                            }
                            setOnErrorListener { _, _, _ ->
                                isPlaying = false
                                videoEnded = true
                                onClose()
                                true
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            onClose()
                        }
                    }
                },
                update = { view ->
                    view.fillScreen = isFillScreen
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Built-in Landscape Cinematic Dua Presentation (Fills entire screen horizontally)
            BuiltInCinematicDuaView(
                prayerName = prayerName,
                isPlaying = isPlaying,
                onComplete = {
                    videoEnded = true
                    onClose()
                }
            )
        }

        // -------------------------------------------------------------
        // 2. CONTROLS OVERLAY: Pure & Clean (No text written on video)
        // -------------------------------------------------------------
        AnimatedVisibility(
            visible = isControlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Discreet close button at top-right
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f))
                        .testTag("btn_close_dua_video")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "إغلاق",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

/**
 * Clean landscape presentation when no custom video is uploaded.
 * Audio plays peacefully without any text clutter.
 */
@Composable
private fun BuiltInCinematicDuaView(
    prayerName: String,
    isPlaying: Boolean,
    onComplete: () -> Unit
) {
    var progressSeconds by remember { mutableIntStateOf(0) }
    val totalSeconds = 22

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            AudioPlayerHelper.playDuaMelody()
            while (progressSeconds < totalSeconds) {
                delay(1000L)
                progressSeconds++
            }
            onComplete()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF063327),
                        Color(0xFF031E17),
                        Color(0xFF010E0B)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Decorative geometric border frame
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .border(1.dp, IslamicGold.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
        )

        // Clean central crescent emblem without any text written on the screen
        Surface(
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.45f),
            border = androidx.compose.foundation.BorderStroke(2.dp, IslamicGold),
            modifier = Modifier.size(90.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.NightsStay,
                    contentDescription = null,
                    tint = IslamicGold,
                    modifier = Modifier.size(50.dp)
                )
            }
        }
    }
}
