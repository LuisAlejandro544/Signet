package com.example.ui.screens.sign

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
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
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.state.SignApkFormState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@Composable
fun SelectApkCard(
    formState: SignApkFormState,
    onApkSelected: (name: String, size: Long, bytes: ByteArray, context: Context) -> Unit,
    onClearApk: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val apkPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val (name, size, bytes) = withContext(Dispatchers.IO) {
                    var resolvedName = "app.apk"
                    var resolvedSize = 0L
                    try {
                        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                            if (cursor.moveToFirst()) {
                                if (nameIndex != -1) resolvedName = cursor.getString(nameIndex) ?: "app.apk"
                                if (sizeIndex != -1) resolvedSize = cursor.getLong(sizeIndex)
                            }
                        }
                    } catch (_: Exception) {}

                    val streamBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: ByteArray(0)
                    if (resolvedSize == 0L) resolvedSize = streamBytes.size.toLong()
                    Triple(resolvedName, resolvedSize, streamBytes)
                }

                if (bytes.isNotEmpty()) {
                    onApkSelected(name, size, bytes, context)
                }
            }
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("card_select_apk"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Android,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "1. Archivo APK a Firmar",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (formState.apkBytes != null) {
                    IconButton(
                        onClick = onClearApk,
                        modifier = Modifier.testTag("btn_clear_selected_apk")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Quitar APK",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            if (formState.apkBytes == null) {
                Text(
                    text = "Selecciona un paquete APK (.apk) sin firmar o firmado previamente desde el almacenamiento de tu teléfono.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = { apkPickerLauncher.launch("*/*") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_pick_apk_file")
                ) {
                    Icon(imageVector = Icons.Default.FileUpload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Seleccionar archivo APK")
                }
            } else {
                // Info of loaded APK
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = formState.apkFileName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )

                        Text(
                            text = "Tamaño: ${formatBytes(formState.apkFileSizeBytes)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (!formState.detectedPackageName.isNullOrBlank()) {
                            Text(
                                text = "Paquete: ${formState.detectedPackageName}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (formState.hasExistingSignatures) {
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text("Firmado (${formState.existingSchemes.joinToString()})") },
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                    )
                                )
                            } else {
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text("Sin firmar") },
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                FilledTonalButton(
                    onClick = { apkPickerLauncher.launch("*/*") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.FileUpload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cambiar archivo APK")
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
