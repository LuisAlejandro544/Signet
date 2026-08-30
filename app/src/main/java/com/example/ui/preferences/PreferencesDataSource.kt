package com.example.ui.preferences

import android.content.Context
import android.content.SharedPreferences
import com.example.crypto.DesktopStorageUtils
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Properties

/**
 * Abstracción de persistencia de preferencias clave-valor multiplataforma.
 * Permite ejecutar Signet indistintamente en Android (SharedPreferences)
 * o en Windows / Desktop (archivo properties en %APPDATA%/Signet).
 */
interface PreferencesDataSource {
    fun getString(key: String, defValue: String): String
    fun putString(key: String, value: String)
    fun getBoolean(key: String, defValue: Boolean): Boolean
    fun putBoolean(key: String, value: Boolean)
}

/**
 * Implementación nativa para Android respaldada por SharedPreferences.
 */
class AndroidPreferencesDataSource(context: Context, prefsName: String) : PreferencesDataSource {
    private val prefs: SharedPreferences = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

    override fun getString(key: String, defValue: String): String {
        return prefs.getString(key, defValue) ?: defValue
    }

    override fun putString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    override fun getBoolean(key: String, defValue: Boolean): Boolean {
        return prefs.getBoolean(key, defValue)
    }

    override fun putBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }
}

/**
 * Implementación de escritorio respaldada por un archivo .properties estándar en %APPDATA%/Signet.
 */
class DesktopPreferencesDataSource(
    private val configFile: File = File(DesktopStorageUtils.getDesktopDataDir(), "signet_preferences.properties")
) : PreferencesDataSource {

    private val properties = Properties()

    init {
        load()
    }

    @Synchronized
    private fun load() {
        if (configFile.exists()) {
            try {
                FileInputStream(configFile).use { fis ->
                    properties.load(fis)
                }
            } catch (_: Exception) {
                // Si ocurre error de lectura, mantener propiedades vacías
            }
        }
    }

    @Synchronized
    private fun save() {
        try {
            configFile.parentFile?.mkdirs()
            FileOutputStream(configFile).use { fos ->
                properties.store(fos, "Signet Desktop User Preferences")
            }
        } catch (_: Exception) {
            // Ignorar fallos de escritura en entornos de solo lectura
        }
    }

    override fun getString(key: String, defValue: String): String {
        return properties.getProperty(key, defValue)
    }

    override fun putString(key: String, value: String) {
        properties.setProperty(key, value)
        save()
    }

    override fun getBoolean(key: String, defValue: Boolean): Boolean {
        val str = properties.getProperty(key) ?: return defValue
        return str.toBoolean()
    }

    override fun putBoolean(key: String, value: Boolean) {
        properties.setProperty(key, value.toString())
        save()
    }
}
