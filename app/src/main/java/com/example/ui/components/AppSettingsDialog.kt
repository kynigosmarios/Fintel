package com.example.ui.components

import android.app.TimePickerDialog
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Household
import com.example.data.model.UserProfile
import com.example.ui.theme.Emerald700
import com.example.util.Localization
import com.google.firebase.auth.FirebaseUser

@Composable
fun AppSettingsDialog(
    currentUser: FirebaseUser?,
    household: Household?,
    isSyncing: Boolean,
    themeMode: String,
    appLanguage: String = "EL",
    launchWithChooser: Boolean,
    userProfile: UserProfile? = null,
    reminderEnabled: Boolean = false,
    reminderHour: Int = 21,
    reminderMinute: Int = 30,
    notificationConsentHandled: Boolean = false,
    bankAutoCatcherEnabled: Boolean = true,
    onAcceptNotificationConsent: () -> Unit = {},
    onToggleReminder: (Boolean) -> Unit = {},
    onSetReminderTime: (Int, Int) -> Unit = { _, _ -> },
    onTestReminder: () -> Unit = {},
    onToggleBankAutoCatcher: (Boolean) -> Unit = {},
    onThemeChange: (String) -> Unit,
    onLanguageChange: (String) -> Unit = {},
    onToggleLaunchWithChooser: (Boolean) -> Unit,
    onCreateHousehold: (String) -> Unit,
    onJoinHousehold: (String) -> Unit,
    onSignOut: () -> Unit,
    onClearAllDataAndSignOut: () -> Unit = {},
    onMessage: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var showNotificationConsentDialog by remember { mutableStateOf(false) }
    var showBankInfoDialog by remember { mutableStateOf(false) }
    val lang = Localization.Language.fromCode(appLanguage)
    val context = androidx.compose.ui.platform.LocalContext.current

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onToggleReminder(true)
        }
    }

    androidx.compose.runtime.LaunchedEffect(reminderEnabled, notificationConsentHandled) {
        if (reminderEnabled && !notificationConsentHandled) {
            showNotificationConsentDialog = true
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .imePadding()
                .widthIn(max = 430.dp)
                .testTag("app_settings_dialog"),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header (Icon + Title + Close Button)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Emerald700.copy(alpha = 0.12f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = null,
                                    tint = Emerald700,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = Localization.appSettings(lang),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = Localization.appSettingsSubtitle(lang),
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier
                            .size(32.dp)
                            .clickable { onDismiss() }
                            .testTag("app_settings_close_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = Localization.close(lang),
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {


                    // 3. Theme Selection (Light, Dark, System) - Only Icons
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ThemeCardOption(
                                icon = Icons.Default.LightMode,
                                isSelected = themeMode == "LIGHT",
                                onClick = { onThemeChange("LIGHT") },
                                modifier = Modifier.weight(1f)
                            )

                            ThemeCardOption(
                                icon = Icons.Default.DarkMode,
                                isSelected = themeMode == "DARK",
                                onClick = { onThemeChange("DARK") },
                                modifier = Modifier.weight(1f)
                            )

                            ThemeCardOption(
                                icon = Icons.Default.SettingsBrightness,
                                isSelected = themeMode == "SYSTEM",
                                onClick = { onThemeChange("SYSTEM") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // 4. Section: Υπενθύμιση καταχώρησης (Entry Reminder)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Emerald700.copy(alpha = 0.12f),
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = if (reminderEnabled) Icons.Default.NotificationsActive else Icons.Default.Bedtime,
                                                contentDescription = null,
                                                tint = Emerald700,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (lang == Localization.Language.ENGLISH) "Entry Reminder" else "Υπενθύμιση καταχώρησης",
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Switch(
                                        checked = reminderEnabled,
                                        onCheckedChange = { isChecked ->
                                            if (isChecked) {
                                                if (!notificationConsentHandled) {
                                                    showNotificationConsentDialog = true
                                                } else {
                                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                        val hasPerm = androidx.core.content.ContextCompat.checkSelfPermission(
                                                            context,
                                                            android.Manifest.permission.POST_NOTIFICATIONS
                                                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                                        if (!hasPerm) {
                                                            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                                        }
                                                    }
                                                    onToggleReminder(true)
                                                }
                                            } else {
                                                onToggleReminder(false)
                                            }
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = Emerald700
                                        ),
                                        modifier = Modifier.testTag("daily_reminder_switch")
                                    )
                                }

                                if (reminderEnabled) {
                                    Spacer(modifier = Modifier.height(14.dp))

                                    var timeInput by remember(reminderHour, reminderMinute) {
                                        mutableStateOf("%02d:%02d".format(reminderHour, reminderMinute))
                                    }
                                    val focusManager = LocalFocusManager.current
                                    val keyboardController = LocalSoftwareKeyboardController.current

                                    // Function to commit time parsing
                                    fun commitTime(str: String) {
                                        val parts = str.trim().split(":", ".")
                                        if (parts.size == 2) {
                                            val h = parts[0].toIntOrNull()
                                            val m = parts[1].toIntOrNull()
                                            if (h != null && m != null && h in 0..23 && m in 0..59) {
                                                onSetReminderTime(h, m)
                                                timeInput = "%02d:%02d".format(h, m)
                                            }
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.clickable {
                                                TimePickerDialog(
                                                    context,
                                                    { _, pickedHour, pickedMinute ->
                                                        onSetReminderTime(pickedHour, pickedMinute)
                                                        timeInput = "%02d:%02d".format(pickedHour, pickedMinute)
                                                    },
                                                    reminderHour,
                                                    reminderMinute,
                                                    true
                                                ).show()
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Schedule,
                                                contentDescription = null,
                                                tint = Emerald700,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (lang == Localization.Language.ENGLISH) "Reminder Time:" else "Ώρα ειδοποίησης:",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable {
                                                    focusManager.clearFocus()
                                                    keyboardController?.hide()
                                                    TimePickerDialog(
                                                        context,
                                                        { _, pickedHour, pickedMinute ->
                                                            onSetReminderTime(pickedHour, pickedMinute)
                                                            timeInput = "%02d:%02d".format(pickedHour, pickedMinute)
                                                        },
                                                        reminderHour,
                                                        reminderMinute,
                                                        true
                                                    ).show()
                                                }
                                                .testTag("reminder_time_picker_button")
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = MaterialTheme.colorScheme.surface,
                                                border = BorderStroke(1.dp, Emerald700.copy(alpha = 0.5f)),
                                                modifier = Modifier
                                                    .width(88.dp)
                                                    .height(40.dp)
                                            ) {
                                                Box(
                                                    contentAlignment = Alignment.Center,
                                                    modifier = Modifier.padding(horizontal = 6.dp)
                                                ) {
                                                    Text(
                                                        text = "%02d:%02d".format(reminderHour, reminderMinute),
                                                        fontSize = 15.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Emerald700,
                                                        textAlign = TextAlign.Center,
                                                        modifier = Modifier.testTag("reminder_time_input")
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(6.dp))

                                            Surface(
                                                shape = CircleShape,
                                                color = Emerald700.copy(alpha = 0.12f),
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Default.Schedule,
                                                        contentDescription = "Pick Time",
                                                        tint = Emerald700,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Section: Ανάγνωση Τραπεζικών Push (Bank Auto-Catcher)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Emerald700.copy(alpha = 0.12f),
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.AccountBalance,
                                                contentDescription = null,
                                                tint = Emerald700,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (lang == Localization.Language.ENGLISH) "Bank Push Reader" else "Ανάγνωση Τραπεζικών Push",
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Switch(
                                        checked = bankAutoCatcherEnabled,
                                        onCheckedChange = { isChecked ->
                                            onToggleBankAutoCatcher(isChecked)
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = Emerald700
                                        ),
                                        modifier = Modifier.testTag("bank_auto_catcher_switch")
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Emerald700.copy(alpha = 0.12f),
                                        border = BorderStroke(1.dp, Emerald700.copy(alpha = 0.35f)),
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clickable { showBankInfoDialog = true }
                                            .testTag("bank_push_info_button")
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Info,
                                                contentDescription = if (lang == Localization.Language.ENGLISH) "Info" else "Πληροφορίες",
                                                tint = Emerald700,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            try {
                                                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                                            } catch (e: Exception) {
                                                onMessage("Μεταβείτε στις Ρυθμίσεις > Ειδοποιήσεις για ενεργοποίηση")
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(38.dp)
                                            .testTag("enable_notification_listener_button"),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Emerald700)
                                    ) {
                                        Text(
                                            text = if (lang == Localization.Language.ENGLISH) "Configure Access" else "Ρύθμιση Πρόσβασης",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Section: Επιλογή Γλώσσας (Language Selection)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            LanguageOptionCard(
                                title = "Ελληνικά",
                                flag = "🇬🇷",
                                isSelected = appLanguage == "EL",
                                onClick = { onLanguageChange("EL") },
                                modifier = Modifier.weight(1f)
                            )

                            LanguageOptionCard(
                                title = "English",
                                flag = "🇬🇧",
                                isSelected = appLanguage == "EN",
                                onClick = { onLanguageChange("EN") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Section: Διαχείριση Δεδομένων (Data Reset Row)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showResetConfirmDialog = true }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteSweep,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = Localization.clearAllData(lang),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Fixed Bottom Save Button (Always visible and scroll never cuts it off)
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Emerald700,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("app_settings_save_button")
                ) {
                    Text(
                        text = if (lang == Localization.Language.ENGLISH) "Save & Close" else "Αποθήκευση",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }

    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            shape = RoundedCornerShape(22.dp),
            title = {
                Text(
                    text = if (lang == Localization.Language.ENGLISH) "Reset All Data?" else "Πλήρης Εκκαθάριση;",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = if (lang == Localization.Language.ENGLISH)
                        "All local expenses will be wiped and you will be signed out so the app returns to its initial clean state."
                    else
                        "Θα διαγραφούν όλα τα τοπικά έξοδα και θα αποσυνδεθείτε από τον λογαριασμό σας ώστε η εφαρμογή να επανέλθει στην αρχική της κατάσταση.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetConfirmDialog = false
                        onDismiss()
                        onClearAllDataAndSignOut()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = if (lang == Localization.Language.ENGLISH) "Clear & Sign Out" else "Εκκαθάριση & Αποσύνδεση",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text(Localization.cancel(lang), fontSize = 12.5.sp)
                }
            }
        )
    }

    if (showNotificationConsentDialog) {
        NotificationConsentDialog(
            appLanguage = appLanguage,
            onAccept = {
                onAcceptNotificationConsent()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val hasPerm = androidx.core.content.ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.POST_NOTIFICATIONS
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    if (!hasPerm) {
                        notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
                onToggleReminder(true)
                showNotificationConsentDialog = false
            },
            onDismiss = {
                onToggleReminder(false)
                showNotificationConsentDialog = false
            }
        )
    }

    if (showBankInfoDialog) {
        AlertDialog(
            onDismissRequest = { showBankInfoDialog = false },
            shape = RoundedCornerShape(22.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccountBalance,
                        contentDescription = null,
                        tint = Emerald700,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (lang == Localization.Language.ENGLISH) "Bank Push Reader" else "Ανάγνωση Τραπεζικών Push",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                }
            },
            text = {
                Text(
                    text = if (lang == Localization.Language.ENGLISH)
                        "This feature automatically detects purchase push notifications STRICTLY from supported banks:\n\n• Eurobank\n• Alpha Bank\n• National Bank of Greece (NBG)\n• Piraeus Bank\n• Revolut\n• Payzy\n• Viber Pay\n• PayPal\n• Snappi\n\nIt NEVER triggers from emails or non-banking notifications. All processing is 100% private and on-device."
                    else
                        "Η λειτουργία αυτή αναγνωρίζει αυτόματα ειδοποιήσεις αγορών (Push Notifications) ΑΥΣΤΗΡΑ και μόνο από τις υποστηριζόμενες τράπεζες:\n\n• Eurobank\n• Alpha Bank\n• Εθνική Τράπεζα (NBG)\n• Τράπεζα Πειραιώς\n• Revolut\n• Payzy\n• Viber Pay\n• PayPal\n• Snappi\n\nΔεν ενεργοποιείται ΠΟΤΕ από emails ή άλλες προσωπικές ειδοποιήσεις. Η επεξεργασία γίνεται 100% τοπικά στη συσκευή σας για απόλυτη ασφάλεια.",
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = { showBankInfoDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald700),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(Localization.close(lang), color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun LanguageOptionCard(
    title: String,
    flag: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) Emerald700.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) Emerald700 else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        ),
        modifier = modifier
            .height(58.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = flag, fontSize = 20.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Emerald700 else MaterialTheme.colorScheme.onSurface
                )
            }
            if (isSelected) {
                Surface(
                    shape = CircleShape,
                    color = Emerald700,
                    modifier = Modifier.size(20.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeCardOption(
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) Emerald700.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) Emerald700 else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        ),
        modifier = modifier
            .height(46.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = if (isSelected) Emerald700 else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
