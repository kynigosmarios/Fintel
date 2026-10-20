package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.EmailVerificationScreen
import com.example.ui.screens.MainScreen
import com.example.ui.screens.NotificationConsentScreen
import com.example.ui.theme.Emerald700
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.OikoNomosViewModel

class MainActivity : ComponentActivity() {

    companion object {
        const val ACTION_QUICK_EXPENSE = "ACTION_QUICK_EXPENSE"
    }

    private val viewModel: OikoNomosViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIntent(intent)

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppRootNavigator(viewModel = viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.action
        val extraAction = intent.getStringExtra("EXTRA_QUICK_ACTION")

        if (action == ACTION_QUICK_EXPENSE || extraAction == ACTION_QUICK_EXPENSE) {
            viewModel.openVoiceInputDialog()
        }

        // Handle household invitation deep link
        val data = intent.data
        if (data != null) {
            val inviteCode = data.getQueryParameter("code")
            if (!inviteCode.isNullOrBlank()) {
                viewModel.joinHousehold(inviteCode)
            }
        }
    }
}

private enum class RootScreen {
    LOADING,
    AUTH,
    EMAIL_VERIFICATION,
    NOTIFICATION_CONSENT,
    MAIN
}

@Composable
fun AppRootNavigator(viewModel: OikoNomosViewModel) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val isUserVerified by viewModel.isUserVerifiedState.collectAsStateWithLifecycle()
    val isAuthInitialized by viewModel.isAuthInitialized.collectAsStateWithLifecycle()
    val notificationConsentCompleted by viewModel.notificationConsentCompleted.collectAsStateWithLifecycle()
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()

    val currentScreen = when {
        !isAuthInitialized -> RootScreen.LOADING
        currentUser == null -> RootScreen.AUTH
        !isUserVerified -> RootScreen.EMAIL_VERIFICATION
        !notificationConsentCompleted -> RootScreen.NOTIFICATION_CONSENT
        else -> RootScreen.MAIN
    }

    Crossfade(targetState = currentScreen, label = "root_navigation_crossfade") { screen ->
        when (screen) {
            RootScreen.LOADING -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Emerald700)
                }
            }
            RootScreen.AUTH -> {
                AuthScreen(viewModel = viewModel)
            }
            RootScreen.EMAIL_VERIFICATION -> {
                currentUser?.let { user ->
                    EmailVerificationScreen(
                        user = user,
                        viewModel = viewModel
                    )
                } ?: AuthScreen(viewModel = viewModel)
            }
            RootScreen.NOTIFICATION_CONSENT -> {
                NotificationConsentScreen(
                    appLanguage = appLanguage,
                    onAccept = { viewModel.acceptNotificationConsent() },
                    onSkip = { viewModel.skipNotificationConsent() }
                )
            }
            RootScreen.MAIN -> {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}
