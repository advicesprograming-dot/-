package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = IslamicGold,
    onPrimary = Color(0xFF221B00),
    primaryContainer = MosqueEmeraldCard,
    onPrimaryContainer = IslamicGoldLight,
    secondary = DigitalGreenLed,
    onSecondary = Color(0xFF00391A),
    background = MosqueEmeraldDark,
    onBackground = EmeraldOnSurfaceDark,
    surface = MosqueEmeraldMedium,
    onSurface = EmeraldOnSurfaceDark,
    surfaceVariant = MosqueEmeraldCard,
    onSurfaceVariant = Color(0xFFBCCEC7),
    outline = IslamicGold.copy(alpha = 0.5f)
)

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimaryLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFBAEBD8),
    onPrimaryContainer = Color(0xFF002116),
    secondary = IslamicGoldDark,
    onSecondary = Color.White,
    background = EmeraldBackgroundLight,
    onBackground = EmeraldOnSurfaceLight,
    surface = EmeraldSurfaceLight,
    onSurface = EmeraldOnSurfaceLight,
    surfaceVariant = Color(0xFFE0ECE6),
    onSurfaceVariant = Color(0xFF3F4D47),
    outline = Color(0xFF708078)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep cohesive Islamic palette by default
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
