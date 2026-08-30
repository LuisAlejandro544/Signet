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
 * APK Signature Scheme v2 generator.
 * Injects the APK Signing Block (Magic: "APK Sig Block 42", ID: 0x7109871a) into the APK.
 */
object ApkV2Signer {

    private const val APK_SIGNATURE_SCHEME_V2_BLOCK_ID = 0x7109871a
    private const val SIGNATURE_RSA_PKCS1_V1_5_WITH_SHA256 = 0x0103
    private const val SIGNATURE_ECDSA_WITH_SHA256 = 0x0201
    private const val CHUNK_SIZE = 1048576 // 1MB
    private val APK_SIG_BLOCK_MAGIC = "APK Sig Block 42".toByteArray(Charsets.US_ASCII)

    fun injectV2Signature(
        zipBytes: ByteArray,
        privateKey: PrivateKey,
        publicKey: PublicKey,
        certificate: X509Certificate,
        bcProvider: BouncyCastleProvider
    ): ByteArray {
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
        // Section 3: EOCD (with updated central directory offset in hashing)
        val eocdBytes = zipBytes.copyOfRange(eocdOffset, zipBytes.size)

        val isEc = privateKey.algorithm.uppercase().contains("EC")
        val algorithmId = if (isEc) SIGNATURE_ECDSA_WITH_SHA256 else SIGNATURE_RSA_PKCS1_V1_5_WITH_SHA256
        val sigAlg = if (isEc) "SHA256withECDSA" else "SHA256withRSA"

        // 2. Compute 1MB chunked content digests across sections
        // Note: For section 3 hashing, the central dir offset in EOCD is virtually updated
        // to point after the signing block. We compute signing block size first.
        val certDer = certificate.encoded
        val pubKeyDer = publicKey.encoded

        // We prepare a preliminary signed data to calculate exact block size
        val dummyContentDigest = ByteArray(32)
        val dummyBlock = buildSigningBlock(
            dummyContentDigest,
            algorithmId,
            privateKey,
            pubKeyDer,
            certDer,
            sigAlg,
            bcProvider
        )

        val signingBlockSize = dummyBlock.size
        val newCentralDirOffset = centralDirOffset + signingBlockSize

        val modifiedEocdBytesForHashing = eocdBytes.clone()
        val eocdBuf = ByteBuffer.wrap(modifiedEocdBytesForHashing).order(ByteOrder.LITTLE_ENDIAN)
        eocdBuf.putInt(16, newCentralDirOffset.toInt())

        // Compute actual 1MB chunked digests
        val contentDigest = computeApkV2Digest(section1Bytes, section2Bytes, modifiedEocdBytesForHashing)

        // 3. Build real Signing Block with real signature
        val finalSigningBlock = buildSigningBlock(
            contentDigest,
            algorithmId,
            privateKey,
            pubKeyDer,
            certDer,
            sigAlg,
            bcProvider
        )

        // 4. Assemble final APK with Signing Block inserted before Central Directory
        val out = ByteArrayOutputStream()
        out.write(section1Bytes)
        out.write(finalSigningBlock)
        out.write(section2Bytes)

        // Update EOCD with new central dir offset in the output file
        val finalEocdBytes = eocdBytes.clone()
        val finalEocdBuf = ByteBuffer.wrap(finalEocdBytes).order(ByteOrder.LITTLE_ENDIAN)
        finalEocdBuf.putInt(16, (centralDirOffset + finalSigningBlock.size).toInt())
        out.write(finalEocdBytes)

        return out.toByteArray()
    }

    private fun computeApkV2Digest(sec1: ByteArray, sec2: ByteArray, sec3: ByteArray): ByteArray {
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

    private fun buildSigningBlock(
        contentDigest: ByteArray,
        algorithmId: Int,
        privateKey: PrivateKey,
        pubKeyDer: ByteArray,
        certDer: ByteArray,
        sigAlg: String,
        bcProvider: BouncyCastleProvider
    ): ByteArray {
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

        // SignedData
        val signedData = ByteBuffer.allocate(digestsSeq.size + certsSeq.size + additionalAttrs.size)
        signedData.put(digestsSeq)
        signedData.put(certsSeq)
        signedData.put(additionalAttrs)
        val signedDataBytes = signedData.array()

        // 4. Generate Signature over signedDataBytes
        val signature = Signature.getInstance(sigAlg, bcProvider)
        signature.initSign(privateKey)
        signature.update(signedDataBytes)
        val sigBytes = signature.sign()

        val sigEntry = ByteBuffer.allocate(4 + 4 + sigBytes.size).order(ByteOrder.LITTLE_ENDIAN)
        sigEntry.putInt(algorithmId)
        sigEntry.putInt(sigBytes.size)
        sigEntry.put(sigBytes)
        val signaturesSeq = lengthPrefixed(lengthPrefixed(sigEntry.array()))

        // 5. Signer block
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

        // 6. ID-Value Pair for Scheme v2
        val pairValue = signersSeq
        val pairLen = 4L + pairValue.size
        val pairBuf = ByteBuffer.allocate(8 + 4 + pairValue.size).order(ByteOrder.LITTLE_ENDIAN)
        pairBuf.putLong(pairLen)
        pairBuf.putInt(APK_SIGNATURE_SCHEME_V2_BLOCK_ID)
        pairBuf.put(pairValue)
        val pairBytes = pairBuf.array()

        // 7. Full APK Signing Block
        val blockSize = (pairBytes.size + 8 + 16).toLong()
        val fullBlock = ByteBuffer.allocate(8 + pairBytes.size + 8 + 16).order(ByteOrder.LITTLE_ENDIAN)
        fullBlock.putLong(blockSize)
        fullBlock.put(pairBytes)
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
