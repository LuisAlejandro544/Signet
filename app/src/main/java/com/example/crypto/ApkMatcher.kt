package com.example.crypto

import com.example.crypto.apk.ApkSigningBlockParser
import com.example.crypto.apk.ApkV1SignatureParser
import com.example.crypto.apk.AxmlManifestParser
import com.example.data.model.ApkCertificateInfo
import com.example.data.model.ApkInfo
import com.example.data.model.ApkMatchResult
import com.example.data.model.KeystoreDetails
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.security.Security
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.Locale

object ApkMatcher {

    private val bcProvider: BouncyCastleProvider by lazy {
        val provider = BouncyCastleProvider()
        try {
            Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
            Security.insertProviderAt(provider, 1)
        } catch (_: Exception) {}
        provider
    }

    init {
        bcProvider
    }

    /**
     * Extracts all signing certificates and package metadata from an APK file byte array.
     * Pure JVM implementation compatible with both Android and Desktop (Windows / Linux / macOS).
     */
    fun analyzeApk(apkBytes: ByteArray, fileName: String = "app.apk"): ApkInfo {
        val certificates = mutableListOf<ApkCertificateInfo>()
        val schemesFound = mutableListOf<String>()
        var packageName: String? = null
        var versionName: String? = null
        var versionCode: Long? = null

        // 1. Direct ZIP Extraction for v1 (META-INF/*.RSA, *.DSA, *.EC)
        try {
            val v1Certs = ApkV1SignatureParser.extractV1Certificates(apkBytes, bcProvider)
            v1Certs.forEach { cert ->
                val info = buildCertInfo(cert, "v1 (JAR Signing)")
                if (certificates.none { it.sha256Fingerprint == info.sha256Fingerprint }) {
                    certificates.add(info)
                }
                if (!schemesFound.contains("v1 (JAR)")) {
                    schemesFound.add("v1 (JAR)")
                }
            }
        } catch (_: Exception) {}

        // 2. Direct APK Signing Block Parser for v2/v3
        try {
            val v2v3Certs = ApkSigningBlockParser.extractV2V3Certificates(apkBytes)
            v2v3Certs.forEach { (cert, schemeName) ->
                val info = buildCertInfo(cert, schemeName)
                if (certificates.none { it.sha256Fingerprint == info.sha256Fingerprint }) {
                    certificates.add(info)
                }
                if (!schemesFound.contains(schemeName)) {
                    schemesFound.add(schemeName)
                }
            }
        } catch (_: Exception) {}

        // 3. Try parsing AndroidManifest.xml from ZIP if packageName not yet resolved
        if (packageName.isNullOrBlank()) {
            packageName = AxmlManifestParser.extractPackageNameFromZip(apkBytes)
        }

        return ApkInfo(
            fileName = fileName,
            fileSizeBytes = apkBytes.size.toLong(),
            packageName = packageName,
            versionName = versionName,
            versionCode = versionCode,
            certificates = certificates,
            signatureSchemesFound = schemesFound
        )
    }

    /**
     * Backward-compatible overload accepting context as optional parameter for Android.
     */
    fun analyzeApk(context: Any?, apkBytes: ByteArray, fileName: String = "app.apk"): ApkInfo {
        return analyzeApk(apkBytes, fileName)
    }

    /**
     * Matches an APK against a target Keystore (or list of Keystores).
     */
    fun matchApkWithKeystoreDetails(apkInfo: ApkInfo, keystore: KeystoreDetails): ApkMatchResult {
        if (apkInfo.certificates.isEmpty()) {
            return ApkMatchResult(
                isMatch = false,
                apkInfo = apkInfo,
                targetKeystoreName = keystore.fileName,
                matchedAlias = null,
                matchedFingerprintSha256 = null,
                reasonMessage = "El archivo APK no contiene firmas digitales reconocibles (APK sin firmar o corrupto)."
            )
        }

        val keystoreSha256 = keystore.sha256Fingerprint.trim().uppercase(Locale.ROOT)
        val matchedCert = apkInfo.certificates.firstOrNull {
            it.sha256Fingerprint.trim().uppercase(Locale.ROOT) == keystoreSha256
        }

        return if (matchedCert != null) {
            ApkMatchResult(
                isMatch = true,
                apkInfo = apkInfo,
                targetKeystoreName = keystore.fileName,
                matchedAlias = keystore.alias,
                matchedFingerprintSha256 = matchedCert.sha256Fingerprint,
                reasonMessage = "¡Coincidencia exacta del 100%! Este Keystore ('${keystore.alias}') es el propietario legítimo que firmó el APK."
            )
        } else {
            val apkFirstFingerprint = apkInfo.certificates.first().sha256Fingerprint
            ApkMatchResult(
                isMatch = false,
                apkInfo = apkInfo,
                targetKeystoreName = keystore.fileName,
                matchedAlias = null,
                matchedFingerprintSha256 = null,
                reasonMessage = "Las firmas no coinciden.\nHuella APK: $apkFirstFingerprint\nHuella Keystore: ${keystore.sha256Fingerprint}"
            )
        }
    }

    private fun buildCertInfo(cert: X509Certificate, scheme: String): ApkCertificateInfo {
        val encoded = cert.encoded
        val sha256 = calculateFingerprint(encoded, "SHA-256")
        val sha1 = calculateFingerprint(encoded, "SHA-1")
        val md5 = calculateFingerprint(encoded, "MD5")

        return ApkCertificateInfo(
            sha256Fingerprint = sha256,
            sha1Fingerprint = sha1,
            md5Fingerprint = md5,
            subjectDn = cert.subjectX500Principal.name,
            issuerDn = cert.issuerX500Principal.name,
            validFrom = cert.notBefore.time,
            validUntil = cert.notAfter.time,
            serialNumber = cert.serialNumber.toString(16).uppercase(Locale.ROOT),
            signatureScheme = scheme
        )
    }

    private fun calculateFingerprint(bytes: ByteArray, algorithm: String): String {
        val md = MessageDigest.getInstance(algorithm)
        val digest = md.digest(bytes)
        return digest.joinToString(":") { "%02X".format(it) }
    }
}
