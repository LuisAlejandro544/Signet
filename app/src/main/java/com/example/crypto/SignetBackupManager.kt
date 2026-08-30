package com.example.crypto

import android.content.Context
import android.util.Base64
import com.example.crypto.backup.BackupIntegrityVerifier
import com.example.crypto.backup.ZipPackageBuilder
import com.example.crypto.backup.ZipPackageExtractor
import com.example.data.model.KeystoreDetails
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/**
 * High-level manager orchestrating backup ZIP creation and intelligent restoration for Signet.
 * Delegates low-level ZIP packaging and extraction to [ZipPackageBuilder] and [ZipPackageExtractor],
 * and cryptographic validation to [BackupIntegrityVerifier].
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
    fun restoreVaultFromZip(context: Context, zipBytes: ByteArray): List<KeystoreDetails> {
        val extracted = ZipPackageExtractor.extractEntries(zipBytes)

        if (extracted.manifestJsonString.isNullOrBlank()) {
            throw SecurityException("El archivo ZIP no contiene el manifiesto maestro de bóveda (${BackupIntegrityVerifier.VAULT_MANIFEST_FILE_NAME}).")
        }

        // Verify Vault master cryptographic signature & all binary hashes
        val verifiedManifests = BackupIntegrityVerifier.verifyVaultManifestAndIntegrity(
            manifestJsonString = extracted.manifestJsonString,
            keystoresMap = extracted.binaryFilesMap
        )

        val keystoresDir = File(context.filesDir, "keystores")
        if (!keystoresDir.exists()) {
            keystoresDir.mkdirs()
        }

        val restoredKeystores = mutableListOf<KeystoreDetails>()

        for (manifestData in verifiedManifests) {
            val matchingBinaryKey = extracted.binaryFilesMap.keys.firstOrNull {
                it.endsWith("/" + manifestData.keystoreFileName, ignoreCase = true) ||
                it.equals(manifestData.keystoreFileName, ignoreCase = true)
            } ?: throw SecurityException("No se encontró el binario para '${manifestData.keystoreFileName}'.")

            val keystoreBytes = extracted.binaryFilesMap[matchingBinaryKey]!!

            // Verify certificate unlocking
            val inspectedList = try {
                KeystoreGenerator.inspectKeystore(ByteArrayInputStream(keystoreBytes), manifestData.storePassword)
            } catch (e: Exception) {
                throw SecurityException("No se pudo desbloquear el keystore '${manifestData.keystoreFileName}': ${e.localizedMessage}")
            }

            val matchingEntry = inspectedList.firstOrNull { it.alias.equals(manifestData.alias, ignoreCase = true) }
                ?: inspectedList.firstOrNull()
                ?: throw IllegalArgumentException("El keystore '${manifestData.keystoreFileName}' no contiene certificados válidos.")

            val targetFileName = manifestData.keystoreFileName.ifBlank { "restored-key.jks" }
            var destinationFile = File(keystoresDir, targetFileName)
            if (destinationFile.exists()) {
                val baseName = targetFileName.substringBeforeLast(".")
                val ext = targetFileName.substringAfterLast(".", "jks")
                destinationFile = File(keystoresDir, "${baseName}_restored_${System.currentTimeMillis()}_${(100..999).random()}.$ext")
            }

            FileOutputStream(destinationFile).use { fos ->
                fos.write(keystoreBytes)
            }

            val base64String = Base64.encodeToString(keystoreBytes, Base64.NO_WRAP)

            val details = KeystoreDetails(
                id = 0,
                fileName = destinationFile.name,
                alias = manifestData.alias.ifBlank { matchingEntry.alias },
                filePath = destinationFile.absolutePath,
                fileSizeBytes = destinationFile.length(),
                storePassword = manifestData.storePassword,
                keyPassword = manifestData.keyPassword.ifBlank { manifestData.storePassword },
                base64Content = base64String,
                sha256Fingerprint = manifestData.sha256Fingerprint.ifBlank { matchingEntry.sha256Fingerprint },
                sha1Fingerprint = manifestData.sha1Fingerprint.ifBlank { matchingEntry.sha1Fingerprint },
                md5Fingerprint = manifestData.md5Fingerprint.ifBlank { matchingEntry.md5Fingerprint },
                validFrom = if (manifestData.validFrom > 0) manifestData.validFrom else matchingEntry.validFrom,
                validUntil = if (manifestData.validUntil > 0) manifestData.validUntil else matchingEntry.validUntil,
                algorithm = manifestData.algorithm.ifBlank { matchingEntry.algorithm },
                subjectDn = manifestData.subjectDn.ifBlank { matchingEntry.subjectDn },
                issuerDn = manifestData.issuerDn.ifBlank { matchingEntry.issuerDn },
                serialNumber = manifestData.serialNumber.ifBlank { matchingEntry.serialNumber },
                certificatePem = manifestData.certificatePem.ifBlank { matchingEntry.certificatePem },
                createdAt = manifestData.createdAt
            )
            restoredKeystores.add(details)
        }

        return restoredKeystores
    }

    /**
     * Intelligently restores any Signet ZIP backup (Single Keystore or Multi-Keystore Vault).
     */
    fun restoreAnyFromZip(context: Context, zipBytes: ByteArray): List<KeystoreDetails> {
        val extracted = ZipPackageExtractor.extractEntries(zipBytes)

        return when {
            extracted.isVault -> restoreVaultFromZip(context, zipBytes)
            extracted.hasSingleManifest -> listOf(restoreFromZip(context, zipBytes))
            else -> throw SecurityException(
                "El archivo ZIP no contiene un manifiesto de respaldo válido de Signet (${BackupIntegrityVerifier.MANIFEST_FILE_NAME} o ${BackupIntegrityVerifier.VAULT_MANIFEST_FILE_NAME})."
            )
        }
    }

    /**
     * Reads a ZIP backup, verifies the anti-tamper signature and identity of Signet,
     * unlocks the keystore, writes it to app storage, and returns the restored KeystoreDetails.
     */
    fun restoreFromZip(context: Context, inputStream: InputStream): KeystoreDetails {
        return restoreFromZip(context, inputStream.readBytes())
    }

    /**
     * Reads a ZIP backup from byte array, verifies the anti-tamper signature and identity of Signet,
     * unlocks the keystore, writes it to app storage, and returns the restored KeystoreDetails.
     */
    fun restoreFromZip(context: Context, zipBytes: ByteArray): KeystoreDetails {
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

        // Verify that the keystore can actually be unlocked with the verified credentials
        val inspectedList = try {
            KeystoreGenerator.inspectKeystore(ByteArrayInputStream(keystoreBytes), manifestData.storePassword)
        } catch (e: Exception) {
            throw SecurityException(
                "No se pudo desbloquear el keystore con las credenciales verificadas: ${e.localizedMessage}"
            )
        }

        val matchingCert = inspectedList.firstOrNull { it.alias.equals(manifestData.alias, ignoreCase = true) }
            ?: inspectedList.firstOrNull()
            ?: throw IllegalArgumentException("El keystore no contiene ningún certificado válido.")

        // Save keystore safely to app storage
        val keystoresDir = File(context.filesDir, "keystores")
        if (!keystoresDir.exists()) {
            keystoresDir.mkdirs()
        }

        val targetFileName = if (manifestData.keystoreFileName.isNotBlank()) {
            manifestData.keystoreFileName
        } else {
            detectedKeystoreFileName.ifBlank { "restored-key.jks" }
        }

        var destinationFile = File(keystoresDir, targetFileName)
        if (destinationFile.exists()) {
            val baseName = targetFileName.substringBeforeLast(".")
            val ext = targetFileName.substringAfterLast(".", "jks")
            destinationFile = File(keystoresDir, "${baseName}_restored_${System.currentTimeMillis()}.$ext")
        }

        FileOutputStream(destinationFile).use { fos ->
            fos.write(keystoreBytes)
        }

        val base64String = Base64.encodeToString(keystoreBytes, Base64.NO_WRAP)

        return KeystoreDetails(
            id = 0,
            fileName = destinationFile.name,
            alias = manifestData.alias.ifBlank { matchingCert.alias },
            filePath = destinationFile.absolutePath,
            fileSizeBytes = destinationFile.length(),
            storePassword = manifestData.storePassword,
            keyPassword = manifestData.keyPassword.ifBlank { manifestData.storePassword },
            base64Content = base64String,
            sha256Fingerprint = manifestData.sha256Fingerprint.ifBlank { matchingCert.sha256Fingerprint },
            sha1Fingerprint = manifestData.sha1Fingerprint.ifBlank { matchingCert.sha1Fingerprint },
            md5Fingerprint = manifestData.md5Fingerprint.ifBlank { matchingCert.md5Fingerprint },
            validFrom = if (manifestData.validFrom > 0) manifestData.validFrom else matchingCert.validFrom,
            validUntil = if (manifestData.validUntil > 0) manifestData.validUntil else matchingCert.validUntil,
            algorithm = manifestData.algorithm.ifBlank { matchingCert.algorithm },
            subjectDn = manifestData.subjectDn.ifBlank { matchingCert.subjectDn },
            issuerDn = manifestData.issuerDn.ifBlank { matchingCert.issuerDn },
            serialNumber = manifestData.serialNumber.ifBlank { matchingCert.serialNumber },
            certificatePem = manifestData.certificatePem.ifBlank { matchingCert.certificatePem },
            createdAt = manifestData.createdAt
        )
    }
}

