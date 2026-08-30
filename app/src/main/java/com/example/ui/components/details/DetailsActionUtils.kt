package com.example.ui.components.details

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PersistableBundle
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.KeystoreDetails
import java.io.File

object DetailsActionUtils {

    fun copyToClipboard(
        context: Context,
        label: String,
        text: String,
        isSensitive: Boolean = false
    ) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)

        // Flag oficial de Android 13+ para evitar previsualizaciones inseguras de contraseñas y secretos
        if (isSensitive && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            clip.description.extras = PersistableBundle().apply {
                putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
            }
        }

        clipboard.setPrimaryClip(clip)

        val message = if (isSensitive) {
            "$label copiado. ⚠️ Precaución: otras aplicaciones con acceso al portapapeles podrían leerlo."
        } else {
            "$label copiado al portapapeles"
        }
        Toast.makeText(context, message, if (isSensitive) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
    }

    fun shareKeystoreFile(context: Context, details: KeystoreDetails) {
        try {
            val file = File(details.filePath)
            if (!file.exists()) {
                Toast.makeText(context, "El archivo original ya no está en el almacenamiento local.", Toast.LENGTH_SHORT).show()
                return
            }
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/octet-stream"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, details.fileName)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Compartir Keystore"))
        } catch (e: Exception) {
            Toast.makeText(context, "Error al compartir archivo: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}

