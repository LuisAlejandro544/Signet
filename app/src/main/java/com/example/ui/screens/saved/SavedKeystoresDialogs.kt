package com.example.ui.screens.saved

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.model.KeystoreDetails
import com.example.ui.state.RestoreUiState

@Composable
fun SavedKeystoresDialogs(
    restoreState: RestoreUiState,
    keystoreToDelete: KeystoreDetails?,
    onDismissRestore: () -> Unit,
    onConfirmDelete: (KeystoreDetails) -> Unit,
    onDismissDelete: () -> Unit
) {
    // Error dialog for backup restoration
    if (restoreState is RestoreUiState.Error) {
        val errorMsg = restoreState.message
        AlertDialog(
            onDismissRequest = onDismissRestore,
            icon = { Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Error de Integridad / Respaldo Inválido") },
            text = {
                Text(
                    text = errorMsg,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            confirmButton = {
                Button(onClick = onDismissRestore) {
                    Text("Entendido")
                }
            }
        )
    }

    // In-progress restoration dialog
    if (restoreState is RestoreUiState.Restoring) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Restaurando Keystore...") },
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(8.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    Text("Verificando firma criptográfica de Signet y certificados...")
                }
            },
            confirmButton = {}
        )
    }

    // Delete confirmation dialog
    if (keystoreToDelete != null) {
        AlertDialog(
            onDismissRequest = onDismissDelete,
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("¿Eliminar Keystore?") },
            text = { Text("Se eliminará '${keystoreToDelete.fileName}' y su registro de la app. Asegúrate de haber guardado o respaldado el archivo si lo necesitas.") },
            confirmButton = {
                Button(
                    onClick = { onConfirmDelete(keystoreToDelete) }
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissDelete) {
                    Text("Cancelar")
                }
            }
        )
    }
}
