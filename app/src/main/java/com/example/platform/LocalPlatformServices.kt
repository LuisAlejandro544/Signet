package com.example.platform

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * CompositionLocal que provee la instancia activa de PlatformServices a la UI de Compose (Android).
 */
val LocalPlatformServices = staticCompositionLocalOf<PlatformServices> {
    DesktopPlatformServices
}
