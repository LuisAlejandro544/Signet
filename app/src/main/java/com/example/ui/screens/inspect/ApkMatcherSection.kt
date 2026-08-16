package com.example.ui.screens.inspect

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crypto.KeystoreGenerator
import com.example.data.model.ApkMatchResult
import com.example.data.model.KeystoreDetails
import com.example.ui.KeystoreViewModel
import com.example.ui.state.ApkMatcherUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApkMatcherSection(
    viewModel: KeystoreViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val savedKeystores by viewModel.savedKeystores.collectAsState()
    val apkState by viewModel.apkMatcherState.collectAsState()

    var selectedApkUri by remember { mutableStateOf<Uri?>(null) }
    var selectedApkFileName by remember { mutableStateOf<String?>(null) }

    // Target Keystore Selection Mode: 0 = Saved Keystores, 1 = External File
    var targetMode by remember { mutableStateOf(0) }

    // Option A: Saved Keystore
    var selectedSavedKeystore by remember { mutableStateOf<KeystoreDetails?>(null) }
    var isSavedDropdownOpen by remember { mutableStateOf(false) }

    // Option B: External Keystore File
    var selectedExternalKeystoreUri by remember { mutableStateOf<Uri?>(null) }
    var selectedExternalKeystoreName by remember { mutableStateOf<String?>(null) }
    var externalKeystorePassword by remember { mutableStateOf("") }
    var isExternalPasswordVisible by remember { mutableStateOf(false) }

    // Preselect first saved keystore if available and none selected yet
    if (selectedSavedKeystore == null && savedKeystores.isNotEmpty()) {
        selectedSavedKeystore = savedKeystores.first()
    }

    val apkPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedApkUri = uri
            selectedApkFileName = uri.lastPathSegment?.substringAfterLast("/") ?: "app.apk"
            viewModel.resetApkMatcher()
        }
    }

    val externalKeystorePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedExternalKeystoreUri = uri
            selectedExternalKeystoreName = uri.lastPathSegment?.substringAfterLast("/") ?: "custom.jks"
            viewModel.resetApkMatcher()
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Explanatory Banner
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Validador de Coincidencia (APK Matcher)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Verifica si un APK existente fue firmado con tu Keystore para evitar errores de actualización en Android.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                        lineHeight = 15.sp
                    )
                }
            }
        }

        // Selection 1: Choose APK File
        OutlinedCard(
            shape = RoundedCornerShape(16.dp),
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
                        Icons.Default.Android,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Paso 1: Seleccionar archivo APK",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = { apkPickerLauncher.launch("application/vnd.android.package-archive") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("choose_apk_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (selectedApkFileName == null) {
                            "Elegir archivo (.apk)"
                        } else {
                            "APK: $selectedApkFileName"
                        },
                        maxLines = 1
                    )
                }
            }
        }

        // Selection 2: Choose Keystore to compare against
        OutlinedCard(
            shape = RoundedCornerShape(16.dp),
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
                        onClick = { targetMode = 0 },
                        label = { Text("Guardados en Signet") }
                    )
                    FilterChip(
                        selected = targetMode == 1,
                        onClick = { targetMode = 1 },
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
                                            selectedSavedKeystore = ks
                                            isSavedDropdownOpen = false
                                            viewModel.resetApkMatcher()
                                        }
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Option B: External File + Password
                    OutlinedButton(
                        onClick = { externalKeystorePickerLauncher.launch("*/*") },
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
                        onValueChange = { externalKeystorePassword = it },
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
                    onClick = {
                        val apkUri = selectedApkUri
                        if (apkUri == null) {
                            Toast.makeText(context, "Por favor selecciona primero un archivo APK.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        val apkBytes = try {
                            context.contentResolver.openInputStream(apkUri)?.use { it.readBytes() }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error al leer APK: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            null
                        }

                        if (apkBytes == null || apkBytes.isEmpty()) {
                            Toast.makeText(context, "El archivo APK está vacío o no se puede leer.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        if (targetMode == 0) {
                            // Match against saved Keystore
                            val targetKs = selectedSavedKeystore
                            if (targetKs != null) {
                                viewModel.matchApkWithKeystore(
                                    context = context,
                                    apkBytes = apkBytes,
                                    apkFileName = selectedApkFileName ?: "app.apk",
                                    targetKeystore = targetKs
                                )
                            }
                        } else {
                            // Match against external Keystore
                            val extUri = selectedExternalKeystoreUri
                            if (extUri == null || externalKeystorePassword.isBlank()) {
                                Toast.makeText(context, "Selecciona el archivo keystore e ingresa su contraseña.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            val ksBytes = try {
                                context.contentResolver.openInputStream(extUri)?.use { it.readBytes() }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error al leer Keystore: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                null
                            }

                            if (ksBytes != null && ksBytes.isNotEmpty()) {
                                try {
                                    val inspectedList = KeystoreGenerator.inspectKeystore(ksBytes, externalKeystorePassword)
                                    val firstItem = inspectedList.firstOrNull()
                                    if (firstItem != null) {
                                        viewModel.matchApkWithKeystore(
                                            context = context,
                                            apkBytes = apkBytes,
                                            apkFileName = selectedApkFileName ?: "app.apk",
                                            targetKeystore = firstItem
                                        )
                                    } else {
                                        Toast.makeText(context, "No se encontraron claves en el keystore.", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Contraseña incorrecta o keystore inválido: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },
                    enabled = selectedApkUri != null &&
                            ((targetMode == 0 && selectedSavedKeystore != null) ||
                                    (targetMode == 1 && selectedExternalKeystoreUri != null && externalKeystorePassword.isNotBlank())) &&
                            apkState !is ApkMatcherUiState.Loading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("validate_apk_match_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (apkState is ApkMatcherUiState.Loading) {
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

        // Result Rendering
        when (val state = apkState) {
            is ApkMatcherUiState.Error -> {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
            is ApkMatcherUiState.Success -> {
                val matchResult = state.matchResult
                val apkInfo = state.apkInfo

                if (matchResult != null) {
                    MatchResultCard(context = context, result = matchResult)
                }

                // Technical APK Metadata & Signer Details Card
                OutlinedCard(
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Metadatos del Paquete APK",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (!apkInfo.packageName.isNullOrBlank()) {
                            DetailItem(label = "Package Name", value = apkInfo.packageName, onCopy = { copyToClipboard(context, "Package Name", apkInfo.packageName) })
                        }
                        if (!apkInfo.versionName.isNullOrBlank() || apkInfo.versionCode != null) {
                            DetailItem(label = "Versión", value = "${apkInfo.versionName ?: ""} (Code: ${apkInfo.versionCode ?: "-"})")
                        }

                        DetailItem(
                            label = "Esquemas de Firma Detectados",
                            value = if (apkInfo.signatureSchemesFound.isNotEmpty()) apkInfo.signatureSchemesFound.joinToString(", ") else "Ninguno detectado"
                        )

                        if (apkInfo.certificates.isNotEmpty()) {
                            Text(
                                text = "Certificados X.509 en APK (${apkInfo.certificates.size}):",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 6.dp)
                            )

                            apkInfo.certificates.forEachIndexed { index, cert ->
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "Certificado #${index + 1} (${cert.signatureScheme})",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "SHA-256: ${cert.sha256Fingerprint}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = "SHA-1: ${cert.sha1Fingerprint}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontFamily = FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Propietario: ${cert.subjectDn}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            else -> {}
        }
    }
}

@Composable
private fun MatchResultCard(
    context: Context,
    result: ApkMatchResult
) {
    val isMatch = result.isMatch
    val containerColor = if (isMatch) Color(0xFF1B5E20).copy(alpha = 0.15f) else MaterialTheme.colorScheme.errorContainer
    val borderColor = if (isMatch) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
    val iconColor = if (isMatch) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
    val textColor = if (isMatch) Color(0xFF1B5E20) else MaterialTheme.colorScheme.onErrorContainer

    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, borderColor, RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = if (isMatch) Icons.Default.CheckCircle else Icons.Default.Close,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = if (isMatch) "¡COINCIDENCIA EXACTA CONFIRMADA!" else "FIRMAS INCOMPATIBLES",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }

            Text(
                text = result.reasonMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                lineHeight = 18.sp
            )

            if (isMatch && !result.matchedFingerprintSha256.isNullOrBlank()) {
                Surface(
                    color = Color.White.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Huella SHA-256 Validada",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                            Text(
                                text = result.matchedFingerprintSha256,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = Color.Black
                            )
                        }
                        IconButton(onClick = {
                            copyToClipboard(context, "SHA-256 Validado", result.matchedFingerprintSha256)
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copiar", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailItem(
    label: String,
    value: String,
    onCopy: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
        if (onCopy != null) {
            IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copiar", modifier = Modifier.size(16.dp))
            }
        }
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard?.setPrimaryClip(clip)
    Toast.makeText(context, "$label copiado al portapapeles", Toast.LENGTH_SHORT).show()
}
