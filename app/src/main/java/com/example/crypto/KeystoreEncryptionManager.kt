package com.example.crypto

import java.io.File
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Gestor de cifrado y descifrado en reposo para credenciales de Keystores en Signet.
 *
 * Protege contraseñas mediante AES-256-GCM respaldado por AndroidKeyStore en Android,
 * o almacenamiento seguro de clave maestra en %APPDATA%/Signet/signet_master.key en Windows/Desktop.
 */
object KeystoreEncryptionManager {

    private const val ANDROID_KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val KEY_ALIAS = "signet_credentials_master_key"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH = 128
    private const val IV_LENGTH = 12
    const val ENC_PREFIX = "enc:v1:"

    // Archivo de clave maestra para entornos de escritorio (Windows / Linux / macOS)
    private val desktopKeyFile: File by lazy {
        File(DesktopStorageUtils.getDesktopDataDir(), "signet_master.key")
    }

    // Clave de respaldo para tests y entornos efímeros donde el almacenamiento no está disponible
    private val fallbackSecretKey: SecretKey by lazy {
        val fallbackSeed = "Signet_Master_Entropy_Key_Repose_2026_Secure_Sign_Seed".toByteArray(Charsets.UTF_8)
        val digest = java.security.MessageDigest.getInstance("SHA-256").digest(fallbackSeed)
        SecretKeySpec(digest, "AES")
    }

    private val secureRandom = SecureRandom()

    private fun getSecretKey(): SecretKey {
        // 1. Intentar AndroidKeyStore si estamos en plataforma Android
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE_PROVIDER)
            keyStore.load(null)
            if (keyStore.containsAlias(KEY_ALIAS)) {
                return keyStore.getKey(KEY_ALIAS, null) as SecretKey
            } else {
                // Generar dinámicamente en AndroidKeyStore
                val kgClass = Class.forName("javax.crypto.KeyGenerator")
                val keyGen = kgClass.getMethod("getInstance", String::class.java, String::class.java)
                    .invoke(null, "AES", ANDROID_KEYSTORE_PROVIDER) as KeyGenerator
                val builderClass = Class.forName("android.security.keystore.KeyGenParameterSpec\$Builder")
                val builderCtor = builderClass.getConstructor(String::class.java, Int::class.javaPrimitiveType)
                val builder = builderCtor.newInstance(KEY_ALIAS, 3) // PURPOSE_ENCRYPT (1) | PURPOSE_DECRYPT (2) = 3
                builderClass.getMethod("setBlockModes", Array<String>::class.java).invoke(builder, arrayOf("GCM"))
                builderClass.getMethod("setEncryptionPaddings", Array<String>::class.java).invoke(builder, arrayOf("NoPadding"))
                builderClass.getMethod("setKeySize", Int::class.javaPrimitiveType).invoke(builder, 256)
                val spec = builderClass.getMethod("build").invoke(builder)
                kgClass.getMethod("init", java.security.spec.AlgorithmParameterSpec::class.java).invoke(keyGen, spec)
                return kgClass.getMethod("generateKey").invoke(keyGen) as SecretKey
            }
        } catch (_: Throwable) {
            // AndroidKeyStore no disponible -> continuar a modo Escritorio / Windows
        }

        // 2. Modo Escritorio (Windows / Linux / macOS): Clave persistente en directorio de datos
        try {
            val file = desktopKeyFile
            if (file.exists() && file.length() == 32L) {
                val keyBytes = file.readBytes()
                return SecretKeySpec(keyBytes, "AES")
            } else {
                val keyGen = KeyGenerator.getInstance("AES")
                keyGen.init(256, secureRandom)
                val secretKey = keyGen.generateKey()
                try {
                    file.parentFile?.mkdirs()
                    file.writeBytes(secretKey.encoded)
                } catch (_: Exception) {
                    // Si el sistema de archivos es de solo lectura, continuar a fallback
                }
                return secretKey
            }
        } catch (_: Throwable) {
            // Error de I/O en escritorio -> usar fallback
        }

        return fallbackSecretKey
    }

    /**
     * Cifra un texto plano usando AES-256-GCM.
     * Retorna el texto con prefijo `enc:v1:<base64(iv + ciphertext)>`.
     */
    fun encrypt(plainText: String?): String {
        if (plainText.isNullOrEmpty()) return plainText ?: ""
        if (plainText.startsWith(ENC_PREFIX)) return plainText // Ya está cifrado

        return try {
            val key = getSecretKey()
            val iv = ByteArray(IV_LENGTH)
            secureRandom.nextBytes(iv)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.ENCRYPT_MODE, key, spec)

            val cipherBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            val combined = ByteArray(iv.size + cipherBytes.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(cipherBytes, 0, combined, iv.size, cipherBytes.size)

            val base64Encoded = Base64Compat.encodeToString(combined, noWrap = true)
            "$ENC_PREFIX$base64Encoded"
        } catch (_: Exception) {
            // En caso extremo de fallo, no corromper la información
            plainText
        }
    }

    /**
     * Descifra un texto cifrado con prefijo `enc:v1:`.
     * Si no tiene el prefijo, lo devuelve directamente (compatibilidad con datos previos en plano).
     */
    fun decrypt(cipherText: String?): String {
        if (cipherText.isNullOrEmpty()) return cipherText ?: ""
        if (!cipherText.startsWith(ENC_PREFIX)) return cipherText // Texto plano heredado

        return try {
            val base64Payload = cipherText.removePrefix(ENC_PREFIX)
            val combined = Base64Compat.decode(base64Payload)

            if (combined.size < IV_LENGTH) return cipherText

            val iv = ByteArray(IV_LENGTH)
            val cipherBytes = ByteArray(combined.size - IV_LENGTH)
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH)
            System.arraycopy(combined, IV_LENGTH, cipherBytes, 0, cipherBytes.size)

            val key = getSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, spec)

            val decryptedBytes = cipher.doFinal(cipherBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (_: Exception) {
            // Si la clave no coincide o fue manipulado, retornar el texto tal cual
            cipherText
        }
    }

    /**
     * Comprueba si una cadena está cifrada con el esquema de Signet.
     */
    fun isEncrypted(value: String?): Boolean {
        return value?.startsWith(ENC_PREFIX) == true
    }
}
