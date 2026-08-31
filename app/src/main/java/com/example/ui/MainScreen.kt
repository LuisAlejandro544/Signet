package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.crypto.DesktopStorageUtils
import com.example.platform.LocalPlatformServices
import com.example.ui.components.KeystoreDetailsSheet
import com.example.ui.components.UpdateDialog
import com.example.ui.screens.GenerateScreen
import com.example.ui.screens.InspectScreen
import com.example.ui.screens.SavedKeystoresScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.sign.SignApkScreen
import com.example.ui.state.GenerationUiState
import com.example.update.UpdateUiState
import kotlinx.coroutines.launch

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
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    header = {
                        IconButton(
                            onClick = {
                                platformServices.openFolder(DesktopStorageUtils.getDesktopDataDir())
                            },
                            modifier = Modifier
                                .padding(vertical = 12.dp)
                                .testTag("btn_desktop_vault_folder")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.FolderOpen,
                                contentDescription = "Abrir Carpeta de Bóveda",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxHeight()
                ) {
                    NavigationRailItem(
                        selected = selectedTab == 0,
                        onClick = { viewModel.setSelectedTab(0) },
                        icon = { Icon(Icons.Filled.Key, contentDescription = "Generar") },
                        label = { Text("Generar") },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("tab_generate")
                    )

                    NavigationRailItem(
                        selected = selectedTab == 1,
                        onClick = { viewModel.setSelectedTab(1) },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (savedKeystores.isNotEmpty()) {
                                        Badge { Text("${savedKeystores.size}") }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (selectedTab == 1) Icons.Filled.Folder else Icons.Outlined.Folder,
                                    contentDescription = "Mis Keystores"
                                )
                            }
                        },
                        label = { Text("Guardados") },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("tab_saved")
                    )

                    NavigationRailItem(
                        selected = selectedTab == 2,
                        onClick = { viewModel.setSelectedTab(2) },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == 2) Icons.Filled.Draw else Icons.Outlined.Draw,
                                contentDescription = "Firmar APK"
                            )
                        },
                        label = { Text("Firmar") },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("tab_sign_apk")
                    )

                    NavigationRailItem(
                        selected = selectedTab == 3,
                        onClick = { viewModel.setSelectedTab(3) },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == 3) Icons.Filled.Search else Icons.Outlined.Search,
                                contentDescription = "Inspeccionar"
                            )
                        },
                        label = { Text("Inspeccionar") },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("tab_inspect")
                    )

                    NavigationRailItem(
                        selected = selectedTab == 4,
                        onClick = { viewModel.setSelectedTab(4) },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == 4) Icons.Filled.Settings else Icons.Outlined.Settings,
                                contentDescription = "Configuración"
                            )
                        },
                        label = { Text("Ajustes") },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("tab_settings")
                    )
                }

                Scaffold(
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = {
                                Text(
                                    text = when (selectedTab) {
                                        0 -> "Signet Desktop — Generar Keystore"
                                        1 -> "Signet Desktop — Mis Keystores Guardados"
                                        2 -> "Signet Desktop — Firmar APK (v1, v2, v3)"
                                        3 -> "Signet Desktop — Inspeccionar Certificados & APKs"
                                        4 -> "Signet Desktop — Configuración & Personalización"
                                        else -> "Signet Desktop"
                                    },
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleLarge
                                )
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                titleContentColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (selectedTab) {
                            0 -> GenerateScreen(viewModel = viewModel)
                            1 -> SavedKeystoresScreen(viewModel = viewModel)
                            2 -> SignApkScreen(
                                viewModel = viewModel,
                                onNavigateToInspectWithApk = { bytes, name ->
                                    viewModel.analyzeApk(null, bytes, name)
                                    viewModel.setSelectedTab(3)
                                }
                            )
                            3 -> InspectScreen(viewModel = viewModel)
                            4 -> SettingsScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        } else {
            // Mobile Compact Layout with Bottom NavigationBar
            Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                        title = {
                            Text(
                                text = when (selectedTab) {
                                    0 -> "Generar Keystore"
                                    1 -> "Mis Keystores"
                                    2 -> "Firmar APK"
                                    3 -> "Inspeccionar"
                                    4 -> "Configuración"
                                    else -> "Signet"
                                },
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge
                            )
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            titleContentColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                },
                bottomBar = {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ) {
                        NavigationBarItem(
                            selected = selectedTab == 0,
                            onClick = { viewModel.setSelectedTab(0) },
                            icon = {
                                Icon(
                                    imageVector = Icons.Filled.Key,
                                    contentDescription = "Generar"
                                )
                            },
                            label = { Text("Generar") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.testTag("tab_generate")
                        )

                        NavigationBarItem(
                            selected = selectedTab == 1,
                            onClick = { viewModel.setSelectedTab(1) },
                            icon = {
                                BadgedBox(
                                    badge = {
                                        if (savedKeystores.isNotEmpty()) {
                                            Badge {
                                                Text("${savedKeystores.size}")
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (selectedTab == 1) Icons.Filled.Folder else Icons.Outlined.Folder,
                                        contentDescription = "Mis Keystores"
                                    )
                                }
                            },
                            label = { Text("Guardados") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.testTag("tab_saved")
                        )

                        NavigationBarItem(
                            selected = selectedTab == 2,
                            onClick = { viewModel.setSelectedTab(2) },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == 2) Icons.Filled.Draw else Icons.Outlined.Draw,
                                    contentDescription = "Firmar APK"
                                )
                            },
                            label = { Text("Firmar") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.testTag("tab_sign_apk")
                        )

                        NavigationBarItem(
                            selected = selectedTab == 3,
                            onClick = { viewModel.setSelectedTab(3) },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == 3) Icons.Filled.Search else Icons.Outlined.Search,
                                    contentDescription = "Inspeccionar"
                                )
                            },
                            label = { Text("Inspeccionar") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.testTag("tab_inspect")
                        )

                        NavigationBarItem(
                            selected = selectedTab == 4,
                            onClick = { viewModel.setSelectedTab(4) },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == 4) Icons.Filled.Settings else Icons.Outlined.Settings,
                                    contentDescription = "Configuración"
                                )
                            },
                            label = { Text("Ajustes") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.testTag("tab_settings")
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (selectedTab) {
                        0 -> GenerateScreen(viewModel = viewModel)
                        1 -> SavedKeystoresScreen(viewModel = viewModel)
                        2 -> SignApkScreen(
                            viewModel = viewModel,
                            onNavigateToInspectWithApk = { bytes, name ->
                                viewModel.analyzeApk(null, bytes, name)
                                viewModel.setSelectedTab(3)
                            }
                        )
                        3 -> InspectScreen(viewModel = viewModel)
                        4 -> SettingsScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }

    // Modal details sheet
    selectedKeystoreForDetail?.let { details ->
        KeystoreDetailsSheet(
            details = details,
            sheetState = sheetState,
            onDismiss = {
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    viewModel.dismissKeystoreDetails()
                }
            }
        )
    }

    // Modal Update notification and download dialog
    (updateState as? UpdateUiState.Available)?.let { availableState ->
        UpdateDialog(
            viewModel = viewModel,
            updateState = availableState,
            onDismiss = {
                viewModel.dismissUpdate()
            }
        )
    }
}
