package com.example.crypto

import com.example.data.local.KeystoreEntity
import com.example.data.model.KeystoreDetails
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class KeystoreEncryptionManagerTest {

    @Test
    fun `encrypt and decrypt returns original password correctly`() {
        val password = "SuperSecretPassword123!@#"
        val encrypted = KeystoreEncryptionManager.encrypt(password)

        assertTrue("El texto cifrado debe comenzar con el prefijo enc:v1:", encrypted.startsWith(KeystoreEncryptionManager.ENC_PREFIX))
        assertNotEquals(password, encrypted)
        assertTrue(KeystoreEncryptionManager.isEncrypted(encrypted))

        val decrypted = KeystoreEncryptionManager.decrypt(encrypted)
        assertEquals("El texto descifrado debe coincidir exactamente con el original", password, decrypted)
    }

    @Test
    fun `decrypt handles legacy unencrypted text transparently`() {
        val legacyPlainPassword = "my_plain_legacy_pass"
        val decrypted = KeystoreEncryptionManager.decrypt(legacyPlainPassword)

        assertEquals("Texto plano sin prefijo debe devolverse tal cual", legacyPlainPassword, decrypted)
        assertFalse(KeystoreEncryptionManager.isEncrypted(legacyPlainPassword))
    }

    @Test
    fun `empty or blank strings are handled safely`() {
        assertEquals("", KeystoreEncryptionManager.encrypt(""))
        assertEquals("", KeystoreEncryptionManager.decrypt(""))
    }

    @Test
    fun `entity mapping encrypts passwords in database entity and decrypts into details`() {
        val details = KeystoreDetails(
            id = 1L,
            fileName = "release-app.jks",
            alias = "prod-key",
            filePath = "/data/user/0/com.example/files/release-app.jks",
            fileSizeBytes = 2048L,
            storePassword = "StoreMasterKey2026!",
            keyPassword = "KeyIndividualPass#99",
            base64Content = "VGhpcyBpcyBhIGZha2Uga2V5c3RvcmU=",
            sha256Fingerprint = "AA:BB:CC:DD:EE",
            sha1Fingerprint = "11:22:33:44:55",
            md5Fingerprint = "99:88:77:66",
            validFrom = 1000L,
            validUntil = 5000L,
            algorithm = "RSA_2048",
            subjectDn = "CN=Signet, O=Signet Dev",
            issuerDn = "CN=Signet, O=Signet Dev",
            serialNumber = "123456789",
            certificatePem = "-----BEGIN CERTIFICATE-----\nMIIB...\n-----END CERTIFICATE-----",
            createdAt = 1000L
        )

        // Convert to entity (persisted state)
        val entity = KeystoreEntity.fromDetails(details)

        assertTrue(entity.storePassword.startsWith(KeystoreEncryptionManager.ENC_PREFIX))
        assertTrue(entity.keyPassword.startsWith(KeystoreEncryptionManager.ENC_PREFIX))
        assertNotEquals(details.storePassword, entity.storePassword)
        assertNotEquals(details.keyPassword, entity.keyPassword)

        // Convert back to details (in-memory domain model)
        val restoredDetails = entity.toDetails()

        assertEquals(details.storePassword, restoredDetails.storePassword)
        assertEquals(details.keyPassword, restoredDetails.keyPassword)
        assertEquals(details.alias, restoredDetails.alias)
        assertEquals(details.fileName, restoredDetails.fileName)
    }
}
