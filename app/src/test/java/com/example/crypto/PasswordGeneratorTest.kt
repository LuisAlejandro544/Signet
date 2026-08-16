package com.example.crypto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordGeneratorTest {

    @Test
    fun `generate ultra secure passwords with high entropy`() {
        val pwd20 = PasswordGenerator.generate(20)
        assertEquals(20, pwd20.length)
        assertTrue(pwd20.any { it.isUpperCase() })
        assertTrue(pwd20.any { it.isLowerCase() })
        assertTrue(pwd20.any { it.isDigit() })
        assertTrue(pwd20.any { it in PasswordGenerator.SYMBOLS })

        val entropy20 = PasswordGenerator.calculateEntropy(pwd20)
        assertTrue(entropy20 >= 100.0) // 20 chars with ~72 pool is > 120 bits

        val strength = PasswordGenerator.evaluateStrength(pwd20)
        assertEquals(PasswordGenerator.PasswordStrength.ULTRA, strength)

        val pwd32 = PasswordGenerator.generate(32)
        assertEquals(32, pwd32.length)
        assertTrue(PasswordGenerator.calculateEntropy(pwd32) > 180.0)
    }

    @Test
    fun `password strength categories are correctly evaluated`() {
        val weak = PasswordGenerator.evaluateStrength("12345")
        assertEquals(PasswordGenerator.PasswordStrength.WEAK, weak)

        val medium = PasswordGenerator.evaluateStrength("Password123")
        assertTrue(medium == PasswordGenerator.PasswordStrength.MEDIUM || medium == PasswordGenerator.PasswordStrength.STRONG)

        val ultra = PasswordGenerator.evaluateStrength("K9#m$2Px!vL8@qZ1*wY4")
        assertEquals(PasswordGenerator.PasswordStrength.ULTRA, ultra)
    }
}
