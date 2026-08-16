package com.example.crypto

import android.content.Context
import android.util.Base64
import com.example.data.model.KeystoreDetails
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object SignetBackupManager {

    private const val APP_SIGNATURE_NAME = "Signet"
    private const val BACKUP_FORMAT_VERSION = 1
    private const val MANIFEST_FILE_NAME = "signet-backup.json"

    // Cryptographic HMAC key seed for Signet anti-tamper integrity verification
    private val HMAC_SECRET_SEED = "SIGNET_ANTI_TAMPER_KEYSTORE_INTEGRITY_SECRET_V1_2026".toByteArray(Charsets.UTF_8)

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
            val keystoreSha256 = calculateSha256(keystoreBytes)

            // 3. Signed JSON Manifest (signet-backup.json)
            val manifestJson = buildSignedManifest(details, cleanKeystoreName, keystoreSha256)
            zos.putNextEntry(ZipEntry(MANIFEST_FILE_NAME))
            zos.write(manifestJson.toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // 4. credentials.txt
            val credentialsText = buildCredentialsText(details, cleanKeystoreName, hasPepk = cleanPepkName != null)
            zos.putNextEntry(ZipEntry("credentials.txt"))
            zos.write(credentialsText.toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // 5. key.properties (Standard for Android Gradle & Flutter projects)
            val keyPropertiesText = buildKeyProperties(details, cleanKeystoreName)
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
            val readmeText = buildReadmeBackup(details, cleanKeystoreName, cleanPepkName)
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

        ZipInputStream(ByteArrayInputStream(zipBytes)).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                val name = entry.name.substringAfterLast("/")
                if (name.equals(MANIFEST_FILE_NAME, ignoreCase = true)) {
                    manifestJsonString = String(zis.readBytes(), Charsets.UTF_8)
                } else if (name.endsWith(".jks", ignoreCase = true) ||
                    name.endsWith(".keystore", ignoreCase = true) ||
                    name.endsWith(".p12", ignoreCase = true)
                ) {
                    detectedKeystoreFileName = name
                    keystoreBytes = zis.readBytes()
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        if (manifestJsonString.isNullOrBlank()) {
            throw SecurityException(
                "El archivo ZIP no contiene el manifiesto de respaldo oficial de Signet ($MANIFEST_FILE_NAME). No es un respaldo válido."
            )
        }

        if (keystoreBytes == null || keystoreBytes!!.isEmpty()) {
            throw IllegalArgumentException(
                "El paquete ZIP no contiene ningún archivo de keystore (.jks / .keystore / .p12)."
            )
        }

        // Parse and verify manifest JSON
        val json = try {
            JSONObject(manifestJsonString!!)
        } catch (e: Exception) {
            throw SecurityException(
                "El manifiesto de respaldo está corrupto o tiene un formato no válido: ${e.message}"
            )
        }

        val appName = json.optString("appName", "")
        if (appName != APP_SIGNATURE_NAME) {
            throw SecurityException(
                "Firma de aplicación inválida. El manifiesto no pertenece a $APP_SIGNATURE_NAME."
            )
        }

        val version = json.optInt("formatVersion", 0)
        if (version != BACKUP_FORMAT_VERSION) {
            throw SecurityException(
                "Versión de formato de respaldo no soportada (Versión $version)."
            )
        }

        val keystoreFileName = json.optString("keystoreFileName", detectedKeystoreFileName ?: "restored-key.jks")
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
        val computedKeystoreSha256 = calculateSha256(keystoreBytes!!)
        if (!manifestKeystoreSha256.equals(computedKeystoreSha256, ignoreCase = true)) {
            throw SecurityException(
                "Alerta de seguridad: El archivo de keystore dentro del ZIP fue alterado o no coincide con la huella registrada en el manifiesto."
            )
        }

        // 2. Verify Cryptographic Signature of Manifest
        val expectedSignature = computeManifestHmac(
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

        if (!MessageDigest.isEqual(providedSignature.toByteArray(Charsets.UTF_8), expectedSignature.toByteArray(Charsets.UTF_8))) {
            throw SecurityException(
                "Firma criptográfica de Signet inválida o paquete alterado. El archivo de respaldo ha sido modificado y no puede ser restaurado por motivos de seguridad."
            )
        }

        // 3. Verify that the keystore can actually be unlocked with the verified credentials
        val inspectedList = try {
            KeystoreGenerator.inspectKeystore(ByteArrayInputStream(keystoreBytes), storePassword)
        } catch (e: Exception) {
            throw SecurityException(
                "No se pudo desbloquear el keystore con las credenciales verificadas: ${e.localizedMessage}"
            )
        }

        val matchingEntry = inspectedList.firstOrNull { it.alias.equals(alias, ignoreCase = true) }
            ?: inspectedList.firstOrNull()
            ?: throw IllegalArgumentException("El keystore no contiene ningún certificado válido.")

        // 4. Save keystore safely to app storage
        val keystoresDir = File(context.filesDir, "keystores")
        if (!keystoresDir.exists()) {
            keystoresDir.mkdirs()
        }

        var destinationFile = File(keystoresDir, keystoreFileName)
        if (destinationFile.exists()) {
            val baseName = keystoreFileName.substringBeforeLast(".")
            val ext = keystoreFileName.substringAfterLast(".", "jks")
            destinationFile = File(keystoresDir, "${baseName}_restored_${System.currentTimeMillis()}.$ext")
        }

        FileOutputStream(destinationFile).use { fos ->
            fos.write(keystoreBytes!!)
        }

        val base64String = Base64.encodeToString(keystoreBytes, Base64.NO_WRAP)

        return KeystoreDetails(
            id = 0,
            fileName = destinationFile.name,
            alias = alias.ifBlank { matchingEntry.alias },
            filePath = destinationFile.absolutePath,
            fileSizeBytes = destinationFile.length(),
            storePassword = storePassword,
            keyPassword = keyPassword.ifBlank { storePassword },
            base64Content = base64String,
            sha256Fingerprint = sha256Fingerprint.ifBlank { matchingEntry.sha256Fingerprint },
            sha1Fingerprint = sha1Fingerprint.ifBlank { matchingEntry.sha1Fingerprint },
            md5Fingerprint = md5Fingerprint.ifBlank { matchingEntry.md5Fingerprint },
            validFrom = if (validFrom > 0) validFrom else matchingEntry.validFrom,
            validUntil = if (validUntil > 0) validUntil else matchingEntry.validUntil,
            algorithm = algorithm.ifBlank { matchingEntry.algorithm },
            subjectDn = subjectDn.ifBlank { matchingEntry.subjectDn },
            issuerDn = issuerDn.ifBlank { matchingEntry.issuerDn },
            serialNumber = serialNumber.ifBlank { matchingEntry.serialNumber },
            certificatePem = certificatePem.ifBlank { matchingEntry.certificatePem },
            createdAt = createdAt
        )
    }

    private fun buildSignedManifest(
        details: KeystoreDetails,
        keystoreFileName: String,
        keystoreSha256: String
    ): String {
        val createdAt = if (details.createdAt > 0) details.createdAt else System.currentTimeMillis()

        val signature = computeManifestHmac(
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
        json.put("appName", APP_SIGNATURE_NAME)
        json.put("formatVersion", BACKUP_FORMAT_VERSION)
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

    private fun computeManifestHmac(
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

    private fun calculateSha256(bytes: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.joinToString("") { "%02X".format(it) }
    }

    private fun buildCredentialsText(
        details: KeystoreDetails,
        keystoreFileName: String,
        hasPepk: Boolean = false
    ): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.ROOT)
        val validFromStr = dateFormat.format(Date(details.validFrom))
        val validUntilStr = dateFormat.format(Date(details.validUntil))

        val pepkNote = if (hasPepk) {
            """
            ------------------------------------------------------------------------
            GOOGLE PLAY APP SIGNING (PEPK)
            ------------------------------------------------------------------------
            Este paquete ZIP incluye tu clave de subida cifrada (.pepk) lista para
            ser importada en Google Play Console (Configuración > Firma de apps).
            """.trimIndent() + "\n\n"
        } else ""

        return """
            ========================================================================
            SIGNET - CREDENCIALES Y METADATOS DE RESPALDO
            ========================================================================
            Archivo Keystore      : $keystoreFileName
            Alias de la Clave     : ${details.alias}
            Contraseña Keystore   : ${details.storePassword}
            Contraseña Clave      : ${details.keyPassword}
            Algoritmo             : ${details.algorithm}
            
            ------------------------------------------------------------------------
            HUELLAS DIGITALES DEL CERTIFICADO (FINGERPRINTS)
            ------------------------------------------------------------------------
            SHA-256 : ${details.sha256Fingerprint}
            SHA-1   : ${details.sha1Fingerprint}
            MD5     : ${details.md5Fingerprint}
            
            ------------------------------------------------------------------------
            VALIDEZ Y PROPIETARIO
            ------------------------------------------------------------------------
            Válido Desde : $validFromStr
            Válido Hasta : $validUntilStr
            Propietario  : ${details.subjectDn}
            Emisor       : ${details.issuerDn}
            Nº Serie     : ${details.serialNumber}
            
            $pepkNote========================================================================
            INSTRUCCIONES DE RESTAURACIÓN:
            Este archivo ZIP puede restaurarse directamente en Signet (Android / PC)
            utilizando la función 'Restaurar Respaldo (.zip)'.
            
            ADVERTENCIA DE SEGURIDAD:
            Este archivo contiene credenciales sensibles. Guárdalo en un gestor seguro
            de contraseñas (KeePass, Bitwarden, 1Password) y no lo compartas públicamente.
            ========================================================================
        """.trimIndent()
    }

    private fun buildKeyProperties(details: KeystoreDetails, keystoreFileName: String): String {
        return """
            # Generado automáticamente por Signet (Keystore Manager)
            # Coloca este archivo en la raíz o en la carpeta android/ de tu proyecto
            storePassword=${details.storePassword}
            keyPassword=${details.keyPassword}
            keyAlias=${details.alias}
            storeFile=../keystores/$keystoreFileName
        """.trimIndent()
    }

    private fun buildReadmeBackup(
        details: KeystoreDetails,
        keystoreFileName: String,
        pepkFileName: String? = null
    ): String {
        val pepkLine = if (!pepkFileName.isNullOrBlank()) {
            "\n6. $pepkFileName -> Clave privada cifrada con la clave pública de Google Play (PEPK) para Google Play App Signing."
        } else ""

        return """
            ========================================================================
            PAQUETE DE RESPALDO DE FIRMA ANDROID - SIGNET
            ========================================================================
            
            Contenido de este paquete:
            1. $keystoreFileName -> Archivo binario PKCS12 de firma Android.
            2. signet-backup.json -> Manifiesto firmado criptográficamente para restauración instantánea en Signet.
            3. credentials.txt -> Resumen legible con claves y huellas SHA-256.
            4. key.properties -> Archivo de configuración listo para Gradle / Flutter.
            5. base64.txt -> Llave codificada en Base64 para GitHub Actions / CI/CD secrets.$pepkLine
            
            ¿Cómo restaurar este respaldo si reinstalas la app o cambias de dispositivo?
            - Abre Signet.
            - Ve a la pestaña 'Mis Keystores' o 'Inspeccionar'.
            - Presiona 'Restaurar Respaldo (.zip)' y selecciona este archivo ZIP.
            - ¡Listo! Todo tu keystore, huellas y credenciales quedarán cargados.
            
            Nota: El archivo signet-backup.json cuenta con una firma anti-manipulación.
            Si el archivo es modificado manualmente, Signet rechazará la importación por seguridad.
        """.trimIndent()
    }
}
