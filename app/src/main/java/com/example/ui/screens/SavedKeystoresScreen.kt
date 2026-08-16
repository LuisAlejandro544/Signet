package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.KeystoreDetails
import com.example.ui.KeystoreViewModel
import com.example.ui.RestoreUiState
import java.text.SimpleDateFormat
import java.util.Date
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
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

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

    // Handle Restore Success & Error Alerts
    LaunchedEffect(restoreState) {
        if (restoreState is RestoreUiState.Success) {
            val restored = (restoreState as RestoreUiState.Success).details
            Toast.makeText(context, "¡Keystore '${restored.fileName}' restaurado con éxito!", Toast.LENGTH_LONG).show()
            viewModel.showKeystoreDetails(restored)
            viewModel.dismissRestoreState()
        }
    }

    if (restoreState is RestoreUiState.Error) {
        val errorMsg = (restoreState as RestoreUiState.Error).message
        AlertDialog(
            onDismissRequest = { viewModel.dismissRestoreState() },
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
                Button(onClick = { viewModel.dismissRestoreState() }) {
                    Text("Entendido")
                }
            }
        )
    }

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

    if (keystoreToDelete != null) {
        AlertDialog(
            onDismissRequest = { keystoreToDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("¿Eliminar Keystore?") },
            text = { Text("Se eliminará '${keystoreToDelete?.fileName}' y su registro de la app. Asegúrate de haber guardado o respaldado el archivo si lo necesitas.") },
            confirmButton = {
                Button(
                    onClick = {
                        keystoreToDelete?.let { viewModel.deleteKeystore(it) }
                        keystoreToDelete = null
                    }
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { keystoreToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (savedKeystores.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Text(
                    text = "Aún no tienes keystores guardados",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Puedes generar uno nuevo desde cero o restaurar un paquete ZIP respaldado previamente en Signet con todas sus credenciales.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = { viewModel.setSelectedTab(0) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Crear un Nuevo Keystore")
                    }

                    OutlinedButton(
                        onClick = { restoreZipLauncher.launch("application/zip") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("restore_backup_zip_button_empty")
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Restaurar Respaldo (.zip)")
                    }
                }
            }
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Restore backup action banner
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Archive,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = "Restaurar Paquete ZIP",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Importa respaldos firmados de Signet con claves y huellas.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Button(
                            onClick = { restoreZipLauncher.launch("application/zip") },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("restore_backup_zip_button_header")
                        ) {
                            Text("Restaurar", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            items(savedKeystores, key = { it.id }) { keystore ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                        .clickable { viewModel.showKeystoreDetails(keystore) }
                        .testTag("keystore_item_${keystore.id}")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Key,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = keystore.fileName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Alias: ${keystore.alias} • ${dateFormat.format(Date(keystore.createdAt))}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            IconButton(onClick = { keystoreToDelete = keystore }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Eliminar",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Fingerprint preview
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "SHA-256: ${keystore.sha256Fingerprint.take(23)}...",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("SHA-256", keystore.sha256Fingerprint))
                                        Toast.makeText(context, "SHA-256 copiado", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.ContentCopy,
                                        contentDescription = "Copiar SHA-256",
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        // Actions Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { viewModel.showKeystoreDetails(keystore) }
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Ver Detalles y Exportar")
                            }
                        }
                    }
                }
            }
        }
    }
}

