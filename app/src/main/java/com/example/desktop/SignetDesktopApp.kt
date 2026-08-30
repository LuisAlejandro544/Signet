package com.example.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.example.platform.DesktopPlatformServices
import com.example.platform.LocalPlatformServices
import com.example.ui.KeystoreViewModel
import com.example.ui.MainScreen
import com.example.ui.screens.WelcomeScreen
import com.example.ui.theme.MyApplicationTheme

/**
 * Contenedor visual de nivel superior para la aplicación Signet en entornos Desktop (Windows, macOS, Linux).
 * Inyecta los servicios de plataforma de escritorio (AWT FileDialog, Portapapeles del sistema,
 * explorador de archivos nativo y persistencia en %APPDATA%/Signet).
 */
@Composable
fun SignetDesktopApp(
    viewModel: KeystoreViewModel = remember { KeystoreViewModel() }
) {
    val themeState by viewModel.themeState.collectAsState()
    val isOnboardingCompleted by viewModel.isOnboardingCompleted.collectAsState()
    val platformServices = remember { DesktopPlatformServices }

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
