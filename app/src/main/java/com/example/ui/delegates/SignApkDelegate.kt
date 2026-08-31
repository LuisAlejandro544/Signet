package com.example.ui.delegates

import android.content.Context
import com.example.crypto.ApkMatcher
import com.example.crypto.Base64Compat
import com.example.crypto.KeystoreEncryptionManager
import com.example.crypto.signer.ApkSigner
import com.example.data.model.ApkSigningOptions
import com.example.data.model.KeystoreDetails
import com.example.ui.state.ApkSigningUiState
import com.example.ui.state.KeystoreSourceMode
import com.example.ui.state.SignApkFormState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * Delegate managing APK signing forms, background APK signature inspection, and signing execution.
 */
class SignApkDelegate {

    private val _signApkFormState = MutableStateFlow(SignApkFormState())
    val signApkFormState: StateFlow<SignApkFormState> = _signApkFormState.asStateFlow()

    private val _apkSigningState = MutableStateFlow<ApkSigningUiState>(ApkSigningUiState.Idle)
    val apkSigningState: StateFlow<ApkSigningUiState> = _apkSigningState.asStateFlow()

    fun updateSignApkForm(transform: (SignApkFormState) -> SignApkFormState) {
        _signApkFormState.value = transform(_signApkFormState.value)
    }

    fun selectApkForSigning(
        name: String,
        size: Long,
        bytes: ByteArray,
        context: Context?,
        scope: CoroutineScope
    ) {
        val baseName = name.removeSuffix(".apk").removeSuffix(".APK")
        val defaultOutput = if (baseName.isNotBlank()) "$baseName-signed.apk" else "app-signed.apk"

        _signApkFormState.value = _signApkFormState.value.copy(
            apkFileName = name,
            apkFileSizeBytes = size,
            apkBytes = bytes,
            outputFileName = defaultOutput
        )

        // Asynchronously analyze APK to detect package and existing signatures
        scope.launch(Dispatchers.Default) {
            try {
                val info = ApkMatcher.analyzeApk(context, bytes, name)
                _signApkFormState.value = _signApkFormState.value.copy(
                    detectedPackageName = info.packageName,
                    detectedVersionName = info.versionName,
                    hasExistingSignatures = info.certificates.isNotEmpty(),
                    existingSchemes = info.signatureSchemesFound
                )
            } catch (_: Exception) {}
        }
    }

    fun clearSelectedApkForSigning() {
        _signApkFormState.value = _signApkFormState.value.copy(
            apkFileName = "",
            apkFileSizeBytes = 0L,
            apkBytes = null,
            detectedPackageName = null,
            detectedVersionName = null,
            hasExistingSignatures = false,
            existingSchemes = emptyList()
        )
    }

    fun selectSavedKeystoreForSigning(keystore: KeystoreDetails) {
        val decryptedStorePassword = KeystoreEncryptionManager.decrypt(keystore.storePassword)
        val decryptedKeyPassword = KeystoreEncryptionManager.decrypt(keystore.keyPassword).ifBlank { decryptedStorePassword }

        _signApkFormState.value = _signApkFormState.value.copy(
            keystoreSourceMode = KeystoreSourceMode.SAVED_KEYSTORE,
            selectedSavedKeystore = keystore,
            alias = keystore.alias,
            keystorePassword = decryptedStorePassword,
            keyPassword = decryptedKeyPassword,
            useSamePassword = true
        )
    }

    fun setExternalKeystoreForSigning(name: String, bytes: ByteArray) {
        _signApkFormState.value = _signApkFormState.value.copy(
            keystoreSourceMode = KeystoreSourceMode.EXTERNAL_FILE,
            externalKeystoreFileName = name,
            externalKeystoreBytes = bytes,
            selectedSavedKeystore = null
        )
    }

    fun resetSigningState() {
        _apkSigningState.value = ApkSigningUiState.Idle
    }

    fun signApk(
        outputDir: File,
        scope: CoroutineScope
    ) {
        val form = _signApkFormState.value
        val apkBytes = form.apkBytes

        if (apkBytes == null || apkBytes.isEmpty()) {
            _apkSigningState.value = ApkSigningUiState.Error("Por favor selecciona un archivo APK válido.")
            return
        }

        if (!form.signV1 && !form.signV2 && !form.signV3) {
            _apkSigningState.value = ApkSigningUiState.Error("Debes habilitar al menos un esquema de firma (Esquema v1, Esquema v2 o Esquema v3).")
            return
        }

        // Resolve Keystore bytes and credentials
        val keystoreBytes: ByteArray
        val alias: String
        val storePassword: String
        val keyPassword: String

        if (form.keystoreSourceMode == KeystoreSourceMode.SAVED_KEYSTORE) {
            val ks = form.selectedSavedKeystore
            if (ks == null) {
                _apkSigningState.value = ApkSigningUiState.Error("Por favor selecciona un Keystore de tu bóveda.")
                return
            }

            alias = ks.alias
            storePassword = if (form.keystorePassword.isNotBlank()) {
                form.keystorePassword
            } else {
                KeystoreEncryptionManager.decrypt(ks.storePassword)
            }
            keyPassword = if (form.useSamePassword) {
                storePassword
            } else if (form.keyPassword.isNotBlank()) {
                form.keyPassword
            } else {
                KeystoreEncryptionManager.decrypt(ks.keyPassword).ifBlank { storePassword }
            }

            // Read keystore bytes from disk or base64
            val localFile = File(ks.filePath)
            keystoreBytes = if (localFile.exists()) {
                localFile.readBytes()
            } else if (ks.base64Content.isNotBlank()) {
                Base64Compat.decode(ks.base64Content)
            } else {
                _apkSigningState.value = ApkSigningUiState.Error("No se pudo localizar el archivo binario del Keystore.")
                return
            }
        } else {
            // External Keystore
            val externalBytes = form.externalKeystoreBytes
            if (externalBytes == null || externalBytes.isEmpty()) {
                _apkSigningState.value = ApkSigningUiState.Error("Por favor carga un archivo Keystore externo (.jks, .keystore, .p12).")
                return
            }
            if (form.alias.isBlank()) {
                _apkSigningState.value = ApkSigningUiState.Error("Por favor ingresa el alias de la clave a firmar.")
                return
            }
            if (form.keystorePassword.isBlank()) {
                _apkSigningState.value = ApkSigningUiState.Error("Por favor ingresa la contraseña del Keystore.")
                return
            }

            keystoreBytes = externalBytes
            alias = form.alias
            storePassword = form.keystorePassword
            keyPassword = if (form.useSamePassword) form.keystorePassword else form.keyPassword
        }

        val options = ApkSigningOptions(
            signV1 = form.signV1,
            signV2 = form.signV2,
            signV3 = form.signV3,
            zipalign = form.zipalign,
            outputFileName = if (form.outputFileName.isBlank()) "app-signed.apk" else form.outputFileName
        )

        _apkSigningState.value = ApkSigningUiState.Signing(
            stepMessage = "Iniciando proceso de firma...",
            progress = 0.05f
        )

        scope.launch(Dispatchers.Default) {
            val result = ApkSigner.signApk(
                apkBytes = apkBytes,
                keystoreBytes = keystoreBytes,
                storePassword = storePassword,
                alias = alias,
                keyPassword = keyPassword,
                options = options,
                outputDirectory = outputDir,
                onProgress = { step, progress ->
                    _apkSigningState.value = ApkSigningUiState.Signing(step, progress)
                }
            )

            if (result.isSuccess) {
                _apkSigningState.value = ApkSigningUiState.Success(result)
            } else {
                _apkSigningState.value = ApkSigningUiState.Error(
                    result.errorMessage ?: "Ocurrió un error inesperado al firmar el archivo APK."
                )
            }
        }
    }

    fun installSignedApk(context: Context, apkFile: File) {
        try {
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            _apkSigningState.value = ApkSigningUiState.Error(
                "No se pudo iniciar el instalador de Android: ${e.localizedMessage}"
            )
        }
    }
}
