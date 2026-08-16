package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.IntegrationInstructions
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.core.content.FileProvider
import com.example.crypto.KeystoreGenerator
import com.example.data.model.KeystoreDetails
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    var showStorePassword by remember { mutableStateOf(false) }
    var showKeyPassword by remember { mutableStateOf(false) }
    var isBase64Expanded by remember { mutableStateOf(false) }
    var selectedSnippetTab by remember { mutableStateOf(CodeSnippetTab.GRADLE_KTS) }

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

    // SAF Document export launcher
    val exportLauncher = rememberLauncherForActivityResult(
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
                    Toast.makeText(context, "Archivo guardado exitosamente.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error al guardar: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
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
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = details.fileName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Alias: ${details.alias} • ${details.algorithm}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = { scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() } }) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar")
                }
            }

            // Quick Export & Share Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        exportLauncher.launch(details.fileName)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("export_keystore_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Exportar Archivo", fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = {
                        shareKeystoreFile(context, details)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("share_keystore_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Compartir", fontWeight = FontWeight.SemiBold)
                }
            }

            // Section: Credenciales Guardadas (Nombre, Alias, Contraseñas con copiar en 1 toque)
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Credenciales Guardadas",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // File Name Item
                    CredentialItem(
                        label = "Nombre del Archivo",
                        value = details.fileName,
                        context = context
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Key Alias Item
                    CredentialItem(
                        label = "Alias de la Clave (Key Alias)",
                        value = details.alias,
                        context = context
                    )

                    // Keystore Password
                    if (details.storePassword.isNotBlank()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        PasswordCredentialItem(
                            label = "Contraseña del Keystore",
                            password = details.storePassword,
                            isVisible = showStorePassword,
                            onToggleVisibility = { showStorePassword = !showStorePassword },
                            context = context
                        )
                    }

                    // Key Password
                    if (details.keyPassword.isNotBlank() && details.keyPassword != details.storePassword) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        PasswordCredentialItem(
                            label = "Contraseña de la Clave (Key Password)",
                            password = details.keyPassword,
                            isVisible = showKeyPassword,
                            onToggleVisibility = { showKeyPassword = !showKeyPassword },
                            context = context
                        )
                    } else if (details.storePassword.isNotBlank()) {
                        Text(
                            text = "✓ La contraseña de la clave es la misma que la del keystore.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Section: Keystore en Base64 (Convertir y Copiar)
            if (base64String.isNotBlank()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
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
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DataObject,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Keystore en Base64",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Text(
                                text = "${base64String.length} chars",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "Ideal para variables de entorno en CI/CD (GitHub Actions KEYSTORE_BASE64, Fastlane, Bitrise) sin subir archivos binarios.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )

                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                        ) {
                            val previewText = if (isBase64Expanded || base64String.length <= 160) {
                                base64String
                            } else {
                                "${base64String.take(80)}...\n...${base64String.takeLast(80)}"
                            }

                            Text(
                                text = previewText,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(10.dp)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    copyToClipboard(context, "Keystore Base64", base64String)
                                },
                                modifier = Modifier.weight(1f).testTag("copy_base64_button"),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Copiar Base64")
                            }

                            if (base64String.length > 160) {
                                OutlinedButton(
                                    onClick = { isBase64Expanded = !isBase64Expanded },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(if (isBase64Expanded) "Colapsar" else "Ver Todo")
                                }
                            }
                        }
                    }
                }
            }

            // Certificate Fingerprints Card (SHA-256, SHA-1, MD5)
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Huellas Digitales del Certificado (Fingerprints)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    FingerprintItem(
                        label = "SHA-256 (Google Play / Firebase / APIs)",
                        value = details.sha256Fingerprint,
                        context = context
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    FingerprintItem(
                        label = "SHA-1 (Legacy / OAuth / Uptodown)",
                        value = details.sha1Fingerprint,
                        context = context
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    FingerprintItem(
                        label = "MD5",
                        value = details.md5Fingerprint,
                        context = context
                    )
                }
            }

            // Certificate Details Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Detalles del Certificado X.509",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    DetailRow(label = "Sujeto / Propietario", value = details.subjectDn)
                    DetailRow(label = "Válido desde", value = dateFormat.format(Date(details.validFrom)))
                    DetailRow(label = "Válido hasta (Expira)", value = dateFormat.format(Date(details.validUntil)))
                    DetailRow(label = "Número de Serie", value = details.serialNumber)
                }
            }

            // Code & CI/CD Snippets Section
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Code,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Configuración de Firma & CI/CD",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Text(
                        text = "Visualiza y copia directamente el código listo para Gradle, pipelines de GitHub Actions o comandos de terminal.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Horizontal FilterChips for Snippet Tabs
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CodeSnippetTab.values().forEach { tab ->
                            FilterChip(
                                selected = selectedSnippetTab == tab,
                                onClick = { selectedSnippetTab = tab },
                                label = { Text(tab.tabLabel, fontSize = 12.sp) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }

                    // Active Snippet Content Resolver
                    val currentSnippet = when (selectedSnippetTab) {
                        CodeSnippetTab.GRADLE_KTS -> KeystoreGenerator.generateGradleKtsSnippet(details.fileName, details.alias)
                        CodeSnippetTab.GITHUB_ACTIONS -> KeystoreGenerator.generateGitHubActionsWorkflow(details.fileName, details.alias)
                        CodeSnippetTab.GRADLE_GROOVY -> KeystoreGenerator.generateGradleGroovySnippet(details.fileName, details.alias)
                        CodeSnippetTab.APKSIGNER -> KeystoreGenerator.generateApksignerSnippet(details.fileName, details.alias)
                        CodeSnippetTab.PEM_CERT -> details.certificatePem
                    }

                    val currentDescription = when (selectedSnippetTab) {
                        CodeSnippetTab.GRADLE_KTS -> "Pega este bloque en 'app/build.gradle.kts'. Lee las contraseñas de variables de entorno para máxima seguridad."
                        CodeSnippetTab.GITHUB_ACTIONS -> "Crea el archivo '.github/workflows/build-and-sign.yml' en tu repositorio y agrega el secreto 'KEYSTORE_BASE64' (Settings > Secrets)."
                        CodeSnippetTab.GRADLE_GROOVY -> "Para proyectos con Groovy DSL tradicional (Flutter / React Native / Android heredado)."
                        CodeSnippetTab.APKSIGNER -> "Comandos oficiales para alinear con zipalign y firmar APKs con soporte de firma v1, v2 y v3."
                        CodeSnippetTab.PEM_CERT -> "Certificado público X.509 en formato PEM para consolas de APIs y proveedores de autenticación."
                    }

                    Text(
                        text = currentDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 16.sp
                    )

                    // Code Viewer Box
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                    ) {
                        Text(
                            text = currentSnippet,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.5.sp,
                                lineHeight = 16.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .padding(12.dp)
                                .horizontalScroll(rememberScrollState())
                        )
                    }

                    // Copy Snippet Action Button
                    Button(
                        onClick = {
                            copyToClipboard(context, selectedSnippetTab.tabLabel, currentSnippet)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("copy_snippet_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Copiar ${selectedSnippetTab.tabLabel}")
                    }
                }
            }
        }
    }
}

enum class CodeSnippetTab(
    val tabLabel: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    GRADLE_KTS("build.gradle.kts", Icons.Default.Code),
    GITHUB_ACTIONS("GitHub Actions (.yml)", Icons.Default.IntegrationInstructions),
    GRADLE_GROOVY("build.gradle (Groovy)", Icons.Default.Code),
    APKSIGNER("apksigner CLI", Icons.Default.Terminal),
    PEM_CERT("Certificado PEM", Icons.Default.Security)
}

@Composable
private fun CredentialItem(
    label: String,
    value: String,
    context: Context
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        IconButton(
            onClick = { copyToClipboard(context, label, value) },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Copiar $label",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun PasswordCredentialItem(
    label: String,
    password: String,
    isVisible: Boolean,
    onToggleVisibility: () -> Unit,
    context: Context
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (isVisible) password else "••••••••••••••••",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onToggleVisibility,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (isVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (isVisible) "Ocultar" else "Mostrar",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = { copyToClipboard(context, label, password) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copiar $label",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun FingerprintItem(
    label: String,
    value: String,
    context: Context
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            IconButton(
                onClick = { copyToClipboard(context, label, value) },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copiar $label",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(10.dp)
            )
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "$label copiado al portapapeles", Toast.LENGTH_SHORT).show()
}

private fun shareKeystoreFile(context: Context, details: KeystoreDetails) {
    try {
        val file = File(details.filePath)
        if (!file.exists()) {
            Toast.makeText(context, "El archivo original ya no está en el almacenamiento local.", Toast.LENGTH_SHORT).show()
            return
        }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, details.fileName)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Compartir Keystore"))
    } catch (e: Exception) {
        Toast.makeText(context, "Error al compartir archivo: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
    }
}

