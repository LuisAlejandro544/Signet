package com.example.crypto

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.example.data.model.ApkCertificateInfo
import com.example.data.model.ApkInfo
import com.example.data.model.ApkMatchResult
import com.example.data.model.KeystoreDetails
import org.bouncycastle.cert.X509CertificateHolder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cms.CMSSignedData
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.security.Security
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

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
     */
    fun analyzeApk(context: Context?, apkBytes: ByteArray, fileName: String = "app.apk"): ApkInfo {
        val certificates = mutableListOf<ApkCertificateInfo>()
        val schemesFound = mutableListOf<String>()
        var packageName: String? = null
        var versionName: String? = null
        var versionCode: Long? = null

        // 1. Try Android PackageManager getPackageArchiveInfo if Context and file are available
        if (context != null) {
            try {
                val tempApk = File(context.cacheDir, "temp_analyze_${System.currentTimeMillis()}.apk")
                FileOutputStream(tempApk).use { it.write(apkBytes) }

                val pm = context.packageManager
                val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    PackageManager.GET_SIGNING_CERTIFICATES or PackageManager.GET_META_DATA
                } else {
                    @Suppress("DEPRECATION")
                    PackageManager.GET_SIGNATURES or PackageManager.GET_META_DATA
                }

                val pkgInfo = pm.getPackageArchiveInfo(tempApk.absolutePath, flags)
                if (pkgInfo != null) {
                    packageName = pkgInfo.packageName
                    versionName = pkgInfo.versionName
                    versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        pkgInfo.longVersionCode
                    } else {
                        @Suppress("DEPRECATION")
                        pkgInfo.versionCode.toLong()
                    }

                    val cf = CertificateFactory.getInstance("X.509")
                    val signingInfo = pkgInfo.signingInfo
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && signingInfo != null) {
                        val signatures = if (signingInfo.hasMultipleSigners()) {
                            signingInfo.apkContentsSigners
                        } else {
                            signingInfo.signingCertificateHistory
                        }
                        signatures?.forEach { sig ->
                            val cert = cf.generateCertificate(ByteArrayInputStream(sig.toByteArray())) as? X509Certificate
                            if (cert != null) {
                                val certInfo = buildCertInfo(cert, "v2/v3 (APK Signing Block)")
                                if (certificates.none { it.sha256Fingerprint == certInfo.sha256Fingerprint }) {
                                    certificates.add(certInfo)
                                    if (!schemesFound.contains("v2/v3")) schemesFound.add("v2/v3")
                                }
                            }
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        pkgInfo.signatures?.forEach { sig ->
                            val cert = cf.generateCertificate(ByteArrayInputStream(sig.toByteArray())) as? X509Certificate
                            if (cert != null) {
                                val certInfo = buildCertInfo(cert, "v1 (JAR Signing)")
                                if (certificates.none { it.sha256Fingerprint == certInfo.sha256Fingerprint }) {
                                    certificates.add(certInfo)
                                    if (!schemesFound.contains("v1 (JAR)")) schemesFound.add("v1 (JAR)")
                                }
                            }
                        }
                    }
                }
                tempApk.delete()
            } catch (_: Exception) {
                // Fallback to direct byte-level analysis
            }
        }

        // 2. Direct ZIP Extraction for v1 (META-INF/*.RSA, *.DSA, *.EC)
        try {
            val v1Certs = extractV1Certificates(apkBytes)
            v1Certs.forEach { cert ->
                val info = buildCertInfo(cert, "v1 (JAR Signing)")
                if (certificates.none { it.sha256Fingerprint == info.sha256Fingerprint }) {
                    certificates.add(info)
                    if (!schemesFound.contains("v1 (JAR)")) schemesFound.add("v1 (JAR)")
                }
            }
        } catch (_: Exception) {}

        // 3. Direct APK Signing Block Parser for v2/v3
        try {
            val v2v3Certs = extractV2V3Certificates(apkBytes)
            v2v3Certs.forEach { (cert, schemeName) ->
                val info = buildCertInfo(cert, schemeName)
                if (certificates.none { it.sha256Fingerprint == info.sha256Fingerprint }) {
                    certificates.add(info)
                    if (!schemesFound.contains(schemeName)) schemesFound.add(schemeName)
                }
            }
        } catch (_: Exception) {}

        // 4. Try parsing AndroidManifest.xml from ZIP if packageName not yet resolved
        if (packageName.isNullOrBlank()) {
            packageName = extractPackageNameFromZip(apkBytes)
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

    /**
     * Extracts X.509 certificates from META-INF signature files (v1 JAR Scheme).
     */
    private fun extractV1Certificates(apkBytes: ByteArray): List<X509Certificate> {
        val certs = mutableListOf<X509Certificate>()
        val cf = CertificateFactory.getInstance("X.509")

        ZipInputStream(ByteArrayInputStream(apkBytes)).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                val name = entry.name.uppercase(Locale.ROOT)
                if (name.startsWith("META-INF/") && (name.endsWith(".RSA") || name.endsWith(".DSA") || name.endsWith(".EC"))) {
                    val sigBytes = zis.readBytes()
                    // Try parsing as PKCS#7 via BouncyCastle
                    try {
                        val cms = CMSSignedData(sigBytes)
                        val store = cms.certificates
                        val converter = JcaX509CertificateConverter().setProvider(bcProvider)
                        for (holder in store.getMatches(null)) {
                            if (holder is X509CertificateHolder) {
                                certs.add(converter.getCertificate(holder))
                            }
                        }
                    } catch (_: Exception) {
                        try {
                            val certList = cf.generateCertificates(ByteArrayInputStream(sigBytes))
                            for (c in certList) {
                                if (c is X509Certificate) certs.add(c)
                            }
                        } catch (_: Exception) {}
                    }
                }
                entry = zis.nextEntry
            }
        }
        return certs
    }

    /**
     * Extracts certificates from the APK Signing Block (v2 / v3 schemes).
     */
    private fun extractV2V3Certificates(apkBytes: ByteArray): List<Pair<X509Certificate, String>> {
        val results = mutableListOf<Pair<X509Certificate, String>>()
        if (apkBytes.size < 32) return results

        val buffer = ByteBuffer.wrap(apkBytes).order(ByteOrder.LITTLE_ENDIAN)

        // Find End of Central Directory (EOCD) from the end of the file
        val eocdOffset = findEocdOffset(buffer) ?: return results
        buffer.position(eocdOffset + 16)
        val centralDirOffset = buffer.int.toLong() and 0xFFFFFFFFL

        if (centralDirOffset < 32 || centralDirOffset > apkBytes.size) return results

        // Check for APK Signing Block magic right before Central Directory
        // Magic string is 16 bytes: 8 bytes size + 8 bytes "APK Sig Block 42"
        val magicOffset = (centralDirOffset - 16).toInt()
        if (magicOffset < 0) return results

        buffer.position(magicOffset)
        val magicLow = buffer.long
        val magicHigh = buffer.long

        // "APK Sig Block 42" magic ASCII: 0x3234206b636f6c42, 0x20676953204b5041 (Little-Endian)
        val magicString = "APK Sig Block 42"
        val magicBytes = ByteArray(16)
        buffer.position(magicOffset)
        buffer.get(magicBytes)

        val isMagicMatch = String(magicBytes.copyOfRange(8, 16), Charsets.US_ASCII) == magicString ||
                String(magicBytes, Charsets.US_ASCII).contains("APK Sig Block")

        if (!isMagicMatch) return results

        buffer.position(magicOffset)
        val blockSizeInFooter = buffer.long
        val blockStartOffset = (centralDirOffset - blockSizeInFooter - 8).toInt()
        if (blockStartOffset < 0) return results

        buffer.position(blockStartOffset)
        val blockSizeInHeader = buffer.long
        if (blockSizeInHeader != blockSizeInFooter) return results

        // Parse key-value ID-value pairs in the block
        val cf = CertificateFactory.getInstance("X.509")
        val pairsBuffer = buffer.slice().order(ByteOrder.LITTLE_ENDIAN)
        pairsBuffer.limit((blockSizeInHeader - 16).toInt())

        while (pairsBuffer.remaining() >= 12) {
            val pairLength = pairsBuffer.long.toInt()
            if (pairLength < 4 || pairLength > pairsBuffer.remaining()) break

            val pairId = pairsBuffer.int
            val valueLength = pairLength - 4
            val valueBytes = ByteArray(valueLength)
            pairsBuffer.get(valueBytes)

            when (pairId) {
                0x7109871a -> { // APK Signature Scheme v2 ID
                    parseSigningBlockScheme(valueBytes, cf, "v2 (Full APK Signature)").forEach { cert ->
                        results.add(Pair(cert, "v2 (Full APK)"))
                    }
                }
                0xf05368c0.toInt() -> { // APK Signature Scheme v3 ID
                    parseSigningBlockScheme(valueBytes, cf, "v3 (Full APK Signature)").forEach { cert ->
                        results.add(Pair(cert, "v3 (Full APK)"))
                    }
                }
            }
        }

        return results
    }

    private fun parseSigningBlockScheme(schemeBytes: ByteArray, cf: CertificateFactory, schemeLabel: String): List<X509Certificate> {
        val certs = mutableListOf<X509Certificate>()
        try {
            val buf = ByteBuffer.wrap(schemeBytes).order(ByteOrder.LITTLE_ENDIAN)
            if (buf.remaining() < 4) return certs
            val signersLen = buf.int
            val signersBuf = buf.slice().order(ByteOrder.LITTLE_ENDIAN)
            signersBuf.limit(signersLen.coerceAtMost(buf.remaining()))

            while (signersBuf.remaining() >= 4) {
                val signerLen = signersBuf.int
                if (signerLen <= 0 || signerLen > signersBuf.remaining()) break
                val signerBytes = ByteArray(signerLen)
                signersBuf.get(signerBytes)

                val signerSlice = ByteBuffer.wrap(signerBytes).order(ByteOrder.LITTLE_ENDIAN)
                val signedDataLen = signerSlice.int
                val signedDataBytes = ByteArray(signedDataLen.coerceAtMost(signerSlice.remaining()))
                signerSlice.get(signedDataBytes)

                val signedDataBuf = ByteBuffer.wrap(signedDataBytes).order(ByteOrder.LITTLE_ENDIAN)
                // Skip digests
                if (signedDataBuf.remaining() >= 4) {
                    val digestsLen = signedDataBuf.int
                    signedDataBuf.position(signedDataBuf.position() + digestsLen.coerceAtMost(signedDataBuf.remaining()))
                }
                // Read certificates length
                if (signedDataBuf.remaining() >= 4) {
                    val certsLen = signedDataBuf.int
                    val certsBuf = signedDataBuf.slice().order(ByteOrder.LITTLE_ENDIAN)
                    certsBuf.limit(certsLen.coerceAtMost(signedDataBuf.remaining()))

                    while (certsBuf.remaining() >= 4) {
                        val certDerLen = certsBuf.int
                        if (certDerLen <= 0 || certDerLen > certsBuf.remaining()) break
                        val certDer = ByteArray(certDerLen)
                        certsBuf.get(certDer)

                        val cert = cf.generateCertificate(ByteArrayInputStream(certDer)) as? X509Certificate
                        if (cert != null) {
                            certs.add(cert)
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return certs
    }

    private fun findEocdOffset(buffer: ByteBuffer): Int? {
        val size = buffer.capacity()
        val maxSearch = 65535 + 22
        val start = (size - 22).coerceAtLeast(0)
        val end = (size - maxSearch).coerceAtLeast(0)

        for (i in start downTo end) {
            if (buffer.get(i) == 0x50.toByte() &&
                buffer.get(i + 1) == 0x4b.toByte() &&
                buffer.get(i + 2) == 0x05.toByte() &&
                buffer.get(i + 3) == 0x06.toByte()
            ) {
                return i
            }
        }
        return null
    }

    private fun extractPackageNameFromZip(apkBytes: ByteArray): String? {
        try {
            ZipInputStream(ByteArrayInputStream(apkBytes)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (entry.name == "AndroidManifest.xml") {
                        val axmlBytes = zis.readBytes()
                        return parsePackageFromAxml(axmlBytes)
                    }
                    entry = zis.nextEntry
                }
            }
        } catch (_: Exception) {}
        return null
    }

    private fun parsePackageFromAxml(bytes: ByteArray): String? {
        // Quick extraction from binary AXML String Pool
        if (bytes.size < 32) return null
        try {
            val content = String(bytes, Charsets.ISO_8859_1)
            // Look for package-like reversed domain name tokens
            val regex = "[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*){1,5}".toRegex()
            val matches = regex.findAll(content).map { it.value }.toList()
            return matches.firstOrNull {
                !it.startsWith("android.") &&
                        !it.startsWith("schemas.") &&
                        !it.startsWith("http.") &&
                        !it.contains("version") &&
                        !it.contains("compile") &&
                        it.contains(".")
            }
        } catch (_: Exception) {}
        return null
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
