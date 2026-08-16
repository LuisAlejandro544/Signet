package com.example.crypto

import android.content.Context
import android.util.Base64
import com.example.crypto.backup.BackupIntegrityVerifier
import com.example.crypto.backup.BackupTemplates
import com.example.data.model.KeystoreDetails
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object SignetBackupManager {

    /**
     * Creates a complete, signed ZIP backup containing the keystore file, signed manifest,
     * credentials, key.properties, base64 string, instructions, and optionally an encrypted .pepk key.
     */
    fun createBackupZip(
        details: KeystoreDetails,
        keystoreBytes: ByteArray,
        pepkBytes: ByteArray? = null,
        pepkFileName: String? = null
    ): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            val cleanKeystoreName = if (details.fileName.isNotBlank()) details.fileName else "release-key.jks"

            // 1. Keystore Binary File
            zos.putNextEntry(ZipEntry(cleanKeystoreName))
            zos.write(keystoreBytes)
            zos.closeEntry()

            // 2. PEPK Encrypted Key if provided
            val cleanPepkName = if (pepkBytes != null && pepkBytes.isNotEmpty()) {
                val name = if (!pepkFileName.isNullOrBlank()) {
                    pepkFileName
                } else {
                    val baseAlias = details.alias.ifBlank { "release-key" }.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
                    "${baseAlias}_encrypted_key.pepk"
                }
                zos.putNextEntry(ZipEntry(name))
                zos.write(pepkBytes)
                zos.closeEntry()
                name
            } else null

            // Calculate Keystore SHA-256 for integrity binding
            val keystoreSha256 = BackupIntegrityVerifier.calculateSha256(keystoreBytes)

            // 3. Signed JSON Manifest (signet-backup.json)
            val manifestJson = BackupIntegrityVerifier.buildSignedManifest(details, cleanKeystoreName, keystoreSha256)
            zos.putNextEntry(ZipEntry(BackupIntegrityVerifier.MANIFEST_FILE_NAME))
            zos.write(manifestJson.toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // 4. credentials.txt
            val credentialsText = BackupTemplates.buildCredentialsText(details, cleanKeystoreName, hasPepk = cleanPepkName != null)
            zos.putNextEntry(ZipEntry("credentials.txt"))
            zos.write(credentialsText.toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // 5. key.properties (Standard for Android Gradle & Flutter projects)
            val keyPropertiesText = BackupTemplates.buildKeyProperties(details, cleanKeystoreName)
            zos.putNextEntry(ZipEntry("key.properties"))
            zos.write(keyPropertiesText.toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // 6. base64.txt (For CI/CD GitHub Actions / Bitrise / Fastlane)
            val base64Content = if (details.base64Content.isNotBlank()) {
                details.base64Content
            } else {
                Base64.encodeToString(keystoreBytes, Base64.NO_WRAP)
            }
            zos.putNextEntry(ZipEntry("base64.txt"))
            zos.write(base64Content.toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // 7. README-BACKUP.txt
            val readmeText = BackupTemplates.buildReadmeBackup(details, cleanKeystoreName, cleanPepkName)
            zos.putNextEntry(ZipEntry("README-BACKUP.txt"))
            zos.write(readmeText.toByteArray(Charsets.UTF_8))
            zos.closeEntry()
        }
        return baos.toByteArray()
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
        var manifestJsonString: String? = null
        var keystoreBytes: ByteArray? = null
        var detectedKeystoreFileName: String? = null

        try {
            ZipInputStream(ByteArrayInputStream(zipBytes)).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    val name = entry.name.substringAfterLast("/")
                    val entryBytes = zis.readBytes()
                    if (name.equals(BackupIntegrityVerifier.MANIFEST_FILE_NAME, ignoreCase = true)) {
                        manifestJsonString = String(entryBytes, Charsets.UTF_8)
                    } else if (name.endsWith(".jks", ignoreCase = true) ||
                        name.endsWith(".keystore", ignoreCase = true) ||
                        name.endsWith(".p12", ignoreCase = true)
                    ) {
                        detectedKeystoreFileName = name
                        keystoreBytes = entryBytes
                    }
                    entry = zis.nextEntry
                }
            }
        } catch (e: SecurityException) {
            throw e
        } catch (e: Exception) {
            throw SecurityException(
                "Error al leer el archivo ZIP de respaldo: ${e.localizedMessage ?: "Formato comprimido no válido"}"
            )
        }

        if (manifestJsonString.isNullOrBlank()) {
            throw SecurityException(
                "El archivo ZIP no contiene el manifiesto de respaldo oficial de Signet (${BackupIntegrityVerifier.MANIFEST_FILE_NAME}). No es un respaldo válido."
            )
        }

        if (keystoreBytes == null || keystoreBytes!!.isEmpty()) {
            throw IllegalArgumentException(
                "El paquete ZIP no contiene ningún archivo de keystore (.jks / .keystore / .p12)."
            )
        }

        // Parse and verify manifest JSON & cryptographic HMAC
        val manifestData = BackupIntegrityVerifier.verifyManifestAndKeystoreIntegrity(
            manifestJsonString = manifestJsonString!!,
            keystoreBytes = keystoreBytes!!
        )

        // Verify that the keystore can actually be unlocked with the verified credentials
        val inspectedList = try {
            KeystoreGenerator.inspectKeystore(ByteArrayInputStream(keystoreBytes), manifestData.storePassword)
        } catch (e: Exception) {
            throw SecurityException(
                "No se pudo desbloquear el keystore con las credenciales verificadas: ${e.localizedMessage}"
            )
        }

        val matchingEntry = inspectedList.firstOrNull { it.alias.equals(manifestData.alias, ignoreCase = true) }
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
            detectedKeystoreFileName ?: "restored-key.jks"
        }

        var destinationFile = File(keystoresDir, targetFileName)
        if (destinationFile.exists()) {
            val baseName = targetFileName.substringBeforeLast(".")
            val ext = targetFileName.substringAfterLast(".", "jks")
            destinationFile = File(keystoresDir, "${baseName}_restored_${System.currentTimeMillis()}.$ext")
        }

        FileOutputStream(destinationFile).use { fos ->
            fos.write(keystoreBytes!!)
        }

        val base64String = Base64.encodeToString(keystoreBytes, Base64.NO_WRAP)

        return KeystoreDetails(
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
    }
}
