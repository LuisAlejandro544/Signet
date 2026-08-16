package com.example.crypto.x509

import com.example.data.model.KeystoreDetails
import java.io.InputStream
import java.security.KeyStore
import java.security.Provider
import java.security.cert.X509Certificate
import java.util.Locale

/**
 * Inspector capable of opening and parsing PKCS12, JKS, and BKS keystores,
 * extracting X.509 certificate chains, serial numbers, DN attributes, and cryptographic fingerprints.
 */
object X509CertificateInspector {

    /**
     * Inspects an existing keystore stream and extracts certificate details.
     */
    fun inspectKeystore(
        inputStream: InputStream,
        password: String,
        provider: Provider? = null
    ): List<KeystoreDetails> {
        return inspectKeystore(inputStream.readBytes(), password, provider)
    }

    /**
     * Inspects an existing keystore byte array across PKCS12, JKS, and BKS formats.
     */
    fun inspectKeystore(
        bytes: ByteArray,
        password: String,
        provider: Provider? = null
    ): List<KeystoreDetails> {
        val results = mutableListOf<KeystoreDetails>()
        val types = listOf("PKCS12", "JKS", "BKS")
        var loadedKeyStore: KeyStore? = null

        // Attempt loading standard providers first, then fallback to BouncyCastle provider
        for (type in types) {
            try {
                val ks = KeyStore.getInstance(type)
                ks.load(bytes.inputStream(), password.toCharArray())
                loadedKeyStore = ks
                break
            } catch (_: Exception) {
                if (provider != null) {
                    try {
                        val ks = KeyStore.getInstance(type, provider)
                        ks.load(bytes.inputStream(), password.toCharArray())
                        loadedKeyStore = ks
                        break
                    } catch (_: Exception) {
                        // Try next format
                    }
                }
            }
        }

        if (loadedKeyStore == null) {
            throw IllegalArgumentException("No se pudo abrir el archivo keystore. Verifica que la contraseña sea correcta o que el formato sea PKCS12 / JKS.")
        }

        val aliases = loadedKeyStore.aliases()
        while (aliases.hasMoreElements()) {
            val alias = aliases.nextElement()
            val cert = loadedKeyStore.getCertificate(alias) as? X509Certificate ?: continue

            val certEncoded = cert.encoded
            val sha256 = X509CertificateUtils.calculateFingerprint(certEncoded, "SHA-256")
            val sha1 = X509CertificateUtils.calculateFingerprint(certEncoded, "SHA-1")
            val md5 = X509CertificateUtils.calculateFingerprint(certEncoded, "MD5")
            val pem = X509CertificateUtils.buildPemCertificate(certEncoded)

            val b64 = java.util.Base64.getEncoder().encodeToString(bytes)

            results.add(
                KeystoreDetails(
                    fileName = "keystore_inspeccionado",
                    alias = alias,
                    filePath = "",
                    fileSizeBytes = bytes.size.toLong(),
                    storePassword = password,
                    keyPassword = password,
                    base64Content = b64,
                    sha256Fingerprint = sha256,
                    sha1Fingerprint = sha1,
                    md5Fingerprint = md5,
                    validFrom = cert.notBefore.time,
                    validUntil = cert.notAfter.time,
                    algorithm = cert.publicKey.algorithm,
                    subjectDn = cert.subjectX500Principal.name,
                    issuerDn = cert.issuerX500Principal.name,
                    serialNumber = cert.serialNumber.toString(16).uppercase(Locale.ROOT),
                    certificatePem = pem,
                    createdAt = System.currentTimeMillis()
                )
            )
        }

        if (results.isEmpty()) {
            throw IllegalArgumentException("El keystore no contiene certificados X.509 válidos.")
        }

        return results
    }
}
