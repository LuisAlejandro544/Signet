package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.KeyAlgorithm
import com.example.ui.FormState
import com.example.ui.GenerationUiState
import com.example.ui.KeystoreViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GenerateScreen(
    viewModel: KeystoreViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val formState by viewModel.formState.collectAsState()
    val generationState by viewModel.generationState.collectAsState()

    val isGenerating = generationState is GenerationUiState.Generating

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Hero / Header Card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Column {
                    Text(
                        text = "Generador de Keystores",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Crea claves criptográficas estándar (.jks / .keystore) para firmar tus APKs de Android y distribuirlas en Uptodown, web o tiendas.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // Error message if any
        if (generationState is GenerationUiState.Error) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = (generationState as GenerationUiState.Error).message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        // Quick Presets
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Plantillas Rápidas",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = formState.alias == "key0" && formState.algorithm == KeyAlgorithm.RSA_2048,
                    onClick = { viewModel.applyPreset("release") },
                    label = { Text("Release Estándar") },
                    leadingIcon = {
                        Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                )
                FilterChip(
                    selected = formState.alias == "upload",
                    onClick = { viewModel.applyPreset("upload") },
                    label = { Text("Upload Key") }
                )
                FilterChip(
                    selected = formState.algorithm == KeyAlgorithm.RSA_4096,
                    onClick = { viewModel.applyPreset("rsa4096") },
                    label = { Text("RSA 4096 Bits") }
                )
            }
        }

        // Section 1: Archivo y Credenciales
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
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

                // File Extension Selection
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Formato de Extensión",
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
                            onClick = { viewModel.setFileExtension("jks") },
                            label = { Text(".jks (Estándar Android)") },
                            leadingIcon = if (formState.fileExtension == "jks") {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            modifier = Modifier.weight(1f).testTag("chip_ext_jks")
                        )
                        FilterChip(
                            selected = formState.fileExtension == "keystore",
                            onClick = { viewModel.setFileExtension("keystore") },
                            label = { Text(".keystore (Tradicional)") },
                            leadingIcon = if (formState.fileExtension == "keystore") {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            modifier = Modifier.weight(1f).testTag("chip_ext_keystore")
                        )
                    }
                }

                // File Name
                OutlinedTextField(
                    value = formState.fileName,
                    onValueChange = { viewModel.updateForm { s -> s.copy(fileName = it) } },
                    label = { Text("Nombre del archivo (Resultado: ${formState.fullFileName})") },
                    placeholder = { Text("ej: release-key") },
                    trailingIcon = {
                        Text(
                            text = ".${formState.fileExtension}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 12.dp)
                        )
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
                    onValueChange = {
                        val newPwd = it
                        viewModel.updateForm { s ->
                            s.copy(
                                storePassword = newPwd,
                                confirmPassword = if (s.storePassword == s.confirmPassword) newPwd else s.confirmPassword
                            )
                        }
                    },
                    label = { Text("Contraseña del Keystore") },
                    visualTransformation = if (formState.isStorePasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = {
                            viewModel.updateForm { s -> s.copy(isStorePasswordVisible = !s.isStorePasswordVisible) }
                        }) {
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

                // Confirm Password
                OutlinedTextField(
                    value = formState.confirmPassword,
                    onValueChange = { viewModel.updateForm { s -> s.copy(confirmPassword = it) } },
                    label = { Text("Confirmar Contraseña") },
                    visualTransformation = if (formState.isStorePasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("confirm_password_input"),
                    shape = RoundedCornerShape(10.dp)
                )

                // Generate Password Button
                OutlinedButton(
                    onClick = { viewModel.generateRandomPassword() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("generate_password_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Generar Contraseña Segura")
                }

                // Key Alias
                OutlinedTextField(
                    value = formState.alias,
                    onValueChange = { viewModel.updateForm { s -> s.copy(alias = it) } },
                    label = { Text("Alias de la clave (Key Alias)") },
                    placeholder = { Text("ej: key0 o upload") },
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
                    Text(
                        text = "Usar misma contraseña para la clave",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Switch(
                        checked = formState.useSamePassword,
                        onCheckedChange = { checked ->
                            viewModel.updateForm { s -> s.copy(useSamePassword = checked) }
                        }
                    )
                }

                if (!formState.useSamePassword) {
                    OutlinedTextField(
                        value = formState.keyPassword,
                        onValueChange = { viewModel.updateForm { s -> s.copy(keyPassword = it) } },
                        label = { Text("Contraseña específica del alias") },
                        visualTransformation = if (formState.isKeyPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = {
                                viewModel.updateForm { s -> s.copy(isKeyPasswordVisible = !s.isKeyPasswordVisible) }
                            }) {
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

        // Section 2: Algoritmo y Validez Deslizable / Personalizada
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
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
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "2. Criptografía y Validez",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Interactive Slider for Certificate Validity
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Validez del Certificado",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "~${formState.validityYears * 365} días de vigencia",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Badge / Interactive number input
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "${formState.validityYears} años",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    // Slidable Control
                    Slider(
                        value = formState.validityYears.toFloat(),
                        onValueChange = { newValue ->
                            viewModel.setValidityYears(newValue.roundToInt())
                        },
                        valueRange = 1f..100f,
                        steps = 98,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("validity_slider")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "1 año (Mínimo)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "25 años (Recomendado)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "100 años (Máx)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Quick Chips including customized choices
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(1, 5, 10, 25, 30, 50, 100).forEach { years ->
                            FilterChip(
                                selected = formState.validityYears == years,
                                onClick = { viewModel.setValidityYears(years) },
                                label = {
                                    Text(
                                        when (years) {
                                            1 -> "1 año"
                                            25 -> "25 años ★"
                                            100 -> "100 años"
                                            else -> "$years años"
                                        }
                                    )
                                }
                            )
                        }
                    }

                    // Info box regarding validity
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Para publicar en Uptodown, F-Droid o tiendas oficiales, se exige un mínimo de 25 años para poder firmar actualizaciones sin que el certificado expire.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }

                // Algorithm selector
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Algoritmo de la Clave",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    KeyAlgorithm.values().forEach { alg ->
                        FilterChip(
                            selected = formState.algorithm == alg,
                            onClick = { viewModel.updateForm { s -> s.copy(algorithm = alg) } },
                            label = { Text(alg.displayName) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // Section 3: Certificado X.500 (Distinguished Name)
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "3. Datos del Certificado (X.500)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = {
                        viewModel.updateForm { s -> s.copy(isAdvancedDnExpanded = !s.isAdvancedDnExpanded) }
                    }) {
                        Icon(
                            imageVector = if (formState.isAdvancedDnExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Expandir/Colapsar"
                        )
                    }
                }

                AnimatedVisibility(visible = formState.isAdvancedDnExpanded) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = formState.commonName,
                            onValueChange = { viewModel.updateForm { s -> s.copy(commonName = it) } },
                            label = { Text("Nombre y Apellidos / App (CN)") },
                            placeholder = { Text("ej: Alejandro Camacho o Mi App") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("common_name_input"),
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = formState.organization,
                            onValueChange = { viewModel.updateForm { s -> s.copy(organization = it) } },
                            label = { Text("Organización / Empresa (O)") },
                            placeholder = { Text("ej: Mi Empresa o Developer") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("organization_input"),
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = formState.organizationalUnit,
                            onValueChange = { viewModel.updateForm { s -> s.copy(organizationalUnit = it) } },
                            label = { Text("Unidad Organizativa (OU)") },
                            placeholder = { Text("ej: Mobile Development") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = formState.locality,
                                onValueChange = { viewModel.updateForm { s -> s.copy(locality = it) } },
                                label = { Text("Ciudad (L)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            OutlinedTextField(
                                value = formState.state,
                                onValueChange = { viewModel.updateForm { s -> s.copy(state = it) } },
                                label = { Text("Estado / Prov (ST)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }

                        OutlinedTextField(
                            value = formState.countryCode,
                            onValueChange = {
                                if (it.length <= 2) {
                                    viewModel.updateForm { s -> s.copy(countryCode = it.uppercase()) }
                                }
                            },
                            label = { Text("Código de País (C) - 2 letras") },
                            placeholder = { Text("ej: ES, MX, US, CO, AR") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("country_code_input"),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }
        }

        // Submit Button
        Button(
            onClick = {
                viewModel.generateKeystore(context)
            },
            enabled = !isGenerating,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("generate_keystore_submit_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isGenerating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.5.dp
                )
                Spacer(Modifier.width(12.dp))
                Text("Generando Claves y Certificado...", fontWeight = FontWeight.Bold)
            } else {
                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text("Generar Archivo Keystore", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}
