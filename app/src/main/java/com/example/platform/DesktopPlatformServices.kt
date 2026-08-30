package com.example.platform

import com.example.crypto.DesktopStorageUtils
import java.awt.Desktop
import java.awt.FileDialog
import java.awt.Frame
import java.awt.GraphicsEnvironment
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File
import java.net.URI
import java.util.concurrent.CancellationException

/**
 * Implementación de PlatformServices para entornos de escritorio (Windows, Winlator, Linux, macOS).
 * Utiliza APIs estándar de Java SE (AWT / Desktop API) compatibles universalmente.
 */
object DesktopPlatformServices : PlatformServices {

    override val platformName: String
        get() {
            val os = System.getProperty("os.name", "Desktop")
            return if (os.contains("Windows", ignoreCase = true)) "Windows / Desktop" else os
        }

    override val isDesktop: Boolean = true

    // Último mensaje mostrado para diagnósticos o UI de escritorio
    var lastToastMessage: String? = null
        private set

    // Buffer en memoria de portapapeles para pruebas y entornos headless
    var lastCopiedText: String? = null
        private set

    override fun pickFile(
        title: String,
        allowedExtensions: List<String>,
        onResult: (PlatformFile?) -> Unit
    ) {
        if (GraphicsEnvironment.isHeadless()) {
            onResult(null)
            return
        }

        try {
            val dialog = FileDialog(null as Frame?, title, FileDialog.LOAD)
            if (allowedExtensions.isNotEmpty()) {
                dialog.setFilenameFilter { _, name ->
                    allowedExtensions.any { ext -> name.endsWith(".$ext", ignoreCase = true) }
                }
            }
            dialog.isVisible = true

            val dir = dialog.directory
            val file = dialog.file
            if (dir != null && file != null) {
                val targetFile = File(dir, file)
                if (targetFile.exists() && targetFile.isFile) {
                    val bytes = targetFile.readBytes()
                    onResult(PlatformFile(name = targetFile.name, size = targetFile.length(), bytes = bytes, file = targetFile))
                    return
                }
            }
            onResult(null)
        } catch (e: Exception) {
            println("[DesktopPlatformServices] Error al abrir diálogo de selección: ${e.message}")
            onResult(null)
        }
    }

    override fun saveFile(
        defaultFileName: String,
        mimeType: String,
        bytes: ByteArray,
        onResult: (Result<File>) -> Unit
    ) {
        if (GraphicsEnvironment.isHeadless()) {
            // Guardado automático en carpeta de Signet para tests / CI / CLI
            try {
                val exportDir = File(DesktopStorageUtils.getAppDirectory(), "exports").apply { mkdirs() }
                val targetFile = File(exportDir, defaultFileName)
                targetFile.writeBytes(bytes)
                onResult(Result.success(targetFile))
            } catch (e: Exception) {
                onResult(Result.failure(e))
            }
            return
        }

        try {
            val dialog = FileDialog(null as Frame?, "Guardar archivo", FileDialog.SAVE)
            dialog.file = defaultFileName
            dialog.isVisible = true

            val dir = dialog.directory
            val file = dialog.file
            if (dir != null && file != null) {
                val targetFile = File(dir, file)
                targetFile.writeBytes(bytes)
                onResult(Result.success(targetFile))
            } else {
                onResult(Result.failure(CancellationException("Operación cancelada por el usuario.")))
            }
        } catch (e: Exception) {
            println("[DesktopPlatformServices] Error al guardar archivo: ${e.message}")
            onResult(Result.failure(e))
        }
    }

    override fun showToast(message: String, isLong: Boolean) {
        lastToastMessage = message
        println("[Signet Desktop] $message")
    }

    override fun copyToClipboard(label: String, text: String) {
        lastCopiedText = text
        if (!GraphicsEnvironment.isHeadless()) {
            try {
                val selection = StringSelection(text)
                Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, selection)
            } catch (e: Exception) {
                println("[DesktopPlatformServices] Error al copiar al portapapeles del sistema: ${e.message}")
            }
        }
    }

    override fun openUrl(url: String) {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            try {
                Desktop.getDesktop().browse(URI(url))
                return
            } catch (_: Exception) {}
        }

        // Fallback para Windows / Winlator
        try {
            val os = System.getProperty("os.name", "").lowercase()
            if (os.contains("win")) {
                Runtime.getRuntime().exec(arrayOf("rundll32", "url.dll,FileProtocolHandler", url))
            } else if (os.contains("mac")) {
                Runtime.getRuntime().exec(arrayOf("open", url))
            } else {
                Runtime.getRuntime().exec(arrayOf("xdg-open", url))
            }
        } catch (e: Exception) {
            println("[DesktopPlatformServices] No se pudo abrir la URL: ${e.message}")
        }
    }

    override fun openFolder(file: File) {
        val target = if (file.isDirectory) file else file.parentFile ?: file
        val os = System.getProperty("os.name", "").lowercase()

        // En Windows / Winlator, resalta el archivo directamente en el Explorador si es un archivo
        if (os.contains("win") && file.exists() && file.isFile) {
            try {
                Runtime.getRuntime().exec(arrayOf("explorer.exe", "/select,", file.absolutePath))
                return
            } catch (_: Exception) {}
        }

        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            try {
                Desktop.getDesktop().open(target)
                return
            } catch (_: Exception) {}
        }

        try {
            if (os.contains("win")) {
                Runtime.getRuntime().exec(arrayOf("explorer.exe", target.absolutePath))
            } else if (os.contains("mac")) {
                Runtime.getRuntime().exec(arrayOf("open", target.absolutePath))
            } else {
                Runtime.getRuntime().exec(arrayOf("xdg-open", target.absolutePath))
            }
        } catch (e: Exception) {
            println("[DesktopPlatformServices] No se pudo abrir el directorio: ${e.message}")
        }
    }

    override fun installApk(apkFile: File) {
        showToast("APK listo en: ${apkFile.absolutePath}")
        openFolder(apkFile)
    }
}
