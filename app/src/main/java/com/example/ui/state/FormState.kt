package com.example.ui.state

import com.example.data.model.KeyAlgorithm

/**
 * State representing the keystore generation form parameters and options.
 */
data class FormState(
    val fileName: String = "release-key",
    val fileExtension: String = "jks", // "jks" or "keystore"
    val storePassword: String = "",
    val confirmPassword: String = "",
    val isStorePasswordVisible: Boolean = false,
    val alias: String = "key0",
    val keyPassword: String = "",
    val isKeyPasswordVisible: Boolean = false,
    val useSamePassword: Boolean = true,
    val validityYears: Int = 25,
    val algorithm: KeyAlgorithm = KeyAlgorithm.RSA_2048,
    val commonName: String = "",
    val organizationalUnit: String = "",
    val organization: String = "",
    val locality: String = "",
    val state: String = "",
    val countryCode: String = "",
    val isAdvancedDnExpanded: Boolean = true
) {
    val fullFileName: String
        get() {
            val cleanName = fileName.trim()
                .removeSuffix(".jks")
                .removeSuffix(".keystore")
                .removeSuffix(".p12")
            val base = if (cleanName.isBlank()) "release-key" else cleanName
            return "$base.$fileExtension"
        }
}
