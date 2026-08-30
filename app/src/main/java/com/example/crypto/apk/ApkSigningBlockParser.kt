package com.example.crypto.apk

import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

object ApkSigningBlockParser {

    /**
     * Extracts certificates from the APK Signing Block (v2 / v3 schemes).
     */
    fun extractV2V3Certificates(apkBytes: ByteArray): List<Pair<X509Certificate, String>> {
        val results = mutableListOf<Pair<X509Certificate, String>>()
        if (apkBytes.size < 32) return results

        val buffer = ByteBuffer.wrap(apkBytes).order(ByteOrder.LITTLE_ENDIAN)

        // Find End of Central Directory (EOCD) from the end of the file
        val eocdOffset = findEocdOffset(buffer) ?: return results
        buffer.position(eocdOffset + 16)
        val centralDirOffset = buffer.int.toLong() and 0xFFFFFFFFL

        if (centralDirOffset < 32 || centralDirOffset > apkBytes.size) return results

        // Check for APK Signing Block magic right before Central Directory
        // Magic string is 16 bytes: "APK Sig Block 42"
        val magicOffset = (centralDirOffset - 16).toInt()
        if (magicOffset < 8) return results

        buffer.position(magicOffset)
        val magicBytes = ByteArray(16)
        buffer.get(magicBytes)
        val magicString = "APK Sig Block 42"
        if (String(magicBytes, Charsets.US_ASCII) != magicString) return results

        val footerSizeOffset = (centralDirOffset - 24).toInt()
        if (footerSizeOffset < 0) return results

        buffer.position(footerSizeOffset)
        val blockSizeInFooter = buffer.long
        val blockStartOffset = (centralDirOffset - blockSizeInFooter - 8).toInt()
        if (blockStartOffset < 0) return results

        buffer.position(blockStartOffset)
        val blockSizeInHeader = buffer.long
        if (blockSizeInHeader != blockSizeInFooter) return results

        // Parse key-value ID-value pairs in the block
        val cf = CertificateFactory.getInstance("X.509")
        val pairsBuffer = buffer.slice().order(ByteOrder.LITTLE_ENDIAN)
        pairsBuffer.limit((blockSizeInHeader - 24).toInt())

        while (pairsBuffer.remaining() >= 12) {
            val pairLength = pairsBuffer.long.toInt()
            if (pairLength < 4 || pairLength > pairsBuffer.remaining()) break

            val pairId = pairsBuffer.int
            val valueLength = pairLength - 4
            val valueBytes = ByteArray(valueLength)
            pairsBuffer.get(valueBytes)

            when (pairId) {
                0x7109871a -> { // APK Signature Scheme v2 ID
                    parseSigningBlockScheme(valueBytes, cf).forEach { cert ->
                        results.add(Pair(cert, "v2 (Full APK)"))
                    }
                }
                0xf05368c0.toInt() -> { // APK Signature Scheme v3 ID
                    parseSigningBlockScheme(valueBytes, cf).forEach { cert ->
                        results.add(Pair(cert, "v3 (Full APK)"))
                    }
                }
            }
        }

        return results
    }

    private fun parseSigningBlockScheme(schemeBytes: ByteArray, cf: CertificateFactory): List<X509Certificate> {
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
}
