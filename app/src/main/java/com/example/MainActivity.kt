package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
