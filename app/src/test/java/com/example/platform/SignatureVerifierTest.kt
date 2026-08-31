package com.example.platform

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SignatureVerifierTest {

    @Test
    fun computeSha256Fingerprint_formatsValidHexPairs() {
        val testBytes = byteArrayOf(0x0A, 0x1B, 0x2C, 0x3D, 0x4E, 0x5F)
        val fingerprint = SignatureVerifier.computeSha256Fingerprint(testBytes)

        assertNotNull(fingerprint)
        assertTrue(fingerprint.contains(":"))
        // SHA-256 has 32 bytes, formatted as 32 pairs separated by 31 colons
        val parts = fingerprint.split(":")
        assertTrue("Expected 32 hex segments for SHA-256", parts.size == 32)
    }

    @Test
    fun verifyAppSignature_handlesApplicationContextGracefully() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val result = SignatureVerifier.verifyAppSignature(context)

        assertNotNull(result)
        assertNotNull(result.status)
        assertNotNull(result.packageName)
    }

    @Test
    fun integrityStatus_attributesAreValid() {
        assertTrue(IntegrityStatus.OFFICIAL_VERIFIED.isSecure)
        assertTrue(IntegrityStatus.DEVELOPMENT_BUILD.isSecure)
        assertTrue(!IntegrityStatus.TAMPERED_OR_MODIFIED.isSecure)
    }
}
