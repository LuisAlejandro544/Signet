package com.example.crypto

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.DistinguishedName
import com.example.data.model.KeyAlgorithm
import com.example.data.model.KeystoreConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ApkMatcherTest {

    @Test
    fun testApkMatcher_ExactMatchAndMismatchDetection() {
        val context: Context = ApplicationProvider.getApplicationContext()

        // 1. Generate Keystore A
        val configA = KeystoreConfig(
            fileName = "key-a.jks",
            storePassword = "password123",
            alias = "releaseA",
            useSamePassword = true,
            validityYears = 25,
            algorithm = KeyAlgorithm.RSA_2048,
            distinguishedName = DistinguishedName(commonName = "SignetApp", organization = "Signet Org")
        )
        val detailsA = KeystoreGenerator.generateKeystore(context, configA)

        // 2. Generate Keystore B
        val configB = KeystoreConfig(
            fileName = "key-b.jks",
            storePassword = "password456",
            alias = "releaseB",
            useSamePassword = true,
            validityYears = 25,
            algorithm = KeyAlgorithm.RSA_2048,
            distinguishedName = DistinguishedName(commonName = "OtherApp", organization = "Other Org")
        )
        val detailsB = KeystoreGenerator.generateKeystore(context, configB)

        // 3. Create a synthetic APK signed with Keystore A's certificate
        val certABytes = java.security.cert.CertificateFactory.getInstance("X.509")
            .generateCertificate(java.io.ByteArrayInputStream(detailsA.certificatePem.toByteArray()))
            .encoded

        val apkStream = java.io.ByteArrayOutputStream()
        java.util.zip.ZipOutputStream(apkStream).use { zos ->
            // Add fake classes.dex
            zos.putNextEntry(java.util.zip.ZipEntry("classes.dex"))
            zos.write("dex_content".toByteArray())
            zos.closeEntry()

            // Add signature file in META-INF/CERT.RSA
            zos.putNextEntry(java.util.zip.ZipEntry("META-INF/CERT.RSA"))
            zos.write(certABytes)
            zos.closeEntry()
        }
        val syntheticApkBytes = apkStream.toByteArray()

        // 4. Analyze synthetic APK
        val apkInfo = ApkMatcher.analyzeApk(
            context = context,
            apkBytes = syntheticApkBytes,
            fileName = "sample-release.apk"
        )

        assertEquals("sample-release.apk", apkInfo.fileName)
        assertTrue(apkInfo.certificates.isNotEmpty())
        assertEquals(detailsA.sha256Fingerprint, apkInfo.certificates.first().sha256Fingerprint)

        // 5. Match with Keystore A -> MUST BE TRUE
        val matchResultA = ApkMatcher.matchApkWithKeystoreDetails(apkInfo, detailsA)
        assertTrue(matchResultA.isMatch)
        assertEquals(detailsA.alias, matchResultA.matchedAlias)
        assertEquals(detailsA.sha256Fingerprint, matchResultA.matchedFingerprintSha256)

        // 6. Match with Keystore B -> MUST BE FALSE
        val matchResultB = ApkMatcher.matchApkWithKeystoreDetails(apkInfo, detailsB)
        assertFalse(matchResultB.isMatch)
        assertNull(matchResultB.matchedAlias)
        assertTrue(matchResultB.reasonMessage.contains("Las firmas no coinciden"))
    }
}
