package com.example.ui.components

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.UserProfile
import com.example.ui.theme.Amber700
import com.example.ui.theme.Emerald100
import com.example.ui.theme.Emerald700
import com.example.util.ImageUtils
import com.example.util.Localization
import com.google.firebase.auth.FirebaseUser
import java.util.Calendar

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
    onUpdateProfilePhoto: (String) -> Unit = {},
    onShowPremiumPaywall: () -> Unit,
    onSignOut: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lang = Localization.Language.fromCode(appLanguage)
    var isEditingProfile by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val base64 = ImageUtils.uriToCompressedBase64(context, uri)
            if (base64 != null) {
                onUpdateProfilePhoto(base64)
            }
        }
    }

    // Editable fields initialized from current user profile
    var editedFirstName by remember {
        mutableStateOf(
            userProfile?.firstName?.ifBlank { null }
                ?: userProfile?.displayName?.split(" ")?.firstOrNull() ?: ""
        )
    }
    var editedLastName by remember {
        mutableStateOf(
            userProfile?.lastName?.ifBlank { null }
                ?: userProfile?.displayName?.split(" ")?.drop(1)?.joinToString(" ") ?: ""
        )
    }
    var editedBirthDate by remember {
        mutableStateOf(userProfile?.birthDate ?: "")
    }
    var editedPhone by remember {
        mutableStateOf(userProfile?.phoneNumber ?: "")
    }
    var editedOccupation by remember {
        mutableStateOf(userProfile?.occupation ?: "")
    }
    var editedCity by remember {
        mutableStateOf(userProfile?.city ?: "")
    }

    // Day / Month / Year sub-states for effortless, glitch-free date editing without sticky numbers
    val initialBirthParts = (userProfile?.birthDate ?: "").split("/").map { it.trim() }
    var birthDayText by remember {
        mutableStateOf(if (initialBirthParts.isNotEmpty() && initialBirthParts[0].isNotBlank()) initialBirthParts[0] else "")
    }
    var birthMonthText by remember {
        mutableStateOf(if (initialBirthParts.size > 1 && initialBirthParts[1].isNotBlank()) initialBirthParts[1] else "")
    }
    var birthYearText by remember {
        mutableStateOf(if (initialBirthParts.size > 2 && initialBirthParts[2].isNotBlank()) initialBirthParts[2] else "")
    }

    var showDatePickerDialog by remember { mutableStateOf(false) }

    // Sync whenever incoming userProfile updates from database or cache (when not actively editing)
    LaunchedEffect(userProfile) {
        if (!isEditingProfile && userProfile != null) {
            val derivedFirst = userProfile.firstName.ifBlank {
                userProfile.displayName.split(" ").firstOrNull() ?: ""
            }
            val derivedLast = userProfile.lastName.ifBlank {
                val parts = userProfile.displayName.split(" ")
                if (parts.size > 1) parts.drop(1).joinToString(" ") else ""
            }
            if (userProfile.firstName.isNotBlank() || editedFirstName.isBlank()) {
                editedFirstName = derivedFirst
            }
            if (userProfile.lastName.isNotBlank() || editedLastName.isBlank()) {
                editedLastName = derivedLast
            }
            if (userProfile.birthDate.isNotBlank() || editedBirthDate.isBlank()) {
                editedBirthDate = userProfile.birthDate
                val parts = userProfile.birthDate.split("/").map { it.trim() }
                birthDayText = parts.getOrNull(0)?.filter { it.isDigit() }?.take(2).orEmpty()
                birthMonthText = parts.getOrNull(1)?.filter { it.isDigit() }?.take(2).orEmpty()
                birthYearText = parts.getOrNull(2)?.filter { it.isDigit() }?.take(4).orEmpty()
            }
            if (userProfile.phoneNumber.isNotBlank() || editedPhone.isBlank()) {
                editedPhone = userProfile.phoneNumber
            }
            if (userProfile.occupation.isNotBlank() || editedOccupation.isBlank()) {
                editedOccupation = userProfile.occupation
            }
            if (userProfile.city.isNotBlank() || editedCity.isBlank()) {
                editedCity = userProfile.city
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
                .widthIn(max = 560.dp)
                .testTag("user_profile_dialog"),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Fixed Header (Title & Close Button)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Emerald700.copy(alpha = 0.12f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isEditingProfile) Icons.Default.Edit else Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Emerald700,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (isEditingProfile) {
                                    if (lang == Localization.Language.ENGLISH) "Edit Profile" else "Επεξεργασία Προφίλ"
                                } else {
                                    Localization.myProfile(lang)
                                },
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = currentUser?.email ?: (userProfile?.email ?: ""),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier
                            .size(34.dp)
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

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                // Scrollable Body - smoothly scrolls all the way to the very bottom!
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. User Identity & Avatar Card
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clickable {
                                            photoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        }
                                ) {
                                    UserAvatar(
                                        photoUrl = userProfile?.photoUrl?.ifBlank { null } ?: currentUser?.photoUrl?.toString(),
                                        displayName = editedFirstName.ifBlank { userProfile?.displayName },
                                        modifier = Modifier.fillMaxSize(),
                                        fontSize = 24.sp
                                    )
                                    Surface(
                                        shape = CircleShape,
                                        color = Emerald700,
                                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.surface),
                                        modifier = Modifier
                                            .size(24.dp)
                                            .align(Alignment.BottomEnd)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.PhotoCamera,
                                                contentDescription = "Change photo",
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    val displayedName = if (editedFirstName.isNotBlank() || editedLastName.isNotBlank()) {
                                        "$editedFirstName $editedLastName".trim()
                                    } else {
                                        userProfile?.displayName?.ifBlank { null } ?: Localization.fintelUser(lang)
                                    }

                                    Text(
                                        text = displayedName,
                                        fontSize = 17.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (userProfile?.isPremium == true) "Fintel Pro Member ★" else "Fintel Free Account",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (userProfile?.isPremium == true) Amber700 else Emerald700
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Avatar Action Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Emerald700.copy(alpha = 0.12f),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            photoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PhotoCamera,
                                            contentDescription = null,
                                            tint = Emerald700,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (lang == Localization.Language.ENGLISH) "Change Avatar" else "Αλλαγή Avatar",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Emerald700
                                        )
                                    }
                                }

                                if (currentUser?.photoUrl != null && userProfile?.photoUrl != currentUser.photoUrl.toString()) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                        modifier = Modifier.clickable {
                                            onUpdateProfilePhoto(currentUser.photoUrl.toString())
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Sync,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Google",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }

                                if (userProfile?.photoUrl?.isNotBlank() == true) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f),
                                        modifier = Modifier.clickable {
                                            onUpdateProfilePhoto("")
                                        }
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Remove avatar",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. Personal Information Section (View Mode vs Spacious Edit Mode)
                    if (isEditingProfile) {
                        // ==========================================
                        // EDIT MODE: Beautiful, spacious full-width form
                        // ==========================================
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                            border = BorderStroke(1.dp, Emerald700.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (lang == Localization.Language.ENGLISH) "Personal Information" else "Προσωπικά Στοιχεία",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Emerald700.copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            text = if (lang == Localization.Language.ENGLISH) "EDITING" else "ΕΠΕΞΕΡΓΑΣΙΑ",
                                            color = Emerald700,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                // 1. First Name
                                OutlinedTextField(
                                    value = editedFirstName,
                                    onValueChange = { editedFirstName = it },
                                    label = { Text(if (lang == Localization.Language.ENGLISH) "First Name" else "Όνομα") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = Emerald700, modifier = Modifier.size(20.dp))
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Emerald700,
                                        focusedLabelColor = Emerald700,
                                        cursorColor = Emerald700
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // 2. Last Name
                                OutlinedTextField(
                                    value = editedLastName,
                                    onValueChange = { editedLastName = it },
                                    label = { Text(if (lang == Localization.Language.ENGLISH) "Last Name" else "Επίθετο") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Badge, contentDescription = null, tint = Emerald700, modifier = Modifier.size(20.dp))
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Emerald700,
                                        focusedLabelColor = Emerald700,
                                        cursorColor = Emerald700
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // 3. Date of Birth (3 independent, glitch-free number fields with zero sticky numbers!)
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.CalendarMonth,
                                                contentDescription = null,
                                                tint = Emerald700,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (lang == Localization.Language.ENGLISH) "Date of Birth (DD / MM / YYYY)" else "Ημερομηνία Γέννησης (ΗΗ / ΜΜ / ΕΕΕΕ)",
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        IconButton(
                                            onClick = { showDatePickerDialog = true },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Edit,
                                                contentDescription = "Open Picker Dialog",
                                                tint = Emerald700,
                                                modifier = Modifier.size(17.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Day
                                        OutlinedTextField(
                                            value = birthDayText,
                                            onValueChange = { input ->
                                                birthDayText = input.filter { it.isDigit() }.take(2)
                                                val dStr = if (birthDayText.isNotBlank()) birthDayText.padStart(2, '0') else ""
                                                val mStr = if (birthMonthText.isNotBlank()) birthMonthText.padStart(2, '0') else ""
                                                editedBirthDate = if (dStr.isNotBlank() || mStr.isNotBlank() || birthYearText.isNotBlank()) {
                                                    "$dStr/$mStr/$birthYearText"
                                                } else ""
                                            },
                                            label = { Text("Ημέρα", fontSize = 11.5.sp) },
                                            placeholder = { Text("ΗΗ", fontSize = 12.sp) },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            shape = RoundedCornerShape(12.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = Emerald700,
                                                focusedLabelColor = Emerald700,
                                                cursorColor = Emerald700
                                            ),
                                            modifier = Modifier.weight(1f)
                                        )

                                        // Month
                                        OutlinedTextField(
                                            value = birthMonthText,
                                            onValueChange = { input ->
                                                birthMonthText = input.filter { it.isDigit() }.take(2)
                                                val dStr = if (birthDayText.isNotBlank()) birthDayText.padStart(2, '0') else ""
                                                val mStr = if (birthMonthText.isNotBlank()) birthMonthText.padStart(2, '0') else ""
                                                editedBirthDate = if (dStr.isNotBlank() || mStr.isNotBlank() || birthYearText.isNotBlank()) {
                                                    "$dStr/$mStr/$birthYearText"
                                                } else ""
                                            },
                                            label = { Text("Μήνας", fontSize = 11.5.sp) },
                                            placeholder = { Text("ΜΜ", fontSize = 12.sp) },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            shape = RoundedCornerShape(12.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = Emerald700,
                                                focusedLabelColor = Emerald700,
                                                cursorColor = Emerald700
                                            ),
                                            modifier = Modifier.weight(1f)
                                        )

                                        // Year
                                        OutlinedTextField(
                                            value = birthYearText,
                                            onValueChange = { input ->
                                                birthYearText = input.filter { it.isDigit() }.take(4)
                                                val dStr = if (birthDayText.isNotBlank()) birthDayText.padStart(2, '0') else ""
                                                val mStr = if (birthMonthText.isNotBlank()) birthMonthText.padStart(2, '0') else ""
                                                editedBirthDate = if (dStr.isNotBlank() || mStr.isNotBlank() || birthYearText.isNotBlank()) {
                                                    "$dStr/$mStr/$birthYearText"
                                                } else ""
                                            },
                                            label = { Text("Έτος", fontSize = 11.5.sp) },
                                            placeholder = { Text("ΕΕΕΕ", fontSize = 12.sp) },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            shape = RoundedCornerShape(12.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = Emerald700,
                                                focusedLabelColor = Emerald700,
                                                cursorColor = Emerald700
                                            ),
                                            modifier = Modifier.weight(1.3f)
                                        )
                                    }
                                }

                                // 4. Mobile Phone
                                OutlinedTextField(
                                    value = editedPhone,
                                    onValueChange = { editedPhone = it },
                                    label = { Text(if (lang == Localization.Language.ENGLISH) "Mobile Phone" else "Κινητό Τηλέφωνο") },
                                    placeholder = { Text("69XXXXXXXX") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Phone, contentDescription = null, tint = Emerald700, modifier = Modifier.size(20.dp))
                                    },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Emerald700,
                                        focusedLabelColor = Emerald700,
                                        cursorColor = Emerald700
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // 5. Occupation
                                OutlinedTextField(
                                    value = editedOccupation,
                                    onValueChange = { editedOccupation = it },
                                    label = { Text(if (lang == Localization.Language.ENGLISH) "Occupation" else "Επάγγελμα") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Work, contentDescription = null, tint = Emerald700, modifier = Modifier.size(20.dp))
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Emerald700,
                                        focusedLabelColor = Emerald700,
                                        cursorColor = Emerald700
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // 6. City
                                OutlinedTextField(
                                    value = editedCity,
                                    onValueChange = { editedCity = it },
                                    label = { Text(if (lang == Localization.Language.ENGLISH) "City" else "Πόλη") },
                                    leadingIcon = {
                                        Icon(Icons.Default.LocationCity, contentDescription = null, tint = Emerald700, modifier = Modifier.size(20.dp))
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Emerald700,
                                        focusedLabelColor = Emerald700,
                                        cursorColor = Emerald700
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                // Action Buttons: Cancel & Save Changes
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            // Revert inputs to current profile
                                            userProfile?.let {
                                                editedFirstName = it.firstName
                                                editedLastName = it.lastName
                                                editedBirthDate = it.birthDate
                                                editedPhone = it.phoneNumber
                                                editedOccupation = it.occupation
                                                editedCity = it.city
                                                val parts = it.birthDate.split("/").map { p -> p.trim() }
                                                birthDayText = parts.getOrNull(0)?.filter { c -> c.isDigit() }?.take(2).orEmpty()
                                                birthMonthText = parts.getOrNull(1)?.filter { c -> c.isDigit() }?.take(2).orEmpty()
                                                birthYearText = parts.getOrNull(2)?.filter { c -> c.isDigit() }?.take(4).orEmpty()
                                            }
                                            isEditingProfile = false
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                    ) {
                                        Text(
                                            text = Localization.cancel(lang),
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.5.sp
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            val dStr = if (birthDayText.isNotBlank()) birthDayText.padStart(2, '0') else ""
                                            val mStr = if (birthMonthText.isNotBlank()) birthMonthText.padStart(2, '0') else ""
                                            val finalDate = if (dStr.isNotBlank() || mStr.isNotBlank() || birthYearText.isNotBlank()) {
                                                "$dStr/$mStr/$birthYearText"
                                            } else {
                                                editedBirthDate
                                            }

                                            val fullName = "$editedFirstName $editedLastName".trim()
                                            editedBirthDate = finalDate
                                            onSaveFullProfile(
                                                editedFirstName,
                                                editedLastName,
                                                finalDate,
                                                editedPhone,
                                                editedOccupation,
                                                editedCity
                                            )
                                            isEditingProfile = false
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Emerald700),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .weight(1.3f)
                                            .height(48.dp)
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = Localization.save(lang),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // ==========================================
                        // VIEW MODE: Clean, elegant list layout
                        // ==========================================
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (lang == Localization.Language.ENGLISH) "Personal Information" else "Προσωπικά Στοιχεία",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Emerald700.copy(alpha = 0.12f),
                                        modifier = Modifier.clickable { isEditingProfile = true }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = null,
                                                tint = Emerald700,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = Localization.edit(lang),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Emerald700
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Information Rows - beautifully structured without squishing
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    ProfileInfoRow(
                                        icon = Icons.Default.Person,
                                        label = if (lang == Localization.Language.ENGLISH) "Full Name" else "Ονοματεπώνυμο",
                                        value = if (editedFirstName.isNotBlank() || editedLastName.isNotBlank())
                                            "$editedFirstName $editedLastName".trim()
                                        else
                                            (userProfile?.displayName?.ifBlank { null } ?: if (lang == Localization.Language.ENGLISH) "Not set" else "Δεν έχει οριστεί")
                                    )

                                    ProfileInfoRow(
                                        icon = Icons.Default.CalendarMonth,
                                        label = if (lang == Localization.Language.ENGLISH) "Date of Birth" else "Ημερομηνία Γέννησης",
                                        value = (userProfile?.birthDate?.ifBlank { null } ?: editedBirthDate).ifBlank { if (lang == Localization.Language.ENGLISH) "Not set" else "Δεν έχει οριστεί" }
                                    )

                                    ProfileInfoRow(
                                        icon = Icons.Default.Phone,
                                        label = if (lang == Localization.Language.ENGLISH) "Mobile Phone" else "Κινητό Τηλέφωνο",
                                        value = (userProfile?.phoneNumber?.ifBlank { null } ?: editedPhone).ifBlank { if (lang == Localization.Language.ENGLISH) "Not set" else "Δεν έχει οριστεί" }
                                    )

                                    ProfileInfoRow(
                                        icon = Icons.Default.Work,
                                        label = if (lang == Localization.Language.ENGLISH) "Occupation" else "Επάγγελμα",
                                        value = (userProfile?.occupation?.ifBlank { null } ?: editedOccupation).ifBlank { if (lang == Localization.Language.ENGLISH) "Not set" else "Δεν έχει οριστεί" }
                                    )

                                    ProfileInfoRow(
                                        icon = Icons.Default.LocationCity,
                                        label = if (lang == Localization.Language.ENGLISH) "City" else "Πόλη",
                                        value = (userProfile?.city?.ifBlank { null } ?: editedCity).ifBlank { if (lang == Localization.Language.ENGLISH) "Not set" else "Δεν έχει οριστεί" }
                                    )

                                    ProfileInfoRow(
                                        icon = Icons.Default.Email,
                                        label = if (lang == Localization.Language.ENGLISH) "Account Email" else "Email Λογαριασμού",
                                        value = currentUser?.email ?: (userProfile?.email ?: (if (lang == Localization.Language.ENGLISH) "Not set" else "Δεν έχει οριστεί"))
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                OutlinedButton(
                                    onClick = { isEditingProfile = true },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Emerald700),
                                    border = BorderStroke(1.2.dp, Emerald700.copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(46.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp), tint = Emerald700)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (lang == Localization.Language.ENGLISH) "Edit Personal Information" else "Επεξεργασία Στοιχείων",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp,
                                        color = Emerald700
                                    )
                                }
                            }
                        }
                    }

                    // 3. Plan / Subscription Card
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = if (userProfile?.isPremium == true) Amber700.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (userProfile?.isPremium == true) Amber700.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = (if (userProfile?.isPremium == true) Amber700 else MaterialTheme.colorScheme.outline).copy(alpha = 0.15f),
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = null,
                                                tint = if (userProfile?.isPremium == true) Amber700 else MaterialTheme.colorScheme.outline,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = if (userProfile?.isPremium == true) "Fintel Pro" else Localization.freePlan(lang),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (userProfile?.isPremium == true)
                                                (if (lang == Localization.Language.ENGLISH) "Unlimited households & members" else "Απεριόριστα εξοδολόγια & μέλη")
                                            else
                                                Localization.activeHouseholdCount(lang, 1),
                                            fontSize = 12.sp,
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
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }

                            if (userProfile?.isPremium != true) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        onDismiss()
                                        onShowPremiumPaywall()
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Amber700),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .testTag("profile_upgrade_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = Localization.upgradeToPro(lang),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    // 4. Stats Snapshot
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
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = Localization.transactionsLabel(lang),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "$expenseCount",
                                    fontSize = 17.sp,
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
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = Localization.totalExpensesLabel(lang),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = String.format(java.util.Locale.US, "%.2f %s", totalSpent, currencySymbol),
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Emerald700
                                )
                            }
                        }
                    }

                    // 5. Sign Out Button
                    Button(
                        onClick = {
                            onDismiss()
                            onSignOut()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("profile_dialog_signout_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = Localization.signOutAccount(lang), fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                        }
                    }

                    // Generous bottom spacer so Sign Out and everything above it is 100% visible and accessible
                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
        }
    }

    // Date Picker Dialog for Profile Edit (with full backspace/delete support and zero stickiness)
    if (showDatePickerDialog) {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)

        var dialogDayText by remember(showDatePickerDialog) {
            mutableStateOf(birthDayText)
        }
        var dialogMonthText by remember(showDatePickerDialog) {
            mutableStateOf(birthMonthText)
        }
        var dialogYearText by remember(showDatePickerDialog) {
            mutableStateOf(birthYearText)
        }

        Dialog(
            onDismissRequest = { showDatePickerDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp)
                    .widthIn(max = 440.dp)
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
                            fontSize = 17.sp,
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
                            value = dialogDayText,
                            onValueChange = { input ->
                                dialogDayText = input.filter { it.isDigit() }.take(2)
                            },
                            label = { Text("Ημέρα") },
                            placeholder = { Text("ΗΗ") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = dialogMonthText,
                            onValueChange = { input ->
                                dialogMonthText = input.filter { it.isDigit() }.take(2)
                            },
                            label = { Text("Μήνας") },
                            placeholder = { Text("ΜΜ") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = dialogYearText,
                            onValueChange = { input ->
                                dialogYearText = input.filter { it.isDigit() }.take(4)
                            },
                            label = { Text("Έτος") },
                            placeholder = { Text("ΕΕΕΕ") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                                val d = dialogDayText.toIntOrNull()?.coerceIn(1, 31) ?: 1
                                val m = dialogMonthText.toIntOrNull()?.coerceIn(1, 12) ?: 1
                                val y = dialogYearText.toIntOrNull()?.coerceIn(1920, currentYear) ?: 1995
                                val dayStr = d.toString().padStart(2, '0')
                                val monthStr = m.toString().padStart(2, '0')
                                birthDayText = dayStr
                                birthMonthText = monthStr
                                birthYearText = y.toString()
                                editedBirthDate = "$dayStr/$monthStr/$y"
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

/**
 * Beautiful full-width detail row for View Mode.
 * Prominently presents Category on top and Value below without text wrapping, squishing, or cutting off.
 */
@Composable
private fun ProfileInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    val isUnset = value == "Δεν έχει οριστεί" || value == "Not set"

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Emerald700.copy(alpha = 0.1f),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Emerald700,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Emerald700,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = value,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isUnset) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
