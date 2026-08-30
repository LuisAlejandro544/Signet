package com.example.crypto.backup

import com.example.data.model.KeystoreDetails
import org.json.JSONObject

/**
 * Parser and builder for multi-keystore vault JSON manifests (signet-vault-backup.json).
 */
object SignetVaultManifestParser {

    /**
     * Builds and signs the Master JSON manifest for a Multi-Keystore Vault ZIP bundle.
     */
    fun buildSignedVaultManifest(
        items: List<BackupIntegrityVerifier.VaultKeystoreEntry>,
        vaultCreatedAt: Long
    ): String {
        val masterSignature = HmacSignatureEngine.computeVaultMasterHmac(items, vaultCreatedAt)

        val json = JSONObject()
        json.put("appName", HmacSignatureEngine.APP_SIGNATURE_NAME)
        json.put("formatVersion", HmacSignatureEngine.VAULT_FORMAT_VERSION)
        json.put("type", "VAULT_BACKUP")
        json.put("createdAt", vaultCreatedAt)
        json.put("totalCount", items.size)
        json.put("signature", masterSignature)

        val itemsArray = org.json.JSONArray()
        for (item in items) {
            val itemObj = JSONObject()
            itemObj.put("folderName", item.folderName)
            itemObj.put("keystoreFileName", item.keystoreFileName)
            itemObj.put("alias", item.details.alias)
            itemObj.put("storePassword", item.details.storePassword)
            itemObj.put("keyPassword", item.details.keyPassword)
            itemObj.put("algorithm", item.details.algorithm)
            itemObj.put("sha256Fingerprint", item.details.sha256Fingerprint)
            itemObj.put("sha1Fingerprint", item.details.sha1Fingerprint)
            itemObj.put("md5Fingerprint", item.details.md5Fingerprint)
            itemObj.put("validFrom", item.details.validFrom)
            itemObj.put("validUntil", item.details.validUntil)
            itemObj.put("createdAt", item.details.createdAt)
            itemObj.put("subjectDn", item.details.subjectDn)
            itemObj.put("issuerDn", item.details.issuerDn)
            itemObj.put("serialNumber", item.details.serialNumber)
            itemObj.put("certificatePem", item.details.certificatePem)
            itemObj.put("keystoreSha256", item.keystoreSha256)
            itemsArray.put(itemObj)
        }
        json.put("keystores", itemsArray)

        return json.toString(2)
    }

    /**
     * Verifies the integrity of a Master Vault Backup manifest and all contained keystore binaries.
     */
    fun verifyVaultManifestAndIntegrity(
        manifestJsonString: String,
        keystoresMap: Map<String, ByteArray>
    ): List<BackupIntegrityVerifier.ManifestData> {
        val json = try {
            JSONObject(manifestJsonString)
        } catch (e: Exception) {
            throw SecurityException("El manifiesto maestro de la bóveda está corrupto: ${e.message}")
        }

        val appName = json.optString("appName", "")
        if (appName != HmacSignatureEngine.APP_SIGNATURE_NAME) {
            throw SecurityException("Firma inválida. El manifiesto de bóveda no pertenece a ${HmacSignatureEngine.APP_SIGNATURE_NAME}.")
        }

        val formatVersion = json.optInt("formatVersion", 0)
        if (formatVersion != HmacSignatureEngine.VAULT_FORMAT_VERSION) {
            throw SecurityException("Versión de formato de bóveda no soportada ($formatVersion).")
        }

        val createdAt = json.optLong("createdAt", 0L)
        val expectedSignature = json.optString("signature", "")
        val itemsArray = json.optJSONArray("keystores") ?: throw SecurityException("El manifiesto de la bóveda no contiene entradas de keystores.")

        val parsedEntries = mutableListOf<BackupIntegrityVerifier.VaultKeystoreEntry>()
        val resultList = mutableListOf<BackupIntegrityVerifier.ManifestData>()

        for (i in 0 until itemsArray.length()) {
            val itemObj = itemsArray.getJSONObject(i)
            val folderName = itemObj.optString("folderName", "keystore_$i")
            val keystoreFileName = itemObj.optString("keystoreFileName", "release-key.jks")
            val alias = itemObj.optString("alias", "")
            val storePassword = itemObj.optString("storePassword", "")
            val keyPassword = itemObj.optString("keyPassword", "")
            val algorithm = itemObj.optString("algorithm", "RSA 2048 bits")
            val sha256Fingerprint = itemObj.optString("sha256Fingerprint", "")
            val sha1Fingerprint = itemObj.optString("sha1Fingerprint", "")
            val md5Fingerprint = itemObj.optString("md5Fingerprint", "")
            val validFrom = itemObj.optLong("validFrom", 0L)
            val validUntil = itemObj.optLong("validUntil", 0L)
            val itemCreatedAt = itemObj.optLong("createdAt", 0L)
            val subjectDn = itemObj.optString("subjectDn", "")
            val issuerDn = itemObj.optString("issuerDn", "")
            val serialNumber = itemObj.optString("serialNumber", "")
            val certificatePem = itemObj.optString("certificatePem", "")
            val manifestKeystoreSha256 = itemObj.optString("keystoreSha256", "")

            // Look up corresponding binary in unpacked map
            val binaryKey = if (keystoresMap.containsKey("$folderName/$keystoreFileName")) {
                "$folderName/$keystoreFileName"
            } else {
                keystoresMap.keys.firstOrNull { it.startsWith("$folderName/") && (it.endsWith(".jks") || it.endsWith(".keystore") || it.endsWith(".p12")) }
                    ?: throw SecurityException("No se encontró el binario del keystore para '$folderName/$keystoreFileName' dentro del ZIP.")
            }

            val binaryBytes = keystoresMap[binaryKey] ?: throw SecurityException("Binario ausente para '$folderName'.")
            val computedSha256 = HmacSignatureEngine.calculateSha256(binaryBytes)

            if (!manifestKeystoreSha256.equals(computedSha256, ignoreCase = true)) {
                throw SecurityException("Alerta de seguridad: El binario de '$folderName/$keystoreFileName' fue alterado o no coincide con la huella registrada.")
            }

            val details = KeystoreDetails(
                id = 0,
                fileName = keystoreFileName,
                alias = alias,
                filePath = "",
                fileSizeBytes = binaryBytes.size.toLong(),
                storePassword = storePassword,
                keyPassword = keyPassword,
                base64Content = "",
                sha256Fingerprint = sha256Fingerprint,
                sha1Fingerprint = sha1Fingerprint,
                md5Fingerprint = md5Fingerprint,
                validFrom = validFrom,
                validUntil = validUntil,
                algorithm = algorithm,
                subjectDn = subjectDn,
                issuerDn = issuerDn,
                serialNumber = serialNumber,
                certificatePem = certificatePem,
                createdAt = itemCreatedAt
            )

            parsedEntries.add(
                BackupIntegrityVerifier.VaultKeystoreEntry(
                    folderName = folderName,
                    keystoreFileName = keystoreFileName,
                    keystoreSha256 = computedSha256,
                    details = details
                )
            )

            resultList.add(
                BackupIntegrityVerifier.ManifestData(
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
                    createdAt = itemCreatedAt,
                    subjectDn = subjectDn,
                    issuerDn = issuerDn,
                    serialNumber = serialNumber,
                    certificatePem = certificatePem
                )
            )
        }

        // Verify Master HMAC Signature
        val computedMasterSignature = HmacSignatureEngine.computeVaultMasterHmac(parsedEntries, createdAt)
        if (!HmacSignatureEngine.verifySignatureMatch(expectedSignature, computedMasterSignature)) {
            throw SecurityException("Firma maestra de la bóveda Signet inválida o paquete alterado. El archivo de respaldo fue manipulado.")
        }

        return resultList
    }
}
