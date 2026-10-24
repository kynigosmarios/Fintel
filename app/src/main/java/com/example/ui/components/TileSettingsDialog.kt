package com.example.ui.components

import android.app.StatusBarManager
import android.content.ComponentName
import android.graphics.drawable.Icon
import android.os.Build
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OfflineBolt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.service.QuickExpenseTileService
import com.example.ui.theme.Emerald100
import com.example.ui.theme.Emerald700
import com.example.util.Localization

@Composable
fun TileSettingsDialog(
    isTileFeatureEnabled: Boolean,
    onToggleFeature: (Boolean) -> Unit,
    onTestQuickAction: () -> Unit,
    onDismiss: () -> Unit,
    appLanguage: String = "EL"
) {
    val lang = Localization.Language.fromCode(appLanguage)
    val context = LocalContext.current
    var addTileStatusMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("tile_settings_dialog"),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Emerald100,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = Emerald700,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (lang == Localization.Language.ENGLISH) "Quick Settings Tile" else "Πλακίδιο Γρήγορων Ρυθμίσεων",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (lang == Localization.Language.ENGLISH) "Instant expense logging tile" else "Γρήγορη καταγραφή εξόδου",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .size(30.dp)
                            .clickable { onDismiss() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = Localization.close(lang),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Toggle Switch (Requirement 1: Enable / Disable inside app)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (lang == Localization.Language.ENGLISH) "Enable Quick Tile" else "Ενεργοποίηση Πλακιδίου",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isTileFeatureEnabled) {
                                    if (lang == Localization.Language.ENGLISH) "Active in phone quick settings" else "Ενεργό στις ρυθμίσεις του κινητού"
                                } else {
                                    if (lang == Localization.Language.ENGLISH) "Disabled" else "Ανενεργό"
                                },
                                fontSize = 11.sp,
                                color = if (isTileFeatureEnabled) Emerald700 else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = isTileFeatureEnabled,
                            onCheckedChange = onToggleFeature,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Emerald700
                            ),
                            modifier = Modifier.testTag("tile_feature_switch")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Add to Quick Settings Button (Android 13+ API)
                Button(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            val statusBarManager = context.getSystemService(StatusBarManager::class.java)
                            statusBarManager?.requestAddTileService(
                                ComponentName(context, QuickExpenseTileService::class.java),
                                "Fintel",
                                Icon.createWithResource(context, R.drawable.fintel_logo),
                                context.mainExecutor
                            ) { result ->
                                addTileStatusMessage = if (result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED) {
                                    if (lang == Localization.Language.ENGLISH) "Tile added successfully!" else "Το πλακίδιο προστέθηκε επιτυχώς!"
                                } else {
                                    if (lang == Localization.Language.ENGLISH) "You can add it manually by dragging down the notification bar." else "Μπορείτε να το προσθέσετε χειροκίνητα σέρνοντας τη μπάρα ειδοποιήσεων."
                                }
                            }
                        } else {
                            addTileStatusMessage = if (lang == Localization.Language.ENGLISH)
                                "Swipe down the notification shade, tap the edit pencil and drag the Fintel tile to active tiles."
                            else
                                "Σύρετε τη μπάρα ειδοποιήσεων προς τα κάτω, πατήστε το μολύβι επεξεργασίας και σύρετε το πλακίδιο Fintel στα ενεργά."
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald700, contentColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("request_add_tile_button")
                ) {
                    Icon(Icons.Default.OfflineBolt, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (lang == Localization.Language.ENGLISH) "Add to Notification Bar" else "Προσθήκη στη Μπάρα Ειδοποιήσεων", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                if (addTileStatusMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = addTileStatusMessage ?: "",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        lineHeight = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Test Action Button
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onTestQuickAction()
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("test_quick_action_button")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (lang == Localization.Language.ENGLISH) "⚡ Test Quick Action Now" else "⚡ Δοκιμή Λειτουργίας Τώρα", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                Spacer(modifier = Modifier.height(10.dp))

                // Explanatory bullets
                Text(
                    text = if (lang == Localization.Language.ENGLISH)
                        "• Voice: Speak ➔ Confirmation / edit screen ➔ Save\n• Manual: Fill form ➔ Direct save"
                    else
                        "• Φωνή: Ομιλία ➔ Οθόνη επιβεβαίωσης / διόρθωσης ➔ Αποθήκευση\n• Χειροκίνητα: Συμπλήρωση ➔ Κατευθείαν αποθήκευση",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
