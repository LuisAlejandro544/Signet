package com.example.crypto.backup

import java.security.MessageDigest
import java.util.Locale
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Specialized cryptographic engine for SHA-256 calculation and HMAC-SHA256 anti-tamper signing.
 */
object HmacSignatureEngine {

    const val APP_SIGNATURE_NAME = "Signet"
    const val BACKUP_FORMAT_VERSION = 1
    const val VAULT_FORMAT_VERSION = 1
    const val MANIFEST_FILE_NAME = "signet-backup.json"
    const val VAULT_MANIFEST_FILE_NAME = "signet-vault-backup.json"

    // Cryptographic HMAC key seeds for Signet anti-tamper integrity verification
    private val HMAC_SECRET_SEED = "SIGNET_ANTI_TAMPER_KEYSTORE_INTEGRITY_SECRET_V1_2026".toByteArray(Charsets.UTF_8)
    private val VAULT_HMAC_SECRET_SEED = "SIGNET_VAULT_ANTI_TAMPER_INTEGRITY_SECRET_V1_2026".toByteArray(Charsets.UTF_8)

    /**
     * Calculates the SHA-256 hex string of a byte array.
     */
    fun calculateSha256(bytes: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.joinToString("") { "%02X".format(it) }
    }

    /**
     * Computes the HMAC-SHA256 signature for single keystore manifest verification.
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

    /**
     * Computes Master HMAC-SHA256 across all entries in the Vault.
     */
    fun computeVaultMasterHmac(items: List<BackupIntegrityVerifier.VaultKeystoreEntry>, vaultCreatedAt: Long): String {
        val canonicalParts = mutableListOf<String>()
        canonicalParts.add("SIGNET_VAULT_BACKUP_V1")
        canonicalParts.add(vaultCreatedAt.toString())
        canonicalParts.add(items.size.toString())

        for (item in items) {
            canonicalParts.add(
                listOf(
                    item.folderName.trim(),
                    item.keystoreFileName.trim(),
                    item.details.alias.trim(),
                    item.details.storePassword,
                    item.details.keyPassword,
                    item.details.sha256Fingerprint.trim(),
                    item.keystoreSha256.trim().uppercase(Locale.ROOT)
                ).joinToString(":")
            )
        }

        val payload = canonicalParts.joinToString("|")
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(VAULT_HMAC_SECRET_SEED, "HmacSHA256"))
        val hmacBytes = mac.doFinal(payload.toByteArray(Charsets.UTF_8))
        return hmacBytes.joinToString("") { "%02X".format(it) }
    }

    /**
     * Verifies constant-time equality between two signatures.
     */
    fun verifySignatureMatch(expectedSignature: String, actualSignature: String): Boolean {
        return MessageDigest.isEqual(
            expectedSignature.toByteArray(Charsets.UTF_8),
            actualSignature.toByteArray(Charsets.UTF_8)
        )
    }
}
