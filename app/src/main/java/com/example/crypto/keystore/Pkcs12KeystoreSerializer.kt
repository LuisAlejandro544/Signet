package com.example.crypto.keystore

import com.example.crypto.Base64Compat
import com.example.crypto.x509.X509CertificateUtils
import com.example.data.model.KeystoreConfig
import com.example.data.model.KeystoreDetails
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.security.KeyPair
import java.security.KeyStore
import java.security.Provider
import java.security.cert.X509Certificate
import java.util.Date
import java.util.Locale

/**
 * Serializer for writing cryptographic keys and certificates to PKCS#12 keystore storage and metadata structures.
 */
object Pkcs12KeystoreSerializer {

    /**
     * Packages a [KeyPair] and [X509Certificate] into PKCS#12 binary format, persists to disk if [saveToFile] is true,
     * and constructs the rich [KeystoreDetails] model.
     */
    fun serialize(
        outputDir: File,
        config: KeystoreConfig,
        keyPair: KeyPair,
        x509Cert: X509Certificate,
        notBefore: Date,
        notAfter: Date,
        provider: Provider,
        saveToFile: Boolean = true
    ): KeystoreDetails {
        val keyStore = KeyStore.getInstance("PKCS12", provider)
        keyStore.load(null, null)

        val keyPassword = if (config.useSamePassword) config.storePassword else config.keyPassword
        keyStore.setKeyEntry(
            config.alias.trim(),
            keyPair.private,
            keyPassword.toCharArray(),
            arrayOf(x509Cert)
        )

        val sanitizedFileName = if (config.fileName.isBlank()) "my-release-key.jks" else {
            val name = config.fileName.trim()
            if (name.endsWith(".jks", ignoreCase = true) ||
                name.endsWith(".keystore", ignoreCase = true) ||
                name.endsWith(".p12", ignoreCase = true) ||
                name.endsWith(".pfx", ignoreCase = true)
            ) {
                name
            } else {
                "$name.jks"
            }
        }

        val keystoreBytes: ByteArray
        ByteArrayOutputStream().use { baos ->
            keyStore.store(baos, config.storePassword.toCharArray())
            keystoreBytes = baos.toByteArray()
        }

        val finalFilePath: String
        val finalFileSize: Long

        if (saveToFile) {
            val keystoresDir = File(outputDir, "keystores")
            if (!keystoresDir.exists()) {
                keystoresDir.mkdirs()
            }
            val outputFile = File(keystoresDir, sanitizedFileName)
            FileOutputStream(outputFile).use { fos ->
                fos.write(keystoreBytes)
            }
            finalFilePath = outputFile.absolutePath
            finalFileSize = outputFile.length()
        } else {
            finalFilePath = ""
            finalFileSize = keystoreBytes.size.toLong()
        }

        val base64String = Base64Compat.encodeToString(keystoreBytes, noWrap = true)

        val certEncoded = x509Cert.encoded
        val sha256 = X509CertificateUtils.calculateFingerprint(certEncoded, "SHA-256")
        val sha1 = X509CertificateUtils.calculateFingerprint(certEncoded, "SHA-1")
        val md5 = X509CertificateUtils.calculateFingerprint(certEncoded, "MD5")
        val pem = X509CertificateUtils.buildPemCertificate(certEncoded)

        return KeystoreDetails(
            fileName = sanitizedFileName,
            alias = config.alias.trim(),
            filePath = finalFilePath,
            fileSizeBytes = finalFileSize,
            storePassword = config.storePassword,
            keyPassword = keyPassword,
            base64Content = base64String,
            sha256Fingerprint = sha256,
            sha1Fingerprint = sha1,
            md5Fingerprint = md5,
            validFrom = notBefore.time,
            validUntil = notAfter.time,
            algorithm = config.algorithm.displayName,
            subjectDn = x509Cert.subjectX500Principal.name,
            issuerDn = x509Cert.issuerX500Principal.name,
            serialNumber = x509Cert.serialNumber.toString(16).uppercase(Locale.ROOT),
            certificatePem = pem,
            createdAt = System.currentTimeMillis()
        )
    }
}
