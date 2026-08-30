package com.example.ui.screens.sign

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ApkSigningResult
import com.example.platform.LocalPlatformServices
import com.example.platform.rememberPlatformFileSaver
import com.example.ui.state.ApkSigningUiState
import java.io.File
import java.util.Locale

@Composable
fun SigningResultCard(
    signingState: ApkSigningUiState,
    onInstallApk: (File) -> Unit,
    onInspectApk: (ByteArray, String) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    val platformServices = LocalPlatformServices.current

    when (signingState) {
        is ApkSigningUiState.Idle -> {
            // Nothing shown
        }

        is ApkSigningUiState.Signing -> {
            Card(
                modifier = modifier
                    .fillMaxWidth()
                    .testTag("card_signing_progress"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Firmando y optimizando APK...",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    LinearProgressIndicator(
                        progress = { signingState.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                    )

                    Text(
                        text = signingState.stepMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        is ApkSigningUiState.Error -> {
            Card(
                modifier = modifier
                    .fillMaxWidth()
                    .testTag("card_signing_error"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Error durante la firma",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Text(
                        text = signingState.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )

                    OutlinedButton(
                        onClick = onReset,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Reintentar")
                    }
                }
            }
        }

        is ApkSigningUiState.Success -> {
            val result = signingState.result
            val saveApkSaver = rememberPlatformFileSaver { saveResult ->
                saveResult.onSuccess {
                    platformServices.showToast("¡APK guardado correctamente!", false)
                }.onFailure { err ->
                    platformServices.showToast("Error al guardar APK: ${err.localizedMessage}", true)
                }
            }

            Card(
                modifier = modifier
                    .fillMaxWidth()
                    .testTag("card_signing_success"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "¡APK Firmado Exitosamente!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Completado en ${result.durationMs} ms",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Archivo: ${result.outputFileName}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Tamaño final: ${formatBytes(result.outputFileSizeBytes)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (result.packageName != null) {
                                Text(
                                    text = "Paquete: ${result.packageName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                result.appliedSchemes.forEach { scheme ->
                                    SuggestionChip(
                                        onClick = {},
                                        label = { Text(scheme, style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                                if (result.zipalignApplied) {
                                    SuggestionChip(
                                        onClick = {},
                                        label = { Text("Zipalign OK", style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }

                            if (result.sha256Fingerprint.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Huella SHA-256 del Certificado:",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = result.sha256Fingerprint,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = {
                                            platformServices.copyToClipboard("SHA-256", result.sha256Fingerprint)
                                            platformServices.showToast("Huella copiada", false)
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copiar huella",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Action buttons
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // 1. Install APK on device
                        if (result.signedApkFile != null) {
                            Button(
                                onClick = { onInstallApk(result.signedApkFile) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_install_signed_apk")
                            ) {
                                Icon(imageVector = Icons.Default.InstallMobile, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Instalar en este dispositivo")
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // 2. Save / Export
                            FilledTonalButton(
                                onClick = {
                                    saveApkSaver.launch(
                                        defaultFileName = result.outputFileName,
                                        mimeType = "application/vnd.android.package-archive",
                                        bytes = result.signedApkBytes ?: ByteArray(0)
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_save_signed_apk")
                            ) {
                                Icon(imageVector = Icons.Default.Download, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Guardar")
                            }

                            // 3. Share APK
                            if (result.signedApkFile != null) {
                                OutlinedButton(
                                    onClick = {
                                        platformServices.shareFile(
                                            file = result.signedApkFile,
                                            mimeType = "application/vnd.android.package-archive"
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("btn_share_signed_apk")
                                ) {
                                    Icon(imageVector = Icons.Default.Share, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Compartir")
                                }
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // 4. Inspect in Signet
                            if (result.signedApkBytes != null) {
                                OutlinedButton(
                                    onClick = { onInspectApk(result.signedApkBytes, result.outputFileName) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("btn_inspect_result_apk")
                                ) {
                                    Icon(imageVector = Icons.Default.Search, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Inspeccionar")
                                }
                            }

                            // 5. Sign Another
                            OutlinedButton(
                                onClick = onReset,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_sign_another_apk")
                            ) {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Firmar Otro")
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format(Locale.ROOT, "%.1f KB", kb)
    val mb = kb / 1024.0
    return String.format(Locale.ROOT, "%.2f MB", mb)
}
