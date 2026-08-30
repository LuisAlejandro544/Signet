package com.example.platform

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Controlador para invocar la selección de archivos de manera agnóstica a la plataforma.
 */
interface PlatformFilePickerLauncher {
    fun launch(
        title: String = "Seleccionar archivo",
        mimeType: String = "*/*",
        allowedExtensions: List<String> = emptyList()
    )
}

/**
 * Controlador para invocar el guardado de archivos de manera agnóstica a la plataforma.
 */
interface PlatformFileSaverLauncher {
    fun launch(
        defaultFileName: String,
        mimeType: String = "*/*",
        bytes: ByteArray
    )
}

/**
 * Composable que proporciona un lanzador unificado de selección de archivos.
 * En Android delega a ActivityResultContracts.GetContent() leyendo metadatos y bytes reales.
 * En Desktop / Windows delega a DesktopPlatformServices con FileDialog nativo.
 */
@Composable
fun rememberPlatformFilePicker(
    onFileSelected: (PlatformFile?) -> Unit
): PlatformFilePickerLauncher {
    val platformServices = LocalPlatformServices.current

    if (platformServices.isDesktop) {
        return remember(platformServices) {
            object : PlatformFilePickerLauncher {
                override fun launch(
                    title: String,
                    mimeType: String,
                    allowedExtensions: List<String>
                ) {
                    platformServices.pickFile(title, allowedExtensions, onFileSelected)
                }
            }
        }
    }

    // Android implementation
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val androidLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) {
            onFileSelected(null)
            return@rememberLauncherForActivityResult
        }

        scope.launch(Dispatchers.IO) {
            try {
                var fileName = "archivo"
                var fileSize = 0L

                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIndex != -1) fileName = cursor.getString(nameIndex) ?: "archivo"
                        if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
                    }
                }

                val bytes = context.contentResolver.openInputStream(uri)?.use { stream ->
                    stream.readBytes()
                } ?: ByteArray(0)

                if (fileSize == 0L) fileSize = bytes.size.toLong()

                withContext(Dispatchers.Main) {
                    onFileSelected(PlatformFile(name = fileName, size = fileSize, bytes = bytes))
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onFileSelected(null)
                }
            }
        }
    }

    return remember(androidLauncher) {
        object : PlatformFilePickerLauncher {
            override fun launch(
                title: String,
                mimeType: String,
                allowedExtensions: List<String>
            ) {
                androidLauncher.launch(mimeType)
            }
        }
    }
}

/**
 * Composable que proporciona un lanzador unificado de guardado de archivos.
 * En Android delega a ActivityResultContracts.CreateDocument().
 * En Desktop / Windows delega a DesktopPlatformServices con FileDialog SAVE nativo.
 */
@Composable
fun rememberPlatformFileSaver(
    onSaved: (Result<File>) -> Unit
): PlatformFileSaverLauncher {
    val platformServices = LocalPlatformServices.current

    if (platformServices.isDesktop) {
        return remember(platformServices) {
            object : PlatformFileSaverLauncher {
                override fun launch(
                    defaultFileName: String,
                    mimeType: String,
                    bytes: ByteArray
                ) {
                    platformServices.saveFile(defaultFileName, mimeType, bytes, onSaved)
                }
            }
        }
    }

    // Android implementation
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingBytes by remember { mutableStateOf<ByteArray?>(null) }
    var pendingFileName by remember { mutableStateOf("archivo") }

    val androidLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri: Uri? ->
        val bytesToWrite = pendingBytes
        pendingBytes = null

        if (uri != null && bytesToWrite != null) {
            val nonNullBytes: ByteArray = bytesToWrite
            scope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { os: java.io.OutputStream ->
                        os.write(nonNullBytes)
                    }
                    val dummyFile = File(context.filesDir, pendingFileName)
                    withContext(Dispatchers.Main) {
                        onSaved(Result.success(dummyFile))
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        onSaved(Result.failure(e))
                    }
                }
            }
        } else {
            onSaved(Result.failure(java.util.concurrent.CancellationException("Guardado cancelado")))
        }
    }

    return remember(androidLauncher) {
        object : PlatformFileSaverLauncher {
            override fun launch(
                defaultFileName: String,
                mimeType: String,
                bytes: ByteArray
            ) {
                pendingBytes = bytes
                pendingFileName = defaultFileName
                androidLauncher.launch(defaultFileName)
            }
        }
    }
}
