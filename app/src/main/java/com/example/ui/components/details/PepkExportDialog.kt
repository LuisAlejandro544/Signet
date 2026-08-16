package com.example.ui.components.details

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.crypto.PepkGenerator
import com.example.crypto.SignetBackupManager
import com.example.crypto.SnippetGenerator
import com.example.data.model.KeystoreDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private enum class PepkDialogTab {
    GENERATE_PEPK,
    CLI_COMMAND
}

private enum class PepkExportMode {
    ZIP_BUNDLE, // Keystore + .pepk + Credentials + Properties + Manifest
    PEPK_ONLY   // Just the .pepk binary file
}

@Composable
fun PepkExportDialog(
    details: KeystoreDetails,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableStateOf(PepkDialogTab.GENERATE_PEPK) }
    var exportMode by remember { mutableStateOf(PepkExportMode.ZIP_BUNDLE) }
    var googlePemContent by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var generatedPepkBytes by remember { mutableStateOf<ByteArray?>(null) }
    var generatedZipBytes by remember { mutableStateOf<ByteArray?>(null) }

    // SAF CreateDocument for saving the .pepk file
    val savePepkLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        if (uri != null && generatedPepkBytes != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(generatedPepkBytes!!)
                }
                Toast.makeText(context, "¡Archivo .pepk guardado exitosamente!", Toast.LENGTH_LONG).show()
                onDismiss()
            } catch (e: Exception) {
                Toast.makeText(context, "Error al guardar .pepk: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // SAF CreateDocument for saving the full ZIP bundle (Keystore + PEPK + Keys)
    val saveZipBundleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        if (uri != null && generatedZipBytes != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(generatedZipBytes!!)
                }
                Toast.makeText(context, "¡Paquete ZIP (Llave + .pepk + Claves) guardado exitosamente!", Toast.LENGTH_LONG).show()
                onDismiss()
            } catch (e: Exception) {
                Toast.makeText(context, "Error al guardar ZIP: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // SAF OpenDocument for picking Google's encryption_public_key.pem
    val pickPemLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val content = inputStream.bufferedReader().readText()
                    googlePemContent = content
                    errorMessage = null
                    Toast.makeText(context, "Clave pública .pem cargada.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                errorMessage = "Error al leer el archivo .pem: ${e.localizedMessage}"
            }
        }
    }

    fun startPepkGeneration() {
        if (googlePemContent.isBlank()) {
            errorMessage = "Pega o selecciona el archivo 'encryption_public_key.pem' de Google Play."
            return
        }

        errorMessage = null
        isProcessing = true

        scope.launch(Dispatchers.IO) {
            try {
                val keystoreBytes = if (details.filePath.isNotBlank() && File(details.filePath).exists()) {
                    File(details.filePath).readBytes()
                } else if (details.base64Content.isNotBlank()) {
                    android.util.Base64.decode(details.base64Content, android.util.Base64.DEFAULT)
                } else {
                    throw IllegalStateException("No se encontraron los datos del keystore en el dispositivo.")
                }

                val pepkData = PepkGenerator.generatePepkFromKeystore(
                    keystoreBytes = keystoreBytes,
                    storePassword = details.storePassword,
                    alias = details.alias,
                    keyPassword = details.keyPassword,
                    googlePublicKeyPem = googlePemContent
                )

                val baseAlias = details.alias.ifBlank { "release-key" }.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
                val pepkFileName = "${baseAlias}_encrypted_key.pepk"

                if (exportMode == PepkExportMode.ZIP_BUNDLE) {
                    val zipData = SignetBackupManager.createBackupZip(
                        details = details,
                        keystoreBytes = keystoreBytes,
                        pepkBytes = pepkData,
                        pepkFileName = pepkFileName
                    )
                    withContext(Dispatchers.Main) {
                        isProcessing = false
                        generatedZipBytes = zipData
                        val baseName = details.fileName.substringBeforeLast(".")
                        saveZipBundleLauncher.launch("${baseName}-play-bundle.zip")
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        isProcessing = false
                        generatedPepkBytes = pepkData
                        savePepkLauncher.launch(pepkFileName)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isProcessing = false
                    errorMessage = "Error al generar PEPK: ${e.localizedMessage ?: e.message}"
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.fillMaxWidth(),
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CloudUpload,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
            }
        },
        title = {
            Text(
                text = "Exportar para Google Play (.pepk)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Genera el archivo cifrado (.pepk) requerido por Google Play App Signing para migrar o registrar tu clave de firma de forma segura.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )

                // Tab Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedTab == PepkDialogTab.GENERATE_PEPK,
                        onClick = { selectedTab = PepkDialogTab.GENERATE_PEPK },
                        label = { Text("Generar .pepk", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )

                    FilterChip(
                        selected = selectedTab == PepkDialogTab.CLI_COMMAND,
                        onClick = { selectedTab = PepkDialogTab.CLI_COMMAND },
                        label = { Text("Comando CLI", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }

                if (selectedTab == PepkDialogTab.GENERATE_PEPK) {
                    // Instruction Banner
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "En Google Play Console ve a 'Configuración > Integridad de la app > Firma de apps' y descarga 'encryption_public_key.pem'.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontSize = 11.5.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }

                    // PEM File Input
                    OutlinedTextField(
                        value = googlePemContent,
                        onValueChange = {
                            googlePemContent = it
                            errorMessage = null
                        },
                        label = { Text("Clave Pública de Google (.pem)") },
                        placeholder = { Text("-----BEGIN PUBLIC KEY-----\n...\n-----END PUBLIC KEY-----") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(115.dp)
                            .testTag("pepk_google_pem_input"),
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                        maxLines = 5,
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Button to load file from disk
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { pickPemLauncher.launch("*/*") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pick_pem_file_button"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Cargar .pem", fontSize = 12.sp)
                        }

                        TextButton(
                            onClick = {
                                googlePemContent = """
                                    -----BEGIN PUBLIC KEY-----
                                    MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAu1SU1LfVLPHCozJAOMjz
                                    wa8WsCOH3G6kFvB9UjYp4Zp8z1jX5P+4k5hL7n2d1pQ0rA9e8f7g6h5j4k3m2n1o
                                    pQrStUvWxYz0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNO
                                    PQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNO
                                    PQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNO
                                    PQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNO
                                    PQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNO
                                    PQIDAQAB
                                    -----END PUBLIC KEY-----
                                """.trimIndent()
                                errorMessage = null
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Ejemplo PEM", fontSize = 12.sp)
                        }
                    }

                    // Export Format Options (ZIP Bundle vs PEPK Only)
                    Text(
                        text = "Formato de exportación:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (exportMode == PepkExportMode.ZIP_BUNDLE) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            }
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { exportMode = PepkExportMode.ZIP_BUNDLE }
                            .border(
                                width = if (exportMode == PepkExportMode.ZIP_BUNDLE) 1.5.dp else 1.dp,
                                color = if (exportMode == PepkExportMode.ZIP_BUNDLE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .testTag("pepk_export_zip_bundle_option")
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            RadioButton(
                                selected = exportMode == PepkExportMode.ZIP_BUNDLE,
                                onClick = { exportMode = PepkExportMode.ZIP_BUNDLE },
                                modifier = Modifier.size(20.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Paquete ZIP Completo",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "Recomendado",
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Incluye tu Keystore (${details.fileName}), el archivo cifrado .pepk, credenciales, propiedades y respaldo firmado.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (exportMode == PepkExportMode.PEPK_ONLY) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            }
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { exportMode = PepkExportMode.PEPK_ONLY }
                            .border(
                                width = if (exportMode == PepkExportMode.PEPK_ONLY) 1.5.dp else 1.dp,
                                color = if (exportMode == PepkExportMode.PEPK_ONLY) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .testTag("pepk_export_only_file_option")
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            RadioButton(
                                selected = exportMode == PepkExportMode.PEPK_ONLY,
                                onClick = { exportMode = PepkExportMode.PEPK_ONLY },
                                modifier = Modifier.size(20.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Solo archivo .pepk",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Exporta únicamente el binario cifrado .pepk para subir directamente a Google Play Console.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }

                    // Error banner
                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    // CLI Command Tab
                    val cliSnippet = SnippetGenerator.generatePepkSnippet(details.fileName, details.alias)

                    Text(
                        text = "Si prefieres usar la herramienta oficial en tu computadora, ejecuta:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                    ) {
                        Text(
                            text = cliSnippet,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            ),
                            modifier = Modifier
                                .padding(10.dp)
                                .horizontalScroll(rememberScrollState())
                        )
                    }

                    Button(
                        onClick = {
                            DetailsActionUtils.copyToClipboard(context, "Comando PEPK", cliSnippet)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Copiar Comando")
                    }
                }
            }
        },
        confirmButton = {
            if (selectedTab == PepkDialogTab.GENERATE_PEPK) {
                Button(
                    onClick = { startPepkGeneration() },
                    enabled = !isProcessing,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("submit_generate_pepk_button")
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Cifrando...")
                    } else {
                        val icon = if (exportMode == PepkExportMode.ZIP_BUNDLE) Icons.Default.Archive else Icons.Default.Lock
                        val buttonText = if (exportMode == PepkExportMode.ZIP_BUNDLE) "Exportar Paquete ZIP" else "Generar y Guardar .pepk"
                        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(buttonText)
                    }
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Cerrar")
            }
        }
    )
}

