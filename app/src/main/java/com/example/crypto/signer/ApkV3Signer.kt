package com.example.crypto.signer

import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature
import java.security.cert.X509Certificate

/**
 * APK Signature Scheme v3 (and unified APK Signing Block) generator.
 * Injects APK Signature Scheme v3 (ID: 0xf05368c0) into the APK Signing Block
 * ("APK Sig Block 42") with target SDK range (minSdkVersion / maxSdkVersion)
 * and support for Key Rotation, fully compliant with Android 9.0+ (API 28+).
 */
object ApkV3Signer {

    const val APK_SIGNATURE_SCHEME_V3_BLOCK_ID = 0xf05368c0.toInt()
    const val APK_SIGNATURE_SCHEME_V2_BLOCK_ID = 0x7109871a

    const val DEFAULT_MIN_SDK = 28 // Android 9.0 (Pie) introduction of Scheme v3
    const val DEFAULT_MAX_SDK = Integer.MAX_VALUE

    private const val SIGNATURE_RSA_PKCS1_V1_5_WITH_SHA256 = 0x0103
    private const val SIGNATURE_ECDSA_WITH_SHA256 = 0x0201
    private const val CHUNK_SIZE = 1048576 // 1MB chunks
    private val APK_SIG_BLOCK_MAGIC = "APK Sig Block 42".toByteArray(Charsets.US_ASCII)

    /**
     * Injects an APK Signing Block containing Scheme v2 and/or Scheme v3 into the ZIP archive.
     */
    fun injectSignatureBlock(
        zipBytes: ByteArray,
        privateKey: PrivateKey,
        publicKey: PublicKey,
        certificate: X509Certificate,
        bcProvider: BouncyCastleProvider,
        signV2: Boolean,
        signV3: Boolean,
        minSdk: Int = DEFAULT_MIN_SDK,
        maxSdk: Int = DEFAULT_MAX_SDK
    ): ByteArray {
        if (!signV2 && !signV3) {
            return zipBytes
        }

        val buffer = ByteBuffer.wrap(zipBytes).order(ByteOrder.LITTLE_ENDIAN)

        // 1. Locate EOCD and Central Directory offset
        val eocdOffset = findEocdOffset(buffer)
            ?: throw IllegalStateException("No se pudo localizar el registro EOCD en el archivo APK.")

        buffer.position(eocdOffset + 16)
        val centralDirOffset = buffer.int.toLong() and 0xFFFFFFFFL

        if (centralDirOffset > zipBytes.size || centralDirOffset > eocdOffset) {
            throw IllegalStateException("Estructura ZIP corrupta: offset del Directorio Central inválido.")
        }

        // Section 1: 0 until centralDirOffset
        val section1Bytes = zipBytes.copyOfRange(0, centralDirOffset.toInt())
        // Section 2: centralDirOffset until eocdOffset
        val section2Bytes = zipBytes.copyOfRange(centralDirOffset.toInt(), eocdOffset)
        // Section 3: EOCD (with updated central directory offset during hashing)
        val eocdBytes = zipBytes.copyOfRange(eocdOffset, zipBytes.size)

        // 2. Precompute dummy ID-value pairs to calculate the exact signing block size
        val dummyDigest = ByteArray(32)
        val dummyPairs = ByteArrayOutputStream()
        if (signV2) {
            dummyPairs.write(buildV2Pair(dummyDigest, privateKey, publicKey, certificate, bcProvider))
        }
        if (signV3) {
            dummyPairs.write(buildV3Pair(dummyDigest, privateKey, publicKey, certificate, bcProvider, minSdk, maxSdk))
        }
        val dummyPairsBytes = dummyPairs.toByteArray()
        val signingBlockSize = dummyPairsBytes.size + 8 + 8 + 16
        val newCentralDirOffset = centralDirOffset + signingBlockSize

        val modifiedEocdBytesForHashing = eocdBytes.clone()
        val eocdBuf = ByteBuffer.wrap(modifiedEocdBytesForHashing).order(ByteOrder.LITTLE_ENDIAN)
        eocdBuf.putInt(16, newCentralDirOffset.toInt())

        // 3. Compute 1MB chunked SHA-256 digest across sections 1, 2, and modified 3
        val contentDigest = computeApkDigest(section1Bytes, section2Bytes, modifiedEocdBytesForHashing)

        // 4. Build actual ID-value pairs with the real content digest
        val realPairs = ByteArrayOutputStream()
        if (signV2) {
            realPairs.write(buildV2Pair(contentDigest, privateKey, publicKey, certificate, bcProvider))
        }
        if (signV3) {
            realPairs.write(buildV3Pair(contentDigest, privateKey, publicKey, certificate, bcProvider, minSdk, maxSdk))
        }
        val realPairsBytes = realPairs.toByteArray()

        // 5. Build full APK Signing Block
        val fullSigningBlock = buildApkSigningBlock(realPairsBytes)

        // If for any rare reason (e.g. variable length ECDSA DER signature) the block size changed,
        // recompute digest with exact size to guarantee cryptographic integrity.
        val finalBlock = if (fullSigningBlock.size == signingBlockSize) {
            fullSigningBlock
        } else {
            val adjustedCentralDirOffset = centralDirOffset + fullSigningBlock.size
            val recheckEocdBytes = eocdBytes.clone()
            ByteBuffer.wrap(recheckEocdBytes).order(ByteOrder.LITTLE_ENDIAN).putInt(16, adjustedCentralDirOffset.toInt())
            val recheckDigest = computeApkDigest(section1Bytes, section2Bytes, recheckEocdBytes)
            val adjustedPairs = ByteArrayOutputStream()
            if (signV2) {
                adjustedPairs.write(buildV2Pair(recheckDigest, privateKey, publicKey, certificate, bcProvider))
            }
            if (signV3) {
                adjustedPairs.write(buildV3Pair(recheckDigest, privateKey, publicKey, certificate, bcProvider, minSdk, maxSdk))
            }
            buildApkSigningBlock(adjustedPairs.toByteArray())
        }

        // 6. Assemble final APK: Section 1 + APK Signing Block + Section 2 (Central Dir) + EOCD (with new Central Dir Offset)
        val out = ByteArrayOutputStream(section1Bytes.size + finalBlock.size + section2Bytes.size + eocdBytes.size)
        out.write(section1Bytes)
        out.write(finalBlock)
        out.write(section2Bytes)

        val finalEocdBytes = eocdBytes.clone()
        val finalEocdBuf = ByteBuffer.wrap(finalEocdBytes).order(ByteOrder.LITTLE_ENDIAN)
        finalEocdBuf.putInt(16, (centralDirOffset + finalBlock.size).toInt())
        out.write(finalEocdBytes)

        return out.toByteArray()
    }

    /**
     * Standalone convenience method for Scheme v3 only.
     */
    fun injectV3Signature(
        zipBytes: ByteArray,
        privateKey: PrivateKey,
        publicKey: PublicKey,
        certificate: X509Certificate,
        bcProvider: BouncyCastleProvider,
        minSdk: Int = DEFAULT_MIN_SDK,
        maxSdk: Int = DEFAULT_MAX_SDK
    ): ByteArray {
        return injectSignatureBlock(
            zipBytes = zipBytes,
            privateKey = privateKey,
            publicKey = publicKey,
            certificate = certificate,
            bcProvider = bcProvider,
            signV2 = false,
            signV3 = true,
            minSdk = minSdk,
            maxSdk = maxSdk
        )
    }

    /**
     * Builds ID-Value pair for Scheme v3 (ID: 0xf05368c0).
     */
    fun buildV3Pair(
        contentDigest: ByteArray,
        privateKey: PrivateKey,
        publicKey: PublicKey,
        certificate: X509Certificate,
        bcProvider: BouncyCastleProvider,
        minSdk: Int = DEFAULT_MIN_SDK,
        maxSdk: Int = DEFAULT_MAX_SDK
    ): ByteArray {
        val isEc = privateKey.algorithm.uppercase().contains("EC")
        val algorithmId = if (isEc) SIGNATURE_ECDSA_WITH_SHA256 else SIGNATURE_RSA_PKCS1_V1_5_WITH_SHA256
        val sigAlg = if (isEc) "SHA256withECDSA" else "SHA256withRSA"

        val certDer = certificate.encoded
        val pubKeyDer = publicKey.encoded

        // 1. Digests block
        val digestEntry = ByteBuffer.allocate(4 + 4 + contentDigest.size).order(ByteOrder.LITTLE_ENDIAN)
        digestEntry.putInt(algorithmId)
        digestEntry.putInt(contentDigest.size)
        digestEntry.put(contentDigest)
        val digestsSeq = lengthPrefixed(lengthPrefixed(digestEntry.array()))

        // 2. Certificates block
        val certsSeq = lengthPrefixed(lengthPrefixed(certDer))

        // 3. Additional attributes block (empty)
        val additionalAttrs = lengthPrefixed(ByteArray(0))

        // 4. SignedData (digests, certs, minSDK, maxSDK, additionalAttrs)
        val signedData = ByteBuffer.allocate(
            digestsSeq.size + certsSeq.size + 4 + 4 + additionalAttrs.size
        ).order(ByteOrder.LITTLE_ENDIAN)
        signedData.put(digestsSeq)
        signedData.put(certsSeq)
        signedData.putInt(minSdk)
        signedData.putInt(maxSdk)
        signedData.put(additionalAttrs)
        val signedDataBytes = signedData.array()

        // 5. Generate signature over signedDataBytes
        val signature = Signature.getInstance(sigAlg, bcProvider)
        signature.initSign(privateKey)
        signature.update(signedDataBytes)
        val sigBytes = signature.sign()

        val sigEntry = ByteBuffer.allocate(4 + 4 + sigBytes.size).order(ByteOrder.LITTLE_ENDIAN)
        sigEntry.putInt(algorithmId)
        sigEntry.putInt(sigBytes.size)
        sigEntry.put(sigBytes)
        val signaturesSeq = lengthPrefixed(lengthPrefixed(sigEntry.array()))

        // 6. Signer block (signedData, minSdk, maxSdk, signatures, publicKey)
        val signer = ByteBuffer.allocate(
            4 + signedDataBytes.size +
            4 + 4 +
            signaturesSeq.size +
            4 + pubKeyDer.size
        ).order(ByteOrder.LITTLE_ENDIAN)
        signer.putInt(signedDataBytes.size)
        signer.put(signedDataBytes)
        signer.putInt(minSdk)
        signer.putInt(maxSdk)
        signer.put(signaturesSeq)
        signer.putInt(pubKeyDer.size)
        signer.put(pubKeyDer)

        val signersSeq = lengthPrefixed(lengthPrefixed(signer.array()))

        // 7. ID-Value pair for Scheme v3
        val pairValue = signersSeq
        val pairLen = 4L + pairValue.size
        val pairBuf = ByteBuffer.allocate(8 + 4 + pairValue.size).order(ByteOrder.LITTLE_ENDIAN)
        pairBuf.putLong(pairLen)
        pairBuf.putInt(APK_SIGNATURE_SCHEME_V3_BLOCK_ID)
        pairBuf.put(pairValue)
        return pairBuf.array()
    }

    /**
     * Builds ID-Value pair for Scheme v2 (ID: 0x7109871a).
     */
    fun buildV2Pair(
        contentDigest: ByteArray,
        privateKey: PrivateKey,
        publicKey: PublicKey,
        certificate: X509Certificate,
        bcProvider: BouncyCastleProvider
    ): ByteArray {
        val isEc = privateKey.algorithm.uppercase().contains("EC")
        val algorithmId = if (isEc) SIGNATURE_ECDSA_WITH_SHA256 else SIGNATURE_RSA_PKCS1_V1_5_WITH_SHA256
        val sigAlg = if (isEc) "SHA256withECDSA" else "SHA256withRSA"

        val certDer = certificate.encoded
        val pubKeyDer = publicKey.encoded

        // 1. Digests block
        val digestEntry = ByteBuffer.allocate(4 + 4 + contentDigest.size).order(ByteOrder.LITTLE_ENDIAN)
        digestEntry.putInt(algorithmId)
        digestEntry.putInt(contentDigest.size)
        digestEntry.put(contentDigest)
        val digestsSeq = lengthPrefixed(lengthPrefixed(digestEntry.array()))

        // 2. Certificates block
        val certsSeq = lengthPrefixed(lengthPrefixed(certDer))

        // 3. Additional attributes block (empty)
        val additionalAttrs = lengthPrefixed(ByteArray(0))

        // 4. SignedData (digests, certs, additionalAttrs)
        val signedData = ByteBuffer.allocate(digestsSeq.size + certsSeq.size + additionalAttrs.size)
        signedData.put(digestsSeq)
        signedData.put(certsSeq)
        signedData.put(additionalAttrs)
        val signedDataBytes = signedData.array()

        // 5. Generate signature over signedDataBytes
        val signature = Signature.getInstance(sigAlg, bcProvider)
        signature.initSign(privateKey)
        signature.update(signedDataBytes)
        val sigBytes = signature.sign()

        val sigEntry = ByteBuffer.allocate(4 + 4 + sigBytes.size).order(ByteOrder.LITTLE_ENDIAN)
        sigEntry.putInt(algorithmId)
        sigEntry.putInt(sigBytes.size)
        sigEntry.put(sigBytes)
        val signaturesSeq = lengthPrefixed(lengthPrefixed(sigEntry.array()))

        // 6. Signer block (signedData, signatures, publicKey)
        val signer = ByteBuffer.allocate(
            4 + signedDataBytes.size +
            signaturesSeq.size +
            4 + pubKeyDer.size
        ).order(ByteOrder.LITTLE_ENDIAN)
        signer.putInt(signedDataBytes.size)
        signer.put(signedDataBytes)
        signer.put(signaturesSeq)
        signer.putInt(pubKeyDer.size)
        signer.put(pubKeyDer)

        val signersSeq = lengthPrefixed(lengthPrefixed(signer.array()))

        // 7. ID-Value pair for Scheme v2
        val pairValue = signersSeq
        val pairLen = 4L + pairValue.size
        val pairBuf = ByteBuffer.allocate(8 + 4 + pairValue.size).order(ByteOrder.LITTLE_ENDIAN)
        pairBuf.putLong(pairLen)
        pairBuf.putInt(APK_SIGNATURE_SCHEME_V2_BLOCK_ID)
        pairBuf.put(pairValue)
        return pairBuf.array()
    }

    private fun buildApkSigningBlock(pairsBytes: ByteArray): ByteArray {
        val blockSize = (pairsBytes.size + 8 + 16).toLong()
        val fullBlock = ByteBuffer.allocate(8 + pairsBytes.size + 8 + 16).order(ByteOrder.LITTLE_ENDIAN)
        fullBlock.putLong(blockSize)
        fullBlock.put(pairsBytes)
        fullBlock.putLong(blockSize)
        fullBlock.put(APK_SIG_BLOCK_MAGIC)
        return fullBlock.array()
    }

    private fun lengthPrefixed(bytes: ByteArray): ByteArray {
        val buf = ByteBuffer.allocate(4 + bytes.size).order(ByteOrder.LITTLE_ENDIAN)
        buf.putInt(bytes.size)
        buf.put(bytes)
        return buf.array()
    }

    fun computeApkDigest(sec1: ByteArray, sec2: ByteArray, sec3: ByteArray): ByteArray {
        val md = MessageDigest.getInstance("SHA-256")
        val chunkDigests = mutableListOf<ByteArray>()

        fun hashSection(bytes: ByteArray) {
            var offset = 0
            while (offset < bytes.size) {
                val chunkSize = (bytes.size - offset).coerceAtMost(CHUNK_SIZE)
                md.reset()
                md.update(0x5a.toByte())
                val lenBuf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(chunkSize).array()
                md.update(lenBuf)
                md.update(bytes, offset, chunkSize)
                chunkDigests.add(md.digest())
                offset += chunkSize
            }
        }

        hashSection(sec1)
        hashSection(sec2)
        hashSection(sec3)

        // Top level digest over all chunk digests
        md.reset()
        md.update(0x5a.toByte())
        val countBuf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(chunkDigests.size).array()
        md.update(countBuf)
        chunkDigests.forEach { md.update(it) }
        return md.digest()
    }

    fun findEocdOffset(buffer: ByteBuffer): Int? {
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
