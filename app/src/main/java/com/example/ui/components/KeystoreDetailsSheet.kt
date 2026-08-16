package com.example.ui.components

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.data.model.KeystoreDetails
import com.example.ui.components.details.DetailsActionUtils
import com.example.ui.components.details.KeystoreBase64Card
import com.example.ui.components.details.KeystoreCodeSnippetsCard
import com.example.ui.components.details.KeystoreCredentialsCard
import com.example.ui.components.details.KeystoreFingerprintsCard
import com.example.ui.components.details.KeystoreHeaderSection
import com.example.ui.components.details.PepkExportDialog
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeystoreDetailsSheet(
    details: KeystoreDetails,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showStorePassword by remember { mutableStateOf(false) }
    var showKeyPassword by remember { mutableStateOf(false) }
    var isBase64Expanded by remember { mutableStateOf(false) }
    var showPepkDialog by remember { mutableStateOf(false) }

    // Resolve Base64 if not already present in details
    val base64String = remember(details) {
        if (details.base64Content.isNotBlank()) {
            details.base64Content
        } else if (details.filePath.isNotBlank()) {
            try {
                val f = File(details.filePath)
                if (f.exists()) {
                    java.util.Base64.getEncoder().encodeToString(f.readBytes())
                } else ""
            } catch (_: Exception) {
                ""
            }
        } else ""
    }

    // SAF Document export launcher for plain Keystore file
    val exportFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null && details.filePath.isNotBlank()) {
            try {
                val sourceFile = File(details.filePath)
                if (sourceFile.exists()) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        FileInputStream(sourceFile).use { input ->
                            input.copyTo(out)
                        }
                    }
                    Toast.makeText(context, "Archivo keystore guardado exitosamente.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error al guardar: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // SAF ZIP bundle export launcher (Keystore + Credentials + Signed Manifest + Snippets)
    val exportZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            try {
                val bytes = if (details.filePath.isNotBlank() && File(details.filePath).exists()) {
                    File(details.filePath).readBytes()
                } else if (details.base64Content.isNotBlank()) {
                    android.util.Base64.decode(details.base64Content, android.util.Base64.DEFAULT)
                } else {
                    null
                }

                if (bytes != null) {
                    val zipData = com.example.crypto.SignetBackupManager.createBackupZip(details, bytes)
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(zipData)
                    }
                    Toast.makeText(context, "Paquete ZIP de respaldo exportado exitosamente.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "No se encontraron los datos del keystore para exportar.", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error al generar ZIP: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Title, alias info, close and action buttons (ZIP Export, PEPK, SAF & Share)
            KeystoreHeaderSection(
                details = details,
                onDismiss = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                },
                onExportZipClick = {
                    val baseName = details.fileName.substringBeforeLast(".")
                    exportZipLauncher.launch("${baseName}-signet-bundle.zip")
                },
                onExportPepkClick = {
                    showPepkDialog = true
                },
                onExportFileClick = {
                    exportFileLauncher.launch(details.fileName)
                },
                onShareClick = {
                    DetailsActionUtils.shareKeystoreFile(context, details)
                }
            )

            // Saved Credentials Card (File name, Key alias, Passwords with visibility & copy)
            KeystoreCredentialsCard(
                details = details,
                showStorePassword = showStorePassword,
                showKeyPassword = showKeyPassword,
                onToggleStorePassword = { showStorePassword = !showStorePassword },
                onToggleKeyPassword = { showKeyPassword = !showKeyPassword },
                context = context
            )

            // Base64 Card (for CI/CD secrets & env variables)
            KeystoreBase64Card(
                base64String = base64String,
                isBase64Expanded = isBase64Expanded,
                onToggleExpand = { isBase64Expanded = !isBase64Expanded },
                context = context
            )

            // Certificate Fingerprints (SHA-256, SHA-1, MD5) & X.509 validity details
            KeystoreFingerprintsCard(
                details = details,
                context = context
            )

            // Code and CI/CD Snippets (Gradle KTS, Groovy, GitHub Actions, apksigner CLI, PEM)
            KeystoreCodeSnippetsCard(
                details = details,
                context = context
            )
        }
    }

    if (showPepkDialog) {
        PepkExportDialog(
            details = details,
            onDismiss = { showPepkDialog = false }
        )
    }
}
