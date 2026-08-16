package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.crypto.KeystoreGenerator
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
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Signet", appName)
  }

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

    // Test inspection of generated file
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
  }

  @Test
  fun `generate gradle and github actions snippets correctly`() {
    val ktsSnippet = KeystoreGenerator.generateGradleKtsSnippet("release.jks", "app_key")
    assertTrue(ktsSnippet.contains("release.jks"))
    assertTrue(ktsSnippet.contains("app_key"))
    assertTrue(ktsSnippet.contains("signingConfigs"))

    val groovySnippet = KeystoreGenerator.generateGradleGroovySnippet("release.jks", "app_key")
    assertTrue(groovySnippet.contains("release.jks"))
    assertTrue(groovySnippet.contains("app_key"))

    val ghWorkflow = KeystoreGenerator.generateGitHubActionsWorkflow("release.jks", "app_key")
    assertTrue(ghWorkflow.contains("KEYSTORE_BASE64"))
    assertTrue(ghWorkflow.contains("release.jks"))
    assertTrue(ghWorkflow.contains("assembleRelease"))

    val apksignerSnippet = KeystoreGenerator.generateApksignerSnippet("release.jks", "app_key")
    assertTrue(apksignerSnippet.contains("apksigner sign"))
    assertTrue(apksignerSnippet.contains("zipalign"))
  }

  @Test
  fun `generate ultra secure passwords with high entropy`() {
    val pwd20 = com.example.crypto.PasswordGenerator.generate(20)
    assertEquals(20, pwd20.length)
    assertTrue(pwd20.any { it.isUpperCase() })
    assertTrue(pwd20.any { it.isLowerCase() })
    assertTrue(pwd20.any { it.isDigit() })
    assertTrue(pwd20.any { it in com.example.crypto.PasswordGenerator.SYMBOLS })

    val entropy20 = com.example.crypto.PasswordGenerator.calculateEntropy(pwd20)
    assertTrue(entropy20 >= 100.0) // 20 chars with ~72 pool is > 120 bits

    val strength = com.example.crypto.PasswordGenerator.evaluateStrength(pwd20)
    assertEquals(com.example.crypto.PasswordGenerator.PasswordStrength.ULTRA, strength)

    val pwd32 = com.example.crypto.PasswordGenerator.generate(32)
    assertEquals(32, pwd32.length)
    assertTrue(com.example.crypto.PasswordGenerator.calculateEntropy(pwd32) > 180.0)
  }

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
    val zipBytes = com.example.crypto.SignetBackupManager.createBackupZip(details, keystoreBytes)
    assertTrue(zipBytes.isNotEmpty())

    // Simulate clean state (file deleted / app reinstalled)
    File(details.filePath).delete()

    // 2. Restore from ZIP
    val restored = com.example.crypto.SignetBackupManager.restoreFromZip(
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
    val zipBytes = com.example.crypto.SignetBackupManager.createBackupZip(details, keystoreBytes)

    // Unpack, modify manifest, repack
    val entries = mutableMapOf<String, ByteArray>()
    java.util.zip.ZipInputStream(java.io.ByteArrayInputStream(zipBytes)).use { zis ->
      var entry = zis.nextEntry
      while (entry != null) {
        entries[entry.name] = zis.readBytes()
        zis.closeEntry()
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
    com.example.crypto.SignetBackupManager.restoreFromZip(
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

    val zipBytes = com.example.crypto.SignetBackupManager.createBackupZip(details, keystoreBytes)

    val entries = mutableMapOf<String, ByteArray>()
    java.util.zip.ZipInputStream(java.io.ByteArrayInputStream(zipBytes)).use { zis ->
      var entry = zis.nextEntry
      while (entry != null) {
        entries[entry.name] = zis.readBytes()
        zis.closeEntry()
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
    com.example.crypto.SignetBackupManager.restoreFromZip(
      context,
      java.io.ByteArrayInputStream(tamperedZipOut.toByteArray())
    )
  }
}
