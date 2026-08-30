package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.data.model.KeystoreDetails
import com.example.ui.KeystoreViewModel
import com.example.ui.screens.saved.EmptySavedKeystoresView
import com.example.ui.screens.saved.KeystoreCardItem
import com.example.ui.screens.saved.SavedKeystoresDialogs
import com.example.ui.screens.saved.SavedKeystoresHeader
import com.example.ui.state.RestoreUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SavedKeystoresScreen(
    viewModel: KeystoreViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val savedKeystores by viewModel.savedKeystores.collectAsState()
    val restoreState by viewModel.restoreState.collectAsState()
    var keystoreToDelete by remember { mutableStateOf<KeystoreDetails?>(null) }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    var pendingVaultBytes by remember { mutableStateOf<ByteArray?>(null) }

    // Vault ZIP Save Launcher
    val exportVaultLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null && pendingVaultBytes != null) {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { os ->
                        os.write(pendingVaultBytes!!)
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "¡Bóveda completa exportada con éxito en archivo ZIP!", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Error al guardar el archivo ZIP: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                } finally {
                    pendingVaultBytes = null
                }
            }
        } else {
            pendingVaultBytes = null
        }
    }

    fun startVaultExport() {
        if (savedKeystores.isEmpty()) {
            Toast.makeText(context, "No hay keystores para exportar.", Toast.LENGTH_SHORT).show()
            return
        }
        coroutineScope.launch {
            val result = viewModel.createVaultBackupZip()
            result.onSuccess { bytes ->
                pendingVaultBytes = bytes
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                exportVaultLauncher.launch("Signet-Boveda-$timestamp.zip")
            }.onFailure { error ->
                Toast.makeText(context, "Error al empaquetar la bóveda: ${error.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // ZIP Backup Restore Launcher (Supports both single backup and full vault backup)
    val restoreZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val zipBytes = context.contentResolver.openInputStream(uri)?.use { stream ->
                    stream.readBytes()
                }
                if (zipBytes != null && zipBytes.isNotEmpty()) {
                    viewModel.restoreFromZip(context, zipBytes)
                } else {
                    Toast.makeText(context, "El archivo seleccionado está vacío.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "No se pudo abrir el archivo ZIP: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Handle Restore Success Alert
    LaunchedEffect(restoreState) {
        if (restoreState is RestoreUiState.Success) {
            val success = restoreState as RestoreUiState.Success
            if (success.isVault) {
                Toast.makeText(
                    context,
                    "¡Bóveda restaurada con éxito! Se importaron ${success.restoredList.size} keystores.",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(
                    context,
                    "¡Keystore '${success.details.fileName}' restaurado con éxito!",
                    Toast.LENGTH_LONG
                ).show()
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
            onRestoreZipClick = { restoreZipLauncher.launch("application/zip") },
            modifier = modifier
        )
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Restore backup action banner
            item {
                SavedKeystoresHeader(
                    totalCount = savedKeystores.size,
                    onRestoreZipClick = { restoreZipLauncher.launch("application/zip") },
                    onExportVaultClick = { startVaultExport() }
                )
            }

            // List of saved keystores
            items(savedKeystores, key = { it.id }) { keystore ->
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
