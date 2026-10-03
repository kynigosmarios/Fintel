package com.example.ui.components

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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.Emerald700

@Composable
fun QuickActionChooserDialog(
    onVoiceClick: () -> Unit,
    onManualClick: () -> Unit,
    onScanReceiptClick: () -> Unit = {},
    onExportReportClick: () -> Unit = {},
    onOpenAppClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .padding(10.dp)
                .testTag("quick_action_chooser_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "OikoNomos",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Επιλέξτε τρόπο καταγραφής",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                                contentDescription = "Κλείσιμο",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Option 1: 🎙️ Φωνητική εντολή
                QuickActionItem(
                    title = "1. Φωνητική εντολή",
                    subtitle = "Μιλήστε άμεσα (π.χ. «15€ σούπερ μάρκετ»)",
                    icon = Icons.Default.Mic,
                    iconBgColor = Emerald700,
                    iconTint = Color.White,
                    borderColor = Emerald700.copy(alpha = 0.4f),
                    testTag = "quick_action_voice_option",
                    onClick = {
                        onDismiss()
                        onVoiceClick()
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Option 2: ⚡ Γρήγορη καταχώρηση
                QuickActionItem(
                    title = "2. Γρήγορη καταχώρηση",
                    subtitle = "Άμεση καταχώρηση ποσού & κατηγορίας",
                    icon = Icons.Default.FlashOn,
                    iconBgColor = Color(0xFF0284C7),
                    iconTint = Color.White,
                    borderColor = Color(0xFF0284C7).copy(alpha = 0.4f),
                    testTag = "quick_action_manual_option",
                    onClick = {
                        onDismiss()
                        onManualClick()
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Option 3: 📸 Σάρωση απόδειξης
                QuickActionItem(
                    title = "3. Σάρωση απόδειξης",
                    subtitle = "Φωτογραφία & αυτόματη εξαγωγή ποσού",
                    icon = Icons.Default.Receipt,
                    iconBgColor = Color(0xFF8B5CF6),
                    iconTint = Color.White,
                    borderColor = Color(0xFF8B5CF6).copy(alpha = 0.4f),
                    testTag = "quick_action_scan_receipt_option",
                    onClick = {
                        onDismiss()
                        onScanReceiptClick()
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Option 4: 📊 Εξαγωγή Excel / CSV
                QuickActionItem(
                    title = "4. Εξαγωγή σε Excel / CSV",
                    subtitle = "Λήψη ή κοινοποίηση αναφοράς εξόδων",
                    icon = Icons.Default.TableChart,
                    iconBgColor = Color(0xFF059669),
                    iconTint = Color.White,
                    borderColor = Color(0xFF059669).copy(alpha = 0.4f),
                    testTag = "quick_action_export_report_option",
                    onClick = {
                        onDismiss()
                        onExportReportClick()
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Option 5: 🏠 Άνοιγμα εφαρμογής
                QuickActionItem(
                    title = "5. Άνοιγμα εφαρμογής",
                    subtitle = "Προβολή εξόδων, στόχων, γραφημάτων & ρυθμίσεων",
                    icon = Icons.Default.Dashboard,
                    iconBgColor = MaterialTheme.colorScheme.surfaceVariant,
                    iconTint = MaterialTheme.colorScheme.onSurface,
                    borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    testTag = "quick_action_open_app_option",
                    onClick = {
                        onDismiss()
                        onOpenAppClick()
                    }
                )
            }
        }
    }
}

@Composable
private fun QuickActionItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconBgColor: Color,
    iconTint: Color,
    borderColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.2.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = iconBgColor,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 10.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
