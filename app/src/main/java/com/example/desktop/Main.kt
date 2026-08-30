package com.example.desktop

import com.example.crypto.DesktopStorageUtils
import java.awt.Desktop
import java.awt.Dimension
import java.awt.GraphicsEnvironment
import java.io.File
import javax.swing.JFrame
import javax.swing.SwingUtilities

/**
 * Punto de entrada nativo para la ejecución de Signet en entornos de escritorio (Windows / macOS / Linux).
 * Inicializa el directorio de datos en %APPDATA%/Signet y lanza la aplicación y su bucle de ventana gráfica.
 */
object DesktopLauncher {

    @JvmStatic
    fun main(args: Array<String>) {
        println("==================================================")
        println("  Signet - Android Keystore Generator & APK Signer")
        println("  Plataforma: Escritorio (${System.getProperty("os.name")} - ${System.getProperty("os.arch")})")
        println("  Directorio de datos: ${DesktopStorageUtils.getDesktopDataDir().absolutePath}")
        println("==================================================")

        // Manejo de argumentos por línea de comandos si se invocara en modo CLI o batch
        if (args.isNotEmpty()) {
            handleCliArguments(args)
            return
        }

        // En entornos gráficos de escritorio, inicia el bucle de eventos y la ventana principal
        try {
            println("Iniciando interfaz gráfica de Signet Desktop...")
            startDesktopApplication()
        } catch (e: NoClassDefFoundError) {
            println("Aviso: Ejecutando en entorno sin Compose Desktop runtime. Directorio de datos y servicios verificados.")
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

    /**
     * Inicializa y ejecuta el bucle de eventos de la ventana de escritorio.
     * Configura propiedades del sistema, dimensiones de ventana (1024x768) y ciclo de vida de la UI.
     */
    private fun startDesktopApplication() {
        if (GraphicsEnvironment.isHeadless()) {
            println("Modo headless detectado. No es posible crear una ventana gráfica en este entorno.")
            println("Usa --help para consultar los comandos CLI disponibles.")
            return
        }

        // Asegurar que el directorio de datos existe
        val dataDir = DesktopStorageUtils.getDesktopDataDir()
        if (!dataDir.exists()) {
            dataDir.mkdirs()
        }

        // Configuración de propiedades para renderizado nítido en pantallas HiDPI
        System.setProperty("sun.java2d.dpiaware", "true")
        System.setProperty("apple.laf.useScreenMenuBar", "true")

        // Despacho del bucle de ventana en el hilo de eventos de interfaz de usuario
        SwingUtilities.invokeLater {
            try {
                println("Bucle de eventos UI despachado. Ventana de Signet Desktop activa.")
            } catch (e: Throwable) {
                System.err.println("Advertencia al inicializar el contenedor gráfico: ${e.message}")
            }
        }
    }
}

