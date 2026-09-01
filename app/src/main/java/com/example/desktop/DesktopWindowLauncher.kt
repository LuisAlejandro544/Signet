package com.example.desktop

import com.example.crypto.DesktopStorageUtils
import java.awt.Dimension
import java.awt.GraphicsEnvironment
import javax.swing.JFrame
import javax.swing.SwingUtilities
import javax.swing.UIManager

/**
 * Inicializador del ciclo de vida gráfico de la aplicación de escritorio.
 * Configura propiedades del sistema, dimensiones de ventana (1150x780) y contenedor Swing/Compose.
 */
object DesktopWindowLauncher {

    fun startApplication() {
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
        System.setProperty("sun.java2d.uiScale.enabled", "true")
        System.setProperty("apple.laf.useScreenMenuBar", "true")

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
        } catch (_: Exception) {}

        // Despacho del bucle de ventana en el hilo de eventos de interfaz de usuario
        SwingUtilities.invokeLater {
            try {
                val frame = JFrame("Signet - Android Keystore & Certificate Tool")
                frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
                frame.minimumSize = Dimension(1000, 700)
                frame.preferredSize = Dimension(1150, 780)
                frame.setSize(1150, 780)
                frame.setLocationRelativeTo(null)

                // Cargar el icono de la ventana (candado criptográfico firmado)
                try {
                    val iconStream = DesktopLauncher::class.java.getResourceAsStream("/app_icon.png")
                        ?: DesktopLauncher::class.java.getResourceAsStream("/icon-256.png")
                    if (iconStream != null) {
                        val iconImage = javax.imageio.ImageIO.read(iconStream)
                        if (iconImage != null) {
                            frame.iconImage = iconImage
                        }
                    }
                } catch (_: Exception) {}

                frame.isVisible = true
                println("Bucle de eventos UI despachado. Ventana de Signet Desktop activa con icono del sistema.")
            } catch (e: Throwable) {
                System.err.println("Advertencia al inicializar el contenedor gráfico: ${e.message}")
            }
        }
    }
}
