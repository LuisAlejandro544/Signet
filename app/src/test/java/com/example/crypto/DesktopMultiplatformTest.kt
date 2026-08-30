package com.example.crypto

import com.example.data.DesktopKeystoreDataSource
import com.example.data.model.KeystoreDetails
import com.example.ui.preferences.DesktopPreferencesDataSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DesktopMultiplatformTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testBase64Compat_encodeAndDecode() {
        val originalText = "Signet_CrossPlatform_Crypto_Payload_2026!#$%"
        val originalBytes = originalText.toByteArray(Charsets.UTF_8)

        val encoded = Base64Compat.encodeToString(originalBytes)
        val decodedBytes = Base64Compat.decode(encoded)
        val decodedText = String(decodedBytes, Charsets.UTF_8)

        assertEquals(originalText, decodedText)
    }

    @Test
    fun testDesktopPreferencesDataSource_persistence() {
        val configFile = tempFolder.newFile("test_prefs.properties")
        val ds1 = DesktopPreferencesDataSource(configFile)

        ds1.putString("test_key", "signet_desktop_value")
        ds1.putBoolean("test_bool", true)

        // Crear una nueva instancia leyendo el mismo archivo en disco
        val ds2 = DesktopPreferencesDataSource(configFile)
        assertEquals("signet_desktop_value", ds2.getString("test_key", "default"))
        assertTrue(ds2.getBoolean("test_bool", false))
    }

    @Test
    fun testDesktopKeystoreDataSource_crudOperations() = runBlocking {
        val testDataDir = tempFolder.newFolder("signet_data")
        val ds = DesktopKeystoreDataSource(testDataDir)

        val sampleDetails = KeystoreDetails(
            id = 0L,
            fileName = "desktop_test.jks",
            alias = "desktop_alias",
            filePath = "/path/to/desktop_test.jks",
            fileSizeBytes = 2048L,
            storePassword = "enc:v1:pass1",
            keyPassword = "enc:v1:pass2",
            base64Content = "AQIDBA==",
            sha256Fingerprint = "AA:BB:CC:DD",
            sha1Fingerprint = "11:22:33:44",
            md5Fingerprint = "55:66:77:88",
            validFrom = 1000L,
            validUntil = 2000L,
            algorithm = "RSA",
            subjectDn = "CN=Test",
            issuerDn = "CN=Test",
            serialNumber = "123456",
            certificatePem = "-----BEGIN CERTIFICATE-----\n-----END CERTIFICATE-----",
            createdAt = System.currentTimeMillis()
        )

        val insertedId = ds.insertKeystore(sampleDetails)
        assertTrue("El ID asignado debe ser mayor a cero", insertedId > 0)

        val retrieved = ds.getKeystoreById(insertedId)
        assertNotNull("El keystore debe existir en el índice", retrieved)
        assertEquals("desktop_test.jks", retrieved?.fileName)
        assertEquals("desktop_alias", retrieved?.alias)

        val allList = ds.getAllKeystores().first()
        assertEquals(1, allList.size)

        // Probar persistencia releyendo desde una nueva instancia
        val dsReloaded = DesktopKeystoreDataSource(testDataDir)
        val reloadedList = dsReloaded.getAllKeystores().first()
        assertEquals(1, reloadedList.size)
        assertEquals("desktop_test.jks", reloadedList[0].fileName)

        // Probar eliminación
        dsReloaded.deleteKeystoreById(insertedId)
        val emptyList = dsReloaded.getAllKeystores().first()
        assertTrue(emptyList.isEmpty())
    }

    @Test
    fun testDesktopStorageUtils_pathResolution() {
        val dataDir = DesktopStorageUtils.getDesktopDataDir()
        assertNotNull(dataDir)
        assertTrue(dataDir.path.isNotBlank())
    }
}
