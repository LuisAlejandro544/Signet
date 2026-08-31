package com.example.ui.delegates

import android.content.Context
import com.example.crypto.ApkMatcher
import com.example.data.model.KeystoreDetails
import com.example.ui.state.ApkMatcherUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Delegate managing APK signature forensics and APK vs Keystore matching workflows.
 */
class ApkMatcherDelegate {

    private val _apkMatcherState = MutableStateFlow<ApkMatcherUiState>(ApkMatcherUiState.Idle)
    val apkMatcherState: StateFlow<ApkMatcherUiState> = _apkMatcherState.asStateFlow()

    fun matchApkWithKeystore(
        context: Context?,
        apkBytes: ByteArray,
        apkFileName: String,
        targetKeystore: KeystoreDetails,
        scope: CoroutineScope
    ) {
        _apkMatcherState.value = ApkMatcherUiState.Loading
        scope.launch(Dispatchers.Default) {
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

    fun analyzeApk(
        context: Context?,
        apkBytes: ByteArray,
        apkFileName: String,
        scope: CoroutineScope
    ) {
        _apkMatcherState.value = ApkMatcherUiState.Loading
        scope.launch(Dispatchers.Default) {
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
