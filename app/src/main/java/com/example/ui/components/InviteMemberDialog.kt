package com.example.ui.components

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Household
import com.example.ui.theme.Emerald700

@Composable
fun InviteMemberDialog(
    household: Household,
    isPro: Boolean,
    appLanguage: String,
    currentUserEmail: String = "",
    onRoleSelected: (String) -> Unit = {},
    onInviteEmail: (String, String) -> Unit,
    onRemoveInvitedEmail: ((String) -> Unit)? = null,
    onMessage: (String) -> Unit,
    onShowPremiumPaywall: () -> Unit,
    onDismiss: () -> Unit
) {
    val isGreek = appLanguage != "EN"
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    var emailInput by remember { mutableStateOf("") }
    // Role state: "CONTRIBUTOR" (Καταχωρητής) or "OWNER" (Ιδιοκτήτης)
    var selectedRole by remember { mutableStateOf("CONTRIBUTOR") }
    var emailToRemove by remember { mutableStateOf<String?>(null) }

    val cleanCode = household.inviteCode.ifBlank { "OIKO-7788" }
    val inviteUrl = "https://play.google.com/store/apps/details?id=com.aistudio.oikonomos.app&code=$cleanCode&role=$selectedRole"

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("invite_member_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Emerald700.copy(alpha = 0.12f),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = "Invite Member",
                                tint = Emerald700,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = if (isGreek) "Πρόσκληση Μέλους" else "Invite Member",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = household.name,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Emerald700
                        )
                    }
                }

                // Section: Role selection (Minimal, concise explanations)
                Text(
                    text = if (isGreek) "ΔΙΚΑΙΩΜΑΤΑ ΠΡΟΣΚΕΚΛΗΜΕΝΟΥ" else "MEMBER PERMISSIONS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    letterSpacing = 0.5.sp
                )

                // Option 1: Καταχωρητής (Contributor)
                val isContributor = selectedRole == "CONTRIBUTOR"
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isContributor) Emerald700.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(
                        if (isContributor) 1.5.dp else 1.dp,
                        if (isContributor) Emerald700 else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            selectedRole = "CONTRIBUTOR"
                            onRoleSelected("CONTRIBUTOR")
                        }
                        .testTag("role_option_contributor")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = if (isContributor) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (isContributor) Emerald700 else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isGreek) "Καταχωρητής" else "Contributor",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isGreek) "Καταχώρηση εσόδων & εξόδων" else "Log expenses & incomes",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Option 2: Ιδιοκτήτης (Owner / Co-owner)
                val isOwner = selectedRole == "OWNER"
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isOwner) Emerald700.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(
                        if (isOwner) 1.5.dp else 1.dp,
                        if (isOwner) Emerald700 else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            selectedRole = "OWNER"
                            onRoleSelected("OWNER")
                        }
                        .testTag("role_option_owner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = if (isOwner) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (isOwner) Emerald700 else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isGreek) "Ιδιοκτήτης" else "Owner",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isGreek) "Πλήρης διαχείριση εξοδολογίου" else "Full household management",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Section: Email Input (Primary Invitation Mechanism)
                Text(
                    text = if (isGreek) "EMAIL ΧΡΗΣΤΗ" else "USER EMAIL",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    letterSpacing = 0.5.sp
                )

                OutlinedTextField(
                    value = emailInput,
                    onValueChange = { emailInput = it },
                    placeholder = { Text(if (isGreek) "π.χ. family@gmail.com" else "e.g. member@gmail.com", fontSize = 13.5.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            tint = Emerald700,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Send
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (emailInput.isNotBlank()) {
                                onInviteEmail(emailInput.trim(), selectedRole)
                                emailInput = ""
                            }
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Emerald700,
                        focusedLabelColor = Emerald700,
                        cursorColor = Emerald700
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("invite_email_input")
                )

                // Primary Add / Send Button
                Button(
                    onClick = {
                        if (emailInput.isBlank() || !emailInput.contains("@") || !emailInput.contains(".")) {
                            onMessage(if (isGreek) "Παρακαλώ εισάγετε έγκυρη διεύθυνση email" else "Please enter a valid email address")
                        } else {
                            onInviteEmail(emailInput.trim(), selectedRole)
                            emailInput = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald700),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("send_invite_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isGreek) "Προσθήκη & Πρόσκληση" else "Add & Invite",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                // Currently invited emails list
                if (household.invitedEmails.isNotEmpty()) {
                    Text(
                        text = if (isGreek) "ΠΡΟΣΚΕΚΛΗΜΕΝΑ EMAIL" else "INVITED EMAILS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        letterSpacing = 0.5.sp
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        household.invitedEmails.distinct().forEach { invitedEmail ->
                            val roleForEmail = household.memberRoles[invitedEmail.lowercase()] ?: "CONTRIBUTOR"
                            val roleLabel = if (roleForEmail == "OWNER")
                                (if (isGreek) "Ιδιοκτήτης" else "Owner")
                            else
                                (if (isGreek) "Καταχωρητής" else "Contributor")

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = invitedEmail,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (roleForEmail == "OWNER") Emerald700.copy(alpha = 0.15f) else Color(0xFF0284C7).copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = roleLabel,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (roleForEmail == "OWNER") Emerald700 else Color(0xFF0284C7),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }

                                        if (onRemoveInvitedEmail != null && !invitedEmail.equals(currentUserEmail, ignoreCase = true)) {
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFFEF4444).copy(alpha = 0.12f))
                                                    .clickable { emailToRemove = invitedEmail }
                                                    .testTag("remove_invited_email_btn"),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = if (isGreek) "Αφαίρεση" else "Remove",
                                                    modifier = Modifier.size(15.dp),
                                                    tint = Color(0xFFEF4444)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    modifier = Modifier.padding(vertical = 2.dp)
                )

                // Secondary option: Share / Copy link
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(inviteUrl))
                            onMessage(if (isGreek) "Ο σύνδεσμος πρόσκλησης αντιγράφηκε!" else "Invite link copied!")
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            tint = Emerald700,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isGreek) "Αντιγραφή" else "Copy Link",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Emerald700
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            val roleText = if (selectedRole == "OWNER")
                                (if (isGreek) "Ιδιοκτήτης" else "Owner")
                            else
                                (if (isGreek) "Καταχωρητής" else "Contributor")
                            val shareText = if (isGreek) """
                                Σε προσκαλώ στο εξοδολόγιο «${household.name}» στο Fintel!
                                
                                Ρόλος: $roleText
                                Σύνδεσμος: $inviteUrl
                                Κωδικός: $cleanCode
                            """.trimIndent() else """
                                You are invited to household "${household.name}" on Fintel!
                                
                                Role: $roleText
                                Link: $inviteUrl
                                Code: $cleanCode
                            """.trimIndent()

                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, if (isGreek) "Πρόσκληση στο Fintel" else "Fintel Invitation")
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            try {
                                context.startActivity(Intent.createChooser(shareIntent, if (isGreek) "Κοινοποίηση" else "Share"))
                            } catch (_: Exception) {}
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = Emerald700,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isGreek) "Κοινοποίηση" else "Share",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Emerald700
                        )
                    }
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        text = if (isGreek) "Κλείσιμο" else "Close",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (emailToRemove != null) {
        val target = emailToRemove!!
        AlertDialog(
            onDismissRequest = { emailToRemove = null },
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
                        text = if (isGreek) "Αφαίρεση Πρόσβασης" else "Revoke Access",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                Text(
                    text = if (isGreek)
                        "Είστε βέβαιοι ότι θέλετε να αφαιρέσετε την πρόσβαση του χρήστη «$target» από αυτό το εξοδολόγιο; Δεν θα μπορεί πλέον να βλέπει ή να καταχωρεί έξοδα."
                    else
                        "Are you sure you want to revoke access for \"$target\" from this household? They will no longer be able to view or log expenses.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val toRemove = emailToRemove
                        emailToRemove = null
                        if (toRemove != null) {
                            onRemoveInvitedEmail?.invoke(toRemove)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = if (isGreek) "Αφαίρεση" else "Revoke",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { emailToRemove = null }) {
                    Text(text = if (isGreek) "Ακύρωση" else "Cancel")
                }
            }
        )
    }
}
