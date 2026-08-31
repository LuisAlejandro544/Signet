package com.example.ui.delegates

import com.example.crypto.PasswordGenerator
import com.example.data.KeystoreRepository
import com.example.data.model.DistinguishedName
import com.example.data.model.KeyAlgorithm
import com.example.data.model.KeystoreConfig
import com.example.data.model.KeystoreDetails
import com.example.ui.state.FormState
import com.example.ui.state.GenerationUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * Delegate managing keystore creation forms, presets, validation, and generation states.
 */
class KeystoreFormDelegate {

    private val _formState = MutableStateFlow(FormState())
    val formState: StateFlow<FormState> = _formState.asStateFlow()

    private val _generationState = MutableStateFlow<GenerationUiState>(GenerationUiState.Idle)
    val generationState: StateFlow<GenerationUiState> = _generationState.asStateFlow()

    private val _selectedKeystoreForDetail = MutableStateFlow<KeystoreDetails?>(null)
    val selectedKeystoreForDetail: StateFlow<KeystoreDetails?> = _selectedKeystoreForDetail.asStateFlow()

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
            "windows", "pfx" -> {
                _formState.value = _formState.value.copy(
                    fileName = "authenticode-codesign",
                    fileExtension = "pfx",
                    alias = "codesign",
                    validityYears = 10,
                    algorithm = KeyAlgorithm.RSA_4096
                )
            }
            "p12" -> {
                _formState.value = _formState.value.copy(
                    fileName = "multiplatform-key",
                    fileExtension = "p12",
                    alias = "app-signer",
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

    fun generateKeystore(
        outputDir: File,
        repository: KeystoreRepository,
        scope: CoroutineScope
    ) {
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

        scope.launch {
            val result = if (form.isEphemeral) {
                repository.generateKeystore(outputDir, config, saveToDatabase = false)
            } else {
                repository.generateKeystore(outputDir, config, saveToDatabase = true)
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

    fun clearDetailsIfMatching(id: Long) {
        if (_selectedKeystoreForDetail.value?.id == id) {
            _selectedKeystoreForDetail.value = null
        }
    }
}
