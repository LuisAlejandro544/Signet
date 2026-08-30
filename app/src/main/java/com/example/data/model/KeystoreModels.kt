package com.example.data.model

enum class KeyAlgorithm(val displayName: String, val keySize: Int, val sigAlg: String) {
    RSA_2048("RSA 2048 bits (Estándar Android)", 2048, "SHA256withRSA"),
    RSA_4096("RSA 4096 bits (Alta Seguridad)", 4096, "SHA256withRSA"),
    EC_P256("EC 256 bits (Curva Elíptica)", 256, "SHA256withECDSA")
}

data class DistinguishedName(
    val commonName: String = "",       // CN: Nombre completo o nombre de la app (ej: John Doe o MyApp)
    val organizationalUnit: String = "", // OU: Unidad organizativa (ej: Mobile Dev)
    val organization: String = "",     // O: Organización / Empresa (ej: Studio / Company)
    val locality: String = "",         // L: Ciudad o Localidad (ej: Madrid)
    val state: String = "",            // ST: Estado o Provincia (ej: Madrid)
    val countryCode: String = ""       // C: Código de país de 2 letras (ej: ES, MX, US)
) {
    fun toRfc2253String(): String {
        val parts = mutableListOf<String>()
        if (commonName.isNotBlank()) parts.add("CN=$commonName")
        if (organizationalUnit.isNotBlank()) parts.add("OU=$organizationalUnit")
        if (organization.isNotBlank()) parts.add("O=$organization")
        if (locality.isNotBlank()) parts.add("L=$locality")
        if (state.isNotBlank()) parts.add("ST=$state")
        if (countryCode.isNotBlank()) parts.add("C=${countryCode.uppercase()}")
        return if (parts.isEmpty()) "CN=Android App" else parts.joinToString(", ")
    }
}

data class KeystoreConfig(
    val fileName: String = "my-release-key.jks",
    val storePassword: String = "",
    val alias: String = "key0",
    val keyPassword: String = "",
    val useSamePassword: Boolean = true,
    val validityYears: Int = 25,
    val algorithm: KeyAlgorithm = KeyAlgorithm.RSA_2048,
    val distinguishedName: DistinguishedName = DistinguishedName()
)

data class KeystoreDetails(
    val id: Long = 0,
    val fileName: String,
    val alias: String,
    val filePath: String,
    val fileSizeBytes: Long,
    val storePassword: String = "",
    val keyPassword: String = "",
    val base64Content: String = "",
    val sha256Fingerprint: String,
    val sha1Fingerprint: String,
    val md5Fingerprint: String,
    val validFrom: Long,
    val validUntil: Long,
    val algorithm: String,
    val subjectDn: String,
    val issuerDn: String,
    val serialNumber: String,
    val certificatePem: String,
    val createdAt: Long = System.currentTimeMillis()
)

data class ApkCertificateInfo(
    val sha256Fingerprint: String,
    val sha1Fingerprint: String,
    val md5Fingerprint: String,
    val subjectDn: String,
    val issuerDn: String,
    val validFrom: Long,
    val validUntil: Long,
    val serialNumber: String,
    val signatureScheme: String
)

data class ApkInfo(
    val fileName: String,
    val fileSizeBytes: Long,
    val packageName: String?,
    val versionName: String?,
    val versionCode: Long?,
    val certificates: List<ApkCertificateInfo>,
    val signatureSchemesFound: List<String>
)

data class ApkMatchResult(
    val isMatch: Boolean,
    val apkInfo: ApkInfo,
    val targetKeystoreName: String,
    val matchedAlias: String?,
    val matchedFingerprintSha256: String?,
    val reasonMessage: String
)

data class ApkSigningOptions(
    val signV1: Boolean = true,
    val signV2: Boolean = true,
    val zipalign: Boolean = true,
    val outputFileName: String = "app-signed.apk"
)

data class ApkSigningResult(
    val isSuccess: Boolean,
    val signedApkBytes: ByteArray? = null,
    val signedApkFile: java.io.File? = null,
    val outputFileName: String,
    val outputFileSizeBytes: Long = 0L,
    val packageName: String? = null,
    val versionName: String? = null,
    val versionCode: Long? = null,
    val sha256Fingerprint: String = "",
    val appliedSchemes: List<String> = emptyList(),
    val zipalignApplied: Boolean = false,
    val durationMs: Long = 0L,
    val errorMessage: String? = null
)
