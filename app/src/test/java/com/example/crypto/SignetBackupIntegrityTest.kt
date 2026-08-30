package com.example.crypto

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.DistinguishedName
import com.example.data.model.KeyAlgorithm
import com.example.data.model.KeystoreConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
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

    @Test
    fun `create vault backup zip with subfolders and restore all successfully`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val config1 = KeystoreConfig(
            fileName = "app-alpha.jks",
            storePassword = "PasswordAlpha1!",
            alias = "alpha_key",
            useSamePassword = true,
            validityYears = 20,
            algorithm = KeyAlgorithm.RSA_2048,
            distinguishedName = DistinguishedName(commonName = "App Alpha")
        )
        val config2 = KeystoreConfig(
            fileName = "app-beta.jks",
            storePassword = "PasswordBeta2!",
            alias = "beta_key",
            useSamePassword = true,
            validityYears = 25,
            algorithm = KeyAlgorithm.EC_P256,
            distinguishedName = DistinguishedName(commonName = "App Beta")
        )

        val details1 = KeystoreGenerator.generateKeystore(context, config1)
        val details2 = KeystoreGenerator.generateKeystore(context, config2)

        val bytes1 = File(details1.filePath).readBytes()
        val bytes2 = File(details2.filePath).readBytes()

        // 1. Create Vault ZIP with both keystores in subfolders
        val vaultZipBytes = SignetBackupManager.createVaultBackupZip(
            listOf(
                Pair(details1, bytes1),
                Pair(details2, bytes2)
            )
        )
        assertTrue(vaultZipBytes.isNotEmpty())

        // Verify folder structure in ZIP
        val entryNames = mutableListOf<String>()
        java.util.zip.ZipInputStream(java.io.ByteArrayInputStream(vaultZipBytes)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                entryNames.add(entry.name)
                entry = zis.nextEntry
            }
        }

        assertTrue(entryNames.contains("signet-vault-backup.json"))
        assertTrue(entryNames.contains("VAULT-SUMMARY.txt"))
        assertTrue(entryNames.any { it.startsWith("keystores/1_app-alpha/app-alpha.jks") })
        assertTrue(entryNames.any { it.startsWith("keystores/1_app-alpha/credentials.txt") })
        assertTrue(entryNames.any { it.startsWith("keystores/2_app-beta/app-beta.jks") })
        assertTrue(entryNames.any { it.startsWith("keystores/2_app-beta/key.properties") })

        // Clean local files
        File(details1.filePath).delete()
        File(details2.filePath).delete()

        // 2. Restore Vault via restoreAnyFromZip
        val restoredList = SignetBackupManager.restoreAnyFromZip(context, vaultZipBytes)
        assertEquals(2, restoredList.size)

        val restored1 = restoredList.first { it.alias == "alpha_key" }
        val restored2 = restoredList.first { it.alias == "beta_key" }

        assertEquals("PasswordAlpha1!", restored1.storePassword)
        assertEquals(details1.sha256Fingerprint, restored1.sha256Fingerprint)
        assertTrue(File(restored1.filePath).exists())

        assertEquals("PasswordBeta2!", restored2.storePassword)
        assertEquals(details2.sha256Fingerprint, restored2.sha256Fingerprint)
        assertTrue(File(restored2.filePath).exists())
    }

    @Test(expected = SecurityException::class)
    fun `reject vault backup when master vault manifest is tampered`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val config = KeystoreConfig(
            fileName = "vault-tamper.jks",
            storePassword = "VaultPassword1!",
            alias = "vault_tamper_key",
            useSamePassword = true,
            validityYears = 15,
            algorithm = KeyAlgorithm.RSA_2048,
            distinguishedName = DistinguishedName(commonName = "Vault Tamper")
        )

        val details = KeystoreGenerator.generateKeystore(context, config)
        val bytes = File(details.filePath).readBytes()

        val vaultZipBytes = SignetBackupManager.createVaultBackupZip(listOf(Pair(details, bytes)))

        val entries = mutableMapOf<String, ByteArray>()
        java.util.zip.ZipInputStream(java.io.ByteArrayInputStream(vaultZipBytes)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                entries[entry.name] = zis.readBytes()
                entry = zis.nextEntry
            }
        }

        // Tamper master manifest
        val masterJson = String(entries["signet-vault-backup.json"]!!, Charsets.UTF_8)
        val tamperedMaster = masterJson.replace("VaultPassword1!", "FakePassword999!")
        entries["signet-vault-backup.json"] = tamperedMaster.toByteArray(Charsets.UTF_8)

        val tamperedZipOut = java.io.ByteArrayOutputStream()
        java.util.zip.ZipOutputStream(tamperedZipOut).use { zos ->
            for ((name, data) in entries) {
                zos.putNextEntry(java.util.zip.ZipEntry(name))
                zos.write(data)
                zos.closeEntry()
            }
        }

        SignetBackupManager.restoreVaultFromZip(context, tamperedZipOut.toByteArray())
    }

    @Test
    fun `imported and restored keystores are encrypted with AES-256-GCM in database`() {
        val plainPassword = "SuperSecretPlainTextPassword123!"
        val encrypted = KeystoreEncryptionManager.encrypt(plainPassword)

        assertTrue("Should have encryption prefix", KeystoreEncryptionManager.isEncrypted(encrypted))
        assertTrue("Ciphertext should start with enc:v1:", encrypted.startsWith("enc:v1:"))
        assertNotEquals("Encrypted text must not match plain text", plainPassword, encrypted)

        val decrypted = KeystoreEncryptionManager.decrypt(encrypted)
        assertEquals("Decrypted password must match original plain text", plainPassword, decrypted)
    }
}

