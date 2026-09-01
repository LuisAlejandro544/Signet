package com.example.ui.screens.generate

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.FormState

@Composable
fun KeystoreDnFields(
    formState: FormState,
    onToggleExpand: () -> Unit,
    onCommonNameChange: (String) -> Unit,
    onOrganizationChange: (String) -> Unit,
    onOrganizationalUnitChange: (String) -> Unit,
    onLocalityChange: (String) -> Unit,
    onStateChange: (String) -> Unit,
    onCountryCodeChange: (String) -> Unit,
    onRandomizeCommonName: () -> Unit = {},
    onRandomizeOrganization: () -> Unit = {},
    onRandomizeOrganizationalUnit: () -> Unit = {},
    onRandomizeLocality: () -> Unit = {},
    onRandomizeState: () -> Unit = {},
    onRandomizeCountryCode: () -> Unit = {},
    onRandomizeAllDnFields: () -> Unit = {},
    modifier: Modifier = Modifier
) {
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
                
                IconButton(onClick = onToggleExpand) {
                    Icon(
                        imageVector = if (formState.isAdvancedDnExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Expandir/Colapsar"
                    )
                }
            }

            AnimatedVisibility(visible = formState.isAdvancedDnExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Educational Banner about Google & Android X.509 standards
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp).padding(top = 2.dp)
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "Requisitos de Identidad (Google / Android X.509)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Google y el ecosistema Android exigen que el certificado contenga al menos un Nombre (CN) u Organización (O) para validar la firma digital. Puedes inventar nombres o generar datos aleatorios para pruebas rápidas y privacidad.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }

                    // Button to randomize all certificate identity fields at once
                    OutlinedButton(
                        onClick = onRandomizeAllDnFields,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("randomize_all_dn_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Autocompletar Identidad Aleatoria",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Common Name (CN) - Mandatory by standard
                    OutlinedTextField(
                        value = formState.commonName,
                        onValueChange = onCommonNameChange,
                        label = { Text("Nombre y Apellidos / App (CN) * Obligatorio") },
                        placeholder = { Text("ej: Alejandro Camacho o Mi App Release") },
                        trailingIcon = {
                            IconButton(
                                onClick = onRandomizeCommonName,
                                modifier = Modifier.testTag("random_cn_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Casino,
                                    contentDescription = "Generar nombre aleatorio",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("common_name_input"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Organization (O) - Recommended/Mandatory by standard
                    OutlinedTextField(
                        value = formState.organization,
                        onValueChange = onOrganizationChange,
                        label = { Text("Organización / Empresa (O) * Obligatorio") },
                        placeholder = { Text("ej: Mi Empresa o Developer Studio") },
                        trailingIcon = {
                            IconButton(
                                onClick = onRandomizeOrganization,
                                modifier = Modifier.testTag("random_org_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Casino,
                                    contentDescription = "Generar organización aleatoria",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("organization_input"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Organizational Unit (OU) - Optional
                    OutlinedTextField(
                        value = formState.organizationalUnit,
                        onValueChange = onOrganizationalUnitChange,
                        label = { Text("Unidad Organizativa (OU) • (Opcional)") },
                        placeholder = { Text("ej: Mobile Development") },
                        trailingIcon = {
                            IconButton(
                                onClick = onRandomizeOrganizationalUnit,
                                modifier = Modifier.testTag("random_ou_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Casino,
                                    contentDescription = "Generar unidad organizativa aleatoria",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("organizational_unit_input"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // City & State - Optional
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = formState.locality,
                            onValueChange = onLocalityChange,
                            label = { Text("Ciudad (L)") },
                            placeholder = { Text("ej: Madrid") },
                            trailingIcon = {
                                IconButton(
                                    onClick = onRandomizeLocality,
                                    modifier = Modifier.testTag("random_locality_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Casino,
                                        contentDescription = "Generar ciudad aleatoria",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("locality_input"),
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = formState.state,
                            onValueChange = onStateChange,
                            label = { Text("Estado / Prov (ST)") },
                            placeholder = { Text("ej: Madrid") },
                            trailingIcon = {
                                IconButton(
                                    onClick = onRandomizeState,
                                    modifier = Modifier.testTag("random_state_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Casino,
                                        contentDescription = "Generar estado aleatorio",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("state_input"),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    // Country Code - Optional (Strict ISO 2 letters if provided)
                    OutlinedTextField(
                        value = formState.countryCode,
                        onValueChange = {
                            if (it.length <= 2) {
                                onCountryCodeChange(it.uppercase())
                            }
                        },
                        label = { Text("Código de País (C) • (Opcional - 2 letras)") },
                        placeholder = { Text("ej: ES, MX, US, CO, AR, CL") },
                        trailingIcon = {
                            IconButton(
                                onClick = onRandomizeCountryCode,
                                modifier = Modifier.testTag("random_country_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Casino,
                                    contentDescription = "Generar país aleatorio",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
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
}
