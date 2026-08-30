package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.crypto.ApkMatcher
import com.example.crypto.KeystoreEncryptionManager
import com.example.crypto.PasswordGenerator
import com.example.crypto.signer.ApkSigner
import com.example.data.KeystoreRepository
import com.example.data.local.AppDatabase
import com.example.data.model.ApkSigningOptions
import com.example.data.model.DistinguishedName
import com.example.data.model.KeyAlgorithm
import com.example.data.model.KeystoreConfig
import com.example.data.model.KeystoreDetails
import com.example.ui.preferences.AppPreferencesManager
import com.example.ui.state.ApkMatcherUiState
import com.example.ui.state.ApkSigningUiState
import com.example.ui.state.FormState
import com.example.ui.state.GenerationUiState
import com.example.ui.state.InspectorUiState
import com.example.ui.state.KeystoreSourceMode
import com.example.ui.state.RestoreUiState
import com.example.ui.state.SignApkFormState
import com.example.ui.theme.ColorPalette
import com.example.ui.theme.ThemeMode
import com.example.ui.theme.ThemeState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

// Typealiases for seamless backward compatibility
typealias FormState = com.example.ui.state.FormState
typealias GenerationUiState = com.example.ui.state.GenerationUiState
typealias RestoreUiState = com.example.ui.state.RestoreUiState
typealias InspectorUiState = com.example.ui.state.InspectorUiState
typealias ApkMatcherUiState = com.example.ui.state.ApkMatcherUiState
typealias ApkSigningUiState = com.example.ui.state.ApkSigningUiState
typealias SignApkFormState = com.example.ui.state.SignApkFormState

class KeystoreViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesManager = AppPreferencesManager(application)

    private val repository: KeystoreRepository = KeystoreRepository(
        AppDatabase.getDatabase(application)
    )

    val savedKeystores: StateFlow<List<KeystoreDetails>> = repository.allKeystores
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 0: Generar, 1: Mis Keystores, 2: Inspeccionar, 3: Configuración
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _formState = MutableStateFlow(FormState())
    val formState: StateFlow<FormState> = _formState.asStateFlow()

    private val _generationState = MutableStateFlow<GenerationUiState>(GenerationUiState.Idle)
    val generationState: StateFlow<GenerationUiState> = _generationState.asStateFlow()

    private val _restoreState = MutableStateFlow<RestoreUiState>(RestoreUiState.Idle)
    val restoreState: StateFlow<RestoreUiState> = _restoreState.asStateFlow()

    private val _inspectorState = MutableStateFlow<InspectorUiState>(InspectorUiState.Idle)
    val inspectorState: StateFlow<InspectorUiState> = _inspectorState.asStateFlow()

    private val _apkMatcherState = MutableStateFlow<ApkMatcherUiState>(ApkMatcherUiState.Idle)
    val apkMatcherState: StateFlow<ApkMatcherUiState> = _apkMatcherState.asStateFlow()

    private val _signApkFormState = MutableStateFlow(SignApkFormState())
    val signApkFormState: StateFlow<SignApkFormState> = _signApkFormState.asStateFlow()

    private val _apkSigningState = MutableStateFlow<ApkSigningUiState>(ApkSigningUiState.Idle)
    val apkSigningState: StateFlow<ApkSigningUiState> = _apkSigningState.asStateFlow()

    private val _selectedKeystoreForDetail = MutableStateFlow<KeystoreDetails?>(null)
    val selectedKeystoreForDetail: StateFlow<KeystoreDetails?> = _selectedKeystoreForDetail.asStateFlow()

    // Theme state backed by AppPreferencesManager
    private val _themeState = MutableStateFlow(preferencesManager.loadThemeState())
    val themeState: StateFlow<ThemeState> = _themeState.asStateFlow()

    // Onboarding and Terms acceptance state
    private val _isOnboardingCompleted = MutableStateFlow(preferencesManager.isOnboardingCompleted())
    val isOnboardingCompleted: StateFlow<Boolean> = _isOnboardingCompleted.asStateFlow()

    fun completeOnboarding() {
        preferencesManager.setOnboardingCompleted(true)
        _isOnboardingCompleted.value = true
    }

    fun resetOnboarding() {
        preferencesManager.setOnboardingCompleted(false)
        _isOnboardingCompleted.value = false
    }

    fun openWebUrl(context: Context, url: String) {
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)).apply {
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    companion object {
        const val PREF_ONBOARDING_COMPLETED = "onboarding_completed"
        const val URL_TERMS = AppPreferencesManager.URL_TERMS
        const val URL_PRIVACY = AppPreferencesManager.URL_PRIVACY
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeState.value = _themeState.value.copy(themeMode = mode)
        preferencesManager.saveThemeMode(mode)
    }

    fun setColorPalette(palette: ColorPalette) {
        _themeState.value = _themeState.value.copy(colorPalette = palette)
        preferencesManager.saveColorPalette(palette)
    }

    fun setSelectedTab(index: Int) {
        _selectedTab.value = index
    }

    fun updateForm(transform: (FormState) -> FormState) {
        _formState.value = transform(_formState.value)
    }

    fun setValidityYears(years: Int) {
        val clamped = years.coerceIn(1, 100)
        _formState.value = _formState.value.copy(validityYears = clamped)
    }

    fun generateRandomPassword(length: Int = 20) {
        val pwd = PasswordGenerator.generate(length = length)
        _formState.value = _formState.value.copy(
            storePassword = pwd,
            confirmPassword = pwd,
            isStorePasswordVisible = true
        )
    }

    fun applyPreset(presetName: String) {
        when (presetName) {
            "release" -> {
                _formState.value = _formState.value.copy(
                    fileName = "release-key",
                    fileExtension = "jks",
                    alias = "key0",
                    validityYears = 25,
                    algorithm = KeyAlgorithm.RSA_2048
                )
            }
            "upload" -> {
                _formState.value = _formState.value.copy(
                    fileName = "upload-key",
                    fileExtension = "jks",
                    alias = "upload",
                    validityYears = 30,
                    algorithm = KeyAlgorithm.RSA_2048
                )
            }
            "rsa4096" -> {
                _formState.value = _formState.value.copy(
                    fileName = "app-high-security",
                    fileExtension = "keystore",
                    alias = "release",
                    validityYears = 25,
                    algorithm = KeyAlgorithm.RSA_4096
                )
            }
        }
    }

    fun setFileExtension(ext: String) {
        _formState.value = _formState.value.copy(fileExtension = ext)
    }

    fun generateKeystore(context: Context) {
        val form = _formState.value

        // Validate inputs
        if (form.fileName.isBlank()) {
            _generationState.value = GenerationUiState.Error("Por favor ingresa un nombre para el archivo keystore.")
            return
        }
        if (form.storePassword.length < 6) {
            _generationState.value = GenerationUiState.Error("La contraseña del keystore debe tener al menos 6 caracteres.")
            return
        }
        if (form.storePassword != form.confirmPassword) {
            _generationState.value = GenerationUiState.Error("Las contraseñas no coinciden.")
            return
        }
        if (form.alias.isBlank()) {
            _generationState.value = GenerationUiState.Error("El alias de la clave no puede estar vacío.")
            return
        }
        if (!form.useSamePassword && form.keyPassword.length < 6) {
            _generationState.value = GenerationUiState.Error("La contraseña de la clave debe tener al menos 6 caracteres.")
            return
        }
        if (form.countryCode.isNotBlank() && form.countryCode.length != 2) {
            _generationState.value = GenerationUiState.Error("El código de país debe ser de 2 letras (ej: ES, MX, US).")
            return
        }

        _generationState.value = GenerationUiState.Generating

        val config = KeystoreConfig(
            fileName = form.fullFileName,
            storePassword = form.storePassword,
            alias = form.alias,
            keyPassword = form.keyPassword,
            useSamePassword = form.useSamePassword,
            validityYears = form.validityYears,
            algorithm = form.algorithm,
            distinguishedName = DistinguishedName(
                commonName = form.commonName,
                organizationalUnit = form.organizationalUnit,
                organization = form.organization,
                locality = form.locality,
                state = form.state,
                countryCode = form.countryCode
            )
        )

        viewModelScope.launch {
            val result = if (form.isEphemeral) {
                repository.generateKeystore(context, config, saveToDatabase = false)
            } else {
                repository.generateKeystore(context, config, saveToDatabase = true)
            }
            result.onSuccess { details ->
                _generationState.value = GenerationUiState.Success(details)
            }.onFailure { error ->
                _generationState.value = GenerationUiState.Error(
                    error.localizedMessage ?: "Error desconocido al generar el archivo keystore."
                )
            }
        }
    }

    fun dismissGenerationState() {
        _generationState.value = GenerationUiState.Idle
    }

    fun showKeystoreDetails(details: KeystoreDetails) {
        _selectedKeystoreForDetail.value = details
    }

    fun dismissKeystoreDetails() {
        _selectedKeystoreForDetail.value = null
    }

    fun deleteKeystore(details: KeystoreDetails) {
        viewModelScope.launch {
            repository.deleteKeystore(details.id, details.filePath)
            if (_selectedKeystoreForDetail.value?.id == details.id) {
                _selectedKeystoreForDetail.value = null
            }
        }
    }

    fun restoreFromZip(context: Context, zipBytes: ByteArray) {
        _restoreState.value = RestoreUiState.Restoring
        viewModelScope.launch {
            val result = repository.restoreAndSaveAnyFromZip(context, zipBytes)
            result.onSuccess { list ->
                if (list.isEmpty()) {
                    _restoreState.value = RestoreUiState.Error("No se encontraron claves válidas en el archivo.")
                } else {
                    _restoreState.value = RestoreUiState.Success(
                        details = list.first(),
                        isVault = list.size > 1,
                        restoredList = list
                    )
                }
            }.onFailure { error ->
                _restoreState.value = RestoreUiState.Error(
                    error.localizedMessage ?: "Error al restaurar el paquete de respaldo ZIP."
                )
            }
        }
    }

    fun restoreFromZip(context: Context, inputStream: java.io.InputStream) {
        restoreFromZip(context, inputStream.readBytes())
    }

    fun dismissRestoreState() {
        _restoreState.value = RestoreUiState.Idle
    }

    suspend fun createBackupZip(details: KeystoreDetails): Result<ByteArray> {
        return repository.createBackupZip(details)
    }

    suspend fun createVaultBackupZip(): Result<ByteArray> {
        val currentKeystores = savedKeystores.value
        if (currentKeystores.isEmpty()) {
            return Result.failure(IllegalStateException("No tienes ningún keystore guardado para exportar."))
        }
        return repository.createVaultBackupZip(currentKeystores)
    }

    fun inspectKeystoreFile(bytes: ByteArray, password: String) {
        _inspectorState.value = InspectorUiState.Loading
        viewModelScope.launch {
            val result = repository.inspectKeystore(bytes, password)
            result.onSuccess { items ->
                _inspectorState.value = InspectorUiState.Success(items)
            }.onFailure { error ->
                _inspectorState.value = InspectorUiState.Error(
                    error.localizedMessage ?: "No se pudo leer el archivo keystore."
                )
            }
        }
    }

    fun inspectKeystoreFile(inputStream: java.io.InputStream, password: String) {
        inspectKeystoreFile(inputStream.readBytes(), password)
    }

    fun resetInspector() {
        _inspectorState.value = InspectorUiState.Idle
    }

    fun matchApkWithKeystore(
        context: Context?,
        apkBytes: ByteArray,
        apkFileName: String,
        targetKeystore: KeystoreDetails
    ) {
        _apkMatcherState.value = ApkMatcherUiState.Loading
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val apkInfo = ApkMatcher.analyzeApk(context, apkBytes, apkFileName)
                val matchResult = ApkMatcher.matchApkWithKeystoreDetails(apkInfo, targetKeystore)
                _apkMatcherState.value = ApkMatcherUiState.Success(
                    apkInfo = apkInfo,
                    matchResult = matchResult
                )
            } catch (e: Exception) {
                _apkMatcherState.value = ApkMatcherUiState.Error(
                    e.localizedMessage ?: "Error al analizar y verificar el archivo APK."
                )
            }
        }
    }

    fun analyzeApk(context: Context?, apkBytes: ByteArray, apkFileName: String) {
        _apkMatcherState.value = ApkMatcherUiState.Loading
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val apkInfo = ApkMatcher.analyzeApk(context, apkBytes, apkFileName)
                _apkMatcherState.value = ApkMatcherUiState.Success(
                    apkInfo = apkInfo,
                    matchResult = null
                )
            } catch (e: Exception) {
                _apkMatcherState.value = ApkMatcherUiState.Error(
                    e.localizedMessage ?: "Error al extraer las firmas del APK."
                )
            }
        }
    }

    fun resetApkMatcher() {
        _apkMatcherState.value = ApkMatcherUiState.Idle
    }

    fun updateSignApkForm(transform: (SignApkFormState) -> SignApkFormState) {
        _signApkFormState.value = transform(_signApkFormState.value)
    }

    fun selectApkForSigning(name: String, size: Long, bytes: ByteArray, context: Context?) {
        val baseName = name.removeSuffix(".apk").removeSuffix(".APK")
        val defaultOutput = if (baseName.isNotBlank()) "$baseName-signed.apk" else "app-signed.apk"

        _signApkFormState.value = _signApkFormState.value.copy(
            apkFileName = name,
            apkFileSizeBytes = size,
            apkBytes = bytes,
            outputFileName = defaultOutput
        )

        // Asynchronously analyze APK to detect package and existing signatures
        viewModelScope.launch(Dispatchers.Default) {
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

    fun signApk(context: Context) {
        val form = _signApkFormState.value
        val apkBytes = form.apkBytes

        if (apkBytes == null || apkBytes.isEmpty()) {
            _apkSigningState.value = ApkSigningUiState.Error("Por favor selecciona un archivo APK válido.")
            return
        }

        if (!form.signV1 && !form.signV2) {
            _apkSigningState.value = ApkSigningUiState.Error("Debes habilitar al menos un esquema de firma (Esquema v1 o Esquema v2).")
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
                android.util.Base64.decode(ks.base64Content, android.util.Base64.NO_WRAP)
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
            zipalign = form.zipalign,
            outputFileName = if (form.outputFileName.isBlank()) "app-signed.apk" else form.outputFileName
        )

        _apkSigningState.value = ApkSigningUiState.Signing(
            stepMessage = "Iniciando proceso de firma...",
            progress = 0.05f
        )

        viewModelScope.launch(Dispatchers.Default) {
            val result = ApkSigner.signApk(
                apkBytes = apkBytes,
                keystoreBytes = keystoreBytes,
                storePassword = storePassword,
                alias = alias,
                keyPassword = keyPassword,
                options = options,
                context = context,
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
