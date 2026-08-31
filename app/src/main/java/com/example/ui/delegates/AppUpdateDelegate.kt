package com.example.ui.delegates

import com.example.ui.res.SignetStrings
import com.example.update.AppUpdateManager
import com.example.update.UpdateUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * Delegate managing GitHub release version checks and update downloads.
 */
class AppUpdateDelegate {

    private val _updateState = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val updateState: StateFlow<UpdateUiState> = _updateState.asStateFlow()

    fun checkForUpdates(
        isManual: Boolean = false,
        isDesktop: Boolean = false,
        currentVersion: String = SignetStrings.APP_VERSION,
        scope: CoroutineScope
    ) {
        scope.launch(Dispatchers.IO) {
            if (isManual) {
                _updateState.value = UpdateUiState.Checking
            }
            try {
                val releaseInfo = AppUpdateManager.checkLatestRelease(currentVersion, isDesktop)
                if (releaseInfo != null) {
                    _updateState.value = UpdateUiState.Available(release = releaseInfo)
                } else if (isManual) {
                    _updateState.value = UpdateUiState.UpToDate(currentVersion)
                }
            } catch (e: Exception) {
                if (isManual) {
                    _updateState.value = UpdateUiState.Error("No se pudo comprobar actualizaciones: ${e.localizedMessage}")
                }
            }
        }
    }

    fun startUpdateDownload(
        targetDirectory: File,
        isDesktop: Boolean = false,
        onDownloaded: ((File) -> Unit)? = null,
        scope: CoroutineScope
    ) {
        val currentState = _updateState.value
        if (currentState !is UpdateUiState.Available) return
        val matchedAsset = currentState.release.matchedAsset
        if (matchedAsset == null) {
            _updateState.value = currentState.copy(
                errorMessage = "No se encontró un archivo instalador compatible con esta plataforma en la versión ${currentState.release.tagName}."
            )
            return
        }

        _updateState.value = currentState.copy(
            isDownloading = true,
            progressPercent = 0,
            errorMessage = null
        )

        scope.launch(Dispatchers.IO) {
            val targetFile = File(targetDirectory, matchedAsset.name)
            val result = AppUpdateManager.downloadUpdate(
                downloadUrl = matchedAsset.downloadUrl,
                targetFile = targetFile,
                onProgress = { downloaded, total, percent ->
                    val s = _updateState.value
                    if (s is UpdateUiState.Available) {
                        _updateState.value = s.copy(
                            isDownloading = true,
                            progressPercent = percent,
                            downloadedBytes = downloaded,
                            totalBytes = total
                        )
                    }
                }
            )

            result.fold(
                onSuccess = { file ->
                    val s = _updateState.value
                    if (s is UpdateUiState.Available) {
                        _updateState.value = s.copy(
                            isDownloading = false,
                            progressPercent = 100,
                            downloadedFile = file
                        )
                    }
                    onDownloaded?.invoke(file)
                },
                onFailure = { err ->
                    val s = _updateState.value
                    if (s is UpdateUiState.Available) {
                        _updateState.value = s.copy(
                            isDownloading = false,
                            errorMessage = "Error en la descarga: ${err.localizedMessage}"
                        )
                    }
                }
            )
        }
    }

    fun dismissUpdate() {
        _updateState.value = UpdateUiState.Idle
    }
}
