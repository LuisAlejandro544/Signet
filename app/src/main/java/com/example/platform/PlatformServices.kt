package com.example.platform

import androidx.compose.runtime.staticCompositionLocalOf
import java.io.File

/**
 * Representa un archivo cargado o seleccionado en cualquier plataforma (Android, Windows, Winlator, Linux).
 */
data class PlatformFile(
    val name: String,
    val size: Long,
    val bytes: ByteArray,
    val file: File? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as PlatformFile
        return name == other.name && size == other.size && bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + size.hashCode()
        result = 31 * result + bytes.contentHashCode()
        return result
    }
}

/**
 * Abstracción de servicios de sistema operativo.
 * Proporciona un contrato unificado para la selección y guardado de archivos,
 * interacción con el portapapeles, notificaciones y exploración de directorios.
 */
interface PlatformServices {
    val platformName: String
    val isDesktop: Boolean

    /**
     * Abre un diálogo nativo para seleccionar un archivo.
     */
    fun pickFile(
        title: String,
        allowedExtensions: List<String>,
        onResult: (PlatformFile?) -> Unit
    )

    /**
     * Abre un diálogo nativo para guardar un archivo.
     */
    fun saveFile(
        defaultFileName: String,
        mimeType: String,
        bytes: ByteArray,
        onResult: (Result<File>) -> Unit
    )

    /**
     * Muestra una notificación o mensaje emergente al usuario.
     */
    fun showToast(message: String, isLong: Boolean = false)

    /**
     * Copia texto al portapapeles del sistema operativo.
     */
    fun copyToClipboard(label: String, text: String)

    /**
     * Abre una URL en el navegador predeterminado del sistema.
     */
    fun openUrl(url: String)

    /**
     * Abre la carpeta contenedora en el explorador del sistema de archivos (Explorador de Windows, etc.).
     */
    fun openFolder(file: File)

    /**
     * Inicia la instalación o despliega el APK firmado.
     */
    fun installApk(apkFile: File)
}

/**
 * CompositionLocal que provee la instancia activa de PlatformServices a cualquier Composable.
 */
val LocalPlatformServices = staticCompositionLocalOf<PlatformServices> {
    DesktopPlatformServices
}
