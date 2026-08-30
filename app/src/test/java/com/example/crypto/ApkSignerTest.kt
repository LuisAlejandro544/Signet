package com.example.crypto

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.crypto.signer.ApkSigner
import com.example.data.model.ApkSigningOptions
import com.example.data.model.DistinguishedName
import com.example.data.model.KeyAlgorithm
import com.example.data.model.KeystoreConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ApkSignerTest {

    private fun createSyntheticUnsignedApk(): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            // AndroidManifest.xml (Stored)
            val manifestData = "FakeBinaryManifestXmlDataForSignetTesting".toByteArray(Charsets.UTF_8)
            val entryManifest = ZipEntry("AndroidManifest.xml").apply {
                method = ZipEntry.STORED
                size = manifestData.size.toLong()
                crc = java.util.zip.CRC32().apply { update(manifestData) }.value
            }
            zos.putNextEntry(entryManifest)
            zos.write(manifestData)
            zos.closeEntry()

            // classes.dex (Deflated)
            val dexData = "SyntheticDexBytecodeSignet2026".toByteArray(Charsets.UTF_8)
            val entryDex = ZipEntry("classes.dex").apply {
                method = ZipEntry.DEFLATED
            }
            zos.putNextEntry(entryDex)
            zos.write(dexData)
            zos.closeEntry()

            // res/raw/asset.png (Stored - requires 4-byte zipalign)
            val assetData = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
            val entryAsset = ZipEntry("res/raw/asset.png").apply {
                method = ZipEntry.STORED
                size = assetData.size.toLong()
                crc = java.util.zip.CRC32().apply { update(assetData) }.value
            }
            zos.putNextEntry(entryAsset)
            zos.write(assetData)
            zos.closeEntry()
        }
        return baos.toByteArray()
    }

    @Test
    fun testApkSigningWorkflow_dualSchemeAndZipalign() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // 1. Generate real Keystore
        val config = KeystoreConfig(
            fileName = "signer-test-key.jks",
            storePassword = "TestPassword2026!",
            alias = "testalias",
            keyPassword = "TestPassword2026!",
            validityYears = 25,
            algorithm = KeyAlgorithm.RSA_2048,
            distinguishedName = DistinguishedName(commonName = "Signet Test Dev", organization = "Signet Labs")
        )
        val generated = KeystoreGenerator.generateKeystore(context, config)
        val jksBytes = File(generated.filePath).readBytes()
        assertNotNull(jksBytes)

        // 2. Create synthetic unsigned APK
        val unsignedApk = createSyntheticUnsignedApk()

        // 3. Sign APK with dual scheme (v1 + v2) and zipalign
        val options = ApkSigningOptions(
            signV1 = true,
            signV2 = true,
            zipalign = true,
            outputFileName = "test-signed.apk"
        )

        val result = ApkSigner.signApk(
            apkBytes = unsignedApk,
            keystoreBytes = jksBytes,
            storePassword = config.storePassword,
            alias = config.alias,
            keyPassword = config.keyPassword,
            options = options,
            context = context
        )

        // 4. Assertions
        assertTrue("La firma del APK debería ser exitosa", result.isSuccess)
        assertNotNull(result.signedApkBytes)
        assertTrue(result.signedApkBytes!!.isNotEmpty())
        assertTrue(result.appliedSchemes.contains("v1 (JAR Signing)"))
        assertTrue(result.appliedSchemes.contains("v2 (APK Signature Scheme v2)"))
        assertEquals(generated.sha256Fingerprint, result.sha256Fingerprint)

        // 5. Verify that APK contains V2 signing block magic
        val signedBytes = result.signedApkBytes!!
        val magicString = String(signedBytes, Charsets.US_ASCII)
        assertTrue("Debe contener la marca mágica de APK Sig Block 42", magicString.contains("APK Sig Block 42"))

        // 6. Verify with ApkMatcher
        val analyzed = ApkMatcher.analyzeApk(context, signedBytes, "test-signed.apk")
        assertTrue("ApkMatcher debe encontrar certificados en el APK firmado", analyzed.certificates.isNotEmpty())
    }

    @Test
    fun testApkSigningWorkflow_tripleScheme_v1_v2_v3_and_zipalign() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // 1. Generate real Keystore
        val config = KeystoreConfig(
            fileName = "signer-test-v3-key.jks",
            storePassword = "TestPassword2026!",
            alias = "v3alias",
            keyPassword = "TestPassword2026!",
            validityYears = 25,
            algorithm = KeyAlgorithm.RSA_2048,
            distinguishedName = DistinguishedName(commonName = "Signet V3 Dev", organization = "Signet Labs")
        )
        val generated = KeystoreGenerator.generateKeystore(context, config)
        val jksBytes = File(generated.filePath).readBytes()
        assertNotNull(jksBytes)

        val unsignedApk = createSyntheticUnsignedApk()

        // 2. Sign APK with triple scheme (v1 + v2 + v3) and zipalign
        val options = ApkSigningOptions(
            signV1 = true,
            signV2 = true,
            signV3 = true,
            zipalign = true,
            outputFileName = "test-signed-triple.apk"
        )

        val result = ApkSigner.signApk(
            apkBytes = unsignedApk,
            keystoreBytes = jksBytes,
            storePassword = config.storePassword,
            alias = config.alias,
            keyPassword = config.keyPassword,
            options = options,
            context = context
        )

        assertTrue("La firma triple del APK debería ser exitosa", result.isSuccess)
        assertNotNull(result.signedApkBytes)
        assertTrue(result.appliedSchemes.contains("v1 (JAR Signing)"))
        assertTrue(result.appliedSchemes.contains("v2 (APK Signature Scheme v2)"))
        assertTrue(result.appliedSchemes.contains("v3 (APK Signature Scheme v3)"))
        assertEquals(generated.sha256Fingerprint, result.sha256Fingerprint)

        val signedBytes = result.signedApkBytes!!
        val magicString = String(signedBytes, Charsets.US_ASCII)
        assertTrue("Debe contener APK Sig Block 42", magicString.contains("APK Sig Block 42"))

        val analyzed = ApkMatcher.analyzeApk(context, signedBytes, "test-signed-triple.apk")
        assertTrue("ApkMatcher debe encontrar certificados", analyzed.certificates.isNotEmpty())
        assertTrue("Debe contener esquema v3", analyzed.signatureSchemesFound.contains("v3 (Full APK)") || analyzed.signatureSchemesFound.contains("v2/v3"))
    }

    @Test
    fun testApkSigningWorkflow_v3Only() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val config = KeystoreConfig(
            fileName = "signer-test-v3-only.jks",
            storePassword = "TestPassword2026!",
            alias = "v3onlyalias",
            keyPassword = "TestPassword2026!"
        )
        val generated = KeystoreGenerator.generateKeystore(context, config)
        val jksBytes = File(generated.filePath).readBytes()

        val unsignedApk = createSyntheticUnsignedApk()

        val options = ApkSigningOptions(
            signV1 = false,
            signV2 = false,
            signV3 = true,
            zipalign = true,
            outputFileName = "test-signed-v3only.apk"
        )

        val result = ApkSigner.signApk(
            apkBytes = unsignedApk,
            keystoreBytes = jksBytes,
            storePassword = config.storePassword,
            alias = config.alias,
            keyPassword = config.keyPassword,
            options = options,
            context = context
        )

        assertTrue("Firma solo v3 debe ser exitosa", result.isSuccess)
        assertNotNull(result.signedApkBytes)
        assertTrue(result.appliedSchemes.contains("v3 (APK Signature Scheme v3)"))
        assertFalse(result.appliedSchemes.contains("v1 (JAR Signing)"))
        assertFalse(result.appliedSchemes.contains("v2 (APK Signature Scheme v2)"))

        val analyzed = ApkMatcher.analyzeApk(context, result.signedApkBytes!!, "test-signed-v3only.apk")
        assertTrue(analyzed.certificates.isNotEmpty())
        assertTrue("Debe detectar v3", analyzed.signatureSchemesFound.contains("v3 (Full APK)") || analyzed.signatureSchemesFound.contains("v2/v3"))
    }

    @Test
    fun testApkSigning_incorrectPassword_failsGracefully() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val config = KeystoreConfig(
            fileName = "signer-test-key2.jks",
            storePassword = "CorrectPassword123!",
            alias = "testalias",
            keyPassword = "CorrectPassword123!"
        )
        val generated = KeystoreGenerator.generateKeystore(context, config)
        val jksBytes = File(generated.filePath).readBytes()

        val unsignedApk = createSyntheticUnsignedApk()

        val result = ApkSigner.signApk(
            apkBytes = unsignedApk,
            keystoreBytes = jksBytes,
            storePassword = "WrongPassword!",
            alias = "testalias",
            keyPassword = "WrongPassword!"
        )

        assertFalse("Debe fallar con contraseña incorrecta", result.isSuccess)
        assertNotNull(result.errorMessage)
    }
}
