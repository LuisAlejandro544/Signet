package com.example.crypto.backup

import com.example.data.model.KeystoreDetails

/**
 * High-level facade for Signet backup anti-tampering verification and manifest signing.
 * Delegates specialized cryptography and parsing tasks to [HmacSignatureEngine],
 * [SignetManifestParser], and [SignetVaultManifestParser].
 */
object BackupIntegrityVerifier {

    const val APP_SIGNATURE_NAME = HmacSignatureEngine.APP_SIGNATURE_NAME
    const val BACKUP_FORMAT_VERSION = HmacSignatureEngine.BACKUP_FORMAT_VERSION
    const val VAULT_FORMAT_VERSION = HmacSignatureEngine.VAULT_FORMAT_VERSION
    const val MANIFEST_FILE_NAME = HmacSignatureEngine.MANIFEST_FILE_NAME
    const val VAULT_MANIFEST_FILE_NAME = HmacSignatureEngine.VAULT_MANIFEST_FILE_NAME

    /**
     * Calculates the SHA-256 hex string of a byte array.
     */
    fun calculateSha256(bytes: ByteArray): String {
        return HmacSignatureEngine.calculateSha256(bytes)
    }

    /**
     * Builds and signs the Master JSON manifest for a Multi-Keystore Vault ZIP bundle.
     */
    fun buildSignedVaultManifest(
        items: List<VaultKeystoreEntry>,
        vaultCreatedAt: Long
    ): String {
        return SignetVaultManifestParser.buildSignedVaultManifest(items, vaultCreatedAt)
    }

    /**
     * Verifies the integrity of a Master Vault Backup manifest and all contained keystore binaries.
     */
    fun verifyVaultManifestAndIntegrity(
        manifestJsonString: String,
        keystoresMap: Map<String, ByteArray>
    ): List<ManifestData> {
        return SignetVaultManifestParser.verifyVaultManifestAndIntegrity(manifestJsonString, keystoresMap)
    }

    /**
     * Computes Master HMAC-SHA256 across all entries in the Vault.
     */
    fun computeVaultMasterHmac(items: List<VaultKeystoreEntry>, vaultCreatedAt: Long): String {
        return HmacSignatureEngine.computeVaultMasterHmac(items, vaultCreatedAt)
    }

    /**
     * Builds and signs the JSON manifest for a single ZIP backup bundle.
     */
    fun buildSignedManifest(
        details: KeystoreDetails,
        keystoreFileName: String,
        keystoreSha256: String
    ): String {
        return SignetManifestParser.buildSignedManifest(details, keystoreFileName, keystoreSha256)
    }

    /**
     * Verifies that the JSON manifest has not been tampered with and matches the keystore byte array.
     */
    fun verifyManifestAndKeystoreIntegrity(
        manifestJsonString: String,
        keystoreBytes: ByteArray
    ): ManifestData {
        return SignetManifestParser.verifyManifestAndKeystoreIntegrity(manifestJsonString, keystoreBytes)
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
        return HmacSignatureEngine.computeManifestHmac(
            keystoreFileName = keystoreFileName,
            alias = alias,
            storePassword = storePassword,
            keyPassword = keyPassword,
            sha256Fingerprint = sha256Fingerprint,
            sha1Fingerprint = sha1Fingerprint,
            validFrom = validFrom,
            validUntil = validUntil,
            createdAt = createdAt,
            keystoreSha256 = keystoreSha256
        )
    }

    data class VaultKeystoreEntry(
        val folderName: String,
        val keystoreFileName: String,
        val keystoreSha256: String,
        val details: KeystoreDetails
    )

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
