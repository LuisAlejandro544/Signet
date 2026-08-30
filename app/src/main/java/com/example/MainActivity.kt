package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.example.platform.AndroidPlatformServices
import com.example.platform.LocalPlatformServices
import com.example.ui.KeystoreViewModel
import com.example.ui.MainScreen
import com.example.ui.screens.WelcomeScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: KeystoreViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeState by viewModel.themeState.collectAsState()
            val isOnboardingCompleted by viewModel.isOnboardingCompleted.collectAsState()
            val platformServices = remember { AndroidPlatformServices(this@MainActivity) }

            CompositionLocalProvider(LocalPlatformServices provides platformServices) {
                MyApplicationTheme(themeState = themeState) {
                    if (!isOnboardingCompleted) {
                        WelcomeScreen(
                            onComplete = { viewModel.completeOnboarding() },
                            viewModel = viewModel
                        )
                    } else {
                        MainScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}
