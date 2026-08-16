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
class SignetBackupIntegrityTest {

    @Test
    fun `create signed backup zip and restore successfully`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val config = KeystoreConfig(
            fileName = "backup-test.jks",
            storePassword = "StorePassword999!",
            alias = "signet_key",
            keyPassword = "KeyPassword999!",
            useSamePassword = false,
            validityYears = 25,
            algorithm = KeyAlgorithm.RSA_2048,
            distinguishedName = DistinguishedName(commonName = "Signet Backup App", organization = "Signet Lab")
        )

        val details = KeystoreGenerator.generateKeystore(context, config)
        val keystoreBytes = File(details.filePath).readBytes()

        // 1. Create signed ZIP bundle
        val zipBytes = SignetBackupManager.createBackupZip(details, keystoreBytes)
        assertTrue(zipBytes.isNotEmpty())

        // Simulate clean state (file deleted / app reinstalled)
        File(details.filePath).delete()

        // 2. Restore from ZIP
        val restored = SignetBackupManager.restoreFromZip(
            context,
            java.io.ByteArrayInputStream(zipBytes)
        )

        assertNotNull(restored)
        assertEquals(details.fileName, restored.fileName)
        assertEquals(details.alias, restored.alias)
        assertEquals("StorePassword999!", restored.storePassword)
        assertEquals("KeyPassword999!", restored.keyPassword)
        assertEquals(details.sha256Fingerprint, restored.sha256Fingerprint)
        assertTrue(File(restored.filePath).exists())
    }

    @Test(expected = SecurityException::class)
    fun `reject backup zip when json manifest is tampered`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val config = KeystoreConfig(
            fileName = "tamper-test.jks",
            storePassword = "OriginalPassword1!",
            alias = "original_alias",
            useSamePassword = true,
            validityYears = 10,
            algorithm = KeyAlgorithm.RSA_2048,
            distinguishedName = DistinguishedName(commonName = "Tamper App")
        )

        val details = KeystoreGenerator.generateKeystore(context, config)
        val keystoreBytes = File(details.filePath).readBytes()

        // Create valid zip
        val zipBytes = SignetBackupManager.createBackupZip(details, keystoreBytes)

        // Unpack, modify manifest, repack
        val entries = mutableMapOf<String, ByteArray>()
        java.util.zip.ZipInputStream(java.io.ByteArrayInputStream(zipBytes)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                entries[entry.name] = zis.readBytes()
                entry = zis.nextEntry
            }
        }

        // Tamper the manifest (e.g. alter storePassword without resigning)
        val manifestJson = String(entries["signet-backup.json"]!!, Charsets.UTF_8)
        val tamperedJson = manifestJson.replace("OriginalPassword1!", "HackedPassword99!")
        entries["signet-backup.json"] = tamperedJson.toByteArray(Charsets.UTF_8)

        // Rebuild zip
        val tamperedZipOut = java.io.ByteArrayOutputStream()
        java.util.zip.ZipOutputStream(tamperedZipOut).use { zos ->
            for ((name, data) in entries) {
                zos.putNextEntry(java.util.zip.ZipEntry(name))
                zos.write(data)
                zos.closeEntry()
            }
        }

        // Should throw SecurityException
        SignetBackupManager.restoreFromZip(
            context,
            java.io.ByteArrayInputStream(tamperedZipOut.toByteArray())
        )
    }

    @Test(expected = SecurityException::class)
    fun `reject backup zip when keystore binary is tampered`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val config = KeystoreConfig(
            fileName = "tamper-bin.jks",
            storePassword = "OriginalPassword1!",
            alias = "original_alias",
            useSamePassword = true,
            validityYears = 10,
            algorithm = KeyAlgorithm.RSA_2048,
            distinguishedName = DistinguishedName(commonName = "Tamper Bin App")
        )

        val details = KeystoreGenerator.generateKeystore(context, config)
        val keystoreBytes = File(details.filePath).readBytes()

        val zipBytes = SignetBackupManager.createBackupZip(details, keystoreBytes)

        val entries = mutableMapOf<String, ByteArray>()
        java.util.zip.ZipInputStream(java.io.ByteArrayInputStream(zipBytes)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                entries[entry.name] = zis.readBytes()
                entry = zis.nextEntry
            }
        }

        // Alter binary file
        val bin = entries["tamper-bin.jks"]!!
        bin[0] = (bin[0] + 1).toByte()
        entries["tamper-bin.jks"] = bin

        val tamperedZipOut = java.io.ByteArrayOutputStream()
        java.util.zip.ZipOutputStream(tamperedZipOut).use { zos ->
            for ((name, data) in entries) {
                zos.putNextEntry(java.util.zip.ZipEntry(name))
                zos.write(data)
                zos.closeEntry()
            }
        }

        // Should throw SecurityException
        SignetBackupManager.restoreFromZip(
            context,
            java.io.ByteArrayInputStream(tamperedZipOut.toByteArray())
        )
    }
}
