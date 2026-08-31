package com.example.ui.navigation

import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Top App Bar for Signet supporting adaptive titles across Desktop, Tablet and Phone layouts.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignetTopAppBar(
    selectedTab: Int,
    isWideScreen: Boolean,
    modifier: Modifier = Modifier
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = if (isWideScreen) {
                    when (selectedTab) {
                        0 -> "Signet Desktop — Generar Keystore"
                        1 -> "Signet Desktop — Mis Keystores Guardados"
                        2 -> "Signet Desktop — Firmar APK (v1, v2, v3)"
                        3 -> "Signet Desktop — Inspeccionar Certificados & APKs"
                        4 -> "Signet Desktop — Configuración & Personalización"
                        else -> "Signet Desktop"
                    }
                } else {
                    when (selectedTab) {
                        0 -> "Generar Keystore"
                        1 -> "Mis Keystores"
                        2 -> "Firmar APK"
                        3 -> "Inspeccionar"
                        4 -> "Configuración"
                        else -> "Signet"
                    }
                },
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        ),
        modifier = modifier
    )
}

/**
 * Navigation Rail for Tablet / Desktop wide screens.
 */
@Composable
fun SignetNavigationRail(
    selectedTab: Int,
    savedCount: Int,
    onTabSelected: (Int) -> Unit,
    onOpenVaultFolder: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationRail(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        header = {
            IconButton(
                onClick = onOpenVaultFolder,
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
        modifier = modifier.fillMaxHeight()
    ) {
        NavigationRailItem(
            selected = selectedTab == 0,
            onClick = { onTabSelected(0) },
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
            onClick = { onTabSelected(1) },
            icon = {
                BadgedBox(
                    badge = {
                        if (savedCount > 0) {
                            Badge { Text("$savedCount") }
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
            onClick = { onTabSelected(2) },
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
            onClick = { onTabSelected(3) },
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
            onClick = { onTabSelected(4) },
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
}

/**
 * Bottom Navigation Bar for Mobile Compact screens.
 */
@Composable
fun SignetNavigationBar(
    selectedTab: Int,
    savedCount: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
    ) {
        NavigationBarItem(
            selected = selectedTab == 0,
            onClick = { onTabSelected(0) },
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
            onClick = { onTabSelected(1) },
            icon = {
                BadgedBox(
                    badge = {
                        if (savedCount > 0) {
                            Badge { Text("$savedCount") }
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
            onClick = { onTabSelected(2) },
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
            onClick = { onTabSelected(3) },
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
            onClick = { onTabSelected(4) },
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
}
