package com.example.ui.screens.inspect.apk

import android.net.Uri
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.model.KeystoreDetails

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApkKeystoreSelectorCard(
    targetMode: Int,
    onTargetModeChanged: (Int) -> Unit,
    savedKeystores: List<KeystoreDetails>,
    selectedSavedKeystore: KeystoreDetails?,
    onSavedKeystoreSelected: (KeystoreDetails) -> Unit,
    selectedExternalKeystoreName: String?,
    onPickExternalKeystore: () -> Unit,
    externalKeystorePassword: String,
    onExternalPasswordChanged: (String) -> Unit,
    isLoading: Boolean,
    isValidateEnabled: Boolean,
    onValidateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isSavedDropdownOpen by remember { mutableStateOf(false) }
    var isExternalPasswordVisible by remember { mutableStateOf(false) }

    OutlinedCard(
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth()
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
                    Icons.Default.Key,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Paso 2: Elegir Keystore de Destino",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            // Selector Mode Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = targetMode == 0,
                    onClick = { onTargetModeChanged(0) },
                    label = { Text("Guardados en Signet") }
                )
                FilterChip(
                    selected = targetMode == 1,
                    onClick = { onTargetModeChanged(1) },
                    label = { Text("Archivo Externo (.jks)") }
                )
            }

            if (targetMode == 0) {
                // Option A: Dropdown from Saved Keystores
                if (savedKeystores.isEmpty()) {
                    Text(
                        text = "No tienes keystores guardados en Signet. Genera uno nuevo o usa la opción 'Archivo Externo'.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedCard(
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isSavedDropdownOpen = true }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = selectedSavedKeystore?.fileName ?: "Selecciona un Keystore",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Alias: ${selectedSavedKeystore?.alias ?: "-"} | ${selectedSavedKeystore?.algorithm ?: "-"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Desplegar")
                            }
                        }

                        DropdownMenu(
                            expanded = isSavedDropdownOpen,
                            onDismissRequest = { isSavedDropdownOpen = false }
                        ) {
                            savedKeystores.forEach { ks ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(ks.fileName, fontWeight = FontWeight.SemiBold)
                                            Text("Alias: ${ks.alias} • ${ks.algorithm}", style = MaterialTheme.typography.bodySmall)
                                        }
                                    },
                                    onClick = {
                                        onSavedKeystoreSelected(ks)
                                        isSavedDropdownOpen = false
                                    }
                                )
                            }
                        }
                    }
                }
            } else {
                // Option B: External File + Password
                OutlinedButton(
                    onClick = onPickExternalKeystore,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (selectedExternalKeystoreName == null) "Elegir archivo .jks / .keystore" else "Keystore: $selectedExternalKeystoreName",
                        maxLines = 1
                    )
                }

                OutlinedTextField(
                    value = externalKeystorePassword,
                    onValueChange = onExternalPasswordChanged,
                    label = { Text("Contraseña del Keystore") },
                    visualTransformation = if (isExternalPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = { isExternalPasswordVisible = !isExternalPasswordVisible }) {
                            Icon(
                                imageVector = if (isExternalPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Ver contraseña"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }

            // Action Button: Compare & Match
            Button(
                onClick = onValidateClick,
                enabled = isValidateEnabled && !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("validate_apk_match_button"),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("Analizando firmas del APK...")
                } else {
                    Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Validar Coincidencia APK vs Keystore")
                }
            }
        }
    }
}
