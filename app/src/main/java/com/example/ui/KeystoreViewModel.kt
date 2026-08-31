package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.crypto.DesktopStorageUtils
import com.example.data.KeystoreRepository
import com.example.data.createAndroidKeystoreRepository
import com.example.data.local.AppDatabase
import com.example.data.model.KeystoreDetails
import com.example.ui.delegates.ApkMatcherDelegate
import com.example.ui.delegates.AppUpdateDelegate
import com.example.ui.delegates.KeystoreFormDelegate
import com.example.ui.delegates.SignApkDelegate
import com.example.ui.preferences.AppPreferencesManager
import com.example.ui.res.SignetStrings
import com.example.ui.state.ApkMatcherUiState
import com.example.ui.state.ApkSigningUiState
import com.example.ui.state.FormState
import com.example.ui.state.GenerationUiState
import com.example.ui.state.InspectorUiState
import com.example.ui.state.RestoreUiState
import com.example.ui.state.SignApkFormState
import com.example.ui.theme.ColorPalette
import com.example.ui.theme.ThemeMode
import com.example.ui.theme.ThemeState
import com.example.update.UpdateUiState
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

/**
 * Main ViewModel orchestrating Signet's state.
 * Deconstructs responsibilities into modular delegates:
 * - [KeystoreFormDelegate]: Form editing, presets, and keystore generation.
 * - [SignApkDelegate]: APK selection, signature inspection, and APK signing.
 * - [ApkMatcherDelegate]: Forensic signature inspection and APK vs Keystore matching.
 * - [AppUpdateDelegate]: Release version checking and binary update downloading.
 */
open class KeystoreViewModel(
    application: Application,
    private val preferencesManager: AppPreferencesManager,
    private val repository: KeystoreRepository,
    private val baseDataDir: File,
    private val formDelegate: KeystoreFormDelegate = KeystoreFormDelegate(),
    private val signApkDelegate: SignApkDelegate = SignApkDelegate(),
    private val apkMatcherDelegate: ApkMatcherDelegate = ApkMatcherDelegate(),
    private val updateDelegate: AppUpdateDelegate = AppUpdateDelegate()
) : androidx.lifecycle.AndroidViewModel(application) {

    constructor(application: Application) : this(
        application = application,
        preferencesManager = AppPreferencesManager(application),
        repository = createAndroidKeystoreRepository(AppDatabase.getDatabase(application)),
        baseDataDir = application.filesDir
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.syncAndRecoverOrphanKeystores(baseDataDir)
            } catch (_: Exception) {}
            try {
                checkForUpdates(isManual = false, isDesktop = false)
            } catch (_: Exception) {}
        }
    }

    constructor() : this(
        application = Application(),
        preferencesManager = AppPreferencesManager(),
        repository = KeystoreRepository(DesktopStorageUtils.getDesktopDataDir()),
        baseDataDir = DesktopStorageUtils.getDesktopDataDir()
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.syncAndRecoverOrphanKeystores(baseDataDir)
            } catch (_: Exception) {}
            try {
                checkForUpdates(isManual = false, isDesktop = true)
            } catch (_: Exception) {}
        }
    }

    fun getVaultDirectory(): File = baseDataDir

    fun syncKeystores(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.syncAndRecoverOrphanKeystores(context.filesDir)
            } catch (_: Exception) {}
        }
    }

    fun syncKeystores(dir: File = baseDataDir) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.syncAndRecoverOrphanKeystores(dir)
            } catch (_: Exception) {}
        }
    }

    val savedKeystores: StateFlow<List<KeystoreDetails>> = repository.allKeystores
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 0: Generar, 1: Mis Keystores, 2: Firmar APK, 3: Inspeccionar, 4: Configuración
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    // Form & Generation States (delegated)
    val formState: StateFlow<FormState> = formDelegate.formState
    val generationState: StateFlow<GenerationUiState> = formDelegate.generationState
    val selectedKeystoreForDetail: StateFlow<KeystoreDetails?> = formDelegate.selectedKeystoreForDetail

    // Restore & Inspector States
    private val _restoreState = MutableStateFlow<RestoreUiState>(RestoreUiState.Idle)
    val restoreState: StateFlow<RestoreUiState> = _restoreState.asStateFlow()

    private val _inspectorState = MutableStateFlow<InspectorUiState>(InspectorUiState.Idle)
    val inspectorState: StateFlow<InspectorUiState> = _inspectorState.asStateFlow()

    // APK Matcher State (delegated)
    val apkMatcherState: StateFlow<ApkMatcherUiState> = apkMatcherDelegate.apkMatcherState

    // APK Signing States (delegated)
    val signApkFormState: StateFlow<SignApkFormState> = signApkDelegate.signApkFormState
    val apkSigningState: StateFlow<ApkSigningUiState> = signApkDelegate.apkSigningState

    // App Update State (delegated)
    val updateState: StateFlow<UpdateUiState> = updateDelegate.updateState

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

    // --- Form & Keystore Generation Delegation ---

    fun updateForm(transform: (FormState) -> FormState) {
        formDelegate.updateForm(transform)
    }

    fun setValidityYears(years: Int) {
        formDelegate.setValidityYears(years)
    }

    fun generateRandomPassword(length: Int = 20) {
        formDelegate.generateRandomPassword(length)
    }

    fun applyPreset(presetName: String) {
        formDelegate.applyPreset(presetName)
    }

    fun setFileExtension(ext: String) {
        formDelegate.setFileExtension(ext)
    }

    fun generateKeystore(context: Context) = generateKeystore(context.filesDir)

    fun generateKeystore(outputDir: File = baseDataDir) {
        formDelegate.generateKeystore(outputDir, repository, viewModelScope)
    }

    fun dismissGenerationState() {
        formDelegate.dismissGenerationState()
    }

    fun showKeystoreDetails(details: KeystoreDetails) {
        formDelegate.showKeystoreDetails(details)
    }

    fun dismissKeystoreDetails() {
        formDelegate.dismissKeystoreDetails()
    }

    fun deleteKeystore(details: KeystoreDetails) {
        viewModelScope.launch {
            repository.deleteKeystore(details.id, details.filePath)
            formDelegate.clearDetailsIfMatching(details.id)
        }
    }

    // --- Backup & Restore ---

    fun restoreFromZip(context: Context, zipBytes: ByteArray) = restoreFromZip(context.filesDir, zipBytes)

    fun restoreFromZip(
        outputDir: File = baseDataDir,
        zipBytes: ByteArray
    ) {
        _restoreState.value = RestoreUiState.Restoring
        viewModelScope.launch {
            val result = repository.restoreAndSaveAnyFromZip(outputDir, zipBytes)
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

    // --- Keystore Inspector ---

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

    // --- APK Matcher Delegation ---

    fun matchApkWithKeystore(
        context: Context?,
        apkBytes: ByteArray,
        apkFileName: String,
        targetKeystore: KeystoreDetails
    ) {
        apkMatcherDelegate.matchApkWithKeystore(context, apkBytes, apkFileName, targetKeystore, viewModelScope)
    }

    fun analyzeApk(context: Context?, apkBytes: ByteArray, apkFileName: String) {
        apkMatcherDelegate.analyzeApk(context, apkBytes, apkFileName, viewModelScope)
    }

    fun resetApkMatcher() {
        apkMatcherDelegate.resetApkMatcher()
    }

    // --- APK Signing Delegation ---

    fun updateSignApkForm(transform: (SignApkFormState) -> SignApkFormState) {
        signApkDelegate.updateSignApkForm(transform)
    }

    fun selectApkForSigning(name: String, size: Long, bytes: ByteArray, context: Context?) {
        signApkDelegate.selectApkForSigning(name, size, bytes, context, viewModelScope)
    }

    fun clearSelectedApkForSigning() {
        signApkDelegate.clearSelectedApkForSigning()
    }

    fun selectSavedKeystoreForSigning(keystore: KeystoreDetails) {
        signApkDelegate.selectSavedKeystoreForSigning(keystore)
    }

    fun setExternalKeystoreForSigning(name: String, bytes: ByteArray) {
        signApkDelegate.setExternalKeystoreForSigning(name, bytes)
    }

    fun resetSigningState() {
        signApkDelegate.resetSigningState()
    }

    fun signApk(context: Context) {
        signApk(outputDir = File(context.cacheDir, "signed_apks").apply { mkdirs() })
    }

    fun signApk(outputDir: File = File(baseDataDir, "signed_apks").apply { mkdirs() }) {
        signApkDelegate.signApk(outputDir, viewModelScope)
    }

    fun installSignedApk(context: Context, apkFile: File) {
        signApkDelegate.installSignedApk(context, apkFile)
    }

    // --- App Update Delegation ---

    fun checkForUpdates(
        isManual: Boolean = false,
        isDesktop: Boolean = false,
        currentVersion: String = SignetStrings.APP_VERSION
    ) {
        updateDelegate.checkForUpdates(isManual, isDesktop, currentVersion, viewModelScope)
    }

    fun startUpdateDownload(
        targetDirectory: File,
        isDesktop: Boolean = false,
        onDownloaded: ((File) -> Unit)? = null
    ) {
        updateDelegate.startUpdateDownload(targetDirectory, isDesktop, onDownloaded, viewModelScope)
    }

    fun dismissUpdate() {
        updateDelegate.dismissUpdate()
    }
}
