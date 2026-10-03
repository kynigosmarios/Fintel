package com.example.ui.components

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.HomeRepairService
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.Widgets
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Household
import com.example.ui.theme.Emerald700
import com.example.widget.OikoNomosWidgetProvider
import com.google.firebase.auth.FirebaseUser

@Composable
fun AppSettingsDialog(
    currentUser: FirebaseUser?,
    household: Household?,
    isSyncing: Boolean,
    themeMode: String,
    launchWithChooser: Boolean,
    onThemeChange: (String) -> Unit,
    onToggleLaunchWithChooser: (Boolean) -> Unit,
    onCreateHousehold: (String) -> Unit,
    onJoinHousehold: (String) -> Unit,
    onSignOut: () -> Unit,
    onMessage: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var widgetAddedMsg by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .padding(vertical = 12.dp)
                .testTag("app_settings_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header (Pinned)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ρυθμίσεις",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .size(32.dp)
                            .clickable { onDismiss() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Κλείσιμο",
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
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Προφίλ & Οικογενειακός Συγχρονισμός (Google Sign-In & Household)
                    FamilyCloudSyncCard(
                        currentUser = currentUser,
                        household = household,
                        isSyncing = isSyncing,
                        onCreateHousehold = onCreateHousehold,
                        onJoinHousehold = onJoinHousehold,
                        onSignOut = onSignOut,
                        onMessage = onMessage
                    )

                    // 2. Πλακίδιο Επιφάνειας Εργασίας (Desktop Tile / Home Screen Widget)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Widgets,
                                        contentDescription = null,
                                        tint = Emerald700,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Πλακίδιο Επιφάνειας Εργασίας",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Switch(
                                    checked = launchWithChooser,
                                    onCheckedChange = onToggleLaunchWithChooser,
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Emerald700
                                    ),
                                    modifier = Modifier.testTag("launch_with_chooser_switch")
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Άνοιγμα με μενού 3 επιλογών (Φωνητική, Γρήγορη καταχώρηση, Άνοιγμα εφαρμογής).",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 15.sp
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Pin to Home Screen Button
                            Button(
                                onClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        val appWidgetManager = context.getSystemService(AppWidgetManager::class.java)
                                        val myProvider = ComponentName(context, OikoNomosWidgetProvider::class.java)
                                        if (appWidgetManager.isRequestPinAppWidgetSupported) {
                                            appWidgetManager.requestPinAppWidget(myProvider, null, null)
                                            widgetAddedMsg = "Επιλέξτε «Προσθήκη» στο παράθυρο που άνοιξε."
                                        } else {
                                            widgetAddedMsg = "Κρατήστε παρατεταμένα στην αρχική οθόνη για να βάλετε το Widget."
                                        }
                                    } else {
                                        widgetAddedMsg = "Κρατήστε παρατεταμένα στην αρχική οθόνη για να βάλετε το Widget."
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .testTag("pin_widget_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Emerald700,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PushPin,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Καρφίτσωμα στην Επιφάνεια Εργασίας",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            if (widgetAddedMsg != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = widgetAddedMsg ?: "",
                                    fontSize = 11.sp,
                                    color = Emerald700,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // 3. Θέμα (Theme) - Beautiful Modern 3-Option Segmented Selector
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Θέμα εμφάνισης",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val themeOptions = listOf(
                                Triple("LIGHT", "Φωτεινό", Icons.Default.LightMode),
                                Triple("DARK", "Σκοτεινό", Icons.Default.DarkMode),
                                Triple("SYSTEM", "Σύστημα", Icons.Default.SettingsBrightness)
                            )

                            themeOptions.forEach { (mode, label, icon) ->
                                val isSelected = themeMode == mode
                                ThemeSegmentButton(
                                    label = label,
                                    icon = icon,
                                    isSelected = isSelected,
                                    onClick = { onThemeChange(mode) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Bottom Close button
                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Text(
                            text = "Κλείσιμο",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeSegmentButton(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) Emerald700 else Color.Transparent,
        modifier = modifier
            .height(42.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
