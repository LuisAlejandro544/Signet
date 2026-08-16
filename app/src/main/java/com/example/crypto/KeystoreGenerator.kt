package com.example.crypto

import android.content.Context
import android.util.Base64
import com.example.data.model.DistinguishedName
import com.example.data.model.KeyAlgorithm
import com.example.data.model.KeystoreConfig
import com.example.data.model.KeystoreDetails
import org.bouncycastle.asn1.x500.X500NameBuilder
import org.bouncycastle.asn1.x500.style.BCStyle
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.Security
import java.security.cert.X509Certificate
import java.security.spec.ECGenParameterSpec
import java.util.Date
import java.util.Locale

object KeystoreGenerator {

    private val bcProvider: BouncyCastleProvider by lazy {
        val provider = BouncyCastleProvider()
        try {
            Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
            Security.insertProviderAt(provider, 1)
        } catch (_: Exception) {
            // Ignore security manager issues if any
        }
        provider
    }

    init {
        // Force provider initialization
        bcProvider
    }

    /**
     * Generates a new PKCS12 keystore file and returns its details and fingerprints.
     */
    fun generateKeystore(context: Context, config: KeystoreConfig): KeystoreDetails {
        val provider = bcProvider

        // 1. Generate KeyPair
        val keyPair: KeyPair = when (config.algorithm) {
            KeyAlgorithm.RSA_2048 -> {
                val kpg = KeyPairGenerator.getInstance("RSA", provider)
                kpg.initialize(2048, SecureRandom())
                kpg.generateKeyPair()
            }
            KeyAlgorithm.RSA_4096 -> {
                val kpg = KeyPairGenerator.getInstance("RSA", provider)
                kpg.initialize(4096, SecureRandom())
                kpg.generateKeyPair()
            }
            KeyAlgorithm.EC_P256 -> {
                val kpg = KeyPairGenerator.getInstance("EC", provider)
                kpg.initialize(ECGenParameterSpec("secp256r1"), SecureRandom())
                kpg.generateKeyPair()
            }
        }

        // 2. Build Distinguished Name (X.500 Name)
        val nameBuilder = X500NameBuilder(BCStyle.INSTANCE)
        val dn = config.distinguishedName
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

        val subjectName = nameBuilder.build()
        val issuerName = subjectName // Self-signed

        // 3. Serial Number & Validity
        val serialNumber = BigInteger(64, SecureRandom())
        // Start 1 hour in the past to avoid timezone / clock drift issues
        val notBefore = Date(System.currentTimeMillis() - (60 * 60 * 1000L))
        val notAfter = Date(System.currentTimeMillis() + (config.validityYears.toLong() * 365L * 24L * 60L * 60L * 1000L))

        // 4. Build X.509 Certificate
        val certBuilder = JcaX509v3CertificateBuilder(
            issuerName,
            serialNumber,
            notBefore,
            notAfter,
            subjectName,
            keyPair.public
        )

        val signer = JcaContentSignerBuilder(config.algorithm.sigAlg)
            .setProvider(provider)
            .build(keyPair.private)

        val certHolder = certBuilder.build(signer)
        val x509Cert: X509Certificate = JcaX509CertificateConverter()
            .setProvider(provider)
            .getCertificate(certHolder)

        // 5. Create KeyStore (PKCS12 format, standard for Android Studio / Gradle / JARSIGNER / APKSIGNER)
        val keyStore = KeyStore.getInstance("PKCS12", provider)
        keyStore.load(null, null)

        val keyPassword = if (config.useSamePassword) config.storePassword else config.keyPassword
        keyStore.setKeyEntry(
            config.alias.trim(),
            keyPair.private,
            keyPassword.toCharArray(),
            arrayOf(x509Cert)
        )

        // 6. Save Keystore to application storage
        val keystoresDir = File(context.filesDir, "keystores")
        if (!keystoresDir.exists()) {
            keystoresDir.mkdirs()
        }

        val sanitizedFileName = if (config.fileName.isBlank()) "my-release-key.jks" else {
            val name = config.fileName.trim()
            if (name.endsWith(".jks", ignoreCase = true) || name.endsWith(".keystore", ignoreCase = true) || name.endsWith(".p12", ignoreCase = true)) {
                name
            } else {
                "$name.jks"
            }
        }

        val outputFile = File(keystoresDir, sanitizedFileName)
        val keystoreBytes: ByteArray
        java.io.ByteArrayOutputStream().use { baos ->
            keyStore.store(baos, config.storePassword.toCharArray())
            keystoreBytes = baos.toByteArray()
        }

        FileOutputStream(outputFile).use { fos ->
            fos.write(keystoreBytes)
        }

        val base64String = java.util.Base64.getEncoder().encodeToString(keystoreBytes)

        // 7. Compute Fingerprints
        val certEncoded = x509Cert.encoded
        val sha256 = calculateFingerprint(certEncoded, "SHA-256")
        val sha1 = calculateFingerprint(certEncoded, "SHA-1")
        val md5 = calculateFingerprint(certEncoded, "MD5")

        val pem = buildPemCertificate(certEncoded)

        return KeystoreDetails(
            fileName = sanitizedFileName,
            alias = config.alias.trim(),
            filePath = outputFile.absolutePath,
            fileSizeBytes = outputFile.length(),
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

    /**
     * Inspects an existing keystore stream and extracts certificate fingerprints & info
     */
    fun inspectKeystore(inputStream: InputStream, password: String): List<KeystoreDetails> {
        return inspectKeystore(inputStream.readBytes(), password)
    }

    /**
     * Inspects an existing keystore byte array and extracts certificate fingerprints & info
     */
    fun inspectKeystore(bytes: ByteArray, password: String): List<KeystoreDetails> {
        val results = mutableListOf<KeystoreDetails>()
        val types = listOf("PKCS12", "JKS", "BKS")
        var loadedKeyStore: KeyStore? = null

        // Try supported keystore types
        for (type in types) {
            try {
                val ks = KeyStore.getInstance(type)
                ks.load(bytes.inputStream(), password.toCharArray())
                loadedKeyStore = ks
                break
            } catch (_: Exception) {
                try {
                    val ks = KeyStore.getInstance(type, bcProvider)
                    ks.load(bytes.inputStream(), password.toCharArray())
                    loadedKeyStore = ks
                    break
                } catch (_: Exception) {
                    // Try next format
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
            val sha256 = calculateFingerprint(certEncoded, "SHA-256")
            val sha1 = calculateFingerprint(certEncoded, "SHA-1")
            val md5 = calculateFingerprint(certEncoded, "MD5")
            val pem = buildPemCertificate(certEncoded)

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

    /**
     * Generates a strong random password using CSPRNG
     */
    fun generateRandomPassword(length: Int = 20): String {
        return PasswordGenerator.generate(length = length)
    }

    /**
     * Formats bytes to hexadecimal colon-separated fingerprint: AA:BB:CC:...
     */
    fun calculateFingerprint(bytes: ByteArray, algorithm: String): String {
        val md = MessageDigest.getInstance(algorithm)
        val digest = md.digest(bytes)
        return digest.joinToString(":") { byte -> "%02X".format(byte) }
    }

    /**
     * Formats X.509 certificate to PEM format
     */
    fun buildPemCertificate(certBytes: ByteArray): String {
        val base64 = Base64.encodeToString(certBytes, Base64.NO_WRAP)
        val chunks = base64.chunked(64).joinToString("\n")
        return "-----BEGIN CERTIFICATE-----\n$chunks\n-----END CERTIFICATE-----"
    }

    /**
     * Generates Gradle build.gradle.kts (Kotlin DSL) signing config snippet
     */
    fun generateGradleKtsSnippet(fileName: String, alias: String): String {
        return SnippetGenerator.generateGradleKtsSnippet(fileName, alias)
    }

    /**
     * Generates Gradle build.gradle (Groovy DSL) signing config snippet
     */
    fun generateGradleGroovySnippet(fileName: String, alias: String): String {
        return SnippetGenerator.generateGradleGroovySnippet(fileName, alias)
    }

    /**
     * Generates a ready-to-use GitHub Actions workflow file for automated building and signing
     */
    fun generateGitHubActionsWorkflow(fileName: String, alias: String): String {
        return SnippetGenerator.generateGitHubActionsWorkflow(fileName, alias)
    }

    /**
     * Generates Gradle build.gradle.kts signing config snippet (Alias for generateGradleKtsSnippet)
     */
    fun generateGradleSnippet(fileName: String, alias: String): String {
        return SnippetGenerator.generateGradleKtsSnippet(fileName, alias)
    }

    /**
     * Generates CLI apksigner command snippet
     */
    fun generateApksignerSnippet(fileName: String, alias: String): String {
        return SnippetGenerator.generateApksignerSnippet(fileName, alias)
    }
}
