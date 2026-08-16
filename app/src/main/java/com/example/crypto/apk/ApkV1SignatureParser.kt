package com.example.crypto.apk

import org.bouncycastle.cert.X509CertificateHolder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cms.CMSSignedData
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.io.ByteArrayInputStream
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

object ApkV1SignatureParser {

    /**
     * Extracts X.509 certificates from META-INF signature files (v1 JAR Scheme).
     */
    fun extractV1Certificates(apkBytes: ByteArray, bcProvider: BouncyCastleProvider): List<X509Certificate> {
        val certs = mutableListOf<X509Certificate>()
        val cf = CertificateFactory.getInstance("X.509")

        ZipInputStream(ByteArrayInputStream(apkBytes)).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                val name = entry.name.uppercase(Locale.ROOT)
                if (name.startsWith("META-INF/") && (name.endsWith(".RSA") || name.endsWith(".DSA") || name.endsWith(".EC"))) {
                    val sigBytes = zis.readBytes()
                    // Try parsing as PKCS#7 via BouncyCastle CMS
                    try {
                        val cms = CMSSignedData(sigBytes)
                        val store = cms.certificates
                        val converter = JcaX509CertificateConverter().setProvider(bcProvider)
                        for (holder in store.getMatches(null)) {
                            if (holder is X509CertificateHolder) {
                                certs.add(converter.getCertificate(holder))
                            }
                        }
                    } catch (_: Exception) {
                        try {
                            val certList = cf.generateCertificates(ByteArrayInputStream(sigBytes))
                            for (c in certList) {
                                if (c is X509Certificate) certs.add(c)
                            }
                        } catch (_: Exception) {}
                    }
                }
                entry = zis.nextEntry
            }
        }
        return certs
    }
}
