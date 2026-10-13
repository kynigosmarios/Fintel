package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.EmailVerificationScreen
import com.example.ui.screens.MainScreen
import com.example.ui.theme.Emerald700
import com.example.ui.theme.Emerald950
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.OikoNomosViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: OikoNomosViewModel by viewModels()
    private lateinit var billingManager: com.example.billing.BillingManager

    companion object {
        const val EXTRA_QUICK_ACTION = "EXTRA_QUICK_ACTION"
        const val ACTION_QUICK_EXPENSE = "ACTION_QUICK_EXPENSE"
        const val ACTION_SHOW_CHOOSER = "com.example.ACTION_SHOW_CHOOSER"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        billingManager = com.example.billing.BillingManager(applicationContext) { isPro ->
            if (isPro) {
                viewModel.activateProFromBilling()
            }
        }
        billingManager.startConnection()
        handleIntent(intent)
        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
            val isAuthInitialized by viewModel.isAuthInitialized.collectAsStateWithLifecycle()
            val isUserVerified by viewModel.isUserVerifiedState.collectAsStateWithLifecycle()

            val isDarkTheme = when (themeMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemInDarkTheme()
            }

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                val notifLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                    contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (isGranted) {
                        com.example.reminder.DailyReminderManager.createNotificationChannel(applicationContext)
                        if (com.example.reminder.DailyReminderManager.isReminderEnabled(applicationContext)) {
                            val h = com.example.reminder.DailyReminderManager.getReminderHour(applicationContext)
                            val m = com.example.reminder.DailyReminderManager.getReminderMinute(applicationContext)
                            com.example.reminder.DailyReminderManager.scheduleDailyReminder(applicationContext, h, m)
                        }
                    }
                }

                androidx.compose.runtime.LaunchedEffect(Unit) {
                    if (androidx.core.content.ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            android.Manifest.permission.POST_NOTIFICATIONS
                        ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                    ) {
                        notifLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            val view = LocalView.current
            if (!view.isInEditMode) {
                SideEffect {
                    val window = (view.context as android.app.Activity).window
                    WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDarkTheme
                }
            }

            MyApplicationTheme(darkTheme = isDarkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (!isAuthInitialized) {
                        SplashScreen()
                    } else if (currentUser == null) {
                        AuthScreen(viewModel = viewModel)
                    } else if (!isUserVerified) {
                        EmailVerificationScreen(user = currentUser!!, viewModel = viewModel)
                    } else {
                        MainScreen(
                            viewModel = viewModel,
                            onLaunchBilling = { activity ->
                                billingManager.launchPurchaseFlow(activity, com.example.billing.BillingManager.SUB_MONTHLY)
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        billingManager.endConnection()
    }

    override fun onResume() {
        super.onResume()
        viewModel.onAppResumed()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == ACTION_SHOW_CHOOSER ||
            intent?.getStringExtra(EXTRA_QUICK_ACTION) == ACTION_QUICK_EXPENSE
        ) {
            if (viewModel.currentUser.value != null) {
                viewModel.openQuickActionChooser()
            }
        }
    }
}

@Composable
private fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Emerald950),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_oikonomos_logo_white),
                contentDescription = "Fintel",
                modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Fintel",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Έξυπνο Εξοδολόγιο & Κοινή Χρήση",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(24.dp))
            CircularProgressIndicator(
                color = Emerald700,
                modifier = Modifier.size(28.dp),
                strokeWidth = 3.dp
            )
        }
    }
}
