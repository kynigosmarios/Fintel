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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Household
import com.example.ui.theme.Amber700
import com.example.ui.theme.Emerald700
import com.example.util.Localization

@Composable
fun ExodologioManagerDialog(
    currentHousehold: Household?,
    allHouseholds: List<Household>,
    isPremium: Boolean,
    isHouseholdOwner: (Household) -> Boolean = { true },
    appLanguage: String = "EL",
    currentUserId: String = "",
    onSelectHousehold: (Household) -> Unit,
    onCreateNewHousehold: (String) -> Unit,
    onDeleteHousehold: ((Household) -> Unit)? = null,
    onShowPremiumPaywall: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val lang = Localization.Language.fromCode(appLanguage)
    var showCreateDialog by remember { mutableStateOf(false) }
    var newHouseholdName by remember { mutableStateOf("") }
    var householdToDelete by remember { mutableStateOf<Household?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp)
                .widthIn(max = 500.dp)
                .testTag("exodologio_manager_dialog"),
            shape = RoundedCornerShape(26.dp),
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Emerald700.copy(alpha = 0.12f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.MenuBook,
                                    contentDescription = null,
                                    tint = Emerald700,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = Localization.householdManager(lang),
                                fontSize = 17.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isPremium)
                                    (if (lang == Localization.Language.ENGLISH) "Pro (Unlimited households)" else "Pro (Απεριόριστα εξοδολόγια)")
                                else
                                    (if (lang == Localization.Language.ENGLISH) "Basic (1 household)" else "Βασικό (1 εξοδολόγιο)"),
                                fontSize = 11.5.sp,
                                color = if (isPremium) Amber700 else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier
                            .size(32.dp)
                            .clickable { onDismiss() }
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

                // Scrollable content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    val isEn = lang == Localization.Language.ENGLISH
                    val myHouseholds = allHouseholds

                    // 1. SECTION: ΤΑ ΕΞΟΔΟΛΟΓΙΑ ΜΟΥ
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = null,
                                tint = Emerald700,
                                modifier = Modifier.size(17.dp)
                            )
                            Text(
                                text = if (isEn) "MY HOUSEHOLDS" else "ΤΑ ΕΞΟΔΟΛΟΓΙΑ ΜΟΥ",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Emerald700,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = Emerald700.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "${myHouseholds.size}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Emerald700,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }

                    myHouseholds.forEach { household ->
                        val isSelected = household.id == currentHousehold?.id
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) Emerald700.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) Emerald700 else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectHousehold(household)
                                    onDismiss()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = Localization.translateHouseholdName(household.name, lang),
                                            fontSize = 14.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Emerald700.copy(alpha = 0.12f),
                                            border = BorderStroke(0.5.dp, Emerald700.copy(alpha = 0.3f))
                                        ) {
                                            Text(
                                                text = if (isEn) "Owner" else "Ιδιοκτήτης",
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Emerald700,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (household.memberUids.size <= 1)
                                            (if (isEn) "1 member" else "1 μέλος")
                                        else
                                            (if (isEn) "${household.memberUids.size} members" else "${household.memberUids.size} μέλη"),
                                        fontSize = 11.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (isSelected) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Emerald700,
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = if (isEn) "Selected" else "Επιλεγμένο",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                        }
                                    }

                                    if (onDeleteHousehold != null && isHouseholdOwner(household)) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFFEF4444).copy(alpha = 0.12f),
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clickable { householdToDelete = household }
                                                .testTag("delete_household_btn_${household.id}")
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = if (isEn) "Delete Household" else "Διαγραφή Εξοδολογίου",
                                                    tint = Color(0xFFEF4444),
                                                    modifier = Modifier.size(17.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Action 1: Add New Exodologio (Pro feature with star & coloring)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isPremium) Emerald700.copy(alpha = 0.08f) else Amber700.copy(alpha = 0.08f),
                        border = BorderStroke(
                            1.dp,
                            if (isPremium) Emerald700.copy(alpha = 0.3f) else Amber700.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isPremium) {
                                    showCreateDialog = true
                                } else {
                                    onShowPremiumPaywall("multi_exodologio")
                                }
                            }
                            .testTag("add_new_exodologio_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = Localization.createNewExodologio(lang),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Amber700.copy(alpha = 0.18f),
                                border = BorderStroke(1.dp, Amber700.copy(alpha = 0.45f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = Amber700,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "PRO",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Amber700
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Close button
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
                        Text(Localization.close(lang), fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            shape = RoundedCornerShape(20.dp),
            title = { Text(if (lang == Localization.Language.ENGLISH) "New Household" else "Νέο Εξοδολόγιο", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = if (lang == Localization.Language.ENGLISH)
                            "Enter a name for the new household (e.g. Home, Vacation, Work, Travel):"
                        else
                            "Δώστε ένα όνομα για το νέο εξοδολόγιο (π.χ. Σπίτι, Εξοχικό, Εργασία, Ταξίδι):",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newHouseholdName,
                        onValueChange = { newHouseholdName = it },
                        placeholder = { Text(if (lang == Localization.Language.ENGLISH) "e.g. Personal Expenses" else "π.χ. Προσωπικά Έξοδα") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newHouseholdName.isNotBlank()) {
                            onCreateNewHousehold(newHouseholdName.trim())
                            showCreateDialog = false
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald700, contentColor = Color.White),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(if (lang == Localization.Language.ENGLISH) "Create" else "Δημιουργία", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text(if (lang == Localization.Language.ENGLISH) "Cancel" else "Ακύρωση")
                }
            }
        )
    }

    if (householdToDelete != null) {
        AlertDialog(
            onDismissRequest = { householdToDelete = null },
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = if (lang == Localization.Language.ENGLISH) "Delete Household" else "Διαγραφή Εξοδολογίου",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                val isSingle = allHouseholds.size <= 1
                Text(
                    text = if (lang == Localization.Language.ENGLISH) {
                        if (isSingle)
                            "Are you sure you want to delete \"${householdToDelete?.name}\"? All its expenses, incomes, categories and members will be permanently removed, and a fresh new household will be created."
                        else
                            "Are you sure you want to delete \"${householdToDelete?.name}\"? All its expenses, incomes, categories and members will be permanently removed."
                    } else {
                        if (isSingle)
                            "Είστε βέβαιοι ότι θέλετε να διαγράψετε οριστικά το εξοδολόγιο «${householdToDelete?.name}»; Όλα τα έσοδα, έξοδα, κατηγορίες και μέλη του θα διαγραφούν οριστικά και θα δημιουργηθεί ένα νέο κενό εξοδολόγιο."
                        else
                            "Είστε βέβαιοι ότι θέλετε να διαγράψετε οριστικά το εξοδολόγιο «${householdToDelete?.name}»; Όλα τα έσοδα, έξοδα, κατηγορίες και μέλη του θα διαγραφούν οριστικά."
                    },
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = householdToDelete
                        householdToDelete = null
                        if (target != null) {
                            onDeleteHousehold?.invoke(target)
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = if (lang == Localization.Language.ENGLISH) "Delete" else "Διαγραφή",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { householdToDelete = null }) {
                    Text(text = if (lang == Localization.Language.ENGLISH) "Cancel" else "Ακύρωση")
                }
            }
        )
    }
}

@Composable
fun PremiumPaywallDialog(
    reason: String, // "multi_exodologio" or "invite_member" or "profile_upgrade"
    appLanguage: String = "EL",
    onUnlockPro: (planId: String) -> Unit,
    onDismiss: () -> Unit
) {
    val lang = Localization.Language.fromCode(appLanguage)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 440.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .testTag("premium_paywall_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Star Badge & Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Amber700.copy(alpha = 0.15f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Amber700,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Fintel Pro",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                val paywallSubtitle = when (reason) {
                    "voice_limit" -> if (lang == Localization.Language.ENGLISH) "Monthly limit of 15 free voice entries reached" else "Συμπληρώσατε το μηνιαίο όριο των 15 φωνητικών καταχωρήσεων"
                    "savings_goal_limit" -> if (lang == Localization.Language.ENGLISH) "Free edition supports up to 1 Savings Goal" else "Η δωρεάν έκδοση υποστηρίζει έως 1 Στόχο Αποταμίευσης"
                    "recurring_expense_limit" -> if (lang == Localization.Language.ENGLISH) "Free edition supports up to 2 Recurring Expenses" else "Η δωρεάν έκδοση υποστηρίζει έως 2 Πάγιες Εντολές"
                    "multi_exodologio" -> if (lang == Localization.Language.ENGLISH) "Unlock unlimited households & wallets" else "Ξεκλειδώστε απεριόριστα ανεξάρτητα εξοδολόγια & πορτοφόλια"
                    "ocr_receipt" -> if (lang == Localization.Language.ENGLISH) "Camera & Gallery Receipt OCR is a Pro feature" else "Η σάρωση αποδείξεων με Κάμερα & OCR είναι δυνατότητα Pro"
                    "export_report" -> if (lang == Localization.Language.ENGLISH) "Exporting reports to PDF & CSV is a Pro feature" else "Η εξαγωγή αναφορών σε PDF & CSV είναι δυνατότητα Pro"
                    else -> null
                }

                if (paywallSubtitle != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = paywallSubtitle,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Amber700,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 3 Simple Direct Activation Buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Button 1: Monthly (2,99€ / μήνα)
                    Button(
                        onClick = {
                            onUnlockPro(com.example.billing.BillingManager.SUB_MONTHLY)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("activate_monthly_pro")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (lang == Localization.Language.ENGLISH) "Monthly" else "Μηνιαία",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp
                            )
                            Text(
                                text = if (lang == Localization.Language.ENGLISH) "2.99€ / month" else "2,99€ / μήνα",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                color = Amber700
                            )
                        }
                    }

                    // Button 2: Yearly (29€ / έτος) - Highlighted
                    Button(
                        onClick = {
                            onUnlockPro(com.example.billing.BillingManager.SUB_YEARLY)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Amber700,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("activate_yearly_pro")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (lang == Localization.Language.ENGLISH) "Yearly" else "Ετήσια",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color.White.copy(alpha = 0.25f)
                                ) {
                                    Text(
                                        text = if (lang == Localization.Language.ENGLISH) "POPULAR" else "ΔΗΜΟΦΙΛΕΣ",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = if (lang == Localization.Language.ENGLISH) "29€ / year" else "29€ / έτος",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.5.sp,
                                color = Color.White
                            )
                        }
                    }

                    // Button 3: Lifetime (49€)
                    Button(
                        onClick = {
                            onUnlockPro(com.example.billing.BillingManager.INAPP_LIFETIME)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("activate_lifetime_pro")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Lifetime",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp
                            )
                            Text(
                                text = "49€",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                color = Amber700
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Benefit List: Titles ONLY without descriptions
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Text(
                            text = if (lang == Localization.Language.ENGLISH) "What Pro includes:" else "Τι περιλαμβάνει το Fintel Pro:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        BenefitRowItem(
                            title = if (lang == Localization.Language.ENGLISH) "Unlimited Voice Entries" else "Απεριόριστη Φωνητική Καταχώρηση"
                        )
                        BenefitRowItem(
                            title = if (lang == Localization.Language.ENGLISH) "Receipt OCR & AADE QR Scanner" else "Σάρωση Αποδείξεων (Κάμερα & OCR)"
                        )
                        BenefitRowItem(
                            title = if (lang == Localization.Language.ENGLISH) "Bank Notifications Auto-Catcher" else "Αυτόματος Αναγνώστης Τραπεζών"
                        )
                        BenefitRowItem(
                            title = if (lang == Localization.Language.ENGLISH) "Multiple Households & Wallets" else "Πολλαπλά Εξοδολόγια & Πορτοφόλια"
                        )
                        BenefitRowItem(
                            title = if (lang == Localization.Language.ENGLISH) "Unlimited Goals & Recurring Expenses" else "Απεριόριστοι Στόχοι & Πάγιες Εντολές"
                        )
                        BenefitRowItem(
                            title = if (lang == Localization.Language.ENGLISH) "Export Reports to PDF & CSV" else "Εξαγωγή Αναφορών σε PDF & CSV"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("dismiss_paywall_button")
                ) {
                    Text(
                        text = if (lang == Localization.Language.ENGLISH) "Not now" else "Όχι τώρα",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun BenefitRowItem(
    title: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            shape = CircleShape,
            color = Amber700.copy(alpha = 0.15f),
            modifier = Modifier.size(18.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Amber700,
                    modifier = Modifier.size(11.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
