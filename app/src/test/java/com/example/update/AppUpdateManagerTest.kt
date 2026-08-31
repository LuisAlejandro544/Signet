package com.example.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateManagerTest {

    @Test
    fun `isNewerVersion correctly compares semantic versions`() {
        // Minor / patch increases
        assertTrue(AppUpdateManager.isNewerVersion("1.0.0", "1.0.1"))
        assertTrue(AppUpdateManager.isNewerVersion("1.0.0", "v1.1.0"))
        assertTrue(AppUpdateManager.isNewerVersion("1.0.0", "2.0.0"))

        // Beta to newer beta
        assertTrue(AppUpdateManager.isNewerVersion("1.0.0-B", "1.0.1-B"))
        assertTrue(AppUpdateManager.isNewerVersion("1.0.0-B", "v1.0.1"))

        // Beta to stable release of same version
        assertTrue(AppUpdateManager.isNewerVersion("1.0.0-B", "1.0.0"))
        assertTrue(AppUpdateManager.isNewerVersion("1.0.0-B", "1.0.0-E"))

        // Older or equal versions
        assertFalse(AppUpdateManager.isNewerVersion("1.0.1", "1.0.0"))
        assertFalse(AppUpdateManager.isNewerVersion("1.0.0", "1.0.0"))
        assertFalse(AppUpdateManager.isNewerVersion("v1.0.0", "1.0.0"))
        assertFalse(AppUpdateManager.isNewerVersion("1.1.0", "1.0.5"))
    }

    @Test
    fun `cleanVersion strips v prefixes`() {
        assertEquals("1.0.0", AppUpdateManager.cleanVersion("v1.0.0"))
        assertEquals("1.0.0-B", AppUpdateManager.cleanVersion("V1.0.0-B"))
        assertEquals("2.1.0", AppUpdateManager.cleanVersion("2.1.0"))
    }

    @Test
    fun `findBestAssetForPlatform selects APK for Android`() {
        val assets = listOf(
            ReleaseAsset("Signet-v1.0.1-B-windows-x64-setup.exe", "https://download/exe", 45000000, "application/octet-stream"),
            ReleaseAsset("Signet-v1.0.1-B-release-signed.apk", "https://download/apk", 28000000, "application/vnd.android.package-archive"),
            ReleaseAsset("Signet-v1.0.1-B-portable.zip", "https://download/zip", 40000000, "application/zip")
        )

        val androidAsset = AppUpdateManager.findBestAssetForPlatform(assets, isDesktop = false)
        assertNotNull(androidAsset)
        assertEquals("Signet-v1.0.1-B-release-signed.apk", androidAsset?.name)
    }

    @Test
    fun `findBestAssetForPlatform selects installer for Windows Desktop`() {
        val assets = listOf(
            ReleaseAsset("Signet-v1.0.1-B-release-signed.apk", "https://download/apk", 28000000, "application/vnd.android.package-archive"),
            ReleaseAsset("Signet-v1.0.1-B-windows-x64-setup.exe", "https://download/exe", 45000000, "application/octet-stream"),
            ReleaseAsset("Signet-v1.0.1-B-portable.zip", "https://download/zip", 40000000, "application/zip")
        )

        val desktopAsset = AppUpdateManager.findBestAssetForPlatform(assets, isDesktop = true)
        assertNotNull(desktopAsset)
        assertEquals("Signet-v1.0.1-B-windows-x64-setup.exe", desktopAsset?.name)
    }

    @Test
    fun `empty assets returns null safely`() {
        assertNull(AppUpdateManager.findBestAssetForPlatform(emptyList(), isDesktop = false))
        assertNull(AppUpdateManager.findBestAssetForPlatform(emptyList(), isDesktop = true))
    }
}
