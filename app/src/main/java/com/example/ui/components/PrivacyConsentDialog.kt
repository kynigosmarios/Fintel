package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.Emerald700
import com.example.util.Localization

@Composable
fun PrivacyConsentDialog(
    appLanguage: String = "EL",
    onAccept: () -> Unit
) {
    val lang = Localization.Language.fromCode(appLanguage)
    var isChecked by remember { mutableStateOf(true) }

    Dialog(
        onDismissRequest = { /* Modal requires user interaction */ },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .testTag("privacy_consent_dialog"),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
            ) {
                // Header Icon & Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Emerald700.copy(alpha = 0.12f),
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = Emerald700,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = if (lang == Localization.Language.ENGLISH) "Privacy & GDPR Consent" else "Προστασία Δεδομένων & GDPR",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (lang == Localization.Language.ENGLISH) "Safe, private & encrypted cloud" else "Ασφαλής, ιδιωτική cloud αποθήκευση",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (lang == Localization.Language.ENGLISH)
                            "To provide seamless multi-device synchronization and secure backups of your budget, expenses, and family members, we process your profile and expense data in accordance with the European General Data Protection Regulation (GDPR)."
                        else
                            "Για να παρέχει απρόσκοπτο συγχρονισμό μεταξύ των συσκευών σας και ασφαλή αντίγραφα ασφαλείας του εξοδολογίου και των μελών σας, η εφαρμογή επεξεργάζεται τα στοιχεία προφίλ και εξόδων σας σύμφωνα με τον Γενικό Κανονισμό Προστασίας Δεδομένων (GDPR).",
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    PrivacyFeatureItem(
                        icon = Icons.Default.Lock,
                        title = if (lang == Localization.Language.ENGLISH) "End-to-End Encryption" else "Κρυπτογράφηση & Ιδιωτικότητα",
                        subtitle = if (lang == Localization.Language.ENGLISH)
                            "Your financial records and profile are stored in your secure private database."
                        else
                            "Τα οικονομικά σας στοιχεία και το προφίλ σας αποθηκεύονται στη δική σας ασφαλή ιδιωτική βάση."
                    )

                    PrivacyFeatureItem(
                        icon = Icons.Default.CloudDone,
                        title = if (lang == Localization.Language.ENGLISH) "No Third-Party Sharing" else "Μηδενική Κοινοποίηση σε Τρίτους",
                        subtitle = if (lang == Localization.Language.ENGLISH)
                            "We never sell or share your data with advertisers or third parties."
                        else
                            "Τα δεδομένα σας δεν πωλούνται και δεν διαβιβάζονται ποτέ σε διαφημιστές ή τρίτους."
                    )

                    PrivacyFeatureItem(
                        icon = Icons.Default.CheckCircle,
                        title = if (lang == Localization.Language.ENGLISH) "Full Ownership & Control" else "Πλήρης Έλεγχος & Διαγραφή",
                        subtitle = if (lang == Localization.Language.ENGLISH)
                            "You can edit or permanently delete your records from Settings at any time."
                        else
                            "Μπορείτε να τροποποιήσετε ή να διαγράψετε οριστικά τα δεδομένα σας οποιαδήποτε στιγμή."
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Consent Checkbox
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Checkbox(
                        checked = isChecked,
                        onCheckedChange = { isChecked = it },
                        colors = CheckboxDefaults.colors(checkedColor = Emerald700),
                        modifier = Modifier.testTag("privacy_consent_checkbox")
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (lang == Localization.Language.ENGLISH)
                            "I consent to the processing of my profile and expense data for app synchronization."
                        else
                            "Συναινώ στην επεξεργασία του προφίλ και των δεδομένων μου για τον συγχρονισμό της εφαρμογής.",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Accept button
                Button(
                    onClick = onAccept,
                    enabled = isChecked,
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald700, contentColor = Color.White),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("accept_privacy_consent_button")
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (lang == Localization.Language.ENGLISH) "Accept & Continue" else "Αποδοχή & Συνέχεια",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun PrivacyFeatureItem(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            shape = CircleShape,
            color = Emerald700.copy(alpha = 0.1f),
            modifier = Modifier
                .size(32.dp)
                .padding(top = 2.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Emerald700,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = subtitle,
                fontSize = 11.5.sp,
                lineHeight = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
