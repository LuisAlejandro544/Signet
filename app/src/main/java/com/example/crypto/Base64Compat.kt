package com.example.crypto

import java.util.Base64

/**
 * Adaptador universal de codificación y decodificación Base64 compatible tanto con
 * Android (API 26+) como con entornos Desktop JVM (Windows, Linux, macOS).
 *
 * Sustituye de forma transparente la dependencia exclusiva de android.util.Base64.
 */
object Base64Compat {

    /**
     * Codifica un array de bytes a una cadena Base64.
     * @param noWrap Si es true, produce una cadena continua sin saltos de línea (equivalente a Base64.NO_WRAP).
     *               Si es false, genera una salida con saltos cada 76 caracteres (equivalente a Base64.DEFAULT).
     */
    fun encodeToString(bytes: ByteArray, noWrap: Boolean = true): String {
        return if (noWrap) {
            Base64.getEncoder().encodeToString(bytes)
        } else {
            Base64.getMimeEncoder().encodeToString(bytes)
        }
    }

    /**
     * Decodifica una cadena Base64 a su representación en bytes.
     * Es tolerante a espacios, saltos de línea (\r, \n) y formatos MIME.
     */
    fun decode(base64String: String): ByteArray {
        val sanitized = base64String.trim().replace("\r", "").replace("\n", "").replace(" ", "")
        return try {
            Base64.getDecoder().decode(sanitized)
        } catch (_: Exception) {
            Base64.getMimeDecoder().decode(sanitized)
        }
    }

    /**
     * Decodifica un array de bytes Base64 a bytes sin procesar.
     */
    fun decode(bytes: ByteArray): ByteArray {
        return Base64.getDecoder().decode(bytes)
    }
}
