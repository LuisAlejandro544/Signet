package com.example.crypto.backup

import com.example.data.model.KeystoreDetails
import org.json.JSONObject
import java.security.MessageDigest
import java.util.Locale
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object BackupIntegrityVerifier {

    const val APP_SIGNATURE_NAME = "Signet"
    const val BACKUP_FORMAT_VERSION = 1
    const val MANIFEST_FILE_NAME = "signet-backup.json"

    // Cryptographic HMAC key seed for Signet anti-tamper integrity verification
    private val HMAC_SECRET_SEED = "SIGNET_ANTI_TAMPER_KEYSTORE_INTEGRITY_SECRET_V1_2026".toByteArray(Charsets.UTF_8)

    /**
     * Calculates the SHA-256 hex string of a byte array.
     */
    fun calculateSha256(bytes: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.joinToString("") { "%02X".format(it) }
    }

    /**
     * Builds and signs the JSON manifest for the ZIP backup bundle.
     */
    fun buildSignedManifest(
        details: KeystoreDetails,
        keystoreFileName: String,
        keystoreSha256: String
    ): String {
        val createdAt = if (details.createdAt > 0) details.createdAt else System.currentTimeMillis()

        val signature = computeManifestHmac(
            keystoreFileName = keystoreFileName,
            alias = details.alias,
            storePassword = details.storePassword,
            keyPassword = details.keyPassword,
            sha256Fingerprint = details.sha256Fingerprint,
            sha1Fingerprint = details.sha1Fingerprint,
            validFrom = details.validFrom,
            validUntil = details.validUntil,
            createdAt = createdAt,
            keystoreSha256 = keystoreSha256
        )

        val json = JSONObject()
        json.put("appName", APP_SIGNATURE_NAME)
        json.put("formatVersion", BACKUP_FORMAT_VERSION)
        json.put("createdAt", createdAt)
        json.put("keystoreFileName", keystoreFileName)
        json.put("alias", details.alias)
        json.put("storePassword", details.storePassword)
        json.put("keyPassword", details.keyPassword)
        json.put("algorithm", details.algorithm)
        json.put("sha256Fingerprint", details.sha256Fingerprint)
        json.put("sha1Fingerprint", details.sha1Fingerprint)
        json.put("md5Fingerprint", details.md5Fingerprint)
        json.put("validFrom", details.validFrom)
        json.put("validUntil", details.validUntil)
        json.put("subjectDn", details.subjectDn)
        json.put("issuerDn", details.issuerDn)
        json.put("serialNumber", details.serialNumber)
        json.put("certificatePem", details.certificatePem)
        json.put("keystoreSha256", keystoreSha256)
        json.put("signature", signature)

        return json.toString(2)
    }

    /**
     * Verifies that the JSON manifest has not been tampered with and matches the keystore byte array.
     */
    fun verifyManifestAndKeystoreIntegrity(
        manifestJsonString: String,
        keystoreBytes: ByteArray
    ): ManifestData {
        val json = try {
            JSONObject(manifestJsonString)
        } catch (e: Exception) {
            throw SecurityException(
                "El manifiesto de respaldo está corrupto o tiene un formato no válido: ${e.message}"
            )
        }

        val appName = json.optString("appName", "")
        if (appName != APP_SIGNATURE_NAME) {
            throw SecurityException(
                "Firma de aplicación inválida. El manifiesto no pertenece a $APP_SIGNATURE_NAME."
            )
        }

        val version = json.optInt("formatVersion", 0)
        if (version != BACKUP_FORMAT_VERSION) {
            throw SecurityException(
                "Versión de formato de respaldo no soportada (Versión $version)."
            )
        }

        val keystoreFileName = json.optString("keystoreFileName", "restored-key.jks")
        val alias = json.optString("alias", "")
        val storePassword = json.optString("storePassword", "")
        val keyPassword = json.optString("keyPassword", "")
        val algorithm = json.optString("algorithm", "RSA 2048 bits")
        val sha256Fingerprint = json.optString("sha256Fingerprint", "")
        val sha1Fingerprint = json.optString("sha1Fingerprint", "")
        val md5Fingerprint = json.optString("md5Fingerprint", "")
        val validFrom = json.optLong("validFrom", 0L)
        val validUntil = json.optLong("validUntil", 0L)
        val createdAt = json.optLong("createdAt", System.currentTimeMillis())
        val subjectDn = json.optString("subjectDn", "CN=Android App")
        val issuerDn = json.optString("issuerDn", "CN=Android App")
        val serialNumber = json.optString("serialNumber", "")
        val certificatePem = json.optString("certificatePem", "")
        val manifestKeystoreSha256 = json.optString("keystoreSha256", "")
        val providedSignature = json.optString("signature", "")

        // 1. Verify Keystore bytes integrity against manifest hash
        val computedKeystoreSha256 = calculateSha256(keystoreBytes)
        if (!manifestKeystoreSha256.equals(computedKeystoreSha256, ignoreCase = true)) {
            throw SecurityException(
                "Alerta de seguridad: El archivo de keystore dentro del ZIP fue alterado o no coincide con la huella registrada en el manifiesto."
            )
        }

        // 2. Verify Cryptographic Signature of Manifest
        val expectedSignature = computeManifestHmac(
            keystoreFileName = keystoreFileName,
            alias = alias,
            storePassword = storePassword,
            keyPassword = keyPassword,
            sha256Fingerprint = sha256Fingerprint,
            sha1Fingerprint = sha1Fingerprint,
            validFrom = validFrom,
            validUntil = validUntil,
            createdAt = createdAt,
            keystoreSha256 = computedKeystoreSha256
        )

        if (!MessageDigest.isEqual(providedSignature.toByteArray(Charsets.UTF_8), expectedSignature.toByteArray(Charsets.UTF_8))) {
            throw SecurityException(
                "Firma criptográfica de Signet inválida o paquete alterado. El archivo de respaldo ha sido modificado y no puede ser restaurado por motivos de seguridad."
            )
        }

        return ManifestData(
            keystoreFileName = keystoreFileName,
            alias = alias,
            storePassword = storePassword,
            keyPassword = keyPassword,
            algorithm = algorithm,
            sha256Fingerprint = sha256Fingerprint,
            sha1Fingerprint = sha1Fingerprint,
            md5Fingerprint = md5Fingerprint,
            validFrom = validFrom,
            validUntil = validUntil,
            createdAt = createdAt,
            subjectDn = subjectDn,
            issuerDn = issuerDn,
            serialNumber = serialNumber,
            certificatePem = certificatePem
        )
    }

    /**
     * Computes the HMAC-SHA256 signature for manifest verification.
     */
    fun computeManifestHmac(
        keystoreFileName: String,
        alias: String,
        storePassword: String,
        keyPassword: String,
        sha256Fingerprint: String,
        sha1Fingerprint: String,
        validFrom: Long,
        validUntil: Long,
        createdAt: Long,
        keystoreSha256: String
    ): String {
        val canonicalPayload = listOf(
            "SIGNET_KEYSTORE_BACKUP_V1",
            keystoreFileName.trim(),
            alias.trim(),
            storePassword,
            keyPassword,
            sha256Fingerprint.trim(),
            sha1Fingerprint.trim(),
            validFrom.toString(),
            validUntil.toString(),
            createdAt.toString(),
            keystoreSha256.trim().uppercase(Locale.ROOT)
        ).joinToString("|")

        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(HMAC_SECRET_SEED, "HmacSHA256"))
        val hmacBytes = mac.doFinal(canonicalPayload.toByteArray(Charsets.UTF_8))
        return hmacBytes.joinToString("") { "%02X".format(it) }
    }

    data class ManifestData(
        val keystoreFileName: String,
        val alias: String,
        val storePassword: String,
        val keyPassword: String,
        val algorithm: String,
        val sha256Fingerprint: String,
        val sha1Fingerprint: String,
        val md5Fingerprint: String,
        val validFrom: Long,
        val validUntil: Long,
        val createdAt: Long,
        val subjectDn: String,
        val issuerDn: String,
        val serialNumber: String,
        val certificatePem: String
    )
}
