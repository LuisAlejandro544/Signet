package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.platform.LocalPlatformServices
import com.example.ui.components.main.MainGlobalSheets
import com.example.ui.navigation.MainTabContent
import com.example.ui.navigation.SignetNavigationBar
import com.example.ui.navigation.SignetNavigationRail
import com.example.ui.navigation.SignetTopAppBar
import com.example.ui.state.GenerationUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: KeystoreViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val savedKeystores by viewModel.savedKeystores.collectAsState()
    val generationState by viewModel.generationState.collectAsState()
    val selectedKeystoreForDetail by viewModel.selectedKeystoreForDetail.collectAsState()
    val updateState by viewModel.updateState.collectAsState()
    val platformServices = LocalPlatformServices.current

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    // If generation succeeds, auto-show the details bottom sheet
    LaunchedEffect(generationState) {
        if (generationState is GenerationUiState.Success) {
            val details = (generationState as GenerationUiState.Success).details
            viewModel.showKeystoreDetails(details)
            viewModel.dismissGenerationState()
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 700.dp

        if (isWideScreen) {
            // Desktop / Tablet Landscape Ergonomic Layout with NavigationRail
            Row(modifier = Modifier.fillMaxSize()) {
                SignetNavigationRail(
                    selectedTab = selectedTab,
                    savedCount = savedKeystores.size,
                    onTabSelected = { viewModel.setSelectedTab(it) },
                    onOpenVaultFolder = {
                        platformServices.openFolder(viewModel.getVaultDirectory())
                    }
                )

                Scaffold(
                    topBar = {
                        SignetTopAppBar(
                            selectedTab = selectedTab,
                            isWideScreen = true
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        MainTabContent(
                            selectedTab = selectedTab,
                            viewModel = viewModel
                        )
                    }
                }
            }
        } else {
            // Mobile Compact Layout with Bottom NavigationBar
            Scaffold(
                topBar = {
                    SignetTopAppBar(
                        selectedTab = selectedTab,
                        isWideScreen = false
                    )
                },
                bottomBar = {
                    SignetNavigationBar(
                        selectedTab = selectedTab,
                        savedCount = savedKeystores.size,
                        onTabSelected = { viewModel.setSelectedTab(it) }
                    )
                },
                modifier = Modifier.fillMaxSize()
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                    ) {
                    MainTabContent(
                        selectedTab = selectedTab,
                        viewModel = viewModel
                    )
                }
            }
        }
    }

    // Modal global sheets and dialogs
    MainGlobalSheets(
        viewModel = viewModel,
        selectedKeystoreForDetail = selectedKeystoreForDetail,
        updateState = updateState,
        sheetState = sheetState,
        scope = scope
    )
}

