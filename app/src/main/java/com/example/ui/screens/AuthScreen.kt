package com.example.ui.screens

import android.app.Activity
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.ui.components.OikoNomosLogo
import com.example.ui.theme.Emerald100
import com.example.ui.theme.Emerald700
import com.example.ui.viewmodel.OikoNomosViewModel
import com.example.util.Localization
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import kotlinx.coroutines.launch
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.util.Calendar

enum class ResetMethod {
    EMAIL,
    PHONE_SMS
}

@Composable
fun AuthScreen(
    viewModel: OikoNomosViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val appLanguage by viewModel.appLanguage.collectAsState()
    val lang = Localization.Language.fromCode(appLanguage)

    var isSignUp by remember { mutableStateOf(false) }
    var isForgotPassword by remember { mutableStateOf(false) }

    // Enhanced Registration Fields (Requirement 1 & 2)
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var occupation by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var resetSuccessMessage by remember { mutableStateOf<String?>(null) }
    var isGoogleSigningIn by remember { mutableStateOf(false) }

    // Password Reset via SMS Phone Option (Requirement 2)
    var resetMethod by remember { mutableStateOf<ResetMethod>(ResetMethod.EMAIL) }
    var resetPhone by remember { mutableStateOf("") }
    var resetPhoneStep by remember { mutableStateOf(1) } // 1: Enter phone, 2: Enter 6-digit code
    var enteredSixDigitCode by remember { mutableStateOf("") }
    var maskedPhoneDisplay by remember { mutableStateOf("") }
    var phoneDemoCodeNotice by remember { mutableStateOf<String?>(null) }

    // Date Picker state
    var showDatePickerDialog by remember { mutableStateOf(false) }

    // Intercept back button when on Forgot Password screen
    BackHandler(enabled = isForgotPassword) {
        isForgotPassword = false
        errorMessage = null
        resetSuccessMessage = null
        resetPhoneStep = 1
        enteredSixDigitCode = ""
        phoneDemoCodeNotice = null
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Logo & App Name
            OikoNomosLogo(size = 60.dp)

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Estia",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF157347)
            )

            Text(
                text = if (lang == Localization.Language.ENGLISH) "Family Expense Tracker" else "Οικογενειακό Εξοδολόγιο",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Auth Card
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isForgotPassword) {
                        // ============================================
                        // Password Reset Mode (Email or Mobile Phone 6-digit SMS)
                        // ============================================
                        Surface(
                            shape = CircleShape,
                            color = Emerald100,
                            modifier = Modifier.size(54.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (resetMethod == ResetMethod.PHONE_SMS) Icons.Default.Phone else Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = Emerald700,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = if (lang == Localization.Language.ENGLISH) "Reset Password" else "Επαναφορά Κωδικού",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = if (lang == Localization.Language.ENGLISH)
                                "Choose how you want to recover your password: via email link or via a 6-digit code sent to your mobile."
                            else
                                "Επιλέξτε τρόπο ανάκτησης: μέσω συνδέσμου στο email ή με 6ψήφιο κωδικό στο κινητό σας τηλέφωνο.",
                            fontSize = 12.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            lineHeight = 17.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Selector: Via Email vs Via Mobile Phone (6-digit code)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (resetMethod == ResetMethod.EMAIL) Emerald700 else Color.Transparent,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clickable {
                                        resetMethod = ResetMethod.EMAIL
                                        errorMessage = null
                                        resetSuccessMessage = null
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = if (lang == Localization.Language.ENGLISH) "Via Email" else "Μέσω Email",
                                        fontSize = 13.sp,
                                        fontWeight = if (resetMethod == ResetMethod.EMAIL) FontWeight.Bold else FontWeight.Medium,
                                        color = if (resetMethod == ResetMethod.EMAIL) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (resetMethod == ResetMethod.PHONE_SMS) Emerald700 else Color.Transparent,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clickable {
                                        resetMethod = ResetMethod.PHONE_SMS
                                        errorMessage = null
                                        resetSuccessMessage = null
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = if (lang == Localization.Language.ENGLISH) "Via Mobile (SMS)" else "Κινητό (SMS)",
                                        fontSize = 13.sp,
                                        fontWeight = if (resetMethod == ResetMethod.PHONE_SMS) FontWeight.Bold else FontWeight.Medium,
                                        color = if (resetMethod == ResetMethod.PHONE_SMS) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (resetSuccessMessage != null) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF10B981).copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF059669),
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = resetSuccessMessage ?: "",
                                        fontSize = 12.5.sp,
                                        color = Color(0xFF047857),
                                        lineHeight = 17.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            Button(
                                onClick = {
                                    isForgotPassword = false
                                    errorMessage = null
                                    resetSuccessMessage = null
                                    resetPhoneStep = 1
                                    enteredSixDigitCode = ""
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Emerald700, contentColor = Color.White),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Text(
                                    text = if (lang == Localization.Language.ENGLISH) "Back to Sign In" else "Επιστροφή στη Σύνδεση",
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        } else if (resetMethod == ResetMethod.EMAIL) {
                            // Method 1: Email Reset Link
                            OutlinedTextField(
                                value = email,
                                onValueChange = {
                                    email = it
                                    errorMessage = null
                                },
                                label = { Text("Email") },
                                placeholder = { Text("example@gmail.com") },
                                leadingIcon = {
                                    Icon(Icons.Default.Email, contentDescription = null, tint = Emerald700)
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("reset_email_field")
                            )

                            if (errorMessage != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Error,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = errorMessage ?: "",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    focusManager.clearFocus()
                                    errorMessage = null
                                    viewModel.sendPasswordResetEmail(
                                        email = email,
                                        onSuccess = {
                                            resetSuccessMessage = if (lang == Localization.Language.ENGLISH)
                                                "A password reset link has been sent to $email. Please check your inbox and spam folder."
                                            else
                                                "Στάλθηκε σύνδεσμος επαναφοράς στο $email. Ελέγξτε τα εισερχόμενά σας και τα spam."
                                        },
                                        onError = { errorMessage = it }
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Emerald700,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("reset_submit_button")
                            ) {
                                Text(
                                    text = if (lang == Localization.Language.ENGLISH) "Send Reset Email" else "Αποστολή Email Επαναφοράς",
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        } else {
                            // Method 2: Mobile Phone SMS with 6-digit Code (Requirement 2)
                            if (resetPhoneStep == 1) {
                                OutlinedTextField(
                                    value = resetPhone,
                                    onValueChange = {
                                        resetPhone = it
                                        errorMessage = null
                                    },
                                    label = { Text(if (lang == Localization.Language.ENGLISH) "Mobile Phone Number" else "Κινητό Τηλέφωνο") },
                                    placeholder = { Text("69XXXXXXXX") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Phone, contentDescription = null, tint = Emerald700)
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Phone,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("reset_phone_field")
                                )

                                if (errorMessage != null) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.errorContainer,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Error,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = errorMessage ?: "",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = {
                                        focusManager.clearFocus()
                                        errorMessage = null
                                        phoneDemoCodeNotice = null
                                        viewModel.sendPasswordResetPhoneCode(
                                            phone = resetPhone,
                                            onSuccess = { generatedCode, maskedPhone, _ ->
                                                maskedPhoneDisplay = maskedPhone
                                                phoneDemoCodeNotice = "📱 SMS Demo: Ο 6ψήφιος κωδικός σας είναι $generatedCode"
                                                resetPhoneStep = 2
                                            },
                                            onError = { errorMessage = it }
                                        )
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Emerald700,
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("reset_phone_submit_button")
                                ) {
                                    Text(
                                        text = if (lang == Localization.Language.ENGLISH) "Send 6-Digit Code (SMS)" else "Αποστολή 6ψήφιου Κωδικού (SMS)",
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            } else {
                                // Step 2: Enter the 6-digit code received
                                if (phoneDemoCodeNotice != null) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Emerald100.copy(alpha = 0.7f),
                                        border = BorderStroke(1.dp, Emerald700.copy(alpha = 0.4f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Phone, contentDescription = null, tint = Emerald700, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = phoneDemoCodeNotice ?: "",
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF065F46)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                }

                                Text(
                                    text = if (lang == Localization.Language.ENGLISH)
                                        "Enter the 6-digit code sent to $maskedPhoneDisplay"
                                    else
                                        "Εισάγετε τον 6ψήφιο κωδικό που λάβατε στο $maskedPhoneDisplay",
                                    fontSize = 12.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = enteredSixDigitCode,
                                    onValueChange = {
                                        if (it.length <= 6) {
                                            enteredSixDigitCode = it
                                            errorMessage = null
                                        }
                                    },
                                    label = { Text(if (lang == Localization.Language.ENGLISH) "6-Digit Code" else "6ψήφιος Κωδικός") },
                                    placeholder = { Text("123456") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Key, contentDescription = null, tint = Emerald700)
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("reset_six_digit_code_field")
                                )

                                if (errorMessage != null) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.errorContainer,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Error,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = errorMessage ?: "",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = {
                                        focusManager.clearFocus()
                                        errorMessage = null
                                        viewModel.verifyPasswordResetPhoneCode(
                                            phone = resetPhone,
                                            enteredCode = enteredSixDigitCode,
                                            onSuccess = { userEmail ->
                                                resetSuccessMessage = if (lang == Localization.Language.ENGLISH)
                                                    "Code verified! A secure password reset link has been dispatched to your linked account ($userEmail)."
                                                else
                                                    "Ο κωδικός επαληθεύτηκε! Στάλθηκε σύνδεσμος επαναφοράς στο συνδεδεμένο email σας ($userEmail)."
                                            },
                                            onError = { errorMessage = it }
                                        )
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Emerald700,
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("verify_code_submit_button")
                                ) {
                                    Text(
                                        text = if (lang == Localization.Language.ENGLISH) "Verify Code & Send Link" else "Επαλήθευση & Αποστολή Συνδέσμου",
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                TextButton(
                                    onClick = {
                                        resetPhoneStep = 1
                                        errorMessage = null
                                    }
                                ) {
                                    Text(
                                        text = if (lang == Localization.Language.ENGLISH) "← Change Phone Number" else "← Αλλαγή Τηλεφώνου",
                                        color = Emerald700,
                                        fontSize = 12.5.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        TextButton(
                            onClick = {
                                isForgotPassword = false
                                errorMessage = null
                                resetSuccessMessage = null
                                resetPhoneStep = 1
                                enteredSixDigitCode = ""
                            },
                            modifier = Modifier.testTag("back_to_login_button")
                        ) {
                            Text(
                                text = if (lang == Localization.Language.ENGLISH) "← Back to Sign In" else "← Επιστροφή στη Σύνδεση",
                                color = Emerald700,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        // ============================================
                        // Normal Sign In / Sign Up Mode
                        // ============================================

                        // Segmented Tab: Sign In / Sign Up
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (!isSignUp) Emerald700 else Color.Transparent,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .clickable {
                                        isSignUp = false
                                        errorMessage = null
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = if (lang == Localization.Language.ENGLISH) "Sign In" else "Σύνδεση",
                                        fontSize = 14.sp,
                                        fontWeight = if (!isSignUp) FontWeight.Bold else FontWeight.Medium,
                                        color = if (!isSignUp) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSignUp) Emerald700 else Color.Transparent,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .clickable {
                                        isSignUp = true
                                        errorMessage = null
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = if (lang == Localization.Language.ENGLISH) "Sign Up" else "Εγγραφή",
                                        fontSize = 14.sp,
                                        fontWeight = if (isSignUp) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSignUp) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Google Sign-In Button (Requirement 2: with Google G icon)
                        OutlinedButton(
                            onClick = {
                                isGoogleSigningIn = true
                                errorMessage = null
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
                                            viewModel.signInWithGoogleToken(
                                                idToken = googleIdToken,
                                                onSuccess = { isGoogleSigningIn = false },
                                                onError = { err ->
                                                    isGoogleSigningIn = false
                                                    errorMessage = err
                                                }
                                            )
                                        } else {
                                            errorMessage = if (lang == Localization.Language.ENGLISH) "Invalid Google credential" else "Μη έγκυρο διαπιστευτήριο Google"
                                            isGoogleSigningIn = false
                                        }
                                    } catch (e: GetCredentialCancellationException) {
                                        Log.d("Auth", "Google sign-in was cancelled")
                                        isGoogleSigningIn = false
                                    } catch (e: Exception) {
                                        Log.e("Auth", "Google Sign-In failed", e)
                                        errorMessage = if (lang == Localization.Language.ENGLISH) "Google Sign-In error: ${e.localizedMessage ?: "Please try again"}" else "Σφάλμα Google Sign-In: ${e.localizedMessage ?: "Προσπαθήστε ξανά"}"
                                        isGoogleSigningIn = false
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("google_auth_button"),
                            enabled = !isGoogleSigningIn
                        ) {
                            if (isGoogleSigningIn) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_google_logo),
                                        contentDescription = "Google",
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = if (isSignUp) (if (lang == Localization.Language.ENGLISH) "Sign up with Google" else "Εγγραφή με Google") else (if (lang == Localization.Language.ENGLISH) "Sign in with Google" else "Σύνδεση με Google"),
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Divider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                            Text(
                                text = if (lang == Localization.Language.ENGLISH) "  or with Email  " else "  ή με Email  ",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Form Fields
                        if (isSignUp) {
                            // First Name & Last Name (Requirement 2 & 3: separate Όνομα, Επίθετο)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = firstName,
                                    onValueChange = {
                                        firstName = it
                                        errorMessage = null
                                    },
                                    label = { Text(if (lang == Localization.Language.ENGLISH) "First Name" else "Όνομα") },
                                    placeholder = { Text("π.χ. Γιώργος") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = Emerald700)
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Right) }),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("auth_first_name_field")
                                )

                                OutlinedTextField(
                                    value = lastName,
                                    onValueChange = {
                                        lastName = it
                                        errorMessage = null
                                    },
                                    label = { Text(if (lang == Localization.Language.ENGLISH) "Last Name" else "Επίθετο") },
                                    placeholder = { Text("π.χ. Παπαδόπουλος") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Badge, contentDescription = null, tint = Emerald700)
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("auth_last_name_field")
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Date of Birth (Requirement 3: Ημερομηνία Γέννησης) with Clickable Dialog Picker
                            OutlinedTextField(
                                value = birthDate,
                                onValueChange = { birthDate = it },
                                readOnly = true,
                                label = { Text(if (lang == Localization.Language.ENGLISH) "Date of Birth *" else "Ημερομηνία Γέννησης *") },
                                placeholder = { Text("ΗΗ/ΜΜ/ΕΕΕΕ") },
                                leadingIcon = {
                                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Emerald700)
                                },
                                trailingIcon = {
                                    IconButton(onClick = { showDatePickerDialog = true }) {
                                        Icon(Icons.Default.CalendarMonth, contentDescription = "Pick Date", tint = Emerald700)
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showDatePickerDialog = true }
                                    .testTag("auth_birthdate_field")
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Mobile Phone Number (Required for 6-digit SMS verification and sync)
                            OutlinedTextField(
                                value = phoneNumber,
                                onValueChange = {
                                    phoneNumber = it
                                    errorMessage = null
                                },
                                label = { Text(if (lang == Localization.Language.ENGLISH) "Mobile Phone (for SMS reset) *" else "Κινητό Τηλέφωνο (για SMS) *") },
                                placeholder = { Text("69XXXXXXXX") },
                                leadingIcon = {
                                    Icon(Icons.Default.Phone, contentDescription = null, tint = Emerald700)
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Phone,
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("auth_phone_field")
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Profession / Occupation & City
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = occupation,
                                    onValueChange = { occupation = it },
                                    label = { Text(if (lang == Localization.Language.ENGLISH) "Occupation" else "Επάγγελμα") },
                                    placeholder = { Text("π.χ. Μηχανικός") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Work, contentDescription = null, tint = Emerald700)
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Right) }),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("auth_occupation_field")
                                )

                                OutlinedTextField(
                                    value = city,
                                    onValueChange = { city = it },
                                    label = { Text(if (lang == Localization.Language.ENGLISH) "City" else "Πόλη") },
                                    placeholder = { Text("π.χ. Αθήνα") },
                                    leadingIcon = {
                                        Icon(Icons.Default.LocationCity, contentDescription = null, tint = Emerald700)
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("auth_city_field")
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        OutlinedTextField(
                            value = email,
                            onValueChange = {
                                email = it
                                errorMessage = null
                            },
                            label = { Text("Email *") },
                            placeholder = { Text("example@gmail.com") },
                            leadingIcon = {
                                Icon(Icons.Default.Email, contentDescription = null, tint = Emerald700)
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_email_field")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                errorMessage = null
                            },
                            label = { Text(if (lang == Localization.Language.ENGLISH) "Password *" else "Κωδικός πρόσβασης *") },
                            placeholder = { Text(if (lang == Localization.Language.ENGLISH) "At least 6 characters" else "Τουλάχιστον 6 χαρακτήρες") },
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = Emerald700)
                            },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (lang == Localization.Language.ENGLISH) "Show password" else "Εμφάνιση κωδικού"
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_password_field")
                        )

                        // Error message
                        if (errorMessage != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Error,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = errorMessage ?: "",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Primary Submit Button
                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                errorMessage = null
                                if (isSignUp) {
                                    viewModel.signUpWithEmail(
                                        firstName = firstName,
                                        lastName = lastName,
                                        birthDate = birthDate,
                                        phoneNumber = phoneNumber,
                                        occupation = occupation,
                                        city = city,
                                        email = email,
                                        pass = password,
                                        onSuccess = {},
                                        onError = { errorMessage = it }
                                    )
                                } else {
                                    viewModel.signInWithEmail(
                                        email = email,
                                        pass = password,
                                        onSuccess = {},
                                        onError = { errorMessage = it }
                                    )
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Emerald700,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("auth_submit_button")
                        ) {
                            Text(
                                text = if (isSignUp) (if (lang == Localization.Language.ENGLISH) "Create Account" else "Δημιουργία Λογαριασμού") else (if (lang == Localization.Language.ENGLISH) "Sign In" else "Σύνδεση"),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        // Requirement 4: Subtle "Forgot Password" link directly below main Sign In button
                        if (!isSignUp) {
                            Spacer(modifier = Modifier.height(12.dp))
                            TextButton(
                                onClick = {
                                    isForgotPassword = true
                                    errorMessage = null
                                    resetSuccessMessage = null
                                },
                                modifier = Modifier.testTag("forgot_password_button")
                            ) {
                                Text(
                                    text = if (lang == Localization.Language.ENGLISH) "Forgot password?" else "Ξεχάσατε τον κωδικό πρόσβασης;",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Emerald700
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Requirement 1: Language Selector with generous pill padding and margins
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier
                    .widthIn(min = 270.dp, max = 320.dp)
                    .testTag("auth_language_selector")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(5.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Greek Option
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (appLanguage == "EL") Emerald700 else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clickable { viewModel.setAppLanguage("EL") }
                            .testTag("auth_language_el")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(text = "🇬🇷", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Ελληνικά",
                                    fontSize = 13.sp,
                                    fontWeight = if (appLanguage == "EL") FontWeight.Bold else FontWeight.Medium,
                                    color = if (appLanguage == "EL") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // English Option
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (appLanguage == "EN") Emerald700 else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clickable { viewModel.setAppLanguage("EN") }
                            .testTag("auth_language_en")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(text = "🇬🇧", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "English",
                                    fontSize = 13.sp,
                                    fontWeight = if (appLanguage == "EN") FontWeight.Bold else FontWeight.Medium,
                                    color = if (appLanguage == "EN") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Date Picker Dialog for Date of Birth
    if (showDatePickerDialog) {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        var selectedDay by remember { mutableStateOf(1) }
        var selectedMonth by remember { mutableStateOf(1) } // 1-12
        var selectedYear by remember { mutableStateOf(1995) }

        Dialog(
            onDismissRequest = { showDatePickerDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
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

                    // Day / Month / Year Selectors
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Day Input
                        OutlinedTextField(
                            value = if (selectedDay > 0) selectedDay.toString() else "",
                            onValueChange = {
                                val d = it.filter { ch -> ch.isDigit() }.take(2).toIntOrNull() ?: 1
                                selectedDay = d.coerceIn(1, 31)
                            },
                            label = { Text("Ημέρα") },
                            placeholder = { Text("1-31") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )

                        // Month Input
                        OutlinedTextField(
                            value = if (selectedMonth > 0) selectedMonth.toString() else "",
                            onValueChange = {
                                val m = it.filter { ch -> ch.isDigit() }.take(2).toIntOrNull() ?: 1
                                selectedMonth = m.coerceIn(1, 12)
                            },
                            label = { Text("Μήνας") },
                            placeholder = { Text("1-12") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )

                        // Year Input
                        OutlinedTextField(
                            value = if (selectedYear > 0) selectedYear.toString() else "",
                            onValueChange = {
                                val y = it.filter { ch -> ch.isDigit() }.take(4).toIntOrNull() ?: 1995
                                selectedYear = y.coerceIn(1920, currentYear)
                            },
                            label = { Text("Έτος") },
                            placeholder = { Text("1995") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                                birthDate = "$dayStr/$monthStr/$selectedYear"
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
