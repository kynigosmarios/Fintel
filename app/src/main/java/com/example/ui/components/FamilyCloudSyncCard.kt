package com.example.ui.components

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.data.model.Household
import com.example.ui.theme.Emerald100
import com.example.ui.theme.Emerald700
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.example.util.Localization
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun FamilyCloudSyncCard(
    currentUser: FirebaseUser?,
    household: Household?,
    isSyncing: Boolean,
    onCreateHousehold: (String) -> Unit,
    onJoinHousehold: (String) -> Unit,
    onSignOut: () -> Unit,
    onMessage: (String) -> Unit,
    appLanguage: String = "EL"
) {
    val lang = Localization.Language.fromCode(appLanguage)
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isSigningIn by remember { mutableStateOf(false) }

    var familyNameInput by remember { mutableStateOf("") }
    var joinCodeInput by remember { mutableStateOf("") }
    var showJoinDialog by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth().testTag("family_cloud_sync_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = Emerald100,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Cloud,
                                contentDescription = null,
                                tint = Emerald700,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (lang == Localization.Language.ENGLISH) "Sync & Family" else "Συγχρονισμός & Οικογένεια",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (currentUser != null) {
                                if (lang == Localization.Language.ENGLISH) "Connected account" else "Συνδεδεμένος λογαριασμός"
                            } else {
                                if (lang == Localization.Language.ENGLISH) "Shared tracking with family" else "Κοινό εξοδολόγιο με τη σύζυγο"
                            },
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // State 1: User NOT logged in -> Google Sign-In button
            if (currentUser == null) {
                Text(
                    text = if (lang == Localization.Language.ENGLISH)
                        "Sign in with your Google account to share the household ledger in real time."
                    else
                        "Συνδεθείτε με το Google λογαριασμό σας για να μοιράζεστε το εξοδολόγιο σε πραγματικό χρόνο.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        isSigningIn = true
                        val credentialManager = CredentialManager.create(context)
                        val serverClientId = try {
                            context.getString(R.string.default_web_client_id)
                        } catch (e: Exception) {
                            "351790236571-093q02uts0ju5do0hp4os8el545l8oai.apps.googleusercontent.com"
                        }

                        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = serverClientId).build()
                        val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

                        coroutineScope.launch {
                            try {
                                val result = credentialManager.getCredential(context as Activity, request)
                                val credential = result.credential
                                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                                    val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                                    Firebase.auth.signInWithCredential(authCredential).await()
                                    onMessage(if (lang == Localization.Language.ENGLISH) "Signed in successfully!" else "Συνδεθήκατε επιτυχώς!")
                                } else {
                                    onMessage(if (lang == Localization.Language.ENGLISH) "Invalid credential" else "Μη έγκυρο διαπιστευτήριο")
                                }
                            } catch (e: GetCredentialCancellationException) {
                                Log.w("Auth", "Google Sign-In cancelled: ${e.message}")
                            } catch (e: Exception) {
                                Log.e("Auth", "Google Sign-In error", e)
                                onMessage(if (lang == Localization.Language.ENGLISH) "Connection error: ${e.localizedMessage}" else "Σφάλμα σύνδεσης: ${e.localizedMessage}")
                            } finally {
                                isSigningIn = false
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald700, contentColor = Color.White),
                    modifier = Modifier.fillMaxWidth().testTag("google_sign_in_button"),
                    enabled = !isSigningIn
                ) {
                    if (isSigningIn) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (lang == Localization.Language.ENGLISH) "Sign in with Google" else "Σύνδεση με Google", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            } else {
                // State 2: User IS logged in
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Emerald700,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = (currentUser.displayName?.take(1) ?: "U").uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentUser.displayName ?: (if (lang == Localization.Language.ENGLISH) "User" else "Χρήστης"),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = currentUser.email ?: "",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    OutlinedButton(
                        onClick = onSignOut,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("sign_out_button")
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = Localization.signOut(lang), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(Localization.signOut(lang), fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Household status
                if (household != null && household.id.isNotBlank()) {
                    // Joined in a Household!
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Emerald100.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, Emerald700.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Group, contentDescription = null, tint = Emerald700, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = Localization.translateHouseholdName(household.name, lang),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Emerald700
                                    )
                                }
                                Text(
                                    text = if (lang == Localization.Language.ENGLISH) "${household.memberUids.size} members connected" else "${household.memberUids.size} μέλη συνδεδεμένα",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = if (lang == Localization.Language.ENGLISH) "Invite code for partner / family:" else "Κωδικός Πρόσκλησης για τη σύζυγο:",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = household.inviteCode,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Emerald700,
                                    letterSpacing = 2.sp
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = ClipData.newPlainText("OikoNomos Invite Code", household.inviteCode)
                                            clipboard.setPrimaryClip(clip)
                                            onMessage(if (lang == Localization.Language.ENGLISH) "Code ${household.inviteCode} copied!" else "Ο κωδικός ${household.inviteCode} αντιγράφηκε!")
                                        },
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp), tint = Emerald700)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (lang == Localization.Language.ENGLISH) "Copy" else "Αντιγραφή", fontSize = 11.sp, color = Emerald700)
                                    }

                                    Button(
                                        onClick = {
                                            val sendIntent = Intent().apply {
                                                action = Intent.ACTION_SEND
                                                putExtra(
                                                    Intent.EXTRA_TEXT,
                                                    if (lang == Localization.Language.ENGLISH)
                                                        "Join me in tracking our family expenses on OikoNomos! Use invite code: ${household.inviteCode}"
                                                    else
                                                        "Έλα να μοιραστούμε τα οικογενειακά μας έξοδα στο OikoNomos! Βάλε τον κωδικό: ${household.inviteCode}"
                                                )
                                                type = "text/plain"
                                            }
                                            val shareIntent = Intent.createChooser(sendIntent, if (lang == Localization.Language.ENGLISH) "Share invite code" else "Κοινοποίηση κωδικού")
                                            context.startActivity(shareIntent)
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Emerald700)
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (lang == Localization.Language.ENGLISH) "Share" else "Κοινοποίηση", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Not in household yet: options to Create or Join
                    Text(
                        text = if (lang == Localization.Language.ENGLISH) "You are not part of a shared household yet:" else "Δεν ανήκετε ακόμα σε κοινό εξοδολόγιο:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (!showCreateDialog && !showJoinDialog) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { showCreateDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Emerald700, contentColor = Color.White),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (lang == Localization.Language.ENGLISH) "Create" else "Δημιουργία", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }

                            OutlinedButton(
                                onClick = { showJoinDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (lang == Localization.Language.ENGLISH) "Connect with code" else "Σύνδεση με κωδικό", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (showCreateDialog) {
                        Column {
                            OutlinedTextField(
                                value = familyNameInput,
                                onValueChange = { familyNameInput = it },
                                label = { Text(if (lang == Localization.Language.ENGLISH) "Name (e.g. Smith Family)" else "Όνομα (π.χ. Οικογένεια Κυνηγός)", fontSize = 11.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        onCreateHousehold(familyNameInput.ifBlank { if (lang == Localization.Language.ENGLISH) "Family" else "Οικογένεια" })
                                        showCreateDialog = false
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Emerald700, contentColor = Color.White),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(if (lang == Localization.Language.ENGLISH) "Start" else "Έναρξη", fontSize = 12.sp, color = Color.White)
                                }
                                OutlinedButton(
                                    onClick = { showCreateDialog = false },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(0.7f)
                                ) {
                                    Text(Localization.cancel(lang), fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    if (showJoinDialog) {
                        Column {
                            OutlinedTextField(
                                value = joinCodeInput,
                                onValueChange = { joinCodeInput = it.uppercase() },
                                label = { Text(if (lang == Localization.Language.ENGLISH) "Partner code (e.g. OIKO-1234)" else "Κωδικός συζύγου (π.χ. OIKO-1234)", fontSize = 11.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (joinCodeInput.isNotBlank()) {
                                            onJoinHousehold(joinCodeInput)
                                            showJoinDialog = false
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Emerald700, contentColor = Color.White),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(if (lang == Localization.Language.ENGLISH) "Connect" else "Σύνδεση", fontSize = 12.sp, color = Color.White)
                                }
                                OutlinedButton(
                                    onClick = { showJoinDialog = false },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(0.7f)
                                ) {
                                    Text(Localization.cancel(lang), fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
