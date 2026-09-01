package com.example.ui.screens.inspect

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.crypto.KeystoreGenerator
import com.example.data.model.KeystoreDetails
import com.example.ui.KeystoreViewModel
import com.example.ui.screens.inspect.apk.ApkErrorCard
import com.example.ui.screens.inspect.apk.ApkFileSelectorCard
import com.example.ui.screens.inspect.apk.ApkKeystoreSelectorCard
import com.example.ui.screens.inspect.apk.ApkMatchResultCard
import com.example.ui.screens.inspect.apk.ApkMatcherHeaderBanner
import com.example.ui.screens.inspect.apk.ApkMetadataDetailsCard
import com.example.ui.state.ApkMatcherUiState

/**
 * Orquestador principal de la vista forense de coincidencia APK vs Keystore.
 * Delega sus tarjetas de interfaz en componentes modulares en [com.example.ui.screens.inspect.apk].
 */
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

    // Option B: External Keystore File
    var selectedExternalKeystoreUri by remember { mutableStateOf<Uri?>(null) }
    var selectedExternalKeystoreName by remember { mutableStateOf<String?>(null) }
    var externalKeystorePassword by remember { mutableStateOf("") }

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
        ApkMatcherHeaderBanner()

        // Step 1: Choose APK File
        ApkFileSelectorCard(
            selectedApkFileName = selectedApkFileName,
            onPickApk = { apkPickerLauncher.launch("application/vnd.android.package-archive") }
        )

        // Step 2: Choose Keystore & Validate
        ApkKeystoreSelectorCard(
            targetMode = targetMode,
            onTargetModeChanged = { targetMode = it },
            savedKeystores = savedKeystores,
            selectedSavedKeystore = selectedSavedKeystore,
            onSavedKeystoreSelected = {
                selectedSavedKeystore = it
                viewModel.resetApkMatcher()
            },
            selectedExternalKeystoreName = selectedExternalKeystoreName,
            onPickExternalKeystore = { externalKeystorePickerLauncher.launch("*/*") },
            externalKeystorePassword = externalKeystorePassword,
            onExternalPasswordChanged = { externalKeystorePassword = it },
            isLoading = apkState is ApkMatcherUiState.Loading,
            isValidateEnabled = selectedApkUri != null &&
                    ((targetMode == 0 && selectedSavedKeystore != null) ||
                            (targetMode == 1 && selectedExternalKeystoreUri != null && externalKeystorePassword.isNotBlank())),
            onValidateClick = {
                val apkUri = selectedApkUri ?: return@ApkKeystoreSelectorCard
                val apkBytes = try {
                    context.contentResolver.openInputStream(apkUri)?.use { it.readBytes() }
                } catch (e: Exception) {
                    Toast.makeText(context, "Error al leer APK: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    null
                }

                if (apkBytes == null || apkBytes.isEmpty()) {
                    Toast.makeText(context, "El archivo APK está vacío o no se puede leer.", Toast.LENGTH_SHORT).show()
                    return@ApkKeystoreSelectorCard
                }

                if (targetMode == 0) {
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
                    val extUri = selectedExternalKeystoreUri
                    if (extUri == null || externalKeystorePassword.isBlank()) {
                        Toast.makeText(context, "Selecciona el archivo keystore e ingresa su contraseña.", Toast.LENGTH_SHORT).show()
                        return@ApkKeystoreSelectorCard
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
            }
        )

        // Results Rendering
        when (val state = apkState) {
            is ApkMatcherUiState.Error -> {
                ApkErrorCard(message = state.message)
            }
            is ApkMatcherUiState.Success -> {
                val matchResult = state.matchResult
                val apkInfo = state.apkInfo

                if (matchResult != null) {
                    ApkMatchResultCard(context = context, result = matchResult)
                }

                ApkMetadataDetailsCard(context = context, apkInfo = apkInfo)
            }
            else -> {}
        }
    }
}
