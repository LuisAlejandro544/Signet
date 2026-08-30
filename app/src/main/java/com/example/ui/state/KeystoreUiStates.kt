package com.example.ui.state

import com.example.data.model.ApkInfo
import com.example.data.model.ApkMatchResult
import com.example.data.model.KeystoreDetails

/**
 * UI State for the Keystore Generation workflow.
 */
sealed interface GenerationUiState {
    object Idle : GenerationUiState
    object Generating : GenerationUiState
    data class Success(val details: KeystoreDetails) : GenerationUiState
    data class Error(val message: String) : GenerationUiState
}

/**
 * UI State for the ZIP Backup Restoration workflow.
 */
sealed interface RestoreUiState {
    object Idle : RestoreUiState
    object Restoring : RestoreUiState
    data class Success(
        val details: KeystoreDetails,
        val isVault: Boolean = false,
        val restoredList: List<KeystoreDetails> = listOf(details)
    ) : RestoreUiState
    data class Error(val message: String) : RestoreUiState
}

/**
 * UI State for the Bulk Vault Export workflow.
 */
sealed interface VaultExportUiState {
    object Idle : VaultExportUiState
    object Exporting : VaultExportUiState
    data class Ready(val zipBytes: ByteArray, val count: Int, val fileName: String) : VaultExportUiState
    data class Error(val message: String) : VaultExportUiState
}

/**
 * UI State for the Keystore File Inspector workflow.
 */
sealed interface InspectorUiState {
    object Idle : InspectorUiState
    object Loading : InspectorUiState
    data class Success(val items: List<KeystoreDetails>) : InspectorUiState
    data class Error(val message: String) : InspectorUiState
}

/**
 * UI State for the APK Matcher & Signature Verification workflow.
 */
sealed interface ApkMatcherUiState {
    object Idle : ApkMatcherUiState
    object Loading : ApkMatcherUiState
    data class Success(
        val apkInfo: ApkInfo,
        val matchResult: ApkMatchResult?
    ) : ApkMatcherUiState
    data class Error(val message: String) : ApkMatcherUiState
}
