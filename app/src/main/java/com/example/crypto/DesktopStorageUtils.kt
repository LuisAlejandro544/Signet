package com.example.crypto

import java.io.File

/**
 * Utilidades para la resolución de directorios y rutas de almacenamiento en entornos
 * de escritorio (Windows, Linux y macOS).
 */
object DesktopStorageUtils {

    /**
     * Obtiene el directorio base de datos de Signet según el sistema operativo.
     * En Windows: %APPDATA%/Signet
     * En Unix/Linux/macOS: ~/.config/signet o ~/.signet
     */
    fun getDesktopDataDir(): File {
        val osName = System.getProperty("os.name")?.lowercase() ?: ""
        val appData = System.getenv("APPDATA")

        val baseDir = when {
            osName.contains("win") && !appData.isNullOrBlank() -> {
                File(appData, "Signet")
            }
            osName.contains("mac") -> {
                val userHome = System.getProperty("user.home") ?: "."
                File(userHome, "Library/Application Support/Signet")
            }
            else -> {
                val xdgConfig = System.getenv("XDG_CONFIG_HOME")
                if (!xdgConfig.isNullOrBlank()) {
                    File(xdgConfig, "signet")
                } else {
                    val userHome = System.getProperty("user.home") ?: "."
                    File(userHome, ".config/signet")
                }
            }
        }

        if (!baseDir.exists()) {
            baseDir.mkdirs()
        }
        return baseDir
    }

    fun getAppDirectory(): File = getDesktopDataDir()

    /**
     * Directorio donde se almacenan físicamente los archivos Keystore (.jks/.keystore) en escritorio.
     */
    fun getDesktopKeystoresDir(): File {
        val keystoresDir = File(getDesktopDataDir(), "keystores")
        if (!keystoresDir.exists()) {
            keystoresDir.mkdirs()
        }
        return keystoresDir
    }
}
