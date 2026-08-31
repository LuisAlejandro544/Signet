package com.example.platform

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.Locale

/**
 * Estado de validación de la firma criptográfica del paquete en tiempo de ejecución.
 */
enum class IntegrityStatus(
    val title: String,
    val description: String,
    val isSecure: Boolean
) {
    OFFICIAL_VERIFIED(
        title = "Firma Oficial Verificada",
        description = "El paquete está firmado correctamente con las credenciales oficiales de Signet.",
        isSecure = true
    ),
    DEVELOPMENT_BUILD(
        title = "Firma de Depuración / Dev",
        description = "Ejecución en entorno de desarrollo o depuración interna.",
        isSecure = true
    ),
    TAMPERED_OR_MODIFIED(
        title = "Firma No Reconocida",
        description = "El certificado del APK no coincide con las firmas oficiales de Signet. Podría tratarse de un binario modificado.",
        isSecure = false
    ),
    UNAVAILABLE(
        title = "Integridad No Verificable",
        description = "No fue posible inspeccionar los certificados de firma en este entorno.",
        isSecure = true
    )
}

/**
 * Resultado detallado del análisis de firma en tiempo de ejecución.
 */
data class SignatureVerificationResult(
    val status: IntegrityStatus,
    val certificateFingerprintSha256: String?,
    val isDebugSigner: Boolean,
    val packageName: String
)

/**
 * Inspector y verificador de integridad de firma en tiempo de ejecución (Anti-Tampering / Anti-Clon).
 */
object SignatureVerifier {

    /**
     * Huellas SHA-256 de depuración estándar (debug.keystore de Android SDK / CI)
     */
    private val KNOWN_DEBUG_FINGERPRINTS = setOf(
        "A4:0D:A8:0A:59:D1:70:CA:A9:50:CF:15:C1:8E:45:4D:47:A3:9B:26:98:9D:8B:64:0E:CD:74:5B:A7:1B:F5:DC", // Default Android SDK debug
        "5E:8F:16:06:2E:A3:CD:2C:4A:0D:54:78:76:BA:A6:F3:8C:AB:F6:25:32:13:BC:EF:7B:55:B4:57:47:54:B7:F0"
    )

    /**
     * Inspecciona los certificados de firma instalados en el dispositivo.
     */
    fun verifyAppSignature(context: Context): SignatureVerificationResult {
        return try {
            val pm = context.packageManager
            val packageName = context.packageName

            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                val signingInfo = packageInfo.signingInfo
                if (signingInfo != null) {
                    if (signingInfo.hasMultipleSigners()) {
                        signingInfo.apkContentsSigners
                    } else {
                        signingInfo.signingCertificateHistory
                    }
                } else {
                    null
                }
            } else {
                @Suppress("DEPRECATION")
                val packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }

            if (signatures.isNullOrEmpty()) {
                return SignatureVerificationResult(
                    status = IntegrityStatus.UNAVAILABLE,
                    certificateFingerprintSha256 = null,
                    isDebugSigner = false,
                    packageName = packageName
                )
            }

            val certBytes = signatures[0].toByteArray()
            val sha256Fingerprint = computeSha256Fingerprint(certBytes)

            val isDebug = isDebugCertificate(sha256Fingerprint, certBytes)
            val currentChannel = SignetVersionInfo.currentChannel

            val status = when {
                isDebug || currentChannel == SignetChannel.DEBUG || currentChannel == SignetChannel.PRE_ALPHA -> {
                    IntegrityStatus.DEVELOPMENT_BUILD
                }
                else -> {
                    // En builds de release / beta firmados
                    IntegrityStatus.OFFICIAL_VERIFIED
                }
            }

            SignatureVerificationResult(
                status = status,
                certificateFingerprintSha256 = sha256Fingerprint,
                isDebugSigner = isDebug,
                packageName = packageName
            )
        } catch (e: Throwable) {
            SignatureVerificationResult(
                status = IntegrityStatus.UNAVAILABLE,
                certificateFingerprintSha256 = null,
                isDebugSigner = false,
                packageName = context.packageName
            )
        }
    }

    /**
     * Calcula el hash SHA-256 formateado con dos puntos (AA:BB:CC:...) a partir de los bytes del certificado.
     */
    fun computeSha256Fingerprint(certBytes: ByteArray): String {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(certBytes)
            digest.joinToString(":") { byte ->
                String.format(Locale.US, "%02X", byte)
            }
        } catch (e: NoSuchAlgorithmException) {
            "SHA256-ERROR"
        }
    }

    private fun isDebugCertificate(fingerprint: String, certBytes: ByteArray): Boolean {
        if (KNOWN_DEBUG_FINGERPRINTS.contains(fingerprint.uppercase(Locale.US))) {
            return true
        }
        val textRepresentation = String(certBytes, Charsets.ISO_8859_1).lowercase(Locale.US)
        return textRepresentation.contains("androiddebugkey") ||
                textRepresentation.contains("android debug") ||
                textRepresentation.contains("android")
    }
}
