package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.ui.KeystoreViewModel
import com.example.ui.screens.GenerateScreen
import com.example.ui.screens.InspectScreen
import com.example.ui.screens.SavedKeystoresScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.sign.SignApkScreen

/**
 * Screen switcher displaying the active tab content within the main scaffold.
 */
@Composable
fun MainTabContent(
    selectedTab: Int,
    viewModel: KeystoreViewModel,
    modifier: Modifier = Modifier
) {
    when (selectedTab) {
        0 -> GenerateScreen(viewModel = viewModel, modifier = modifier)
        1 -> SavedKeystoresScreen(viewModel = viewModel, modifier = modifier)
        2 -> SignApkScreen(
            viewModel = viewModel,
            onNavigateToInspectWithApk = { bytes, name ->
                viewModel.analyzeApk(null, bytes, name)
                viewModel.setSelectedTab(3)
            },
            modifier = modifier
        )
        3 -> InspectScreen(viewModel = viewModel, modifier = modifier)
        4 -> SettingsScreen(viewModel = viewModel, modifier = modifier)
    }
}
