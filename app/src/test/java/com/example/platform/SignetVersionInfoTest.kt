package com.example.platform

import org.junit.Assert.assertEquals
import org.junit.Test

class SignetVersionInfoTest {

    @Test
    fun `resolve channel matches correct channel from version name and package id`() {
        // Debug channel
        assertEquals(SignetChannel.DEBUG, SignetChannel.resolve("1.0.0-D", "com.signet.app.debug"))
        assertEquals(SignetChannel.DEBUG, SignetChannel.resolve("1.0.0-dev-D", "com.signet.app.debug"))

        // Pre-Alpha / Dev channel
        assertEquals(SignetChannel.PRE_ALPHA, SignetChannel.resolve("1.0.0-dev", "com.signet.app.dev"))
        assertEquals(SignetChannel.PRE_ALPHA, SignetChannel.resolve("1.0.0.dev", "com.signet.app.dev"))
        assertEquals(SignetChannel.PRE_ALPHA, SignetChannel.resolve("1.0.0-A", "com.signet.app.dev"))

        // Beta channel
        assertEquals(SignetChannel.BETA, SignetChannel.resolve("1.0.0-B", "com.signet.app.beta"))
        assertEquals(SignetChannel.BETA, SignetChannel.resolve("1.0.1-B", "com.signet.app.beta"))

        // Stable channel
        assertEquals(SignetChannel.STABLE, SignetChannel.resolve("1.0.0-E", "com.signet.app"))
        assertEquals(SignetChannel.STABLE, SignetChannel.resolve("1.0.0", "com.signet.app"))
    }

    @Test
    fun `channel attributes are correct and consistent`() {
        assertEquals("-D", SignetChannel.DEBUG.tagSuffix)
        assertEquals(".debug", SignetChannel.DEBUG.packageSuffix)

        assertEquals("-dev", SignetChannel.PRE_ALPHA.tagSuffix)
        assertEquals(".dev", SignetChannel.PRE_ALPHA.packageSuffix)

        assertEquals("-B", SignetChannel.BETA.tagSuffix)
        assertEquals(".beta", SignetChannel.BETA.packageSuffix)

        assertEquals("-E", SignetChannel.STABLE.tagSuffix)
        assertEquals("", SignetChannel.STABLE.packageSuffix)
    }
}
