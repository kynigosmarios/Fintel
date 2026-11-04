package com.example.ui.components

import android.app.Activity
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.util.Localization
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun FamilyCloudSyncCard(
    currentUser: FirebaseUser?,
    household: Household?,
    isSyncing: Boolean,
    onCreateHousehold: (String) -> Unit,
    onSignOut: () -> Unit,
    onMessage: (String) -> Unit,
    appLanguage: String = "EL"
) {
    val lang = Localization.Language.fromCode(appLanguage)
    val isGreek = appLanguage != "EN"
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isSigningIn by remember { mutableStateOf(false) }

    var familyNameInput by remember { mutableStateOf("") }
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
                            text = if (lang == Localization.Language.ENGLISH) "Cloud Sync & Backup" else "Συγχρονισμός Cloud",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (currentUser != null) {
                                if (lang == Localization.Language.ENGLISH) "Connected account" else "Συνδεδεμένος λογαριασμός"
                            } else {
                                if (lang == Localization.Language.ENGLISH) "Cloud backup & sync" else "Αντίγραφα ασφαλείας & συγχρονισμός"
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
                        "Sign in with your Google account to back up and sync your expenses and households in real time across your devices."
                    else
                        "Συνδεθείτε με το Google λογαριασμό σας για δημιουργία αντιγράφων ασφαλείας και συγχρονισμό των εξόδων και εξοδολογίων σας σε πραγματικό χρόνο.",
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
                                    text = if (lang == Localization.Language.ENGLISH)
                                        "Active"
                                    else
                                        "Ενεργό",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = if (isGreek)
                                    "Όλα τα δεδομένα του εξοδολογίου συγχρονίζονται με ασφάλεια στο Google Cloud / Firebase."
                                else
                                    "All household data is securely backed up and synchronized to Google Cloud / Firebase.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 15.sp
                            )
                        }
                    }
                } else {
                    // Not in household yet: option to Create
                    Text(
                        text = if (isGreek)
                            "Δεν έχει επιλεγεί εξοδολόγιο. Μπορείτε να δημιουργήσετε νέο εξοδολόγιο:"
                        else
                            "No active household selected. You can create a new household:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 15.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (!showCreateDialog) {
                        Button(
                            onClick = { showCreateDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Emerald700, contentColor = Color.White),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (lang == Localization.Language.ENGLISH) "Create Household" else "Δημιουργία Εξοδολογίου", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
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
                }
            }
        }
    }
}
