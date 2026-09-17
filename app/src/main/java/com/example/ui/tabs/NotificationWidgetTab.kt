package com.example.ui.tabs

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AppSettingsEntity
import com.example.ui.theme.IslamicGold
import com.example.ui.theme.DigitalGreenLed
import com.example.receiver.MosqueStripWidgetProvider
import com.example.receiver.PrayerTimesWidgetProvider
import com.example.receiver.SalawatWidgetProvider
import com.example.receiver.WidgetActionReceiver
import com.example.ui.PrayerViewModel
import com.example.util.AppStrings
import com.example.util.HijriCalendarHelper
import com.example.util.PrayerTimesCalculator
import com.example.util.PrayerWidgetHelper
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun NotificationWidgetTab(
    viewModel: PrayerViewModel,
    settings: AppSettingsEntity,
    lang: String
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Live ticking timer for preview (ticks every second!)
    var currentMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentMillis = System.currentTimeMillis()
            delay(1000L)
        }
    }

    val cal = remember(currentMillis / 60000) { Calendar.getInstance() }
    val schedule = remember(settings, cal) {
        PrayerTimesCalculator.calculateTimes(cal.time, settings)
    }
    val (currentPrayer, nextPrayer) = remember(schedule, currentMillis) {
        PrayerWidgetHelper.getCurrentAndNextPrayer(schedule, currentMillis)
    }

    // Remaining time to next prayer
    val remainingToNextMs = (nextPrayer.timestamp - currentMillis).coerceAtLeast(0L)
    val nextCountdownFormatted = remember(remainingToNextMs, settings.notificationBarShowSeconds) {
        PrayerWidgetHelper.formatDuration(remainingToNextMs, settings.notificationBarShowSeconds)
    }

    // Remaining time to next Salawat
    val salawatRemainingMs = (settings.nextSalawatTimestamp - currentMillis).coerceAtLeast(0L)
    val salawatCountdownFormatted = remember(salawatRemainingMs) {
        PrayerWidgetHelper.formatSalawatTimer(salawatRemainingMs)
    }

    // Hijri date
    val hijriDate = remember(settings.hijriAdjustmentDays, cal) {
        HijriCalendarHelper.getHijriDate(cal.time, settings.hijriAdjustmentDays)
    }
    val hijriString = remember(hijriDate, cal) {
        val dayOfWeekAr = SimpleDateFormat("EEEE", Locale("ar")).format(cal.time)
        "$dayOfWeekAr ${hijriDate.formattedAr}"
    }
    val gregorianString = remember(cal) {
        SimpleDateFormat("yyyy-M-d", Locale.ENGLISH).format(cal.time)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Tab Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Widgets,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(30.dp)
            )
            Column {
                Text(
                    text = AppStrings.get("tab_notification_widget", lang),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = AppStrings.get("notif_widget_desc", lang),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Master Toggle Card for Time Alerts / Ongoing Notification
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
                        text = "تفعيل التنبيه بالوقت وإشعارات المتابعة",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = IslamicGold
                    )
                    Text(
                        text = "تشغيل أو إيقاف إشعار العد التنازلي المستمر ووقت الصلوات",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = settings.timeAlertsEnabled,
                    onCheckedChange = { viewModel.setTimeAlertsEnabled(it) },
                    colors = SwitchDefaults.colors(checkedThumbColor = DigitalGreenLed)
                )
            }
        }

        // -------------------------------------------------------------
        // SECTION 1: Theme Style Selection (Emerald vs Burgundy vs Classic)
        // -------------------------------------------------------------
        Text(
            text = "اختيار مظهر ولون شريط الإشعارات والتطبيقات المصغرة",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Theme 1: Emerald (Image 1)
            val isEmerald = settings.widgetThemeStyle == "EMERALD" || settings.widgetThemeStyle.isBlank()
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isEmerald) Color(0xFF072E24) else MaterialTheme.colorScheme.surfaceVariant
                ),
                border = androidx.compose.foundation.BorderStroke(
                    if (isEmerald) 2.dp else 1.dp,
                    if (isEmerald) Color(0xFFF6C953) else Color.Transparent
                ),
                modifier = Modifier
                    .weight(1f)
                    .clickable { viewModel.setWidgetThemeStyle("EMERALD") }
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0B3A2F))
                            .border(1.5.dp, Color(0xFFF6C953), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🕌", fontSize = 11.sp)
                    }
                    Text(
                        text = "الزمردي للمساجد",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isEmerald) Color(0xFFF6C953) else MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "الشريط الأخضر",
                        fontSize = 10.sp,
                        color = if (isEmerald) Color(0xFFA3D9C9) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (isEmerald) {
                        Text("✓ مفعّل", fontSize = 10.sp, color = Color(0xFFF6C953), fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Theme 2: Burgundy (Image 2)
            val isBurgundy = settings.widgetThemeStyle == "BURGUNDY"
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isBurgundy) Color(0xFF450711) else MaterialTheme.colorScheme.surfaceVariant
                ),
                border = androidx.compose.foundation.BorderStroke(
                    if (isBurgundy) 2.dp else 1.dp,
                    if (isBurgundy) Color(0xFFF6C953) else Color.Transparent
                ),
                modifier = Modifier
                    .weight(1f)
                    .clickable { viewModel.setWidgetThemeStyle("BURGUNDY") }
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF5A0B18))
                            .border(1.5.dp, Color(0xFFF6C953), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🕌", fontSize = 11.sp)
                    }
                    Text(
                        text = "العنابي الملكي",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isBurgundy) Color(0xFFF6C953) else MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "الشريط العنابي",
                        fontSize = 10.sp,
                        color = if (isBurgundy) Color(0xFFF3B4BE) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (isBurgundy) {
                        Text("✓ مفعّل", fontSize = 10.sp, color = Color(0xFFF6C953), fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Theme 3: Classic Cyan
            val isClassic = settings.widgetThemeStyle == "CLASSIC"
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isClassic) Color(0xFF0C1D2E) else MaterialTheme.colorScheme.surfaceVariant
                ),
                border = androidx.compose.foundation.BorderStroke(
                    if (isClassic) 2.dp else 1.dp,
                    if (isClassic) Color(0xFF00A2E8) else Color.Transparent
                ),
                modifier = Modifier
                    .weight(1f)
                    .clickable { viewModel.setWidgetThemeStyle("CLASSIC") }
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0077B6))
                            .border(1.5.dp, Color(0xFF00A2E8), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("⚡", fontSize = 11.sp)
                    }
                    Text(
                        text = "الكلاسيكي الأزرق",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isClassic) Color(0xFF00A2E8) else MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "الشريط السماوي",
                        fontSize = 10.sp,
                        color = if (isClassic) Color(0xFF80D8FF) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (isClassic) {
                        Text("✓ مفعّل", fontSize = 10.sp, color = Color(0xFF00A2E8), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // SECTION 2: Exact Mosque Clock Display Strip (Image 1 & 2 Live Replica)
        // -------------------------------------------------------------
        Text(
            text = "معاينة شريط ساعة المسجد (Mosque Clock Display Strip)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        val stripBgColor = if (settings.widgetThemeStyle == "BURGUNDY") Color(0xFF450711) else if (settings.widgetThemeStyle == "CLASSIC") Color(0xFF0C1723) else Color(0xFF062E25)
        val stripBorderColor = if (settings.widgetThemeStyle == "BURGUNDY") Color(0xFF7A1324) else if (settings.widgetThemeStyle == "CLASSIC") Color(0xFF1B324D) else Color(0xFF0F5A47)
        val stripGoldColor = if (settings.widgetThemeStyle == "CLASSIC") Color(0xFFE5A93C) else Color(0xFFF6C953)
        val stripMutedColor = if (settings.widgetThemeStyle == "BURGUNDY") Color(0xFFF0AAB6) else if (settings.widgetThemeStyle == "CLASSIC") Color(0xFFA0B2C6) else Color(0xFFA3D9C9)
        val stripDomeBg = if (settings.widgetThemeStyle == "BURGUNDY") Color(0xFF5A0B18) else if (settings.widgetThemeStyle == "CLASSIC") Color(0xFF142436) else Color(0xFF0B3A2F)
        val stripVerse = if (settings.widgetThemeStyle == "BURGUNDY") "﴿ وَمَن يُؤْمِن بِاللَّهِ يَهْدِ قَلْبَهُ ﴾" else "﴿ إِنَّ الَّذِينَ آمَنُوا وَعَمِلُوا الصَّالِحَاتِ سَيَجْعَلُ لَهُمُ الرَّحْمَٰنُ وُدًّا ﴾"
        val stripHadith = if (settings.widgetThemeStyle == "BURGUNDY") "قال ﷺ: ما ملأ ابن آدم وعاء شرا من بطنه" else "قال ﷺ: إن من أحبكم إلي وأقربكم مني مجلسا يوم القيامة أحاسنكم أخلاقا"

        // Digital Mosque Strip Live Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = stripBgColor),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, stripBorderColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Top Mosque Bar: Left Crescent & Mosque name, Right Time & Date
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("🕌", fontSize = 16.sp)
                        Text(
                            text = "مسجد الهدى • " + settings.cityName.ifBlank { "مكة المكرمة" },
                            color = stripGoldColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Live digital clock with seconds HH:mm:ss
                    Text(
                        text = SimpleDateFormat("hh:mm:ss a", Locale.ENGLISH).format(java.util.Date(currentMillis)),
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Subtitle Row: Hijri & Gregorian Dates
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$hijriString • $gregorianString",
                        color = stripMutedColor,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "درجة الحرارة: 28°",
                        color = stripMutedColor,
                        fontSize = 10.sp
                    )
                }

                // Middle Hero: Circular Countdown Dial & Next Prayer Announcement
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Circular Dial
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(CircleShape)
                            .background(stripDomeBg)
                            .border(2.dp, stripGoldColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = nextCountdownFormatted,
                                color = stripGoldColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "المتبقي لـ ${nextPrayer.arabicName}",
                                color = Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Quranic Verse + Next Prayer Header
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = stripVerse,
                            color = stripGoldColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 15.sp
                        )
                        Text(
                            text = "الصلاة القادمة: ${nextPrayer.arabicName} الساعة " + PrayerWidgetHelper.formatClockTime(
                                nextPrayer.hour24,
                                nextPrayer.minute,
                                settings.timeFormat24
                            ),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stripHadith,
                            color = stripMutedColor,
                            fontSize = 9.sp,
                            lineHeight = 13.sp
                        )
                    }
                }

                // 6 Columns Dome Row (Fajr, Sunrise, Dhuhr, Asr, Maghrib, Isha)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val mosquePrayers = listOf(
                        Triple("الفجر", schedule.fajr, schedule.fajr.id == nextPrayer.id),
                        Triple("الشروق", schedule.sunrise, false),
                        Triple(schedule.dhuhr.arabicName, schedule.dhuhr, schedule.dhuhr.id == nextPrayer.id),
                        Triple("العصر", schedule.asr, schedule.asr.id == nextPrayer.id),
                        Triple("المغرب", schedule.maghrib, schedule.maghrib.id == nextPrayer.id),
                        Triple("العشاء", schedule.isha, schedule.isha.id == nextPrayer.id)
                    )

                    mosquePrayers.forEach { (name, pTime, isNext) ->
                        val isHighlighted = isNext
                        val colBg = if (isHighlighted) stripGoldColor else stripDomeBg
                        val titleColor = if (isHighlighted) Color(0xFF062E25) else stripMutedColor
                        val timeColor = if (isHighlighted) Color.Black else Color.White

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp, bottomStart = 6.dp, bottomEnd = 6.dp))
                                .background(colBg)
                                .border(
                                    1.dp,
                                    if (isHighlighted) Color.White else stripBorderColor,
                                    RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp, bottomStart = 6.dp, bottomEnd = 6.dp)
                                )
                                .padding(vertical = 5.dp, horizontal = 2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(1.dp)
                        ) {
                            Text(
                                text = name,
                                color = titleColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = PrayerWidgetHelper.formatClockTime(
                                    pTime.hour24,
                                    pTime.minute,
                                    settings.timeFormat24
                                ),
                                color = timeColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }

                // Salawat Bottom Row
                if (settings.salawatEnabled) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(stripDomeBg)
                            .border(1.dp, stripBorderColor, RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = {
                                viewModel.playSalawatNow()
                                Toast.makeText(context, "ﷺ اللهم صل وسلم على نبينا محمد ﷺ", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = stripGoldColor,
                                contentColor = Color(0xFF072E24)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text(
                                text = "صلّ الآن ﷺ",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "الصلاة على النبي ﷺ: يتبقى $salawatCountdownFormatted",
                            color = stripGoldColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // SECTION 3: Standard Notification Bar & Prayer Widget Live Preview
        // -------------------------------------------------------------
        Text(
            text = "معاينة شريط الإشعارات والتطبيق المصغر للصلوات",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = stripBgColor),
            border = androidx.compose.foundation.BorderStroke(1.dp, stripBorderColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Top Header: Right "🕌 مواقيت الصلاة", Left City
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = settings.cityName.ifBlank { "مكة المكرمة" },
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "مواقيت الصلاة 🕌",
                        color = stripGoldColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Hero Banner: Next prayer & ticking countdown
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(stripDomeBg)
                        .border(1.dp, stripBorderColor, RoundedCornerShape(10.dp))
                        .padding(vertical = 10.dp, horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "يتبقى $nextCountdownFormatted على",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = nextPrayer.arabicName,
                            color = stripGoldColor,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                // 5 Prayer Times Columns
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val prayers = listOf(
                        schedule.fajr,
                        schedule.dhuhr,
                        schedule.asr,
                        schedule.maghrib,
                        schedule.isha
                    )

                    prayers.forEach { prayer ->
                        val isCurrent = prayer.id == currentPrayer.id
                        val isNext = prayer.id == nextPrayer.id

                        val colBg = if (isNext) stripDomeBg else Color.Transparent
                        val textColor = if (isNext) stripGoldColor else Color.White
                        val timeColor = if (isNext) stripGoldColor else stripMutedColor
                        val badgeText = if (isCurrent) "الحالية" else if (isNext) "القادمة" else ""
                        val badgeBg = if (isNext) stripGoldColor else Color.Transparent
                        val badgeTextColor = if (isNext) Color.Black else Color.White

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(colBg)
                                .padding(vertical = 4.dp, horizontal = 2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            if (badgeText.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(badgeBg)
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = badgeText,
                                        color = badgeTextColor,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.height(13.dp))
                            }

                            Text(
                                text = prayer.arabicName,
                                color = textColor,
                                fontSize = 12.sp,
                                fontWeight = if (isCurrent || isNext) FontWeight.Bold else FontWeight.Normal
                            )

                            Text(
                                text = PrayerWidgetHelper.formatClockTime(
                                    prayer.hour24,
                                    prayer.minute,
                                    settings.timeFormat24
                                ),
                                color = timeColor,
                                fontSize = 10.sp,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                // Hijri & Gregorian Date Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = gregorianString,
                        color = stripMutedColor,
                        fontSize = 11.sp
                    )
                    Text(
                        text = hijriString,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // -------------------------------------------------------------
        // SECTION 2: Android System Notification Shade Preview
        // -------------------------------------------------------------
        Text(
            text = "معاينة إشعار الصلاة على النبي ﷺ الدائم (Notification Shade)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1218)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1C2733)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Notification Shade Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Prayer Time & Adhan • تذكير مستمر • $salawatCountdownFormatted",
                        color = Color(0xFFA0B2C6),
                        fontSize = 11.sp
                    )
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = Color(0xFFE5A93C),
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Dark Green Forest Card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0D3B2E)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Title row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "الصلاة على النبي ﷺ",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "تذكير دائم",
                                color = Color(0xFFE5A93C),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Inner cyan-bordered card with Digital LED countdown
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF0A281E))
                                .border(1.dp, Color(0xFF1DE9B6), RoundedCornerShape(10.dp))
                                .padding(vertical = 10.dp, horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = "الوقت المتبقي للتذكير القادم:",
                                    color = Color(0xFFA0B2C6),
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = salawatCountdownFormatted,
                                    color = Color(0xFF00E5FF),
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "الموعد القادم: " + remember(settings.nextSalawatTimestamp) {
                                        val c = Calendar.getInstance().apply { timeInMillis = settings.nextSalawatTimestamp }
                                        PrayerWidgetHelper.formatClockTime(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false)
                                    },
                                    color = Color(0xFFA0B2C6),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        // Bottom action row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    viewModel.playSalawatNow()
                                    Toast.makeText(context, "ﷺ اللهم صل وسلم على نبينا محمد ﷺ", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFE5A93C),
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text(
                                    text = "صلّ الآن ﷺ",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Text(
                                text = "الصوت: بالترتيب (صوت مختلف في كل مرة)",
                                color = Color.White,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // SECTION 3: Settings & Configuration Controls
        // -------------------------------------------------------------
        Text(
            text = "إعدادات شريط الإشعارات والتطبيقات المصغرة",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Switch 1: Ongoing Notification
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "شريط الإشعارات المستمر (Ongoing Notification)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "إبقاء بطاقة مواقيت الصلاة والعد التنازلي ظاهرة باستمرار في ستارة إشعارات الهاتف",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.notificationBarEnabled,
                        onCheckedChange = { viewModel.setNotificationBarEnabled(it) }
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Switch 2: Show Seconds in Notification Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "تشغيل عداد الثواني في شريط الإشعارات",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "تحديث العداد لحظياً بالثواني التنازلية الدقيقة (HH:mm:ss) لمواقيت الصلاة والصلاة على النبي",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.notificationBarShowSeconds,
                        onCheckedChange = { viewModel.setNotificationBarShowSeconds(it) }
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Switch 3: Show Seconds in Widgets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "تشغيل عداد الثواني في التطبيقات المصغرة (الودجت)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "تحديث عداد مواقيت الصلاة وعدّاد الصلاة على النبي لحظياً بالثواني التنازلية في شاشة هاتفك الرئيسية",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.widgetShowSeconds,
                        onCheckedChange = { viewModel.setWidgetShowSeconds(it) }
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Switch 4: Show Salawat Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "إظهار شريط وعدّاد الصلاة على النبي ﷺ",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "إظهار عداد الصلاة على النبي ﷺ مع زر (صلّ الآن) في البطاقة وشريط الإشعارات والودجت",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.notificationBarShowSalawat,
                        onCheckedChange = { viewModel.setNotificationBarShowSalawat(it) }
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Button 1: Pin Mosque Clock Strip Widget (Images 1 & 2)
                Button(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            val appWidgetManager = context.getSystemService(AppWidgetManager::class.java)
                            val mosqueProvider = ComponentName(context, MosqueStripWidgetProvider::class.java)
                            if (appWidgetManager.isRequestPinAppWidgetSupported) {
                                val callbackIntent = Intent(context, WidgetActionReceiver::class.java)
                                val callbackPendingIntent = PendingIntent.getBroadcast(
                                    context,
                                    2,
                                    callbackIntent,
                                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                                )
                                appWidgetManager.requestPinAppWidget(mosqueProvider, null, callbackPendingIntent)
                                Toast.makeText(context, "تم إرسال طلب إضافة شريط ساعة المسجد للشاشة الرئيسية", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "اضغط مطولاً على الشاشة الرئيسية ثم اختر شريط ساعة المسجد", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            Toast.makeText(context, "اضغط مطولاً على الشاشة الرئيسية لإضافة شريط ساعة المسجد", Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (settings.widgetThemeStyle == "BURGUNDY") Color(0xFF5A0B18) else Color(0xFF073B2E),
                        contentColor = Color(0xFFF6C953)
                    )
                ) {
                    Text("🕌", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "إضافة شريط ساعة المسجد (النمط العريض) للشاشة الرئيسية", fontWeight = FontWeight.Bold)
                }

                // Button 2: Pin Main Prayer Times Widget
                FilledTonalButton(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            val appWidgetManager = context.getSystemService(AppWidgetManager::class.java)
                            val myProvider = ComponentName(context, PrayerTimesWidgetProvider::class.java)
                            if (appWidgetManager.isRequestPinAppWidgetSupported) {
                                val callbackIntent = Intent(context, WidgetActionReceiver::class.java)
                                val callbackPendingIntent = PendingIntent.getBroadcast(
                                    context,
                                    0,
                                    callbackIntent,
                                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                                )
                                appWidgetManager.requestPinAppWidget(myProvider, null, callbackPendingIntent)
                                Toast.makeText(context, "تم إرسال طلب إضافة ودجت مواقيت الصلاة لشاشتك الرئيسية", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "يمكنك إضافة الودجت بالضغط مطولاً على الشاشة الرئيسية ثم اختيار مواقيت الصلاة", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            Toast.makeText(context, "اضغط مطولاً على الشاشة الرئيسية لإضافة ودجت مواقيت الصلاة", Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.AddHome, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "إضافة ودجت مواقيت الصلاة (النمط المدمج)", fontWeight = FontWeight.Bold)
                }

                // Button 3: Pin Standalone Salawat Widget
                OutlinedButton(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            val appWidgetManager = context.getSystemService(AppWidgetManager::class.java)
                            val salawatProvider = ComponentName(context, SalawatWidgetProvider::class.java)
                            if (appWidgetManager.isRequestPinAppWidgetSupported) {
                                val callbackIntent = Intent(context, WidgetActionReceiver::class.java)
                                val callbackPendingIntent = PendingIntent.getBroadcast(
                                    context,
                                    1,
                                    callbackIntent,
                                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                                )
                                appWidgetManager.requestPinAppWidget(salawatProvider, null, callbackPendingIntent)
                                Toast.makeText(context, "تم إرسال طلب إضافة ودجت الصلاة على النبي ﷺ للشاشة الرئيسية", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "يمكنك إضافة الودجت بالضغط مطولاً على الشاشة الرئيسية ثم اختيار الصلاة على النبي", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            Toast.makeText(context, "اضغط مطولاً على الشاشة الرئيسية لإضافة ودجت الصلاة على النبي ﷺ", Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFE5A93C))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "إضافة ودجت الصلاة على النبي ﷺ (مع عداد الثواني)", fontWeight = FontWeight.Bold)
                }

                // Button 3: Update Notification & Widget Now
                FilledTonalButton(
                    onClick = {
                        viewModel.refreshNotificationAndWidgets()
                        Toast.makeText(context, "تم تحديث شريط الإشعارات والتطبيقات المصغرة بنجاح", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Sync, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "تحديث شريط الإشعارات والودجت فوراً", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
