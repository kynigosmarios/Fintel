package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.MainScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.OikoNomosViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: OikoNomosViewModel by viewModels()

    companion object {
        const val EXTRA_QUICK_ACTION = "EXTRA_QUICK_ACTION"
        const val ACTION_QUICK_EXPENSE = "ACTION_QUICK_EXPENSE"
        const val ACTION_SHOW_CHOOSER = "com.example.ACTION_SHOW_CHOOSER"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)
        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
            val isDarkTheme = when (themeMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemInDarkTheme()
            }

            MyApplicationTheme(darkTheme = isDarkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (currentUser == null) {
                        AuthScreen(viewModel = viewModel)
                    } else {
                        MainScreen(viewModel = viewModel)
                    }
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
        if (intent?.action == ACTION_SHOW_CHOOSER ||
            intent?.getStringExtra(EXTRA_QUICK_ACTION) == ACTION_QUICK_EXPENSE
        ) {
            if (viewModel.currentUser.value != null) {
                viewModel.openQuickActionChooser()
            }
        }
    }
}
