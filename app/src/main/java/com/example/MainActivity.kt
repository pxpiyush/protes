package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.navigation.ProtesShell
import com.example.core.theme.ProtesTheme
import com.example.features.ProtesViewModel

class MainActivity : ComponentActivity() {

    // Inject our core Protes ViewModel with its custom factory
    private val viewModel: ProtesViewModel by viewModels {
        ProtesViewModel.Factory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge drawing
        enableEdgeToEdge()

        setContent {
            // Collect theme configuration reactively from Room database
            val preferences by viewModel.userPreferences.collectAsState()
            val isDarkTheme = when (preferences.themeMode) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }

            val context = androidx.compose.ui.platform.LocalContext.current
            var isUnlocked by remember {
                mutableStateOf(!com.example.core.security.SecurityManager.isLockOpenAppEnabled(context))
            }

            ProtesTheme(darkTheme = isDarkTheme) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (isUnlocked) {
                        // Main shell
                        ProtesShell(
                            viewModel = viewModel,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        com.example.core.security.AppLockOverlay(
                            isBiometricEnabled = preferences.isBiometricEnabled,
                            onUnlocked = { isUnlocked = true }
                        )
                    }
                }
            }
        }
    }
}
