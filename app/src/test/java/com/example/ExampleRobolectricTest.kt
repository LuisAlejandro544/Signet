package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.crypto.KeystoreGenerator
import com.example.crypto.PasswordGenerator
import com.example.crypto.generateKeystore
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

/**
 * End-to-End integration and smoke test suite for the Signet Android application.
 */
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
    fun `smoke test complete keystore generation and integrity pipeline`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val config = KeystoreConfig(
            fileName = "smoke-test.jks",
            storePassword = PasswordGenerator.generate(20),
            alias = "smoke_alias",
            useSamePassword = true,
            validityYears = 25,
            algorithm = KeyAlgorithm.RSA_2048,
            distinguishedName = DistinguishedName(commonName = "Smoke Test App", organization = "Signet Lab")
        )

        val details = KeystoreGenerator.generateKeystore(context, config)

        assertNotNull(details)
        assertEquals("smoke-test.jks", details.fileName)
        assertEquals("smoke_alias", details.alias)
        assertTrue(File(details.filePath).exists())
        assertTrue(details.sha256Fingerprint.isNotBlank())
        assertTrue(details.base64Content.isNotBlank())
    }
}
