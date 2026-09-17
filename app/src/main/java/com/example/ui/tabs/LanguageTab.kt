package com.example.ui.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.IslamicGold
import com.example.util.AppStrings

@Composable
fun LanguageTab(
    currentLanguage: String,
    onLanguageSelected: (String) -> Unit,
    isDarkMode: Boolean = false,
    onThemeToggle: (Boolean) -> Unit = {}
) {
    val languages = listOf(
        Triple("ar", "العربية", "اللغة الافتراضية مع دعم كامل للاتجاه"),
        Triple("en", "English", "Default English translation"),
        Triple("fr", "Français", "Traduction complète en français")
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Theme Selection (Light / Dark mode)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, IslamicGold.copy(alpha = 0.5f), RoundedCornerShape(18.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                        contentDescription = null,
                        tint = IslamicGold,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = if (currentLanguage == "ar") "مظهر التطبيق (فاتح / داكن)" else "App Theme (Light / Dark)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Light Mode Button
                    Button(
                        onClick = { onThemeToggle(false) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_theme_light"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isDarkMode) IslamicGold else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (!isDarkMode) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.LightMode, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (currentLanguage == "ar") "الوضع الفاتح" else "Light Mode",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Dark Mode Button
                    Button(
                        onClick = { onThemeToggle(true) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_theme_dark"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDarkMode) IslamicGold else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isDarkMode) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.DarkMode, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (currentLanguage == "ar") "الوضع الداكن" else "Dark Mode",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Language,
                contentDescription = null,
                tint = IslamicGold,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = AppStrings.get("select_language", currentLanguage),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Text(
            text = when (currentLanguage) {
                "fr" -> "Le changement de langue s'applique instantanément à l'ensemble de l'application."
                "en" -> "Changing the language immediately updates all screens across the application."
                else -> "عند اختيار أي لغة، يتم تغيير لغة التطبيق بالكامل فوراً."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        languages.forEach { (code, name, subtitle) ->
            val isSelected = currentLanguage == code
            val borderColor = if (isSelected) IslamicGold else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            val bgColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.surface
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(2.dp, borderColor, RoundedCornerShape(16.dp))
                    .clickable { onLanguageSelected(code) }
                    .testTag("lang_option_$code"),
                colors = CardDefaults.cardColors(containerColor = bgColor)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = name,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) IslamicGold else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Selected",
                            tint = IslamicGold,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}
