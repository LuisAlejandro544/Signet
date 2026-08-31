package com.example.crypto.keys

import com.example.data.model.KeyAlgorithm
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.Provider
import java.security.SecureRandom
import java.security.spec.ECGenParameterSpec

/**
 * Factory for creating cryptographic key pairs (RSA 2048/4096, EC P-256) with CSPRNG.
 */
object KeyPairFactory {

    /**
     * Generates a new [KeyPair] using the specified [KeyAlgorithm] and security [Provider].
     */
    fun generateKeyPair(algorithm: KeyAlgorithm, provider: Provider): KeyPair {
        return when (algorithm) {
            KeyAlgorithm.RSA_2048 -> {
                val kpg = KeyPairGenerator.getInstance("RSA", provider)
                kpg.initialize(2048, SecureRandom())
                kpg.generateKeyPair()
            }
            KeyAlgorithm.RSA_4096 -> {
                val kpg = KeyPairGenerator.getInstance("RSA", provider)
                kpg.initialize(4096, SecureRandom())
                kpg.generateKeyPair()
            }
            KeyAlgorithm.EC_P256 -> {
                val kpg = KeyPairGenerator.getInstance("EC", provider)
                kpg.initialize(ECGenParameterSpec("secp256r1"), SecureRandom())
                kpg.generateKeyPair()
            }
        }
    }
}
