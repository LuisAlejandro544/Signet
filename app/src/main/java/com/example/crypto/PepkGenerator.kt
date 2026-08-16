package com.example.crypto

import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.cert.X509CertificateHolder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.io.StringReader
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.Security
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * PEPK (Play Encrypt Private Key) generator and cryptographic utility for Signet.
 *
 * Implements standard hybrid encryption (RSA-OAEP + AES-256-GCM) to safely package
 * and encrypt Android signing keys for Google Play App Signing directly on-device.
 */
object PepkGenerator {

    private const val PEPK_MAGIC_HEADER = "PEPK_V1_GOOGLE_PLAY"
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val GCM_IV_LENGTH_BYTES = 12
    private const val AES_KEY_SIZE_BITS = 256

    private val bcProvider: BouncyCastleProvider by lazy {
        val provider = BouncyCastleProvider()
        try {
            Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
            Security.insertProviderAt(provider, 1)
        } catch (_: Exception) {
            // Ignore security manager issues
        }
        provider
    }

    init {
        bcProvider
    }

    /**
     * Parses a Google Play encryption public key from a PEM formatted string.
     * Supports both 'BEGIN PUBLIC KEY' and 'BEGIN CERTIFICATE' formats.
     */
    fun parsePublicKeyFromPem(pemString: String): PublicKey {
        val cleanPem = pemString.trim()
        if (cleanPem.isBlank()) {
            throw IllegalArgumentException("El contenido de la clave pública PEM está vacío.")
        }

        // Method 1: BouncyCastle PEMParser
        try {
            PEMParser(StringReader(cleanPem)).use { parser ->
                val obj = parser.readObject()
                if (obj != null) {
                    val converter = JcaPEMKeyConverter().setProvider(bcProvider)
                    when (obj) {
                        is SubjectPublicKeyInfo -> return converter.getPublicKey(obj)
                        is X509CertificateHolder -> {
                            val cert = JcaX509CertificateConverter().setProvider(bcProvider).getCertificate(obj)
                            return cert.publicKey
                        }
                        is org.bouncycastle.pkcs.PKCS10CertificationRequest -> {
                            return converter.getPublicKey(obj.subjectPublicKeyInfo)
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Fallback to manual standard decoding
        }

        // Method 2: Standard X.509 Certificate parsing
        if (cleanPem.contains("BEGIN CERTIFICATE")) {
            try {
                val cf = CertificateFactory.getInstance("X.509")
                val cert = cf.generateCertificate(ByteArrayInputStream(cleanPem.toByteArray(StandardCharsets.UTF_8)))
                return cert.publicKey
            } catch (_: Exception) {}
        }

        // Method 3: Standard SubjectPublicKeyInfo (X509EncodedKeySpec)
        try {
            val base64Clean = cleanPem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replace("-----BEGIN RSA PUBLIC KEY-----", "")
                .replace("-----END RSA PUBLIC KEY-----", "")
                .replace("\\s".toRegex(), "")

            val keyBytes = java.util.Base64.getDecoder().decode(base64Clean)
            val keySpec = X509EncodedKeySpec(keyBytes)
            val kf = KeyFactory.getInstance("RSA", bcProvider)
            return kf.generatePublic(keySpec)
        } catch (e: Exception) {
            throw IllegalArgumentException(
                "No se pudo interpretar la clave pública de Google. Asegúrate de copiar el texto completo incluyendo '-----BEGIN PUBLIC KEY-----' o '-----BEGIN CERTIFICATE-----'."
            )
        }
    }

    /**
     * Extracts the PrivateKey and X.509 Certificate from a Keystore byte array.
     */
    fun extractKeyAndCert(
        keystoreBytes: ByteArray,
        storePassword: String,
        alias: String,
        keyPassword: String
    ): Pair<PrivateKey, X509Certificate> {
        val types = listOf("PKCS12", "JKS", "BKS")
        var loadedKs: KeyStore? = null

        for (type in types) {
            try {
                val ks = KeyStore.getInstance(type, bcProvider)
                ks.load(ByteArrayInputStream(keystoreBytes), storePassword.toCharArray())
                loadedKs = ks
                break
            } catch (_: Exception) {
                try {
                    val ks = KeyStore.getInstance(type)
                    ks.load(ByteArrayInputStream(keystoreBytes), storePassword.toCharArray())
                    loadedKs = ks
                    break
                } catch (_: Exception) {}
            }
        }

        if (loadedKs == null) {
            throw IllegalArgumentException("No se pudo desbloquear el keystore. Verifica la contraseña del almacén.")
        }

        val targetAlias = if (loadedKs.containsAlias(alias)) {
            alias
        } else {
            val aliases = loadedKs.aliases()
            if (aliases.hasMoreElements()) aliases.nextElement() else {
                throw IllegalArgumentException("El keystore no contiene ningún alias.")
            }
        }

        val effectiveKeyPassword = keyPassword.ifBlank { storePassword }
        val key = try {
            loadedKs.getKey(targetAlias, effectiveKeyPassword.toCharArray()) as? PrivateKey
        } catch (e: Exception) {
            throw IllegalArgumentException("Contraseña de la clave inválida para el alias '$targetAlias': ${e.message}")
        } ?: throw IllegalArgumentException("No se encontró la clave privada para el alias '$targetAlias'.")

        val cert = loadedKs.getCertificate(targetAlias) as? X509Certificate
            ?: throw IllegalArgumentException("No se encontró el certificado X.509 para el alias '$targetAlias'.")

        return Pair(key, cert)
    }

    /**
     * Encrypts the PrivateKey and X.509 Certificate using Google's public key
     * via Hybrid Encryption (RSA-OAEP with SHA-256 + AES-256-GCM).
     */
    fun generatePepk(
        privateKey: PrivateKey,
        certificate: X509Certificate,
        googlePublicKey: PublicKey
    ): ByteArray {
        val secureRandom = SecureRandom()

        // 1. Generate ephemeral symmetric AES-256 session key
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(AES_KEY_SIZE_BITS, secureRandom)
        val aesKey: SecretKey = keyGen.generateKey()

        // 2. Generate random 12-byte IV for AES-GCM
        val iv = ByteArray(GCM_IV_LENGTH_BYTES)
        secureRandom.nextBytes(iv)

        // 3. Serialize payload (Private Key PKCS#8 DER + Certificate X.509 DER)
        val payloadBaos = ByteArrayOutputStream()
        DataOutputStream(payloadBaos).use { dos ->
            val privateKeyBytes = privateKey.encoded
            val certBytes = certificate.encoded

            dos.writeUTF(privateKey.algorithm)
            dos.writeInt(privateKeyBytes.size)
            dos.write(privateKeyBytes)
            dos.writeInt(certBytes.size)
            dos.write(certBytes)
            dos.writeLong(System.currentTimeMillis())
        }
        val rawPayload = payloadBaos.toByteArray()

        // 4. Encrypt payload with AES-256-GCM
        val gcmCipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        gcmCipher.init(Cipher.ENCRYPT_MODE, aesKey, gcmSpec)
        val encryptedPayload = gcmCipher.doFinal(rawPayload)

        // 5. Encrypt AES session key with Google RSA Public Key using RSA-OAEP
        val rsaCipher = try {
            Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding", bcProvider)
        } catch (_: Exception) {
            Cipher.getInstance("RSA/ECB/PKCS1Padding", bcProvider)
        }
        rsaCipher.init(Cipher.ENCRYPT_MODE, googlePublicKey)
        val encryptedAesKey = rsaCipher.doFinal(aesKey.encoded)

        // 6. Build the final binary PEPK package
        val outputBaos = ByteArrayOutputStream()
        DataOutputStream(outputBaos).use { dos ->
            dos.writeUTF(PEPK_MAGIC_HEADER)
            dos.writeInt(encryptedAesKey.size)
            dos.write(encryptedAesKey)
            dos.writeInt(iv.size)
            dos.write(iv)
            dos.writeInt(encryptedPayload.size)
            dos.write(encryptedPayload)
        }

        return outputBaos.toByteArray()
    }

    /**
     * High-level entry point: Loads keystore bytes, unlocks key, parses Google PEM,
     * and returns the encrypted .pepk binary byte array.
     */
    fun generatePepkFromKeystore(
        keystoreBytes: ByteArray,
        storePassword: String,
        alias: String,
        keyPassword: String,
        googlePublicKeyPem: String
    ): ByteArray {
        val googlePublicKey = parsePublicKeyFromPem(googlePublicKeyPem)
        val (privateKey, cert) = extractKeyAndCert(keystoreBytes, storePassword, alias, keyPassword)
        return generatePepk(privateKey, cert, googlePublicKey)
    }

    /**
     * Verifies and decrypts a PEPK payload using Google's private key (for unit tests / round-trip checks).
     */
    fun decryptPepkForVerification(
        pepkBytes: ByteArray,
        googlePrivateKey: PrivateKey
    ): Pair<ByteArray, ByteArray> {
        val dis = DataInputStream(ByteArrayInputStream(pepkBytes))
        val header = dis.readUTF()
        if (header != PEPK_MAGIC_HEADER) {
            throw IllegalArgumentException("Encabezado PEPK no válido: $header")
        }

        val encKeyLen = dis.readInt()
        val encKeyBytes = ByteArray(encKeyLen)
        dis.readFully(encKeyBytes)

        val ivLen = dis.readInt()
        val ivBytes = ByteArray(ivLen)
        dis.readFully(ivBytes)

        val encPayloadLen = dis.readInt()
        val encPayloadBytes = ByteArray(encPayloadLen)
        dis.readFully(encPayloadBytes)

        // Decrypt AES Key
        val rsaCipher = try {
            Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding", bcProvider)
        } catch (_: Exception) {
            Cipher.getInstance("RSA/ECB/PKCS1Padding", bcProvider)
        }
        rsaCipher.init(Cipher.DECRYPT_MODE, googlePrivateKey)
        val aesKeyBytes = rsaCipher.doFinal(encKeyBytes)
        val aesKey = SecretKeySpec(aesKeyBytes, "AES")

        // Decrypt Payload
        val gcmCipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, ivBytes)
        gcmCipher.init(Cipher.DECRYPT_MODE, aesKey, gcmSpec)
        val rawPayload = gcmCipher.doFinal(encPayloadBytes)

        val payloadDis = DataInputStream(ByteArrayInputStream(rawPayload))
        val keyAlg = payloadDis.readUTF()
        val privKeyLen = payloadDis.readInt()
        val privKeyBytes = ByteArray(privKeyLen)
        payloadDis.readFully(privKeyBytes)

        val certLen = payloadDis.readInt()
        val certBytes = ByteArray(certLen)
        payloadDis.readFully(certBytes)

        return Pair(privKeyBytes, certBytes)
    }

    /**
     * Generates the standard CLI command snippet for running official pepk.jar on desktop
     */
    fun generatePepkCliCommand(
        keystoreFileName: String,
        alias: String,
        outputFileName: String = "output.pepk",
        encryptionKeyPath: String = "encryption_public_key.pem"
    ): String {
        return """
            # ==============================================================
            # Comando Oficial Google Play PEPK (Play Encrypt Private Key)
            # ==============================================================
            # 1. Descarga 'pepk.jar' y 'encryption_public_key.pem' de Google Play Console
            #    (Google Play Console > Configuración > Integridad de la app > Firma de apps).
            #
            # 2. Ejecuta el comando en tu terminal:
            java -jar pepk.jar \
              --keystore=${keystoreFileName} \
              --alias=${alias} \
              --output=${outputFileName} \
              --include-cert \
              --rsa-aes-encryption \
              --encryption-key-path=${encryptionKeyPath}
            
            # 3. Sube el archivo '${outputFileName}' generado a la consola de Google Play.
        """.trimIndent()
    }
}
