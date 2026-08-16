package com.example.crypto

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.DistinguishedName
import com.example.data.model.KeyAlgorithm
import com.example.data.model.KeystoreConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PepkGeneratorTest {

    @Test
    fun `generate and verify pepk payload with Google Play hybrid encryption`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val config = KeystoreConfig(
            fileName = "pepk-test.jks",
            storePassword = "PepkStorePassword123!",
            alias = "upload_key",
            keyPassword = "PepkStorePassword123!",
            useSamePassword = true,
            validityYears = 25,
            algorithm = KeyAlgorithm.RSA_2048,
            distinguishedName = DistinguishedName(commonName = "Google Play App")
        )

        val details = KeystoreGenerator.generateKeystore(context, config)
        val keystoreBytes = File(details.filePath).readBytes()

        // 1. Create a simulated Google Play RSA KeyPair (2048-bit)
        val kpg = java.security.KeyPairGenerator.getInstance("RSA")
        kpg.initialize(2048)
        val googleKeyPair = kpg.generateKeyPair()

        // Convert simulated Google Public Key to PEM
        val pubKeyBase64 = java.util.Base64.getMimeEncoder(64, "\n".toByteArray()).encodeToString(googleKeyPair.public.encoded)
        val googlePem = "-----BEGIN PUBLIC KEY-----\n$pubKeyBase64\n-----END PUBLIC KEY-----"

        // 2. Generate .pepk binary package
        val pepkBytes = PepkGenerator.generatePepkFromKeystore(
            keystoreBytes = keystoreBytes,
            storePassword = "PepkStorePassword123!",
            alias = "upload_key",
            keyPassword = "PepkStorePassword123!",
            googlePublicKeyPem = googlePem
        )

        assertNotNull(pepkBytes)
        assertTrue(pepkBytes.isNotEmpty())

        // 3. Verify and decrypt using Google Private Key
        val (decryptedPrivKeyBytes, decryptedCertBytes) = PepkGenerator.decryptPepkForVerification(
            pepkBytes = pepkBytes,
            googlePrivateKey = googleKeyPair.private
        )

        assertTrue(decryptedPrivKeyBytes.isNotEmpty())
        assertTrue(decryptedCertBytes.isNotEmpty())

        // Verify the decrypted certificate matches the keystore fingerprint
        val cf = java.security.cert.CertificateFactory.getInstance("X.509")
        val cert = cf.generateCertificate(java.io.ByteArrayInputStream(decryptedCertBytes)) as java.security.cert.X509Certificate
        val md = java.security.MessageDigest.getInstance("SHA-256")
        val certFingerprint = md.digest(cert.encoded).joinToString(":") { "%02X".format(it) }
        assertEquals(details.sha256Fingerprint, certFingerprint)

        // 4. Test ZIP bundle containing PEPK export
        val zipBundleBytes = SignetBackupManager.createBackupZip(
            details = details,
            keystoreBytes = keystoreBytes,
            pepkBytes = pepkBytes,
            pepkFileName = "upload_key_encrypted_key.pepk"
        )
        assertTrue(zipBundleBytes.isNotEmpty())

        val zipEntries = mutableMapOf<String, ByteArray>()
        java.util.zip.ZipInputStream(java.io.ByteArrayInputStream(zipBundleBytes)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                zipEntries[entry.name] = zis.readBytes()
                entry = zis.nextEntry
            }
        }

        assertTrue(zipEntries.containsKey("pepk-test.jks"))
        assertTrue(zipEntries.containsKey("upload_key_encrypted_key.pepk"))
        assertTrue(zipEntries.containsKey("signet-backup.json"))
        assertTrue(zipEntries.containsKey("credentials.txt"))
        assertTrue(zipEntries.containsKey("key.properties"))
        assertTrue(zipEntries.containsKey("base64.txt"))
        assertTrue(zipEntries.containsKey("README-BACKUP.txt"))

        // Simulate clean restore state (file deleted)
        File(details.filePath).delete()

        // Verify restore works seamlessly with the ZIP bundle containing PEPK
        val restoredFromBundle = SignetBackupManager.restoreFromZip(
            context,
            java.io.ByteArrayInputStream(zipBundleBytes)
        )
        assertNotNull(restoredFromBundle)
        assertEquals(details.fileName, restoredFromBundle.fileName)
        assertEquals(details.alias, restoredFromBundle.alias)
        assertEquals(details.sha256Fingerprint, restoredFromBundle.sha256Fingerprint)
        assertTrue(File(restoredFromBundle.filePath).exists())
    }
}
