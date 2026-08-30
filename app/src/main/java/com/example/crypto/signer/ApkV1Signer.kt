package com.example.crypto.signer

import android.util.Base64
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder
import org.bouncycastle.cms.CMSProcessableByteArray
import org.bouncycastle.cms.CMSSignedDataGenerator
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.cert.X509Certificate
import java.util.zip.ZipEntry

/**
 * Generates JAR Signing Scheme v1 files (MANIFEST.MF, CERT.SF, CERT.RSA / CERT.EC).
 */
object ApkV1Signer {

    fun generateV1Entries(
        entries: List<RawZipEntry>,
        privateKey: PrivateKey,
        certificate: X509Certificate,
        bcProvider: BouncyCastleProvider
    ): List<RawZipEntry> {
        val result = mutableListOf<RawZipEntry>()
        val md = MessageDigest.getInstance("SHA-256")

        // 1. Generate META-INF/MANIFEST.MF
        val manifestSb = StringBuilder()
        manifestSb.append("Manifest-Version: 1.0\r\n")
        manifestSb.append("Created-By: 1.0 (Signet - Sovereign Android Tool)\r\n\r\n")

        val entryManifestSections = mutableMapOf<String, ByteArray>()

        for (entry in entries) {
            val digest = md.digest(entry.data)
            val digestB64 = Base64.encodeToString(digest, Base64.NO_WRAP)

            val sectionSb = StringBuilder()
            sectionSb.append("Name: ${entry.name}\r\n")
            sectionSb.append("SHA-256-Digest: $digestB64\r\n\r\n")

            val sectionBytes = sectionSb.toString().toByteArray(Charsets.UTF_8)
            entryManifestSections[entry.name] = sectionBytes
            manifestSb.append(sectionSb)
        }

        val manifestBytes = manifestSb.toString().toByteArray(Charsets.UTF_8)
        result.add(
            RawZipEntry(
                name = "META-INF/MANIFEST.MF",
                data = manifestBytes,
                compressionMethod = ZipEntry.DEFLATED
            )
        )

        // 2. Generate META-INF/CERT.SF
        val sfSb = StringBuilder()
        sfSb.append("Signature-Version: 1.0\r\n")
        sfSb.append("Created-By: 1.0 (Signet - Sovereign Android Tool)\r\n")

        val manifestDigest = md.digest(manifestBytes)
        val manifestDigestB64 = Base64.encodeToString(manifestDigest, Base64.NO_WRAP)
        sfSb.append("SHA-256-Digest-Manifest: $manifestDigestB64\r\n\r\n")

        for (entry in entries) {
            val sectionBytes = entryManifestSections[entry.name] ?: continue
            val sectionDigest = md.digest(sectionBytes)
            val sectionDigestB64 = Base64.encodeToString(sectionDigest, Base64.NO_WRAP)

            sfSb.append("Name: ${entry.name}\r\n")
            sfSb.append("SHA-256-Digest: $sectionDigestB64\r\n\r\n")
        }

        val sfBytes = sfSb.toString().toByteArray(Charsets.UTF_8)
        result.add(
            RawZipEntry(
                name = "META-INF/CERT.SF",
                data = sfBytes,
                compressionMethod = ZipEntry.DEFLATED
            )
        )

        // 3. Generate PKCS#7 CMS block (CERT.RSA or CERT.EC)
        val keyAlgorithm = privateKey.algorithm.uppercase()
        val sigAlg = if (keyAlgorithm.contains("EC")) "SHA256withECDSA" else "SHA256withRSA"
        val certBlockName = if (keyAlgorithm.contains("EC")) "META-INF/CERT.EC" else "META-INF/CERT.RSA"

        val certHolder = JcaX509CertificateHolder(certificate)
        val digestProvider = JcaDigestCalculatorProviderBuilder().setProvider(bcProvider).build()
        val contentSigner = JcaContentSignerBuilder(sigAlg).setProvider(bcProvider).build(privateKey)

        val signerInfoGen = JcaSignerInfoGeneratorBuilder(digestProvider).build(contentSigner, certHolder)

        val cmsGen = CMSSignedDataGenerator()
        cmsGen.addSignerInfoGenerator(signerInfoGen)
        cmsGen.addCertificate(certHolder)

        val cmsData = cmsGen.generate(CMSProcessableByteArray(sfBytes), true)
        val pkcs7Bytes = cmsData.encoded

        result.add(
            RawZipEntry(
                name = certBlockName,
                data = pkcs7Bytes,
                compressionMethod = ZipEntry.DEFLATED
            )
        )

        return result
    }
}
