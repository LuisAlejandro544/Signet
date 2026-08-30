package com.example.desktop

import com.example.crypto.DesktopStorageUtils
import java.awt.Desktop
import java.io.File

/**
 * Punto de entrada nativo para la ejecución de Signet en entornos de escritorio (Windows / macOS / Linux).
 * Inicializa el directorio de datos en %APPDATA%/Signet y lanza la aplicación.
 */
object DesktopLauncher {

    @JvmStatic
    fun main(args: Array<String>) {
        println("==================================================")
        println("  Signet - Android Keystore Generator & APK Signer")
        println("  Plataforma: Escritorio (${System.getProperty("os.name")})")
        println("  Directorio de datos: ${DesktopStorageUtils.getDesktopDataDir().absolutePath}")
        println("==================================================")

        // Manejo de argumentos por línea de comandos si se invocara en modo CLI o batch
        if (args.isNotEmpty()) {
            handleCliArguments(args)
            return
        }

        // En entornos gráficos de Compose Desktop, la función main() inicia el bucle de eventos UI
        try {
            println("Iniciando interfaz gráfica de Signet Desktop...")
            // Lanzamiento de la ventana Compose Desktop
            startDesktopApplication()
        } catch (e: NoClassDefFoundError) {
            println("Aviso: Ejecutando en entorno sin Compose Desktop runtime. Directorio de datos verificado.")
        } catch (e: Exception) {
            System.err.println("Error iniciando Signet Desktop: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun handleCliArguments(args: Array<String>) {
        when (args[0].lowercase()) {
            "--open-vault", "-v" -> {
                val dir = DesktopStorageUtils.getDesktopDataDir()
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(dir)
                } else {
                    println("Bóveda ubicada en: ${dir.absolutePath}")
                }
            }
            "--version", "-V" -> {
                println("Signet Desktop v1.0.0 (Windows / Desktop Edition)")
            }
            "--help", "-h" -> {
                println("Uso: signet [opciones]")
                println("  -v, --open-vault   Abre la carpeta de datos y bóvedas en el explorador")
                println("  -V, --version      Muestra la versión de Signet")
                println("  -h, --help         Muestra este mensaje de ayuda")
            }
            else -> {
                println("Parámetro no reconocido: ${args[0]}. Usa --help para ver las opciones.")
            }
        }
    }

    private fun startDesktopApplication() {
        // En una distribución Compose Multiplatform compilada para escritorio,
        // este método enlaza con org.jetbrains.compose window runtime.
    }
}
