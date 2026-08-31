package com.example.crypto

import com.example.crypto.backup.BackupIntegrityVerifier
import com.example.crypto.backup.VaultRestorationCoordinator
import com.example.crypto.backup.ZipPackageBuilder
import com.example.crypto.backup.ZipPackageExtractor
import com.example.data.model.KeystoreDetails
import java.io.File

/**
 * High-level manager orchestrating backup ZIP creation and intelligent restoration for Signet.
 * Delegates low-level ZIP packaging and extraction to [ZipPackageBuilder] and [ZipPackageExtractor],
 * cryptographic validation to [BackupIntegrityVerifier], and file restoration to [VaultRestorationCoordinator].
 */
object SignetBackupManager {

    /**
     * Creates a complete, signed ZIP backup containing the keystore file, signed manifest,
     * credentials, key.properties, base64 string, and instructions.
     */
    fun createBackupZip(
        details: KeystoreDetails,
        keystoreBytes: ByteArray
    ): ByteArray {
        return ZipPackageBuilder.createBackupZip(details, keystoreBytes)
    }

    /**
     * Creates a complete Master Vault ZIP backup containing all saved keystores organized into
     * dedicated subfolders, a signed master manifest, and an inventory summary.
     */
    fun createVaultBackupZip(
        items: List<Pair<KeystoreDetails, ByteArray>>
    ): ByteArray {
        return ZipPackageBuilder.createVaultBackupZip(items)
    }

    /**
     * Restores a Vault backup containing multiple keystores in folders, verifying the master anti-tamper signature.
     */
    fun restoreVaultFromZip(outputDir: File, zipBytes: ByteArray): List<KeystoreDetails> {
        val extracted = ZipPackageExtractor.extractEntries(zipBytes)

        if (extracted.manifestJsonString.isNullOrBlank()) {
            throw SecurityException("El archivo ZIP no contiene el manifiesto maestro de bóveda (${BackupIntegrityVerifier.VAULT_MANIFEST_FILE_NAME}).")
        }

        // Verify Vault master cryptographic signature & all binary hashes
        val verifiedManifests = BackupIntegrityVerifier.verifyVaultManifestAndIntegrity(
            manifestJsonString = extracted.manifestJsonString,
            keystoresMap = extracted.binaryFilesMap
        )

        return VaultRestorationCoordinator.processAndPersistVaultEntries(
            outputDir = outputDir,
            verifiedManifests = verifiedManifests,
            binaryFilesMap = extracted.binaryFilesMap
        )
    }

    /**
     * Intelligently restores any Signet ZIP backup (Single Keystore or Multi-Keystore Vault).
     */
    fun restoreAnyFromZip(outputDir: File, zipBytes: ByteArray): List<KeystoreDetails> {
        val extracted = ZipPackageExtractor.extractEntries(zipBytes)

        return when {
            extracted.isVault -> restoreVaultFromZip(outputDir, zipBytes)
            extracted.hasSingleManifest -> listOf(restoreFromZip(outputDir, zipBytes))
            else -> throw SecurityException(
                "El archivo ZIP no contiene un manifiesto de respaldo válido de Signet (${BackupIntegrityVerifier.MANIFEST_FILE_NAME} o ${BackupIntegrityVerifier.VAULT_MANIFEST_FILE_NAME})."
            )
        }
    }

    /**
     * Reads a ZIP backup from byte array, verifies the anti-tamper signature and identity of Signet,
     * unlocks the keystore, writes it to the specified output directory, and returns the restored KeystoreDetails.
     */
    fun restoreFromZip(outputDir: File, zipBytes: ByteArray): KeystoreDetails {
        val extracted = ZipPackageExtractor.extractEntries(zipBytes)

        if (extracted.manifestJsonString.isNullOrBlank()) {
            throw SecurityException(
                "El archivo ZIP no contiene el manifiesto de respaldo oficial de Signet (${BackupIntegrityVerifier.MANIFEST_FILE_NAME}). No es un respaldo válido."
            )
        }

        if (extracted.binaryFilesMap.isEmpty()) {
            throw IllegalArgumentException(
                "El paquete ZIP no contiene ningún archivo de keystore (.jks / .keystore / .p12)."
            )
        }

        val matchingEntry = extracted.binaryFilesMap.entries.first()
        val detectedKeystoreFileName = matchingEntry.key.substringAfterLast("/")
        val keystoreBytes = matchingEntry.value

        // Parse and verify manifest JSON & cryptographic HMAC
        val manifestData = BackupIntegrityVerifier.verifyManifestAndKeystoreIntegrity(
            manifestJsonString = extracted.manifestJsonString,
            keystoreBytes = keystoreBytes
        )

        return VaultRestorationCoordinator.processAndPersistRestoredKeystore(
            outputDir = outputDir,
            keystoreBytes = keystoreBytes,
            manifestData = manifestData,
            fallbackFileName = detectedKeystoreFileName
        )
    }
}



