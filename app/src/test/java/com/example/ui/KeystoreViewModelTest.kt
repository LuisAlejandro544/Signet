package com.example.ui

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.KeyAlgorithm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class KeystoreViewModelTest {

    @Test
    fun `onboarding state and legal URL endpoints verification`() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = KeystoreViewModel(application)

        // Verify initial onboarding state
        assertNotNull(viewModel.isOnboardingCompleted.value)

        // Complete onboarding
        viewModel.completeOnboarding()
        assertTrue(viewModel.isOnboardingCompleted.value)

        // Reset onboarding
        viewModel.resetOnboarding()
        assertFalse(viewModel.isOnboardingCompleted.value)

        // Verify URLs
        assertEquals(
            "https://signet-web.luisalejandrososacamacho9.workers.dev/terms/",
            KeystoreViewModel.URL_TERMS
        )
        assertEquals(
            "https://signet-web.luisalejandrososacamacho9.workers.dev/privacy/",
            KeystoreViewModel.URL_PRIVACY
        )
    }

    @Test
    fun `presets and form validations update state correctly`() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = KeystoreViewModel(application)

        // Apply release preset
        viewModel.applyPreset("release")
        val formRelease = viewModel.formState.value
        assertEquals("release-key", formRelease.fileName)
        assertEquals("jks", formRelease.fileExtension)
        assertEquals("key0", formRelease.alias)
        assertEquals(25, formRelease.validityYears)
        assertEquals(KeyAlgorithm.RSA_2048, formRelease.algorithm)

        // Apply rsa4096 preset
        viewModel.applyPreset("rsa4096")
        val form4096 = viewModel.formState.value
        assertEquals("app-high-security", form4096.fileName)
        assertEquals("keystore", form4096.fileExtension)
        assertEquals("release", form4096.alias)
        assertEquals(KeyAlgorithm.RSA_4096, form4096.algorithm)

        // Apply windows preset (.pfx)
        viewModel.applyPreset("windows")
        val formWindows = viewModel.formState.value
        assertEquals("authenticode-codesign", formWindows.fileName)
        assertEquals("pfx", formWindows.fileExtension)
        assertEquals("codesign", formWindows.alias)
        assertEquals(KeyAlgorithm.RSA_4096, formWindows.algorithm)
        assertEquals("authenticode-codesign.pfx", formWindows.fullFileName)

        // Apply multiplatform preset (.p12)
        viewModel.applyPreset("p12")
        val formP12 = viewModel.formState.value
        assertEquals("multiplatform-key", formP12.fileName)
        assertEquals("p12", formP12.fileExtension)
        assertEquals("app-signer", formP12.alias)
        assertEquals("multiplatform-key.p12", formP12.fullFileName)

        // Password generation
        viewModel.generateRandomPassword(24)
        val formWithPwd = viewModel.formState.value
        assertEquals(24, formWithPwd.storePassword.length)
        assertEquals(formWithPwd.storePassword, formWithPwd.confirmPassword)
        assertTrue(formWithPwd.isStorePasswordVisible)

        // Ephemeral Mode Toggle
        assertFalse(viewModel.formState.value.isEphemeral)
        viewModel.updateForm { it.copy(isEphemeral = true) }
        assertTrue(viewModel.formState.value.isEphemeral)
    }

    @Test
    fun `orphan keystore files auto recovery test`() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val tempDir = application.filesDir
        val sampleKeystore = java.io.File(tempDir, "recovered_sample.jks")
        sampleKeystore.writeBytes("SAMPLE_KEYSTORE_BINARY_DATA".toByteArray())

        val viewModel = KeystoreViewModel(application)
        viewModel.syncKeystores(tempDir)

        // Clean up
        if (sampleKeystore.exists()) {
            sampleKeystore.delete()
        }
    }
}
