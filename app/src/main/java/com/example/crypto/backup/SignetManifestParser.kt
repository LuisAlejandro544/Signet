package com.example.crypto.backup

import com.example.data.model.KeystoreDetails
import org.json.JSONObject

/**
 * Parser and builder for single keystore JSON manifests (signet-backup.json).
 */
object SignetManifestParser {

    /**
     * Builds and signs the JSON manifest for the ZIP backup bundle.
     */
    fun buildSignedManifest(
        details: KeystoreDetails,
        keystoreFileName: String,
        keystoreSha256: String
    ): String {
        val createdAt = if (details.createdAt > 0) details.createdAt else System.currentTimeMillis()

        val signature = HmacSignatureEngine.computeManifestHmac(
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
        json.put("appName", HmacSignatureEngine.APP_SIGNATURE_NAME)
        json.put("formatVersion", HmacSignatureEngine.BACKUP_FORMAT_VERSION)
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
    ): BackupIntegrityVerifier.ManifestData {
        val json = try {
            JSONObject(manifestJsonString)
        } catch (e: Exception) {
            throw SecurityException(
                "El manifiesto de respaldo está corrupto o tiene un formato no válido: ${e.message}"
            )
        }

        val appName = json.optString("appName", "")
        if (appName != HmacSignatureEngine.APP_SIGNATURE_NAME) {
            throw SecurityException(
                "Firma de aplicación inválida. El manifiesto no pertenece a ${HmacSignatureEngine.APP_SIGNATURE_NAME}."
            )
        }

        val version = json.optInt("formatVersion", 0)
        if (version != HmacSignatureEngine.BACKUP_FORMAT_VERSION) {
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
        val computedKeystoreSha256 = HmacSignatureEngine.calculateSha256(keystoreBytes)
        if (!manifestKeystoreSha256.equals(computedKeystoreSha256, ignoreCase = true)) {
            throw SecurityException(
                "Alerta de seguridad: El archivo de keystore dentro del ZIP fue alterado o no coincide con la huella registrada en el manifiesto."
            )
        }

        // 2. Verify Cryptographic Signature of Manifest
        val expectedSignature = HmacSignatureEngine.computeManifestHmac(
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

        if (!HmacSignatureEngine.verifySignatureMatch(providedSignature, expectedSignature)) {
            throw SecurityException(
                "Firma criptográfica de Signet inválida o paquete alterado. El archivo de respaldo ha sido modificado y no puede ser restaurado por motivos de seguridad."
            )
        }

        return BackupIntegrityVerifier.ManifestData(
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
}
