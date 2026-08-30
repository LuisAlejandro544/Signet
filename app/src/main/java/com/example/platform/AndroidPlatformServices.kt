package com.example.platform

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

/**
 * Implementación de PlatformServices para el sistema operativo Android.
 * Conecta las acciones con Context, Toasts, Intents y FileProvider.
 */
class AndroidPlatformServices(private val context: Context) : PlatformServices {

    override val platformName: String = "Android"
    override val isDesktop: Boolean = false

    override fun pickFile(
        title: String,
        allowedExtensions: List<String>,
        onResult: (PlatformFile?) -> Unit
    ) {
        // En Android, los selectores visuales se gestionan de forma reactiva con ActivityResultLauncher
        // en la capa Compose (ver rememberPlatformFilePicker).
        onResult(null)
    }

    override fun saveFile(
        defaultFileName: String,
        mimeType: String,
        bytes: ByteArray,
        onResult: (Result<File>) -> Unit
    ) {
        // En Android, el guardado interactivo se realiza mediante ActivityResultContracts.CreateDocument
        // o guardado directo en caché/almacenamiento local.
        try {
            val target = File(context.filesDir, defaultFileName)
            target.writeBytes(bytes)
            onResult(Result.success(target))
        } catch (e: Exception) {
            onResult(Result.failure(e))
        }
    }

    override fun showToast(message: String, isLong: Boolean) {
        Toast.makeText(
            context,
            message,
            if (isLong) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
        ).show()
    }

    override fun copyToClipboard(label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard?.setPrimaryClip(clip)
        showToast("Copiado al portapapeles")
    }

    override fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            showToast("No se pudo abrir el navegador: ${e.localizedMessage}")
        }
    }

    override fun openFolder(file: File) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.fromFile(file), "*/*")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    override fun installApk(apkFile: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            showToast("No se pudo iniciar el instalador de Android: ${e.localizedMessage}")
        }
    }
}
