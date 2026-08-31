package com.example.crypto

import com.example.crypto.keys.KeyPairFactory
import com.example.crypto.keystore.Pkcs12KeystoreSerializer
import com.example.crypto.x509.X509CertificateBuilder
import com.example.crypto.x509.X509CertificateInspector
import com.example.crypto.x509.X509CertificateUtils
import com.example.data.model.KeystoreConfig
import com.example.data.model.KeystoreDetails
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.io.File
import java.io.InputStream
import java.security.Security
import java.util.Date

/**
 * Unified facade for cryptographic Android PKCS#12 signing keystores and certificates.
 * Delegates key generation to [KeyPairFactory], certificate building to [X509CertificateBuilder],
 * and serialization to [Pkcs12KeystoreSerializer].
 */
object KeystoreGenerator {

    val bcProvider: BouncyCastleProvider by lazy {
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
     * When [saveToFile] is false, the keystore is generated purely in memory (Zero-Footprint / Ephemeral mode).
     */
    fun generateKeystore(
        outputDir: File,
        config: KeystoreConfig,
        saveToFile: Boolean = true
    ): KeystoreDetails {
        val provider = bcProvider

        // 1. Generate KeyPair via modular factory
        val keyPair = KeyPairFactory.generateKeyPair(config.algorithm, provider)

        // 2. Serial Number & Validity
        val notBefore = Date(System.currentTimeMillis() - (60 * 60 * 1000L))
        val notAfter = Date(System.currentTimeMillis() + (config.validityYears.toLong() * 365L * 24L * 60L * 60L * 1000L))

        // 3. Build X.509 Certificate via modular builder
        val x509Cert = X509CertificateBuilder.buildSelfSignedCertificate(
            keyPair = keyPair,
            algorithm = config.algorithm,
            distinguishedName = config.distinguishedName,
            validityYears = config.validityYears,
            notBefore = notBefore,
            notAfter = notAfter,
            provider = provider
        )

        // 4. Serialize to PKCS#12 Keystore via modular serializer
        return Pkcs12KeystoreSerializer.serialize(
            outputDir = outputDir,
            config = config,
            keyPair = keyPair,
            x509Cert = x509Cert,
            notBefore = notBefore,
            notAfter = notAfter,
            provider = provider,
            saveToFile = saveToFile
        )
    }

    /**
     * Inspects an existing keystore stream and extracts certificate fingerprints & info.
     */
    fun inspectKeystore(inputStream: InputStream, password: String): List<KeystoreDetails> {
        return X509CertificateInspector.inspectKeystore(inputStream, password, bcProvider)
    }

    /**
     * Inspects an existing keystore byte array and extracts certificate fingerprints & info.
     */
    fun inspectKeystore(bytes: ByteArray, password: String): List<KeystoreDetails> {
        return X509CertificateInspector.inspectKeystore(bytes, password, bcProvider)
    }

    /**
     * Generates a strong random password using CSPRNG.
     */
    fun generateRandomPassword(length: Int = 20): String {
        return PasswordGenerator.generate(length = length)
    }

    /**
     * Formats bytes to hexadecimal colon-separated fingerprint: AA:BB:CC:...
     */
    fun calculateFingerprint(bytes: ByteArray, algorithm: String): String {
        return X509CertificateUtils.calculateFingerprint(bytes, algorithm)
    }

    /**
     * Formats X.509 certificate to PEM format.
     */
    fun buildPemCertificate(certBytes: ByteArray): String {
        return X509CertificateUtils.buildPemCertificate(certBytes)
    }

    /**
     * Generates Gradle build.gradle.kts (Kotlin DSL) signing config snippet.
     */
    fun generateGradleKtsSnippet(fileName: String, alias: String): String {
        return SnippetGenerator.generateGradleKtsSnippet(fileName, alias)
    }

    /**
     * Generates Gradle build.gradle (Groovy DSL) signing config snippet.
     */
    fun generateGradleGroovySnippet(fileName: String, alias: String): String {
        return SnippetGenerator.generateGradleGroovySnippet(fileName, alias)
    }

    /**
     * Generates a ready-to-use GitHub Actions workflow file for automated building and signing.
     */
    fun generateGitHubActionsWorkflow(fileName: String, alias: String): String {
        return SnippetGenerator.generateGitHubActionsWorkflow(fileName, alias)
    }

    /**
     * Generates Gradle build.gradle.kts signing config snippet (Alias for generateGradleKtsSnippet).
     */
    fun generateGradleSnippet(fileName: String, alias: String): String {
        return SnippetGenerator.generateGradleKtsSnippet(fileName, alias)
    }

    /**
     * Generates CLI apksigner command snippet.
     */
    fun generateApksignerSnippet(fileName: String, alias: String): String {
        return SnippetGenerator.generateApksignerSnippet(fileName, alias)
    }
}
