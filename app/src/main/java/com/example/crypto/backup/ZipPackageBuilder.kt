package com.example.crypto.backup

import com.example.crypto.Base64Compat
import com.example.data.model.KeystoreDetails
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Builds ZIP archive binary streams for single keystores and multi-keystore vaults.
 */
object ZipPackageBuilder {

    /**
     * Creates a complete, signed ZIP backup containing the keystore file, signed manifest,
     * credentials, key.properties, base64 string, and instructions.
     */
    fun createBackupZip(
        details: KeystoreDetails,
        keystoreBytes: ByteArray
    ): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            val cleanKeystoreName = if (details.fileName.isNotBlank()) details.fileName else "release-key.jks"

            // 1. Keystore Binary File
            zos.putNextEntry(ZipEntry(cleanKeystoreName))
            zos.write(keystoreBytes)
            zos.closeEntry()

            // Calculate Keystore SHA-256 for integrity binding
            val keystoreSha256 = BackupIntegrityVerifier.calculateSha256(keystoreBytes)

            // 2. Signed JSON Manifest (signet-backup.json)
            val manifestJson = BackupIntegrityVerifier.buildSignedManifest(details, cleanKeystoreName, keystoreSha256)
            zos.putNextEntry(ZipEntry(BackupIntegrityVerifier.MANIFEST_FILE_NAME))
            zos.write(manifestJson.toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // 3. credentials.txt
            val credentialsText = BackupTemplates.buildCredentialsText(details, cleanKeystoreName)
            zos.putNextEntry(ZipEntry("credentials.txt"))
            zos.write(credentialsText.toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // 4. key.properties (Standard for Android Gradle & Flutter projects)
            val keyPropertiesText = BackupTemplates.buildKeyProperties(details, cleanKeystoreName)
            zos.putNextEntry(ZipEntry("key.properties"))
            zos.write(keyPropertiesText.toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // 5. base64.txt (For CI/CD GitHub Actions / Bitrise / Fastlane)
            val base64Content = if (details.base64Content.isNotBlank()) {
                details.base64Content
            } else {
                Base64Compat.encodeToString(keystoreBytes, noWrap = true)
            }
            zos.putNextEntry(ZipEntry("base64.txt"))
            zos.write(base64Content.toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // 6. README-BACKUP.txt
            val readmeText = BackupTemplates.buildReadmeBackup(details, cleanKeystoreName)
            zos.putNextEntry(ZipEntry("README-BACKUP.txt"))
            zos.write(readmeText.toByteArray(Charsets.UTF_8))
            zos.closeEntry()
        }
        return baos.toByteArray()
    }

    /**
     * Creates a complete Master Vault ZIP backup containing all saved keystores organized into
     * dedicated subfolders, a signed master manifest, and an inventory summary.
     */
    fun createVaultBackupZip(
        items: List<Pair<KeystoreDetails, ByteArray>>
    ): ByteArray {
        if (items.isEmpty()) {
            throw IllegalArgumentException("No hay ningún keystore en la bóveda para exportar.")
        }

        val vaultCreatedAt = System.currentTimeMillis()
        val baos = ByteArrayOutputStream()
        val vaultEntries = mutableListOf<BackupIntegrityVerifier.VaultKeystoreEntry>()

        ZipOutputStream(baos).use { zos ->
            // 1. Pack each keystore into its subfolder inside keystores/
            for ((idx, pair) in items.withIndex()) {
                val details = pair.first
                val keystoreBytes = pair.second
                val cleanKeystoreName = if (details.fileName.isNotBlank()) details.fileName else "release-key.jks"
                val sanitizedBase = cleanKeystoreName.substringBeforeLast(".").replace("[^a-zA-Z0-9_-]".toRegex(), "_")
                val folderName = "${idx + 1}_$sanitizedBase"
                val prefix = "keystores/$folderName"

                val keystoreSha256 = BackupIntegrityVerifier.calculateSha256(keystoreBytes)

                vaultEntries.add(
                    BackupIntegrityVerifier.VaultKeystoreEntry(
                        folderName = folderName,
                        keystoreFileName = cleanKeystoreName,
                        keystoreSha256 = keystoreSha256,
                        details = details
                    )
                )

                // Write keystore binary in folder
                zos.putNextEntry(ZipEntry("$prefix/$cleanKeystoreName"))
                zos.write(keystoreBytes)
                zos.closeEntry()

                // Individual signet-backup.json in folder
                val singleManifest = BackupIntegrityVerifier.buildSignedManifest(details, cleanKeystoreName, keystoreSha256)
                zos.putNextEntry(ZipEntry("$prefix/${BackupIntegrityVerifier.MANIFEST_FILE_NAME}"))
                zos.write(singleManifest.toByteArray(Charsets.UTF_8))
                zos.closeEntry()

                // credentials.txt in folder
                val credentialsText = BackupTemplates.buildCredentialsText(details, cleanKeystoreName)
                zos.putNextEntry(ZipEntry("$prefix/credentials.txt"))
                zos.write(credentialsText.toByteArray(Charsets.UTF_8))
                zos.closeEntry()

                // key.properties in folder
                val keyPropertiesText = BackupTemplates.buildKeyProperties(details, cleanKeystoreName)
                zos.putNextEntry(ZipEntry("$prefix/key.properties"))
                zos.write(keyPropertiesText.toByteArray(Charsets.UTF_8))
                zos.closeEntry()

                // base64.txt in folder
                val base64Content = if (details.base64Content.isNotBlank()) {
                    details.base64Content
                } else {
                    Base64Compat.encodeToString(keystoreBytes, noWrap = true)
                }
                zos.putNextEntry(ZipEntry("$prefix/base64.txt"))
                zos.write(base64Content.toByteArray(Charsets.UTF_8))
                zos.closeEntry()

                // README-BACKUP.txt in folder
                val readmeText = BackupTemplates.buildReadmeBackup(details, cleanKeystoreName)
                zos.putNextEntry(ZipEntry("$prefix/README-BACKUP.txt"))
                zos.write(readmeText.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }

            // 2. Root: signet-vault-backup.json (Master signed manifest)
            val masterManifestJson = BackupIntegrityVerifier.buildSignedVaultManifest(vaultEntries, vaultCreatedAt)
            zos.putNextEntry(ZipEntry(BackupIntegrityVerifier.VAULT_MANIFEST_FILE_NAME))
            zos.write(masterManifestJson.toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // 3. Root: VAULT-SUMMARY.txt
            val summaryText = BackupTemplates.buildVaultSummaryText(items.map { it.first }, vaultCreatedAt)
            zos.putNextEntry(ZipEntry("VAULT-SUMMARY.txt"))
            zos.write(summaryText.toByteArray(Charsets.UTF_8))
            zos.closeEntry()
        }

        return baos.toByteArray()
    }
}
