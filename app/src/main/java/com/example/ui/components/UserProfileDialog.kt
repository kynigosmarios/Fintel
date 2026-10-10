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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.UserProfile
import com.example.ui.theme.Amber700
import com.example.ui.theme.Emerald100
import com.example.ui.theme.Emerald700
import com.example.util.Localization
import com.google.firebase.auth.FirebaseUser

@Composable
fun UserProfileDialog(
    currentUser: FirebaseUser?,
    userProfile: UserProfile?,
    expenseCount: Int,
    totalSpent: Double,
    currencySymbol: String,
    appLanguage: String = "EL",
    onSaveDisplayName: (String) -> Unit,
    onSaveFullProfile: (firstName: String, lastName: String, birthDate: String, phone: String, occupation: String, city: String) -> Unit = { _, _, _, _, _, _ -> },
    onShowPremiumPaywall: () -> Unit,
    onSignOut: () -> Unit,
    onDismiss: () -> Unit
) {
    val lang = Localization.Language.fromCode(appLanguage)
    var isEditingProfile by remember { mutableStateOf(false) }

    // Editable fields initialized from current user profile
    var editedFirstName by remember(userProfile?.firstName, userProfile?.displayName) {
        mutableStateOf(
            userProfile?.firstName?.ifBlank { null }
                ?: userProfile?.displayName?.split(" ")?.firstOrNull() ?: ""
        )
    }
    var editedLastName by remember(userProfile?.lastName, userProfile?.displayName) {
        mutableStateOf(
            userProfile?.lastName?.ifBlank { null }
                ?: userProfile?.displayName?.split(" ")?.drop(1)?.joinToString(" ") ?: ""
        )
    }
    var editedBirthDate by remember(userProfile?.birthDate) {
        mutableStateOf(userProfile?.birthDate ?: "")
    }
    var editedPhone by remember(userProfile?.phoneNumber) {
        mutableStateOf(userProfile?.phoneNumber ?: "")
    }
    var editedOccupation by remember(userProfile?.occupation) {
        mutableStateOf(userProfile?.occupation ?: "")
    }
    var editedCity by remember(userProfile?.city) {
        mutableStateOf(userProfile?.city ?: "")
    }

    var showDatePickerDialog by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .padding(vertical = 12.dp)
                .testTag("user_profile_dialog"),
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Emerald700.copy(alpha = 0.12f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Emerald700,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = Localization.myProfile(lang),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
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

                // Scrollable Body
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // User Identity Card & Detailed Personal Information
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            // Header with Avatar and Edit Mode Toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Emerald700,
                                        modifier = Modifier.size(50.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            val initial = editedFirstName.take(1).ifBlank {
                                                userProfile?.displayName?.take(1) ?: "X"
                                            }.uppercase()
                                            Text(
                                                text = initial,
                                                color = Color.White,
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = if (editedFirstName.isNotBlank() || editedLastName.isNotBlank()) {
                                                "$editedFirstName $editedLastName".trim()
                                            } else {
                                                userProfile?.displayName?.ifBlank { null } ?: "Χρήστης Estia"
                                            },
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = currentUser?.email ?: "",
                                            fontSize = 11.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Surface(
                                    shape = CircleShape,
                                    color = if (isEditingProfile) Emerald700 else Emerald700.copy(alpha = 0.12f),
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clickable {
                                            if (isEditingProfile) {
                                                // Save profile
                                                val fullName = "$editedFirstName $editedLastName".trim()
                                                onSaveDisplayName(fullName)
                                                onSaveFullProfile(
                                                    editedFirstName,
                                                    editedLastName,
                                                    editedBirthDate,
                                                    editedPhone,
                                                    editedOccupation,
                                                    editedCity
                                                )
                                                isEditingProfile = false
                                            } else {
                                                isEditingProfile = true
                                            }
                                        }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (isEditingProfile) Icons.Default.Check else Icons.Default.Edit,
                                            contentDescription = if (isEditingProfile) Localization.save(lang) else Localization.edit(lang),
                                            tint = if (isEditingProfile) Color.White else Emerald700,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            if (isEditingProfile) {
                                // EDITING MODE: Editable inputs with labels
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = editedFirstName,
                                            onValueChange = { editedFirstName = it },
                                            label = { Text(if (lang == Localization.Language.ENGLISH) "First Name" else "Όνομα") },
                                            singleLine = true,
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = editedLastName,
                                            onValueChange = { editedLastName = it },
                                            label = { Text(if (lang == Localization.Language.ENGLISH) "Last Name" else "Επίθετο") },
                                            singleLine = true,
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    // Date of birth
                                    OutlinedTextField(
                                        value = editedBirthDate,
                                        onValueChange = { editedBirthDate = it },
                                        readOnly = true,
                                        label = { Text(if (lang == Localization.Language.ENGLISH) "Date of Birth" else "Ημερομηνία Γέννησης") },
                                        placeholder = { Text("ΗΗ/ΜΜ/ΕΕΕΕ") },
                                        leadingIcon = {
                                            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Emerald700, modifier = Modifier.size(18.dp))
                                        },
                                        trailingIcon = {
                                            IconButton(onClick = { showDatePickerDialog = true }) {
                                                Icon(Icons.Default.CalendarMonth, contentDescription = "Pick Date", tint = Emerald700)
                                            }
                                        },
                                        singleLine = true,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { showDatePickerDialog = true }
                                    )

                                    // Mobile Phone
                                    OutlinedTextField(
                                        value = editedPhone,
                                        onValueChange = { editedPhone = it },
                                        label = { Text(if (lang == Localization.Language.ENGLISH) "Mobile Phone" else "Κινητό Τηλέφωνο") },
                                        placeholder = { Text("69XXXXXXXX") },
                                        leadingIcon = {
                                            Icon(Icons.Default.Phone, contentDescription = null, tint = Emerald700, modifier = Modifier.size(18.dp))
                                        },
                                        singleLine = true,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = editedOccupation,
                                            onValueChange = { editedOccupation = it },
                                            label = { Text(if (lang == Localization.Language.ENGLISH) "Occupation" else "Επάγγελμα") },
                                            singleLine = true,
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = editedCity,
                                            onValueChange = { editedCity = it },
                                            label = { Text(if (lang == Localization.Language.ENGLISH) "City" else "Πόλη") },
                                            singleLine = true,
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Button(
                                        onClick = {
                                            val fullName = "$editedFirstName $editedLastName".trim()
                                            onSaveDisplayName(fullName)
                                            onSaveFullProfile(
                                                editedFirstName,
                                                editedLastName,
                                                editedBirthDate,
                                                editedPhone,
                                                editedOccupation,
                                                editedCity
                                            )
                                            isEditingProfile = false
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Emerald700),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(42.dp)
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(text = Localization.save(lang), color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            } else {
                                // VIEW MODE: Elegantly styled rows for each field synced with DB
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    ProfileInfoRow(
                                        icon = Icons.Default.CalendarMonth,
                                        label = if (lang == Localization.Language.ENGLISH) "Date of Birth" else "Ημερομηνία Γέννησης",
                                        value = editedBirthDate.ifBlank { "Δεν έχει οριστεί" }
                                    )
                                    ProfileInfoRow(
                                        icon = Icons.Default.Phone,
                                        label = if (lang == Localization.Language.ENGLISH) "Mobile Phone" else "Κινητό Τηλέφωνο",
                                        value = editedPhone.ifBlank { "Δεν έχει οριστεί" }
                                    )
                                    ProfileInfoRow(
                                        icon = Icons.Default.Work,
                                        label = if (lang == Localization.Language.ENGLISH) "Occupation" else "Επάγγελμα",
                                        value = editedOccupation.ifBlank { "Δεν έχει οριστεί" }
                                    )
                                    ProfileInfoRow(
                                        icon = Icons.Default.LocationCity,
                                        label = if (lang == Localization.Language.ENGLISH) "City" else "Πόλη",
                                        value = editedCity.ifBlank { "Δεν έχει οριστεί" }
                                    )
                                }
                            }
                        }
                    }

                    // Plan / Subscription Card
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (userProfile?.isPremium == true) Amber700.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (userProfile?.isPremium == true) Amber700.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = if (userProfile?.isPremium == true) Amber700 else MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = if (userProfile?.isPremium == true) "Estia Pro" else Localization.freePlan(lang),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (userProfile?.isPremium == true)
                                                (if (lang == Localization.Language.ENGLISH) "Unlimited households & members" else "Απεριόριστα εξοδολόγια & μέλη")
                                            else
                                                Localization.activeHouseholdCount(lang, 1),
                                            fontSize = 11.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (userProfile?.isPremium == true) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Amber700.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = if (lang == Localization.Language.ENGLISH) "ACTIVE" else "ΕΝΕΡΓΟ",
                                            color = Amber700,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }

                            if (userProfile?.isPremium != true) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = {
                                        onDismiss()
                                        onShowPremiumPaywall()
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Amber700),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(40.dp)
                                        .testTag("profile_upgrade_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = Localization.upgradeToPro(lang),
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    // Stats Snapshot
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = Localization.transactionsLabel(lang),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "$expenseCount",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = Localization.totalExpensesLabel(lang),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = String.format(java.util.Locale.US, "%.2f %s", totalSpent, currencySymbol),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Emerald700
                                )
                            }
                        }
                    }

                    // Sign Out Button
                    Button(
                        onClick = {
                            onDismiss()
                            onSignOut()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("profile_dialog_signout_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = Localization.signOutAccount(lang), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }

    // Date Picker Dialog for Profile Edit
    if (showDatePickerDialog) {
        val calendar = java.util.Calendar.getInstance()
        val currentYear = calendar.get(java.util.Calendar.YEAR)
        var selectedDay by remember { mutableStateOf(1) }
        var selectedMonth by remember { mutableStateOf(1) }
        var selectedYear by remember { mutableStateOf(1995) }

        Dialog(
            onDismissRequest = { showDatePickerDialog = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Emerald100,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Emerald700, modifier = Modifier.size(22.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (lang == Localization.Language.ENGLISH) "Select Date of Birth" else "Επιλογή Ημερομηνίας Γέννησης",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = if (selectedDay > 0) selectedDay.toString() else "",
                            onValueChange = {
                                val d = it.filter { ch -> ch.isDigit() }.take(2).toIntOrNull() ?: 1
                                selectedDay = d.coerceIn(1, 31)
                            },
                            label = { Text("Ημέρα") },
                            placeholder = { Text("1-31") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = if (selectedMonth > 0) selectedMonth.toString() else "",
                            onValueChange = {
                                val m = it.filter { ch -> ch.isDigit() }.take(2).toIntOrNull() ?: 1
                                selectedMonth = m.coerceIn(1, 12)
                            },
                            label = { Text("Μήνας") },
                            placeholder = { Text("1-12") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = if (selectedYear > 0) selectedYear.toString() else "",
                            onValueChange = {
                                val y = it.filter { ch -> ch.isDigit() }.take(4).toIntOrNull() ?: 1995
                                selectedYear = y.coerceIn(1920, currentYear)
                            },
                            label = { Text("Έτος") },
                            placeholder = { Text("1995") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1.3f)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showDatePickerDialog = false }) {
                            Text(text = if (lang == Localization.Language.ENGLISH) "Cancel" else "Άκυρο", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val dayStr = selectedDay.toString().padStart(2, '0')
                                val monthStr = selectedMonth.toString().padStart(2, '0')
                                editedBirthDate = "$dayStr/$monthStr/$selectedYear"
                                showDatePickerDialog = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Emerald700)
                        ) {
                            Text(text = if (lang == Localization.Language.ENGLISH) "Confirm" else "Επιβεβαίωση", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Emerald700,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = label,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
