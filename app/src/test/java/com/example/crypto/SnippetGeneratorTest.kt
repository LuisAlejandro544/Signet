package com.example.crypto

import org.junit.Assert.assertTrue
import org.junit.Test

class SnippetGeneratorTest {

    @Test
    fun `generate gradle and github actions snippets correctly`() {
        val ktsSnippet = SnippetGenerator.generateGradleKtsSnippet("release.jks", "app_key")
        assertTrue(ktsSnippet.contains("release.jks"))
        assertTrue(ktsSnippet.contains("app_key"))
        assertTrue(ktsSnippet.contains("signingConfigs"))

        val groovySnippet = SnippetGenerator.generateGradleGroovySnippet("release.jks", "app_key")
        assertTrue(groovySnippet.contains("release.jks"))
        assertTrue(groovySnippet.contains("app_key"))

        val ghWorkflow = SnippetGenerator.generateGitHubActionsWorkflow("release.jks", "app_key")
        assertTrue(ghWorkflow.contains("KEYSTORE_BASE64"))
        assertTrue(ghWorkflow.contains("release.jks"))
        assertTrue(ghWorkflow.contains("assembleRelease"))

        val apksignerSnippet = SnippetGenerator.generateApksignerSnippet("release.jks", "app_key")
        assertTrue(apksignerSnippet.contains("apksigner sign"))
        assertTrue(apksignerSnippet.contains("zipalign"))

        val windowsSnippet = SnippetGenerator.generateWindowsSignToolSnippet("app-codesign.pfx", "codesign")
        assertTrue(windowsSnippet.contains("signtool sign"))
        assertTrue(windowsSnippet.contains("app-codesign.pfx"))
        assertTrue(windowsSnippet.contains("Set-AuthenticodeSignature"))

        val openSslSnippet = SnippetGenerator.generateOpenSslSnippet("app-codesign.pfx", "codesign")
        assertTrue(openSslSnippet.contains("openssl pkcs12"))
        assertTrue(openSslSnippet.contains("cert.pem"))
    }
}
