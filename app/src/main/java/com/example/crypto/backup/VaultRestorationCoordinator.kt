package com.example.crypto.backup

import com.example.crypto.Base64Compat
import com.example.crypto.KeystoreGenerator
import com.example.crypto.backup.BackupIntegrityVerifier.ManifestData
import com.example.data.model.KeystoreDetails
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream

/**
 * Coordinator for persisting and verifying restored keystores from ZIP backups.
 * Handles filename collision resolution, keystore unlocking verification, and [KeystoreDetails] assembly.
 */
object VaultRestorationCoordinator {

    /**
     * Unlocks, persists, and builds a [KeystoreDetails] instance for a single restored keystore.
     */
    fun processAndPersistRestoredKeystore(
        outputDir: File,
        keystoreBytes: ByteArray,
        manifestData: ManifestData,
        fallbackFileName: String = "restored-key.jks"
    ): KeystoreDetails {
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

        // Save keystore safely to storage
        val keystoresDir = File(outputDir, "keystores")
        if (!keystoresDir.exists()) {
            keystoresDir.mkdirs()
        }

        val targetFileName = if (manifestData.keystoreFileName.isNotBlank()) {
            manifestData.keystoreFileName
        } else {
            fallbackFileName.ifBlank { "restored-key.jks" }
        }

        var destinationFile = File(keystoresDir, targetFileName)
        if (destinationFile.exists()) {
            val baseName = targetFileName.substringBeforeLast(".")
            val ext = targetFileName.substringAfterLast(".", "jks")
            destinationFile = File(keystoresDir, "${baseName}_restored_${System.currentTimeMillis()}_${(100..999).random()}.$ext")
        }

        FileOutputStream(destinationFile).use { fos ->
            fos.write(keystoreBytes)
        }

        val base64String = Base64Compat.encodeToString(keystoreBytes, noWrap = true)

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

    /**
     * Restores all verified entries from a Vault backup into disk storage and builds [KeystoreDetails] items.
     */
    fun processAndPersistVaultEntries(
        outputDir: File,
        verifiedManifests: List<ManifestData>,
        binaryFilesMap: Map<String, ByteArray>
    ): List<KeystoreDetails> {
        val restoredKeystores = mutableListOf<KeystoreDetails>()

        for (manifestData in verifiedManifests) {
            val matchingBinaryKey = binaryFilesMap.keys.firstOrNull {
                it.endsWith("/" + manifestData.keystoreFileName, ignoreCase = true) ||
                it.equals(manifestData.keystoreFileName, ignoreCase = true)
            } ?: throw SecurityException("No se encontró el binario para '${manifestData.keystoreFileName}'.")

            val keystoreBytes = binaryFilesMap[matchingBinaryKey]!!
            val details = processAndPersistRestoredKeystore(
                outputDir = outputDir,
                keystoreBytes = keystoreBytes,
                manifestData = manifestData,
                fallbackFileName = manifestData.keystoreFileName
            )
            restoredKeystores.add(details)
        }

        return restoredKeystores
    }
}
