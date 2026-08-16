package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.crypto.KeystoreGenerator
import com.example.crypto.PasswordGenerator
import com.example.data.KeystoreRepository
import com.example.data.local.AppDatabase
import com.example.data.model.DistinguishedName
import com.example.data.model.KeyAlgorithm
import com.example.data.model.KeystoreConfig
import com.example.data.model.KeystoreDetails
import com.example.ui.theme.ColorPalette
import com.example.ui.theme.ThemeMode
import com.example.ui.theme.ThemeState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FormState(
    val fileName: String = "release-key",
    val fileExtension: String = "jks", // "jks" or "keystore"
    val storePassword: String = "",
    val confirmPassword: String = "",
    val isStorePasswordVisible: Boolean = false,
    val alias: String = "key0",
    val keyPassword: String = "",
    val isKeyPasswordVisible: Boolean = false,
    val useSamePassword: Boolean = true,
    val validityYears: Int = 25,
    val algorithm: KeyAlgorithm = KeyAlgorithm.RSA_2048,
    val commonName: String = "",
    val organizationalUnit: String = "",
    val organization: String = "",
    val locality: String = "",
    val state: String = "",
    val countryCode: String = "",
    val isAdvancedDnExpanded: Boolean = true
) {
    val fullFileName: String
        get() {
            val cleanName = fileName.trim()
                .removeSuffix(".jks")
                .removeSuffix(".keystore")
                .removeSuffix(".p12")
            val base = if (cleanName.isBlank()) "release-key" else cleanName
            return "$base.$fileExtension"
        }
}

sealed interface GenerationUiState {
    object Idle : GenerationUiState
    object Generating : GenerationUiState
    data class Success(val details: KeystoreDetails) : GenerationUiState
    data class Error(val message: String) : GenerationUiState
}

sealed interface RestoreUiState {
    object Idle : RestoreUiState
    object Restoring : RestoreUiState
    data class Success(val details: KeystoreDetails) : RestoreUiState
    data class Error(val message: String) : RestoreUiState
}

sealed interface InspectorUiState {
    object Idle : InspectorUiState
    object Loading : InspectorUiState
    data class Success(val items: List<KeystoreDetails>) : InspectorUiState
    data class Error(val message: String) : InspectorUiState
}

class KeystoreViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("keystore_generator_prefs", Context.MODE_PRIVATE)

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

    private val _selectedKeystoreForDetail = MutableStateFlow<KeystoreDetails?>(null)
    val selectedKeystoreForDetail: StateFlow<KeystoreDetails?> = _selectedKeystoreForDetail.asStateFlow()

    // Theme state with saved preferences
    private val _themeState = MutableStateFlow(loadInitialThemeState())
    val themeState: StateFlow<ThemeState> = _themeState.asStateFlow()

    private fun loadInitialThemeState(): ThemeState {
        val savedModeName = prefs.getString("theme_mode", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        val savedPaletteName = prefs.getString("color_palette", ColorPalette.DYNAMIC.name) ?: ColorPalette.DYNAMIC.name

        val mode = try {
            ThemeMode.valueOf(savedModeName)
        } catch (_: Exception) {
            ThemeMode.SYSTEM
        }

        val palette = try {
            ColorPalette.valueOf(savedPaletteName)
        } catch (_: Exception) {
            ColorPalette.DYNAMIC
        }

        return ThemeState(themeMode = mode, colorPalette = palette)
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeState.value = _themeState.value.copy(themeMode = mode)
        prefs.edit().putString("theme_mode", mode.name).apply()
    }

    fun setColorPalette(palette: ColorPalette) {
        _themeState.value = _themeState.value.copy(colorPalette = palette)
        prefs.edit().putString("color_palette", palette.name).apply()
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

        // Validate
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
            val result = repository.restoreAndSaveKeystoreFromZip(context, zipBytes)
            result.onSuccess { details ->
                _restoreState.value = RestoreUiState.Success(details)
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
}
