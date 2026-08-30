package com.example.crypto.backup

import java.io.ByteArrayInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * Extracts and unpacks ZIP archive entries in-memory.
 */
object ZipPackageExtractor {

    data class ExtractedZipEntries(
        val manifestJsonString: String?,
        val binaryFilesMap: Map<String, ByteArray>,
        val detectedSingleKeystoreFileName: String?,
        val isVault: Boolean,
        val hasSingleManifest: Boolean
    )

    /**
     * Inspects a ZIP archive in a single pass to discover its type, manifests, and binary entries.
     */
    fun extractEntries(zipBytes: ByteArray): ExtractedZipEntries {
        var masterManifestJson: String? = null
        var singleManifestJson: String? = null
        var detectedSingleKeystoreFileName: String? = null
        var isVault = false
        var hasSingleManifest = false
        val binaryFilesMap = mutableMapOf<String, ByteArray>()

        try {
            ZipInputStream(ByteArrayInputStream(zipBytes)).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    val entryName = entry.name
                    val entryBytes = zis.readBytes()
                    val simpleName = entryName.substringAfterLast("/")

                    if (simpleName.equals(BackupIntegrityVerifier.VAULT_MANIFEST_FILE_NAME, ignoreCase = true) ||
                        entryName.endsWith("/" + BackupIntegrityVerifier.VAULT_MANIFEST_FILE_NAME, ignoreCase = true)
                    ) {
                        isVault = true
                        masterManifestJson = String(entryBytes, Charsets.UTF_8)
                    } else if (simpleName.equals(BackupIntegrityVerifier.MANIFEST_FILE_NAME, ignoreCase = true)) {
                        hasSingleManifest = true
                        if (singleManifestJson == null) {
                            singleManifestJson = String(entryBytes, Charsets.UTF_8)
                        }
                    }

                    if (entryName.endsWith(".jks", ignoreCase = true) ||
                        entryName.endsWith(".keystore", ignoreCase = true) ||
                        entryName.endsWith(".p12", ignoreCase = true)
                    ) {
                        val cleanPath = entryName.removePrefix("keystores/").trimStart('/')
                        binaryFilesMap[cleanPath] = entryBytes
                        if (detectedSingleKeystoreFileName == null) {
                            detectedSingleKeystoreFileName = simpleName
                        }
                    }
                    entry = zis.nextEntry
                }
            }
        } catch (e: SecurityException) {
            throw e
        } catch (e: Exception) {
            throw SecurityException("Error al leer el archivo ZIP de respaldo: ${e.localizedMessage ?: "Formato comprimido no válido"}")
        }

        return ExtractedZipEntries(
            manifestJsonString = if (isVault) masterManifestJson else singleManifestJson,
            binaryFilesMap = binaryFilesMap,
            detectedSingleKeystoreFileName = detectedSingleKeystoreFileName,
            isVault = isVault,
            hasSingleManifest = hasSingleManifest
        )
    }
}
