package com.example.crypto.x509

import com.example.data.model.DistinguishedName
import com.example.data.model.KeyAlgorithm
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x500.X500NameBuilder
import org.bouncycastle.asn1.x500.style.BCStyle
import org.bouncycastle.asn1.x509.BasicConstraints
import org.bouncycastle.asn1.x509.ExtendedKeyUsage
import org.bouncycastle.asn1.x509.Extension
import org.bouncycastle.asn1.x509.KeyPurposeId
import org.bouncycastle.asn1.x509.KeyUsage
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.math.BigInteger
import java.security.KeyPair
import java.security.Provider
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.Date
import java.util.Locale

/**
 * Builder for creating self-signed X.509 certificates with universal Code Signing ASN.1 extensions.
 */
object X509CertificateBuilder {

    /**
     * Builds an X.500 Name from a [DistinguishedName] configuration.
     */
    fun buildSubjectName(dn: DistinguishedName): X500Name {
        val nameBuilder = X500NameBuilder(BCStyle.INSTANCE)
        var hasAttribute = false

        if (dn.commonName.isNotBlank()) {
            nameBuilder.addRDN(BCStyle.CN, dn.commonName.trim())
            hasAttribute = true
        }
        if (dn.organizationalUnit.isNotBlank()) {
            nameBuilder.addRDN(BCStyle.OU, dn.organizationalUnit.trim())
            hasAttribute = true
        }
        if (dn.organization.isNotBlank()) {
            nameBuilder.addRDN(BCStyle.O, dn.organization.trim())
            hasAttribute = true
        }
        if (dn.locality.isNotBlank()) {
            nameBuilder.addRDN(BCStyle.L, dn.locality.trim())
            hasAttribute = true
        }
        if (dn.state.isNotBlank()) {
            nameBuilder.addRDN(BCStyle.ST, dn.state.trim())
            hasAttribute = true
        }
        if (dn.countryCode.isNotBlank()) {
            nameBuilder.addRDN(BCStyle.C, dn.countryCode.trim().uppercase(Locale.ROOT))
            hasAttribute = true
        }
        if (!hasAttribute) {
            nameBuilder.addRDN(BCStyle.CN, "Android Signing Key")
        }

        return nameBuilder.build()
    }

    /**
     * Builds and self-signs an [X509Certificate] with the given parameters and universal Code Signing extensions.
     */
    fun buildSelfSignedCertificate(
        keyPair: KeyPair,
        algorithm: KeyAlgorithm,
        distinguishedName: DistinguishedName,
        validityYears: Int,
        notBefore: Date,
        notAfter: Date,
        provider: Provider
    ): X509Certificate {
        val subjectName = buildSubjectName(distinguishedName)
        val issuerName = subjectName // Self-signed
        val serialNumber = BigInteger(64, SecureRandom())

        val certBuilder = JcaX509v3CertificateBuilder(
            issuerName,
            serialNumber,
            notBefore,
            notAfter,
            subjectName,
            keyPair.public
        )

        // Universal Extensions for Android APKs & Desktop/Windows Code Signing Authenticode (.pfx / .p12)
        try {
            certBuilder.addExtension(Extension.basicConstraints, true, BasicConstraints(false))
            certBuilder.addExtension(
                Extension.keyUsage,
                true,
                KeyUsage(KeyUsage.digitalSignature or KeyUsage.keyEncipherment or KeyUsage.dataEncipherment)
            )
            certBuilder.addExtension(
                Extension.extendedKeyUsage,
                false,
                ExtendedKeyUsage(arrayOf(KeyPurposeId.id_kp_codeSigning, KeyPurposeId.id_kp_clientAuth))
            )
        } catch (_: Exception) {
            // Fallback gracefully if extension construction throws
        }

        val signer = JcaContentSignerBuilder(algorithm.sigAlg)
            .setProvider(provider)
            .build(keyPair.private)

        val certHolder = certBuilder.build(signer)
        return JcaX509CertificateConverter()
            .setProvider(provider)
            .getCertificate(certHolder)
    }
}
