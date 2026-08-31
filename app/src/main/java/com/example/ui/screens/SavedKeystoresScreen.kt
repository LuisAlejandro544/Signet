package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterAltOff
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.KeystoreDetails
import com.example.platform.LocalPlatformServices
import com.example.platform.rememberPlatformFilePicker
import com.example.platform.rememberPlatformFileSaver
import com.example.ui.KeystoreViewModel
import com.example.ui.screens.saved.EmptySavedKeystoresView
import com.example.ui.screens.saved.KeystoreCardItem
import com.example.ui.screens.saved.SavedKeystoresDialogs
import com.example.ui.screens.saved.SavedKeystoresHeader
import com.example.ui.screens.saved.SavedKeystoresSearchAndFilterSection
import com.example.ui.state.RestoreUiState
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SavedKeystoresScreen(
    viewModel: KeystoreViewModel,
    modifier: Modifier = Modifier
) {
    val platformServices = LocalPlatformServices.current
    val coroutineScope = rememberCoroutineScope()
    val savedKeystores by viewModel.savedKeystores.collectAsState()
    val filteredSavedKeystores by viewModel.filteredSavedKeystores.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedFilter by viewModel.selectedSortFilter.collectAsState()
    val restoreState by viewModel.restoreState.collectAsState()
    var keystoreToDelete by remember { mutableStateOf<KeystoreDetails?>(null) }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    // Vault ZIP Save Launcher (Plataforma agnóstica)
    val exportVaultSaver = rememberPlatformFileSaver { result ->
        result.onSuccess {
            platformServices.showToast("¡Bóveda completa exportada con éxito en archivo ZIP!", true)
        }.onFailure { error ->
            platformServices.showToast("Error al guardar el archivo ZIP: ${error.localizedMessage}", true)
        }
    }

    fun startVaultExport() {
        if (savedKeystores.isEmpty()) {
            platformServices.showToast("No hay keystores para exportar.")
            return
        }
        coroutineScope.launch {
            val result = viewModel.createVaultBackupZip()
            result.onSuccess { bytes ->
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                exportVaultSaver.launch(
                    defaultFileName = "Signet-Boveda-$timestamp.zip",
                    mimeType = "application/zip",
                    bytes = bytes
                )
            }.onFailure { error ->
                platformServices.showToast("Error al empaquetar la bóveda: ${error.localizedMessage}", true)
            }
        }
    }

    // ZIP Backup Restore Launcher (Plataforma agnóstica)
    val restoreZipPicker = rememberPlatformFilePicker { platformFile ->
        if (platformFile != null) {
            if (platformFile.bytes.isNotEmpty()) {
                viewModel.restoreFromZip(zipBytes = platformFile.bytes)
            } else {
                platformServices.showToast("El archivo seleccionado está vacío.")
            }
        }
    }

    // Handle Restore Success Alert
    LaunchedEffect(restoreState) {
        if (restoreState is RestoreUiState.Success) {
            val success = restoreState as RestoreUiState.Success
            if (success.isVault) {
                platformServices.showToast(
                    "¡Bóveda restaurada con éxito! Se importaron ${success.restoredList.size} keystores.",
                    true
                )
            } else {
                platformServices.showToast(
                    "¡Keystore '${success.details.fileName}' restaurado con éxito!",
                    true
                )
            }
            viewModel.showKeystoreDetails(success.details)
            viewModel.dismissRestoreState()
        }
    }

    // Dialogs
    SavedKeystoresDialogs(
        restoreState = restoreState,
        keystoreToDelete = keystoreToDelete,
        onDismissRestore = { viewModel.dismissRestoreState() },
        onConfirmDelete = {
            viewModel.deleteKeystore(it)
            keystoreToDelete = null
        },
        onDismissDelete = { keystoreToDelete = null }
    )

    if (savedKeystores.isEmpty()) {
        EmptySavedKeystoresView(
            onCreateNewClick = { viewModel.setSelectedTab(0) },
            onRestoreZipClick = {
                restoreZipPicker.launch(
                    title = "Seleccionar paquete ZIP de respaldo",
                    mimeType = "application/zip",
                    allowedExtensions = listOf("zip")
                )
            },
            modifier = modifier
        )
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Restore and Export Backup action banner
            item {
                SavedKeystoresHeader(
                    totalCount = savedKeystores.size,
                    onRestoreZipClick = {
                        restoreZipPicker.launch(
                            title = "Seleccionar paquete ZIP de respaldo",
                            mimeType = "application/zip",
                            allowedExtensions = listOf("zip")
                        )
                    },
                    onExportVaultClick = { startVaultExport() }
                )
            }

            // Search Bar and Sorting/Filtering section
            item {
                SavedKeystoresSearchAndFilterSection(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                    selectedFilter = selectedFilter,
                    onFilterSelect = { viewModel.setSortFilter(it) },
                    totalCount = savedKeystores.size,
                    filteredCount = filteredSavedKeystores.size,
                    onResetFilters = { viewModel.resetFilters() }
                )
            }

            // Empty state for search/filter query
            if (filteredSavedKeystores.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                            .testTag("empty_search_results_card")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterAltOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                text = "Sin resultados para la búsqueda",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (searchQuery.isNotBlank()) {
                                    "No se encontró ninguna clave que coincida con \"$searchQuery\"."
                                } else {
                                    "No hay claves con el criterio seleccionado."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = { viewModel.resetFilters() },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("reset_filters_empty_view_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RestartAlt,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("Restablecer Filtros", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            } else {
                // List of filtered saved keystores
                items(filteredSavedKeystores, key = { it.id }) { keystore ->
                    KeystoreCardItem(
                        keystore = keystore,
                        dateFormat = dateFormat,
                        onDetailsClick = { viewModel.showKeystoreDetails(it) },
                        onDeleteClick = { keystoreToDelete = it }
                    )
                }
            }
        }
    }
}
