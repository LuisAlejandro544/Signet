package com.example.crypto

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.crypto.x509.X509CertificateInspector
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
import java.io.FileInputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class KeystoreGeneratorTest {

    @Test
    fun `generate RSA 2048 keystore with base64 and credentials`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val config = KeystoreConfig(
            fileName = "custom-app.keystore",
            storePassword = "TestPassword123!",
            alias = "mykey",
            useSamePassword = true,
            validityYears = 25,
            algorithm = KeyAlgorithm.RSA_2048,
            distinguishedName = DistinguishedName(
                commonName = "Test App",
                organization = "Test Org",
                countryCode = "ES"
            )
        )

        val details = KeystoreGenerator.generateKeystore(context, config)

        assertNotNull(details)
        assertEquals("custom-app.keystore", details.fileName)
        assertEquals("mykey", details.alias)
        assertEquals("TestPassword123!", details.storePassword)
        assertEquals("TestPassword123!", details.keyPassword)
        assertTrue(details.base64Content.isNotBlank())
        assertTrue(details.sha256Fingerprint.isNotBlank())
        assertTrue(details.sha1Fingerprint.isNotBlank())
        assertTrue(details.md5Fingerprint.isNotBlank())
        assertTrue(File(details.filePath).exists())

        // Validate Base64 decode matches file bytes
        val decodedBytes = java.util.Base64.getDecoder().decode(details.base64Content)
        val fileBytes = File(details.filePath).readBytes()
        assertTrue(decodedBytes.contentEquals(fileBytes))

        // Test inspection of generated file via KeystoreGenerator and X509CertificateInspector
        FileInputStream(File(details.filePath)).use { stream ->
            val inspected = KeystoreGenerator.inspectKeystore(stream, "TestPassword123!")
            assertEquals(1, inspected.size)
            assertEquals("mykey", inspected[0].alias)
            assertEquals(details.sha256Fingerprint, inspected[0].sha256Fingerprint)
            assertTrue(inspected[0].base64Content.isNotBlank())
            assertEquals("TestPassword123!", inspected[0].storePassword)
        }
    }

    @Test
    fun `generate EC P256 keystore successfully`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val config = KeystoreConfig(
            fileName = "ec-test-key.p12",
            storePassword = "SecurePass123!",
            alias = "ec-alias",
            useSamePassword = true,
            validityYears = 10,
            algorithm = KeyAlgorithm.EC_P256,
            distinguishedName = DistinguishedName(commonName = "EC App")
        )

        val details = KeystoreGenerator.generateKeystore(context, config)
        assertNotNull(details)
        assertTrue(details.sha256Fingerprint.isNotBlank())
        assertEquals(KeyAlgorithm.EC_P256.displayName, details.algorithm)
    }

    @Test
    fun `inspect keystore with x509 certificate inspector directly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val config = KeystoreConfig(
            fileName = "inspector-test.jks",
            storePassword = "InspectorPass99!",
            alias = "insp_alias",
            useSamePassword = true,
            validityYears = 5,
            algorithm = KeyAlgorithm.RSA_2048,
            distinguishedName = DistinguishedName(commonName = "Inspector App")
        )

        val details = KeystoreGenerator.generateKeystore(context, config)
        val fileBytes = File(details.filePath).readBytes()

        val results = X509CertificateInspector.inspectKeystore(
            bytes = fileBytes,
            password = "InspectorPass99!",
            provider = KeystoreGenerator.bcProvider
        )

        assertEquals(1, results.size)
        assertEquals("insp_alias", results[0].alias)
        assertEquals(details.sha256Fingerprint, results[0].sha256Fingerprint)
        assertEquals(details.sha1Fingerprint, results[0].sha1Fingerprint)
    }
}
