package com.example.crypto.x509

import com.example.crypto.Base64Compat
import java.security.MessageDigest

/**
 * Utility functions for cryptographic hashing, fingerprint formatting, and PEM conversions.
 */
object X509CertificateUtils {

    /**
     * Formats bytes to hexadecimal colon-separated fingerprint: AA:BB:CC:...
     */
    fun calculateFingerprint(bytes: ByteArray, algorithm: String): String {
        val md = MessageDigest.getInstance(algorithm)
        val digest = md.digest(bytes)
        return digest.joinToString(":") { byte -> "%02X".format(byte) }
    }

    /**
     * Formats X.509 certificate binary bytes into standard RFC 7468 PEM format.
     */
    fun buildPemCertificate(certBytes: ByteArray): String {
        val base64 = Base64Compat.encodeToString(certBytes, noWrap = true)
        val chunks = base64.chunked(64).joinToString("\n")
        return "-----BEGIN CERTIFICATE-----\n$chunks\n-----END CERTIFICATE-----"
    }
}
