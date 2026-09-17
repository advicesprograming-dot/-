package com.example.ui.tabs

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.data.local.PrayerAlertEntity
import com.example.ui.theme.DigitalAmberLed
import com.example.ui.theme.DigitalGreenLed
import com.example.ui.theme.IslamicGold
import com.example.util.AppStrings
import com.example.util.AudioPlayerHelper
import com.example.util.FileStorageHelper
import java.util.Calendar

@Composable
fun AlertsTab(
    alerts: List<PrayerAlertEntity>,
    language: String,
    preAdhanAlertsEnabled: Boolean,
    onSetPreAdhanAlertsEnabled: (Boolean) -> Unit,
    onAddAlert: (String, Int, String, String?, String) -> Unit,
    onUpdateAlert: (PrayerAlertEntity) -> Unit,
    onToggleAlert: (PrayerAlertEntity) -> Unit,
    onDeleteAlert: (PrayerAlertEntity) -> Unit
) {
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    var alertToEdit by remember { mutableStateOf<PrayerAlertEntity?>(null) }

    // Check battery optimization status
    val pm = remember { context.getSystemService(Context.POWER_SERVICE) as? PowerManager }
    val isIgnoringBatteryOptimizations = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && pm != null) {
            pm.isIgnoringBatteryOptimizations(context.packageName)
        } else true
    }

    // Check overlay permission
    val canDrawOverlays = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else true
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Master Toggle Card for Pre-Adhan Alerts
        item {
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
                            text = "تفعيل التنبيهات قبل الأذان",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = IslamicGold
                        )
                        Text(
                            text = "تشغيل أو إيقاف جميع التنبيهات المسبقة قبل مواعيد الصلوات",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = preAdhanAlertsEnabled,
                        onCheckedChange = onSetPreAdhanAlertsEnabled,
                        colors = SwitchDefaults.colors(checkedThumbColor = DigitalGreenLed)
                    )
                }
            }
        }

        // Top Action: Add New Custom Alert Button
        item {
            Button(
                onClick = { showAddDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_add_alert"),
                colors = ButtonDefaults.buttonColors(containerColor = IslamicGold, contentColor = Color.Black),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.AddAlarm, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = AppStrings.get("add_alert_btn", language),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }

        // Active Alerts List
        if (alerts.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.NotificationsOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = "لا توجد تنبيهات مخصصة حالياً",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "اضغط على زر (إضافة تنبيه جديد) بالأعلى لإضافة تنبيهات قبل الأذان بالدقائق والأيام التي تختارها.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(alerts, key = { it.id }) { alert ->
                AlertItemCard(
                    alert = alert,
                    language = language,
                    onToggle = { onToggleAlert(alert) },
                    onEdit = { alertToEdit = alert },
                    onDelete = { onDeleteAlert(alert) },
                    onTest = {
                        AudioPlayerHelper.playAudioUri(context, alert.ringtoneUri)
                    }
                )
            }
        }

        if (showAddDialog || alertToEdit != null) {
            item {
                AddAlertDialog(
                    language = language,
                    existingAlert = alertToEdit,
                    onDismiss = {
                        showAddDialog = false
                        alertToEdit = null
                    },
                    onSave = { target, min, days, uri, name ->
                        if (alertToEdit != null) {
                            onUpdateAlert(
                                alertToEdit!!.copy(
                                    prayerTarget = target,
                                    minutesBefore = min,
                                    repeatDays = days,
                                    ringtoneUri = uri,
                                    ringtoneName = name
                                )
                            )
                            alertToEdit = null
                        } else {
                            onAddAlert(target, min, days, uri, name)
                            showAddDialog = false
                        }
                    }
                )
            }
        }

        // Section: Permissions & Execution reliability checklist
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = IslamicGold)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = AppStrings.get("permissions_title", language),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = IslamicGold
                        )
                    }

                    Text(
                        text = "لضمان عمل التنبيهات والأذان بدقة كاملة عندما يكون الهاتف مقفلاً دون تجميد أو إغلاق بواسطة النظام، يرجى تفعيل الصلاحيات التالية:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                    // 1. Battery Optimization
                    PermissionRow(
                        title = AppStrings.get("perm_battery_title", language),
                        description = AppStrings.get("perm_battery_desc", language),
                        isGranted = isIgnoringBatteryOptimizations,
                        buttonText = if (!isIgnoringBatteryOptimizations) AppStrings.get("perm_battery_action", language) else null,
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                try {
                                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                    context.startActivity(intent)
                                }
                            }
                        }
                    )

                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                    // 2. Draw over other apps / Overlay
                    PermissionRow(
                        title = AppStrings.get("perm_overlay_title", language),
                        description = AppStrings.get("perm_overlay_desc", language),
                        isGranted = canDrawOverlays,
                        buttonText = if (!canDrawOverlays) AppStrings.get("perm_overlay_action", language) else null,
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                            }
                        }
                    )

                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                    // 3. Auto-start
                    PermissionRow(
                        title = AppStrings.get("perm_autostart_title", language),
                        description = AppStrings.get("perm_autostart_desc", language),
                        isGranted = true,
                        buttonText = null,
                        onClick = {}
                    )

                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                    // 4. Media & Audio Files
                    PermissionRow(
                        title = AppStrings.get("perm_files_title", language),
                        description = AppStrings.get("perm_files_desc", language),
                        isGranted = true,
                        buttonText = AppStrings.get("perm_files_action", language),
                        onClick = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            context.startActivity(intent)
                        }
                    )

                    // 5. Special Samsung Device Care (Galaxy A30s & all One UI devices)
                    if (Build.MANUFACTURER.equals("samsung", ignoreCase = true)) {
                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        PermissionRow(
                            title = AppStrings.get("perm_samsung_title", language),
                            description = AppStrings.get("perm_samsung_desc", language),
                            isGranted = true,
                            buttonText = AppStrings.get("perm_samsung_action", language),
                            onClick = {
                                val intents = listOf(
                                    Intent().setComponent(ComponentName("com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity")),
                                    Intent().setComponent(ComponentName("com.samsung.android.sm", "com.samsung.android.sm.ui.battery.BatteryActivity")),
                                    Intent("com.samsung.android.sm.ACTION_BATTERY"),
                                    Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
                                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                )
                                for (targetIntent in intents) {
                                    try {
                                        targetIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        context.startActivity(targetIntent)
                                        break
                                    } catch (_: Exception) {}
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddAlertDialog(
            language = language,
            onDismiss = { showAddDialog = false },
            onSave = { target, minutes, days, ringtoneUri, ringtoneName ->
                onAddAlert(target, minutes, days, ringtoneUri, ringtoneName)
                showAddDialog = false
            }
        )
    }
}

fun formatDisplayDays(repeatDays: String, language: String): String {
    if (repeatDays == "ALL" || repeatDays.isBlank()) {
        return if (language == "ar") "جميع الأيام" else "All days"
    }
    val parts = repeatDays.split(",").mapNotNull { it.trim().toIntOrNull() }
    if (parts.size == 7) {
        return if (language == "ar") "جميع الأيام" else "All days"
    }
    val dayNames = parts.map { dayNum ->
        when (dayNum) {
            Calendar.SUNDAY -> if (language == "ar") "الأحد" else "Sun"
            Calendar.MONDAY -> if (language == "ar") "الاثنين" else "Mon"
            Calendar.TUESDAY -> if (language == "ar") "الثلاثاء" else "Tue"
            Calendar.WEDNESDAY -> if (language == "ar") "الأربعاء" else "Wed"
            Calendar.THURSDAY -> if (language == "ar") "الخميس" else "Thu"
            Calendar.FRIDAY -> if (language == "ar") "الجمعة" else "Fri"
            Calendar.SATURDAY -> if (language == "ar") "السبت" else "Sat"
            else -> ""
        }
    }.filter { it.isNotBlank() }

    return dayNames.joinToString("، ")
}

@Composable
fun AlertItemCard(
    alert: PrayerAlertEntity,
    language: String,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTest: () -> Unit
) {
    val formattedDays = formatDisplayDays(alert.repeatDays, language)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(
                1.dp,
                if (alert.isEnabled) DigitalGreenLed.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                RoundedCornerShape(14.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    val prayerDisplay = when (alert.prayerTarget) {
                        "ALL" -> AppStrings.get("all_prayers", language)
                        "FAJR" -> "صلاة الفجر"
                        "DHUHR" -> "صلاة الظهر"
                        "ASR" -> "صلاة العصر"
                        "MAGHRIB" -> "صلاة المغرب"
                        "ISHA" -> "صلاة العشاء"
                        "JUMUAH" -> "صلاة الجمعة"
                        else -> alert.prayerTarget
                    }
                    Text(
                        text = "$prayerDisplay • يتبقى ${alert.minutesBefore} دقيقة",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = if (alert.isEnabled) IslamicGold else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "أيام التكرار: $formattedDays",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = DigitalGreenLed
                    )
                    Text(
                        text = "${AppStrings.get("alert_ringtone", language)}: ${alert.ringtoneName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = alert.isEnabled,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(checkedThumbColor = DigitalGreenLed)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("تعديل")
                }
                Spacer(Modifier.width(4.dp))
                TextButton(onClick = onTest) {
                    Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(AppStrings.get("test_alert", language))
                }
                Spacer(Modifier.width(4.dp))
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun PermissionRow(
    title: String,
    description: String,
    isGranted: Boolean,
    buttonText: String?,
    onClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            if (isGranted) {
                Text(text = "مفعّل ✓", color = DigitalGreenLed, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            } else {
                Text(text = "يتطلب تفعيل", color = DigitalAmberLed, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
        Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (!isGranted && buttonText != null) {
            OutlinedButton(
                onClick = onClick,
                modifier = Modifier.padding(top = 4.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = IslamicGold)
            ) {
                Text(buttonText)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAlertDialog(
    language: String,
    existingAlert: PrayerAlertEntity? = null,
    onDismiss: () -> Unit,
    onSave: (String, Int, String, String?, String) -> Unit
) {
    val context = LocalContext.current
    var selectedPrayer by remember { mutableStateOf(existingAlert?.prayerTarget ?: "ALL") }
    var selectedMinutes by remember { mutableStateOf(existingAlert?.minutesBefore ?: 15) }
    var customMinutesText by remember { mutableStateOf("") }
    var ringtoneUri by remember { mutableStateOf<String?>(existingAlert?.ringtoneUri) }
    var ringtoneName by remember { mutableStateOf(existingAlert?.ringtoneName ?: "نغمة التنبيه الافتراضية") }

    val isAllDaysInit = existingAlert == null || existingAlert.repeatDays == "ALL"
    var isAllDays by remember { mutableStateOf(isAllDaysInit) }
    val selectedDays = remember {
        val list = mutableStateListOf(1, 2, 3, 4, 5, 6, 7)
        if (existingAlert != null && existingAlert.repeatDays != "ALL") {
            list.clear()
            existingAlert.repeatDays.split(",").mapNotNull { it.trim().toIntOrNull() }.forEach { list.add(it) }
        }
        list
    }

    val daysList = listOf(
        Pair(Calendar.SATURDAY, if (language == "ar") "السبت" else "Sat"),
        Pair(Calendar.SUNDAY, if (language == "ar") "الأحد" else "Sun"),
        Pair(Calendar.MONDAY, if (language == "ar") "الاثنين" else "Mon"),
        Pair(Calendar.TUESDAY, if (language == "ar") "الثلاثاء" else "Tue"),
        Pair(Calendar.WEDNESDAY, if (language == "ar") "الأربعاء" else "Wed"),
        Pair(Calendar.THURSDAY, if (language == "ar") "الخميس" else "Thu"),
        Pair(Calendar.FRIDAY, if (language == "ar") "الجمعة" else "Fri")
    )

    val prayers = listOf(
        Pair("ALL", AppStrings.get("all_prayers", language)),
        Pair("FAJR", "صلاة الفجر"),
        Pair("DHUHR", "صلاة الظهر"),
        Pair("ASR", "صلاة العصر"),
        Pair("MAGHRIB", "صلاة المغرب"),
        Pair("ISHA", "صلاة العشاء"),
        Pair("JUMUAH", "صلاة الجمعة")
    )

    val minuteOptions = listOf(5, 10, 15, 20, 30, 45, 60)

    val audioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val savedPath = FileStorageHelper.saveUriToInternalStorage(context, it, "alerts_audio", "alert")
            ringtoneUri = savedPath
            ringtoneName = it.lastPathSegment ?: "ملف صوتي مخصص"
        }
    }

    val ringtonePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.getParcelableExtra<Uri>(android.media.RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            if (uri != null) {
                ringtoneUri = uri.toString()
                ringtoneName = android.media.RingtoneManager.getRingtone(context, uri).getTitle(context) ?: "نغمة النظام"
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(AppStrings.get("add_alert_btn", language), fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Target Prayer
                Text(AppStrings.get("target_prayer", language), fontWeight = FontWeight.SemiBold)
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    prayers.forEach { (code, name) ->
                        FilterChip(
                            selected = selectedPrayer == code,
                            onClick = { selectedPrayer = code },
                            label = { Text(name) }
                        )
                    }
                }

                // 2. Time Before Adhan (Minutes)
                Text("الوقت قبل الأذان (الدقائق)", fontWeight = FontWeight.SemiBold)
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    minuteOptions.forEach { m ->
                        FilterChip(
                            selected = selectedMinutes == m,
                            onClick = {
                                selectedMinutes = m
                                customMinutesText = ""
                            },
                            label = { Text("$m دقيقة") }
                        )
                    }
                }

                OutlinedTextField(
                    value = customMinutesText,
                    onValueChange = {
                        customMinutesText = it
                        it.toIntOrNull()?.let { m -> selectedMinutes = m }
                    },
                    label = { Text("أو أدخل عدد دقائق مخصص") },
                    modifier = Modifier.fillMaxWidth()
                )

                // 3. Days of the week selection (User-requested feature)
                Text("أيام صدور التنبيه", fontWeight = FontWeight.SemiBold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = isAllDays,
                        onClick = {
                            isAllDays = true
                            selectedDays.clear()
                            selectedDays.addAll(listOf(1, 2, 3, 4, 5, 6, 7))
                        },
                        label = { Text(if (language == "ar") "جميع الأيام" else "All days") }
                    )
                }

                Text(
                    text = "أو اختر أيام محددة من الأسبوع:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    daysList.forEach { (dayCode, dayLabel) ->
                        val isSelected = selectedDays.contains(dayCode)
                        FilterChip(
                            selected = isSelected && !isAllDays,
                            onClick = {
                                isAllDays = false
                                if (isSelected) {
                                    if (selectedDays.size > 1) {
                                        selectedDays.remove(dayCode)
                                    }
                                } else {
                                    selectedDays.add(dayCode)
                                    if (selectedDays.size == 7) {
                                        isAllDays = true
                                    }
                                }
                            },
                            label = { Text(dayLabel) }
                        )
                    }
                }

                // 4. Alert Ringtone
                Text(AppStrings.get("alert_ringtone", language), fontWeight = FontWeight.SemiBold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(android.media.RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_TYPE, android.media.RingtoneManager.TYPE_ALARM or android.media.RingtoneManager.TYPE_NOTIFICATION)
                                putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                            }
                            ringtonePicker.launch(intent)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("نغمة الهاتف", fontSize = 12.sp)
                    }
                    
                    OutlinedButton(
                        onClick = { audioPicker.launch("audio/*") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.AudioFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("ملف صوتي", fontSize = 12.sp)
                    }
                }
                Text("المحدد: $ringtoneName", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val daysString = if (isAllDays || selectedDays.size == 7) {
                        "ALL"
                    } else {
                        selectedDays.sorted().joinToString(",")
                    }
                    onSave(selectedPrayer, selectedMinutes, daysString, ringtoneUri, ringtoneName)
                },
                colors = ButtonDefaults.buttonColors(containerColor = IslamicGold, contentColor = Color.Black)
            ) {
                Text(AppStrings.get("save_alert", language), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(AppStrings.get("cancel", language))
            }
        }
    )
}
