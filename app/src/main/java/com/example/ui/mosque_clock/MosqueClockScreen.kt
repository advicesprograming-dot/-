package com.example.ui.mosque_clock

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AppSettingsEntity
import com.example.data.local.PrayerAlertEntity
import com.example.ui.adhan.AdhanScreenActivity
import com.example.ui.theme.*
import com.example.util.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MosqueClockScreen(
    settings: AppSettingsEntity,
    schedule: PrayerSchedule?,
    currentCalendar: Calendar,
    onOpenMenu: () -> Unit,
    onTestSalawat: () -> Unit
) {
    val context = LocalContext.current
    val lang = settings.language

    // Digital time formatting
    val hour = currentCalendar.get(Calendar.HOUR_OF_DAY)
    val minute = currentCalendar.get(Calendar.MINUTE)
    val second = currentCalendar.get(Calendar.SECOND)

    val digitalHours = if (settings.timeFormat24) {
        String.format("%02d", hour)
    } else {
        val h12 = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
        String.format("%02d", h12)
    }
    val digitalMinutes = String.format("%02d", minute)
    val digitalSeconds = String.format("%02d", second)
    val amPmSuffix = if (!settings.timeFormat24) {
        if (hour >= 12) {
            if (lang == "ar") "مساءً" else "PM"
        } else {
            if (lang == "ar") "صباحاً" else "AM"
        }
    } else ""

    // Hijri & Gregorian Dates (cached per day instead of every second to prevent CPU lag)
    val currentDay = currentCalendar.get(Calendar.DAY_OF_YEAR)
    val hijriDate = remember(settings.hijriAdjustmentDays, currentDay) {
        HijriCalendarHelper.getHijriDate(currentCalendar.time, settings.hijriAdjustmentDays)
    }

    val gregorianFormat = remember(lang) {
        val locale = when (lang) {
            "fr" -> Locale.FRENCH
            "en" -> Locale.ENGLISH
            else -> Locale("ar")
        }
        SimpleDateFormat("EEEE d MMMM yyyy", locale)
    }
    val gregorianString = remember(currentDay, lang) {
        gregorianFormat.format(currentCalendar.time)
    }

    // Stable, vibrant LED color without 60fps recomposition overhead on low-end GPUs
    val ledAlpha = 1.0f

    // Next Prayer live countdown ticking with currentCalendar every second
    val nowMs = currentCalendar.timeInMillis
    val nextPrayerTimestamp = schedule?.nextPrayer?.timestamp ?: 0L
    val timeToNextMillis = if (nextPrayerTimestamp > nowMs) {
        nextPrayerTimestamp - nowMs
    } else {
        (schedule?.timeToNextMillis ?: 0L).coerceAtLeast(0L)
    }
    val remainingHours = timeToNextMillis / (1000 * 60 * 60)
    val remainingMins = (timeToNextMillis / (1000 * 60)) % 60
    val remainingSecs = (timeToNextMillis / 1000) % 60
    val formattedCountdown = String.format("%02d:%02d:%02d", remainingHours, remainingMins, remainingSecs)

    val isDark = settings.isDarkMode

    Scaffold(
        bottomBar = {
            Surface(
                color = if (isDark) MosqueEmeraldMedium else MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shadowElevation = 12.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(width = 1.dp, color = if (isDark) IslamicGold.copy(alpha = 0.35f) else Color(0xFFDCE5E0))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Quick Action: Test Adhan Screen
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(context, AdhanScreenActivity::class.java).apply {
                                val nextP = schedule?.nextPrayer
                                putExtra("EXTRA_PRAYER_ID", nextP?.id ?: "DHUHR")
                                putExtra("EXTRA_PRAYER_NAME", nextP?.arabicName ?: "الظهر")
                                putExtra("EXTRA_IS_ALERT", false)
                            }
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = if (isDark) IslamicGold else EmeraldPrimaryLight),
                        modifier = Modifier.testTag("btn_quick_test_adhan")
                    ) {
                        Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("تجربة شاشة الأذان", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    // Main Menu Button
                    Button(
                        onClick = onOpenMenu,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) IslamicGold else EmeraldPrimaryLight,
                            contentColor = if (isDark) Color.Black else Color.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .padding(start = 8.dp)
                            .testTag("btn_menu")
                    ) {
                        Icon(Icons.Default.Menu, contentDescription = null, tint = if (isDark) Color.Black else Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = AppStrings.get("menu_button", lang),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = if (isDark) {
                            listOf(
                                MosqueEmeraldDark,
                                MosqueEmeraldMedium,
                                MosqueEmeraldDark
                            )
                        } else {
                            listOf(
                                Color(0xFFF4F7F5),
                                Color(0xFFE8EFEA),
                                Color(0xFFF4F7F5)
                            )
                        }
                    )
                )
                .padding(innerPadding)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Electronic Mosque Clock Frame Top Arch Header
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.5.dp,
                            brush = Brush.linearGradient(listOf(IslamicGold, IslamicGoldLight, IslamicGoldDark)),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .shadow(if (isDark) 12.dp else 4.dp, RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) MosqueEmeraldCard else Color.White
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Mosque Location / City & Timezone Tag
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = if (isDark) IslamicGold else EmeraldPrimaryLight,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = settings.cityName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) IslamicGold else EmeraldPrimaryLight
                            )
                            Text(
                                text = "• ${settings.timezoneId}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Central Digital LED Clock Display
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isDark) Color.Black.copy(alpha = 0.85f) else Color(0xFF13201A))
                                .border(1.dp, IslamicGold.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    // Hours
                                    Text(
                                        text = digitalHours,
                                        fontSize = 46.sp,
                                        fontWeight = FontWeight.Black,
                                        color = DigitalGreenLed.copy(alpha = ledAlpha),
                                        fontFamily = FontFamily.Monospace,
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        text = ":",
                                        fontSize = 42.sp,
                                        fontWeight = FontWeight.Black,
                                        color = DigitalGreenLed.copy(alpha = ledAlpha),
                                        modifier = Modifier.padding(horizontal = 2.dp)
                                    )
                                    // Minutes
                                    Text(
                                        text = digitalMinutes,
                                        fontSize = 46.sp,
                                        fontWeight = FontWeight.Black,
                                        color = DigitalGreenLed.copy(alpha = ledAlpha),
                                        fontFamily = FontFamily.Monospace,
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        text = ":",
                                        fontSize = 42.sp,
                                        fontWeight = FontWeight.Black,
                                        color = DigitalGreenLed.copy(alpha = ledAlpha),
                                        modifier = Modifier.padding(horizontal = 2.dp)
                                    )
                                    // Seconds
                                    Text(
                                        text = digitalSeconds,
                                        fontSize = 46.sp,
                                        fontWeight = FontWeight.Black,
                                        color = DigitalAmberLed.copy(alpha = ledAlpha),
                                        fontFamily = FontFamily.Monospace,
                                        letterSpacing = 1.sp
                                    )

                                    if (amPmSuffix.isNotBlank()) {
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = amPmSuffix,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = DigitalAmberLed
                                        )
                                    }
                                }
                            }
                        }

                        // Dual Dates: Hijri & Gregorian
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Hijri Date
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.NightsStay,
                                    contentDescription = null,
                                    tint = if (isDark) IslamicGold else IslamicGoldDark,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = when (lang) {
                                        "fr" -> hijriDate.formattedFr
                                        "en" -> hijriDate.formattedEn
                                        else -> hijriDate.formattedAr
                                    },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDark) IslamicGold else IslamicGoldDark
                                )
                            }

                            // Gregorian Date
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = gregorianString,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // شعار وبطاقة الصلاة على النبي ﷺ (Bar & Emblem prominently visible on top)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onTestSalawat() }
                        .border(
                            width = 1.5.dp,
                            brush = Brush.horizontalGradient(
                                if (isDark) listOf(IslamicGold, IslamicGoldLight, IslamicGold)
                                else listOf(EmeraldPrimaryLight, IslamicGold, EmeraldPrimaryLight)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .shadow(if (isDark) 6.dp else 2.dp, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF09291D) else Color(0xFFEAF5EE)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isDark) IslamicGold.copy(alpha = 0.2f) else EmeraldPrimaryLight.copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isDark) IslamicGold else EmeraldPrimaryLight
                                ),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "ﷺ",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (isDark) IslamicGold else EmeraldPrimaryLight
                                    )
                                }
                            }

                            Column {
                                Text(
                                    text = "ﷺ صَلِّ عَلَى الحَبِيبِ المُصْطَفَى ﷺ",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isDark) IslamicGold else EmeraldPrimaryLight
                                )
                                Text(
                                    text = "اللهم صل وسلم وبارك على نبينا محمد",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isDark) Color(0xFFD4ECE1) else Color(0xFF1E4E38)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isDark) IslamicGold else EmeraldPrimaryLight,
                            modifier = Modifier.padding(start = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.VolumeUp,
                                    contentDescription = "استماع",
                                    tint = if (isDark) Color.Black else Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "استمع",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color.Black else Color.White
                                )
                            }
                        }
                    }
                }
            }

            // 2. Next Prayer Glowing Banner
            item {
                schedule?.let { sched ->
                    val nextPrayer = sched.nextPrayer
                    val prayerDisplayName = when (lang) {
                        "fr" -> nextPrayer.frenchName
                        "en" -> nextPrayer.englishName
                        else -> nextPrayer.arabicName
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.5.dp,
                                if (isDark) DigitalAmberLed.copy(alpha = 0.8f) else IslamicGold,
                                RoundedCornerShape(14.dp)
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) Color.Black.copy(alpha = 0.7f) else Color(0xFFFFF9EC)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "${AppStrings.get("next_prayer", lang)}: $prayerDisplayName",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) IslamicGold else IslamicGoldDark
                                )
                                Text(
                                    text = String.format(AppStrings.get("remaining_on_adhan", lang), formattedCountdown, prayerDisplayName),
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Text(
                                text = formattedCountdown,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isDark) DigitalAmberLed else Color(0xFFC47D00),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // 3. Electronic Mosque Clock Prayers Matrix (Fajr, Sunrise, Dhuhr/Jumuah, Asr, Maghrib, Isha)
            item {
                schedule?.let { sched ->
                    val prayerItems = listOf(
                        sched.fajr,
                        sched.sunrise,
                        sched.dhuhr,
                        sched.asr,
                        sched.maghrib,
                        sched.isha
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                if (isDark) 2.dp else 1.dp,
                                if (isDark) IslamicGold.copy(alpha = 0.5f) else Color(0xFFD4DFDA),
                                RoundedCornerShape(18.dp)
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) MosqueEmeraldMedium else Color.White
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Table Header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (isDark) Color.Black.copy(alpha = 0.5f) else Color(0xFFEBF3EF),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "الصلاة",
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) IslamicGold else EmeraldPrimaryLight,
                                    fontSize = 14.sp,
                                    modifier = Modifier.weight(1.2f)
                                )
                                Text(
                                    text = "الأذان",
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) IslamicGold else EmeraldPrimaryLight,
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1.2f)
                                )
                                Text(
                                    text = AppStrings.get("iqama", lang),
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) IslamicGold else EmeraldPrimaryLight,
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            prayerItems.forEach { prayer ->
                                val isNext = sched.nextPrayer.id == prayer.id
                                val isSunrise = prayer.id == "SUNRISE"

                                val formattedTime = PrayerTimesCalculator.formatTime(
                                    prayer.hour24,
                                    prayer.minute,
                                    settings.timeFormat24,
                                    lang
                                )

                                // Iqama time approximation (e.g. +15 or +20 min after adhan in mosque clocks)
                                val iqamaMin = (prayer.minute + if (prayer.id == "FAJR") 25 else 15) % 60
                                val iqamaHour = (prayer.hour24 + (prayer.minute + if (prayer.id == "FAJR") 25 else 15) / 60) % 24
                                val formattedIqama = if (isSunrise) "-" else PrayerTimesCalculator.formatTime(
                                    iqamaHour,
                                    iqamaMin,
                                    settings.timeFormat24,
                                    lang
                                )

                                val rowBorder = if (isNext) {
                                    BorderStroke(2.dp, if (isDark) DigitalGreenLed else EmeraldPrimaryLight)
                                } else {
                                    BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE8EFEA))
                                }

                                val rowBg = if (isNext) {
                                    if (isDark) Color.Black.copy(alpha = 0.85f) else Color(0xFFE2F4EC)
                                } else {
                                    if (isDark) Color.Black.copy(alpha = 0.45f) else Color(0xFFF8FAF9)
                                }

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("prayer_row_${prayer.id}"),
                                    shape = RoundedCornerShape(10.dp),
                                    border = rowBorder,
                                    colors = CardDefaults.cardColors(containerColor = rowBg)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Prayer Name
                                        Row(
                                            modifier = Modifier.weight(1.2f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (isNext) {
                                                Icon(
                                                    Icons.Default.PlayArrow,
                                                    contentDescription = null,
                                                    tint = if (isDark) DigitalGreenLed else EmeraldPrimaryLight,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(Modifier.width(4.dp))
                                            }
                                            Text(
                                                text = when (lang) {
                                                    "fr" -> prayer.frenchName
                                                    "en" -> prayer.englishName
                                                    else -> prayer.arabicName
                                                },
                                                fontSize = 17.sp,
                                                fontWeight = if (isNext) FontWeight.Black else FontWeight.Bold,
                                                color = if (isNext) {
                                                    if (isDark) DigitalGreenLed else EmeraldPrimaryLight
                                                } else if (isSunrise) {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                } else {
                                                    if (isDark) Color.White else Color(0xFF1B2E28)
                                                }
                                            )
                                        }

                                        // Adhan Time (Digital LED style)
                                        Text(
                                            text = formattedTime,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Black,
                                            fontFamily = FontFamily.Monospace,
                                            textAlign = TextAlign.Center,
                                            color = if (isNext) {
                                                if (isDark) DigitalGreenLed else EmeraldPrimaryLight
                                            } else {
                                                if (isDark) DigitalAmberLed else Color(0xFF8F6200)
                                            },
                                            modifier = Modifier.weight(1.2f)
                                        )

                                        // Iqama Time
                                        Text(
                                            text = formattedIqama,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            textAlign = TextAlign.End,
                                            color = if (isNext) {
                                                if (isDark) IslamicGold else IslamicGoldDark
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. Salawat Quick Bar
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTestSalawat() }
                        .border(
                            1.dp,
                            if (isDark) IslamicGold.copy(alpha = 0.35f) else Color(0xFFDCE6E1),
                            RoundedCornerShape(12.dp)
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) MosqueEmeraldCard else Color.White
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Favorite,
                                contentDescription = null,
                                tint = if (isDark) IslamicGold else EmeraldPrimaryLight,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "ﷺ صلّ على الحبيب المصطفى ﷺ",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) IslamicGold else EmeraldPrimaryLight
                            )
                        }

                        Text(
                            text = "اضغط للاستماع",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
