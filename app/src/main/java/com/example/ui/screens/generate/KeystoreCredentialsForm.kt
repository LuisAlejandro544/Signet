package com.example.ui.screens.generate

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crypto.PasswordGenerator
import com.example.ui.FormState

@Composable
fun KeystoreCredentialsForm(
    formState: FormState,
    onSetFileExtension: (String) -> Unit,
    onFileNameChange: (String) -> Unit,
    onStorePasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onToggleStorePasswordVisibility: () -> Unit,
    onGenerateRandomPassword: (Int) -> Unit,
    onAliasChange: (String) -> Unit,
    onToggleUseSamePassword: (Boolean) -> Unit,
    onKeyPasswordChange: (String) -> Unit,
    onToggleKeyPasswordVisibility: () -> Unit,
    onRandomizeFileName: () -> Unit = {},
    onRandomizeAlias: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedPasswordLength by remember { mutableIntStateOf(20) }

    // Real-time strength and entropy evaluation
    val entropy = remember(formState.storePassword) {
        PasswordGenerator.calculateEntropy(formState.storePassword)
    }
    val strength = remember(formState.storePassword) {
        PasswordGenerator.evaluateStrength(formState.storePassword)
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
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
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "1. Archivo y Contraseñas",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "* Obligatorio",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // File Extension Selection
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Formato de Extensión *",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = formState.fileExtension == "jks",
                        onClick = { onSetFileExtension("jks") },
                        label = { Text(".jks (Android)") },
                        leadingIcon = if (formState.fileExtension == "jks") {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        modifier = Modifier.weight(1f).testTag("chip_ext_jks")
                    )
                    FilterChip(
                        selected = formState.fileExtension == "keystore",
                        onClick = { onSetFileExtension("keystore") },
                        label = { Text(".keystore (Tradicional)") },
                        leadingIcon = if (formState.fileExtension == "keystore") {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        modifier = Modifier.weight(1f).testTag("chip_ext_keystore")
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = formState.fileExtension == "p12",
                        onClick = { onSetFileExtension("p12") },
                        label = { Text(".p12 (Multiplataforma)") },
                        leadingIcon = if (formState.fileExtension == "p12") {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        modifier = Modifier.weight(1f).testTag("chip_ext_p12")
                    )
                    FilterChip(
                        selected = formState.fileExtension == "pfx",
                        onClick = { onSetFileExtension("pfx") },
                        label = { Text(".pfx (Windows / PC)") },
                        leadingIcon = if (formState.fileExtension == "pfx") {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        modifier = Modifier.weight(1f).testTag("chip_ext_pfx")
                    )
                }
            }

            // File Name
            OutlinedTextField(
                value = formState.fileName,
                onValueChange = onFileNameChange,
                label = { Text("Nombre del archivo * (${formState.fullFileName})") },
                placeholder = { Text("ej: release-key") },
                trailingIcon = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        IconButton(
                            onClick = onRandomizeFileName,
                            modifier = Modifier.size(36.dp).testTag("random_filename_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Casino,
                                contentDescription = "Generar nombre de archivo aleatorio",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = ".${formState.fileExtension}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("file_name_input"),
                shape = RoundedCornerShape(10.dp)
            )

            // Keystore Password
            OutlinedTextField(
                value = formState.storePassword,
                onValueChange = onStorePasswordChange,
                label = { Text("Contraseña del Keystore * (mínimo 6 caracteres)") },
                visualTransformation = if (formState.isStorePasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = onToggleStorePasswordVisibility) {
                        Icon(
                            imageVector = if (formState.isStorePasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Mostrar contraseña"
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("keystore_password_input"),
                shape = RoundedCornerShape(10.dp)
            )

            // Password strength & entropy bar if password is not empty
            if (formState.storePassword.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val progress = (entropy / 100.0).coerceIn(0.0, 1.0).toFloat()
                    val color = when (strength) {
                        PasswordGenerator.PasswordStrength.ULTRA -> Color(0xFF10B981) // Emerald
                        PasswordGenerator.PasswordStrength.STRONG -> Color(0xFF3B82F6) // Blue
                        PasswordGenerator.PasswordStrength.MEDIUM -> Color(0xFFF59E0B) // Amber
                        PasswordGenerator.PasswordStrength.WEAK -> Color(0xFFEF4444) // Red
                    }

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = color,
                        trackColor = MaterialTheme.colorScheme.outlineVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
                            Text(
                                text = "Seguridad: ${strength.label}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = color
                            )
                        }
                        Text(
                            text = "${entropy.toInt()} bits de entropía",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Confirm Password
            OutlinedTextField(
                value = formState.confirmPassword,
                onValueChange = onConfirmPasswordChange,
                label = { Text("Confirmar Contraseña *") },
                visualTransformation = if (formState.isStorePasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("confirm_password_input"),
                shape = RoundedCornerShape(10.dp)
            )

            // Ultra-Secure Password Generator Section
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Generador Criptográfico (CSPRNG)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "A-Z, a-z, 0-9, símbolos",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Length selector chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(16, 20, 24, 32).forEach { length ->
                            FilterChip(
                                selected = selectedPasswordLength == length,
                                onClick = { selectedPasswordLength = length },
                                label = { Text("${length}c", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }

                    Button(
                        onClick = { onGenerateRandomPassword(selectedPasswordLength) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("generate_password_button"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Generar Contraseña Ultra Segura ($selectedPasswordLength caracteres)")
                    }
                }
            }

            // Key Alias
            OutlinedTextField(
                value = formState.alias,
                onValueChange = onAliasChange,
                label = { Text("Alias de la clave (Key Alias) *") },
                placeholder = { Text("ej: key0 o upload") },
                trailingIcon = {
                    IconButton(
                        onClick = onRandomizeAlias,
                        modifier = Modifier.size(36.dp).testTag("random_alias_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Casino,
                            contentDescription = "Generar alias aleatorio",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("alias_input"),
                shape = RoundedCornerShape(10.dp)
            )

            // Same password toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Usar misma contraseña para la clave",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Recomendado por Google y Android Gradle Plugin",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = formState.useSamePassword,
                    onCheckedChange = onToggleUseSamePassword
                )
            }

            if (!formState.useSamePassword) {
                OutlinedTextField(
                    value = formState.keyPassword,
                    onValueChange = onKeyPasswordChange,
                    label = { Text("Contraseña específica del alias *") },
                    visualTransformation = if (formState.isKeyPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = onToggleKeyPasswordVisibility) {
                            Icon(
                                imageVector = if (formState.isKeyPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Mostrar contraseña"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }
    }
}
