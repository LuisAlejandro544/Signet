package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.crypto.ApkMatcher
import com.example.crypto.PasswordGenerator
import com.example.data.KeystoreRepository
import com.example.data.local.AppDatabase
import com.example.data.model.DistinguishedName
import com.example.data.model.KeyAlgorithm
import com.example.data.model.KeystoreConfig
import com.example.data.model.KeystoreDetails
import com.example.ui.preferences.AppPreferencesManager
import com.example.ui.state.ApkMatcherUiState
import com.example.ui.state.FormState
import com.example.ui.state.GenerationUiState
import com.example.ui.state.InspectorUiState
import com.example.ui.state.RestoreUiState
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

// Typealiases for seamless backward compatibility
typealias FormState = com.example.ui.state.FormState
typealias GenerationUiState = com.example.ui.state.GenerationUiState
typealias RestoreUiState = com.example.ui.state.RestoreUiState
typealias InspectorUiState = com.example.ui.state.InspectorUiState
typealias ApkMatcherUiState = com.example.ui.state.ApkMatcherUiState

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
            val result = repository.generateAndSaveKeystore(context, config)
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
}
