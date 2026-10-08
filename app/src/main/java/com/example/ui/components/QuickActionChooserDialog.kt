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
import androidx.compose.material.icons.filled.TrendingUp
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
import com.example.util.Localization

@Composable
fun QuickActionChooserDialog(
    appLanguage: String = "EL",
    onVoiceClick: () -> Unit,
    onManualClick: () -> Unit,
    onIncomeClick: () -> Unit = {},
    onOpenAppClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val lang = Localization.Language.fromCode(appLanguage)
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
                    .padding(20.dp)
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
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (lang == Localization.Language.ENGLISH) "Choose entry method" else "Επιλέξτε τρόπο καταγραφής",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

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
                                contentDescription = Localization.close(lang),
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Option 1: 🎙️ 1. Φωνητική εντολή
                QuickActionItem(
                    title = if (lang == Localization.Language.ENGLISH) "1. Voice Command" else "1. Φωνητική εντολή",
                    subtitle = if (lang == Localization.Language.ENGLISH) "Speak directly (e.g. \"€15 supermarket\")" else "Μιλήστε άμεσα (π.χ. «15€ σούπερ μάρκετ»)",
                    icon = Icons.Default.Mic,
                    iconBgColor = Emerald700,
                    iconTint = Color.White,
                    borderColor = Emerald700.copy(alpha = 0.45f),
                    testTag = "quick_action_voice_option",
                    onClick = {
                        onDismiss()
                        onVoiceClick()
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Option 2: ⚡ 2. Γρήγορη καταχώρηση εξόδου
                QuickActionItem(
                    title = if (lang == Localization.Language.ENGLISH) "2. Add Expense" else "2. Καταχώρηση Εξόδου",
                    subtitle = if (lang == Localization.Language.ENGLISH) "Direct manual expense logging" else "Άμεση καταχώρηση ποσού, κατηγορίας & μελών",
                    icon = Icons.Default.FlashOn,
                    iconBgColor = Color(0xFF0284C7),
                    iconTint = Color.White,
                    borderColor = Color(0xFF0284C7).copy(alpha = 0.45f),
                    testTag = "quick_action_manual_option",
                    onClick = {
                        onDismiss()
                        onManualClick()
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Option 3: 💰 3. Καταχώρηση Εσόδου
                QuickActionItem(
                    title = if (lang == Localization.Language.ENGLISH) "3. Add Income" else "3. Καταχώρηση Εσόδου",
                    subtitle = if (lang == Localization.Language.ENGLISH) "Member income (salary, rent, bonus)" else "Συνεισφορά μέλους (μισθός, ενοίκιο, bonus)",
                    icon = Icons.Default.TrendingUp,
                    iconBgColor = Color(0xFF059669),
                    iconTint = Color.White,
                    borderColor = Color(0xFF059669).copy(alpha = 0.45f),
                    testTag = "quick_action_income_option",
                    onClick = {
                        onDismiss()
                        onIncomeClick()
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Option 4: 🏠 4. Άνοιγμα εφαρμογής
                QuickActionItem(
                    title = if (lang == Localization.Language.ENGLISH) "4. Open App" else "4. Άνοιγμα εφαρμογής",
                    subtitle = if (lang == Localization.Language.ENGLISH) "View expenses, analytics & settings" else "Προβολή εξόδων, εσόδων, στατιστικών & ρυθμίσεων",
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
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = iconBgColor,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
