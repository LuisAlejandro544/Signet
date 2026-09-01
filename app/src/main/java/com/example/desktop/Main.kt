package com.example.desktop

import com.example.crypto.DesktopStorageUtils
import com.example.desktop.cli.DesktopCliHandler

/**
 * Punto de entrada nativo para la ejecución de Signet en entornos de escritorio (Windows / macOS / Linux).
 * Soporta modo gráfico interactivo completo y operaciones CLI headless para Windows Terminal / PowerShell.
 */
object DesktopLauncher {

    @JvmStatic
    fun main(args: Array<String>) {
        // Manejo de argumentos por línea de comandos para Windows Terminal / PowerShell
        if (args.isNotEmpty()) {
            DesktopCliHandler.handle(args)
            return
        }

        println("==================================================")
        println("  Signet - Android Keystore Generator & APK Signer")
        println("  Plataforma: Escritorio (${System.getProperty("os.name")} - ${System.getProperty("os.arch")})")
        println("  Directorio de datos: ${DesktopStorageUtils.getDesktopDataDir().absolutePath}")
        println("==================================================")

        // En entornos gráficos de escritorio, inicia el contenedor de ventana
        try {
            println("Iniciando interfaz gráfica de Signet Desktop...")
            DesktopWindowLauncher.startApplication()
        } catch (e: NoClassDefFoundError) {
            println("Aviso: Ejecutando en entorno sin Compose Desktop runtime. Directorio de datos y servicios verificados.")
        } catch (e: Exception) {
            System.err.println("Error iniciando Signet Desktop: ${e.message}")
            e.printStackTrace()
        }
    }
}
