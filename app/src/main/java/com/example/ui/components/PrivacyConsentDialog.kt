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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
    val isEnglish = lang == Localization.Language.ENGLISH

    Dialog(
        onDismissRequest = { /* Modal requires user interaction */ },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false, usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp)
                .widthIn(max = 500.dp)
                .testTag("privacy_consent_dialog"),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
            ) {
                // Header Icon & Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Emerald700.copy(alpha = 0.12f),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = Emerald700,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = if (isEnglish) "Data Privacy & Sync" else "Προστασία & Κοινή Χρήση Δεδομένων",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isEnglish) "Encrypted & private cloud storage" else "Ασφαλής & ιδιωτική αποθήκευση",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = if (isEnglish)
                        "Your budget records are stored in a private, encrypted cloud database (GDPR compliant) to ensure your expenses stay synchronized across your devices and are securely accessible to your household."
                    else
                        "Τα δεδομένα σας αποθηκεύονται σε ασφαλή, κρυπτογραφημένη βάση στο cloud (σύμφωνα με το GDPR), ώστε τα έξοδά σας να συγχρονίζονται σε όλες τις συσκευές σας και να είναι προσβάσιμα στο εξοδολόγιό σας.",
                    fontSize = 13.sp,
                    lineHeight = 18.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SimplifiedPrivacyBullet(
                        icon = Icons.Default.Lock,
                        title = if (isEnglish) "Full Encryption" else "Πλήρης Κρυπτογράφηση",
                        subtitle = if (isEnglish)
                            "Your personal finances are encrypted and protected."
                        else
                            "Τα προσωπικά σας οικονομικά στοιχεία είναι πλήρως κρυπτογραφημένα."
                    )

                    SimplifiedPrivacyBullet(
                        icon = Icons.Default.CloudDone,
                        title = if (isEnglish) "No Third-Party Sharing" else "Μηδενική Κοινοποίηση σε Τρίτους",
                        subtitle = if (isEnglish)
                            "We never sell or share your data with advertisers."
                        else
                            "Τα στοιχεία σας δεν πωλούνται και δεν διαβιβάζονται σε διαφημιστές."
                    )

                    SimplifiedPrivacyBullet(
                        icon = Icons.Default.Group,
                        title = if (isEnglish) "Private & Secure" else "Απόλυτη Ιδιωτικότητα",
                        subtitle = if (isEnglish)
                            "Your personal finances belong strictly to you and remain protected."
                        else
                            "Τα προσωπικά σας οικονομικά ανήκουν αποκλειστικά σε εσάς και παραμένουν προστατευμένα."
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Single Accept button
                Button(
                    onClick = onAccept,
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
                        text = if (isEnglish) "Accept & Continue" else "Αποδοχή & Συνέχεια",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun SimplifiedPrivacyBullet(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            shape = CircleShape,
            color = Emerald700.copy(alpha = 0.1f),
            modifier = Modifier.size(32.dp)
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
            Text(
                text = subtitle,
                fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
