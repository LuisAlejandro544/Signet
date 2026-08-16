package com.example.ui.screens

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.ui.KeystoreViewModel
import com.example.ui.screens.settings.ColorPaletteSection
import com.example.ui.screens.settings.DistributionInfoSection
import com.example.ui.screens.settings.LegalLinksSection
import com.example.ui.screens.settings.SettingsHeaderCard
import com.example.ui.screens.settings.ThemeModeSection

@Composable
fun SettingsScreen(
    viewModel: KeystoreViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val themeState by viewModel.themeState.collectAsState()
    val isAndroid12OrAbove = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Encabezado visual
        SettingsHeaderCard()

        // Sección 1: Modo de Pantalla (Claro / Oscuro / Negro 100% AMOLED / Sistema)
        ThemeModeSection(
            themeState = themeState,
            onSelectMode = { mode -> viewModel.setThemeMode(mode) }
        )

        // Sección 2: Paleta de Color y Material You
        ColorPaletteSection(
            themeState = themeState,
            isAndroid12OrAbove = isAndroid12OrAbove,
            onSelectPalette = { palette -> viewModel.setColorPalette(palette) }
        )

        // Sección 3: Marco Legal y Políticas de Uso (Portal Web Oficial)
        LegalLinksSection(
            context = context,
            viewModel = viewModel
        )

        // Sección 4: Seguridad y Distribución (Uptodown / GitHub Releases / GPL v3)
        DistributionInfoSection()

        Spacer(Modifier.height(16.dp))
    }
}
