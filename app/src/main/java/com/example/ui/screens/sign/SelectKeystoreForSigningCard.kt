package com.example.ui.screens.sign

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.model.KeystoreDetails
import com.example.ui.state.KeystoreSourceMode
import com.example.ui.state.SignApkFormState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SelectKeystoreForSigningCard(
    formState: SignApkFormState,
    savedKeystores: List<KeystoreDetails>,
    onSelectSavedKeystore: (KeystoreDetails) -> Unit,
    onSetExternalKeystore: (name: String, bytes: ByteArray) -> Unit,
    onUpdateForm: ((SignApkFormState) -> SignApkFormState) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val keystorePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val (name, bytes) = withContext(Dispatchers.IO) {
                    var resolvedName = "custom.jks"
                    try {
                        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            if (cursor.moveToFirst() && nameIndex != -1) {
                                resolvedName = cursor.getString(nameIndex) ?: "custom.jks"
                            }
                        }
                    } catch (_: Exception) {}

                    val streamBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: ByteArray(0)
                    Pair(resolvedName, streamBytes)
                }

                if (bytes.isNotEmpty()) {
                    onSetExternalKeystore(name, bytes)
                }
            }
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("card_select_keystore_for_signing"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Key,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "2. Keystore de Firma",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Mode Tabs: Saved vs External
            TabRow(
                selectedTabIndex = if (formState.keystoreSourceMode == KeystoreSourceMode.SAVED_KEYSTORE) 0 else 1,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = formState.keystoreSourceMode == KeystoreSourceMode.SAVED_KEYSTORE,
                    onClick = {
                        onUpdateForm { it.copy(keystoreSourceMode = KeystoreSourceMode.SAVED_KEYSTORE) }
                    },
                    text = { Text("Mis Guardados (${savedKeystores.size})") },
                    modifier = Modifier.testTag("tab_source_saved")
                )
                Tab(
                    selected = formState.keystoreSourceMode == KeystoreSourceMode.EXTERNAL_FILE,
                    onClick = {
                        onUpdateForm { it.copy(keystoreSourceMode = KeystoreSourceMode.EXTERNAL_FILE) }
                    },
                    text = { Text("Cargar Externo") },
                    modifier = Modifier.testTag("tab_source_external")
                )
            }

            if (formState.keystoreSourceMode == KeystoreSourceMode.SAVED_KEYSTORE) {
                if (savedKeystores.isEmpty()) {
                    Text(
                        text = "No tienes ningún Keystore guardado en la bóveda. Puedes generar uno en la pestaña 'Generar' o cargar un archivo externo aquí.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Text(
                        text = "Selecciona el Keystore a utilizar:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        savedKeystores.forEach { ks ->
                            val isSelected = formState.selectedSavedKeystore?.id == ks.id
                            val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            val cardBg = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(if (isSelected) 2.dp else 1.dp, borderColor, RoundedCornerShape(12.dp))
                                    .clickable { onSelectSavedKeystore(ks) }
                                    .testTag("keystore_select_item_${ks.id}"),
                                colors = CardDefaults.cardColors(containerColor = cardBg)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = ks.fileName,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Alias: ${ks.alias} • ${ks.algorithm}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "SHA-256: ${ks.sha256Fingerprint.take(23)}...",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontFamily = FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "Seleccionado",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Keystore Password field
                    OutlinedTextField(
                        value = formState.keystorePassword,
                        onValueChange = { pwd -> onUpdateForm { it.copy(keystorePassword = pwd) } },
                        label = { Text("Contraseña del Keystore") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_sign_saved_password"),
                        visualTransformation = if (formState.isKeystorePasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { onUpdateForm { it.copy(isKeystorePasswordVisible = !it.isKeystorePasswordVisible) } }) {
                                Icon(
                                    imageVector = if (formState.isKeystorePasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        singleLine = true
                    )
                }
            } else {
                // External Keystore Mode
                if (formState.externalKeystoreBytes == null) {
                    Button(
                        onClick = { keystorePickerLauncher.launch("*/*") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_pick_external_keystore")
                    ) {
                        Icon(imageVector = Icons.Default.FileOpen, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Seleccionar archivo .jks / .keystore")
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = formState.externalKeystoreFileName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "Archivo cargado en memoria",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Button(onClick = { keystorePickerLauncher.launch("*/*") }) {
                                Text("Cambiar")
                            }
                        }
                    }

                    OutlinedTextField(
                        value = formState.keystorePassword,
                        onValueChange = { pwd -> onUpdateForm { it.copy(keystorePassword = pwd) } },
                        label = { Text("Contraseña del Keystore") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_sign_external_store_pwd"),
                        visualTransformation = if (formState.isKeystorePasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { onUpdateForm { it.copy(isKeystorePasswordVisible = !it.isKeystorePasswordVisible) } }) {
                                Icon(
                                    imageVector = if (formState.isKeystorePasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = formState.alias,
                        onValueChange = { a -> onUpdateForm { it.copy(alias = a) } },
                        label = { Text("Alias de la Clave") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_sign_external_alias"),
                        singleLine = true
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = formState.useSamePassword,
                            onCheckedChange = { chk -> onUpdateForm { it.copy(useSamePassword = chk) } }
                        )
                        Text(
                            text = "Usar la misma contraseña para la clave",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    AnimatedVisibility(visible = !formState.useSamePassword) {
                        OutlinedTextField(
                            value = formState.keyPassword,
                            onValueChange = { pwd -> onUpdateForm { it.copy(keyPassword = pwd) } },
                            label = { Text("Contraseña de la Clave") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_sign_external_key_pwd"),
                            visualTransformation = if (formState.isKeyPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { onUpdateForm { it.copy(isKeyPasswordVisible = !it.isKeyPasswordVisible) } }) {
                                    Icon(
                                        imageVector = if (formState.isKeyPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null
                                    )
                                }
                            },
                            singleLine = true
                        )
                    }
                }
            }
        }
    }
}
