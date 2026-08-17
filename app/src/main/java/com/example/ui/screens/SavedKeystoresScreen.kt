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
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun SavedKeystoresScreen(
    viewModel: KeystoreViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val savedKeystores by viewModel.savedKeystores.collectAsState()
    val restoreState by viewModel.restoreState.collectAsState()
    var keystoreToDelete by remember { mutableStateOf<KeystoreDetails?>(null) }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    // ZIP Backup Restore Launcher
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
            val restored = (restoreState as RestoreUiState.Success).details
            Toast.makeText(context, "¡Keystore '${restored.fileName}' restaurado con éxito!", Toast.LENGTH_LONG).show()
            viewModel.showKeystoreDetails(restored)
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
                    onRestoreZipClick = { restoreZipLauncher.launch("application/zip") }
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
