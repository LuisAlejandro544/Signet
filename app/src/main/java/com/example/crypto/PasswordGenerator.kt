package com.example.crypto

import java.security.SecureRandom
import kotlin.math.log2

/**
 * High-performance, cryptographically secure password generator using CSPRNG (SecureRandom).
 * Designed for Android Keystore passwords, CI/CD secrets, and signing credentials.
 */
object PasswordGenerator {

    private val secureRandom = SecureRandom()

    const val UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    const val LOWERCASE = "abcdefghijklmnopqrstuvwxyz"
    const val DIGITS = "0123456789"
    // Terminal and Gradle safe symbols (avoiding quotes and backslashes)
    const val SYMBOLS = "!@#$%&*-_=+"

    enum class PasswordStrength(val label: String, val minBits: Double) {
        WEAK("Débil", 0.0),
        MEDIUM("Media", 45.0),
        STRONG("Fuerte", 70.0),
        ULTRA("Ultra Segura", 95.0)
    }

    /**
     * Generates a cryptographically strong random password guaranteed to include characters
     * from all selected sets (uppercase, lowercase, digits, symbols).
     */
    fun generate(
        length: Int = 20,
        includeUppercase: Boolean = true,
        includeLowercase: Boolean = true,
        includeDigits: Boolean = true,
        includeSymbols: Boolean = true
    ): String {
        val poolBuilder = StringBuilder()
        val guaranteedChars = mutableListOf<Char>()

        if (includeUppercase) {
            poolBuilder.append(UPPERCASE)
            guaranteedChars.add(UPPERCASE[secureRandom.nextInt(UPPERCASE.length)])
        }
        if (includeLowercase) {
            poolBuilder.append(LOWERCASE)
            guaranteedChars.add(LOWERCASE[secureRandom.nextInt(LOWERCASE.length)])
        }
        if (includeDigits) {
            poolBuilder.append(DIGITS)
            guaranteedChars.add(DIGITS[secureRandom.nextInt(DIGITS.length)])
        }
        if (includeSymbols) {
            poolBuilder.append(SYMBOLS)
            guaranteedChars.add(SYMBOLS[secureRandom.nextInt(SYMBOLS.length)])
        }

        val pool = poolBuilder.toString()
        if (pool.isEmpty()) {
            return generate(length = length, includeUppercase = true, includeLowercase = true, includeDigits = true, includeSymbols = true)
        }

        val effectiveLength = length.coerceAtLeast(guaranteedChars.size)
        val passwordChars = ArrayList<Char>(effectiveLength)
        passwordChars.addAll(guaranteedChars)

        for (i in guaranteedChars.size until effectiveLength) {
            val randomIndex = secureRandom.nextInt(pool.length)
            passwordChars.add(pool[randomIndex])
        }

        // Fisher-Yates cryptographically secure shuffle
        for (i in passwordChars.size - 1 downTo 1) {
            val j = secureRandom.nextInt(i + 1)
            val temp = passwordChars[i]
            passwordChars[i] = passwordChars[j]
            passwordChars[j] = temp
        }

        return passwordChars.joinToString("")
    }

    /**
     * Calculates the entropy in bits for a given password based on character pool size.
     */
    fun calculateEntropy(password: String): Double {
        if (password.isEmpty()) return 0.0

        var poolSize = 0
        if (password.any { it in UPPERCASE }) poolSize += UPPERCASE.length
        if (password.any { it in LOWERCASE }) poolSize += LOWERCASE.length
        if (password.any { it in DIGITS }) poolSize += DIGITS.length
        if (password.any { it in SYMBOLS }) poolSize += SYMBOLS.length
        if (poolSize == 0) poolSize = 26

        val entropyPerChar = log2(poolSize.toDouble())
        return password.length * entropyPerChar
    }

    /**
     * Evaluates password strength category.
     */
    fun evaluateStrength(password: String): PasswordStrength {
        val bits = calculateEntropy(password)
        return when {
            bits >= PasswordStrength.ULTRA.minBits -> PasswordStrength.ULTRA
            bits >= PasswordStrength.STRONG.minBits -> PasswordStrength.STRONG
            bits >= PasswordStrength.MEDIUM.minBits -> PasswordStrength.MEDIUM
            else -> PasswordStrength.WEAK
        }
    }
}
