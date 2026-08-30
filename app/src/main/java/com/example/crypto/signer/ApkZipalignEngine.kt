package com.example.crypto.signer

import com.example.data.model.ApkSigningResult
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.CRC32
import java.util.zip.ZipEntry

/**
 * Custom Zip Entry model holding payload and compression parameters.
 */
data class RawZipEntry(
    val name: String,
    val data: ByteArray,
    val compressionMethod: Int = ZipEntry.DEFLATED,
    val time: Long = System.currentTimeMillis(),
    val extra: ByteArray = ByteArray(0)
)

/**
 * High-performance 4-byte Zipalign Engine for Android APK files.
 * Aligns uncompressed entries (STORED) on 4-byte boundaries relative to file start.
 */
object ApkZipalignEngine {

    private const val LOCAL_FILE_HEADER_SIG = 0x04034b50
    private const val CENTRAL_DIR_HEADER_SIG = 0x02014b50
    private const val EOCD_SIG = 0x06054b50

    /**
     * Rebuilds and aligns a collection of RawZipEntry items into a fully 4-byte aligned ZIP archive.
     */
    fun buildAlignedZip(entries: List<RawZipEntry>): ByteArray {
        val out = ByteArrayOutputStream()
        val cdOut = ByteArrayOutputStream()
        var centralDirEntriesCount = 0

        for (entry in entries) {
            val fileNameBytes = entry.name.toByteArray(Charsets.UTF_8)
            val isStored = entry.compressionMethod == ZipEntry.STORED
            val uncompressedData = entry.data
            val (compressedData, methodToUse, crcValue) = if (isStored) {
                val crc = CRC32()
                crc.update(uncompressedData)
                Triple(uncompressedData, 0, crc.value)
            } else {
                val deflaterOut = ByteArrayOutputStream()
                val deflater = java.util.zip.Deflater(java.util.zip.Deflater.DEFAULT_COMPRESSION, true)
                val deflaterStream = java.util.zip.DeflaterOutputStream(deflaterOut, deflater)
                deflaterStream.write(uncompressedData)
                deflaterStream.finish()
                val deflatedBytes = deflaterOut.toByteArray()
                deflater.end()

                val crc = CRC32()
                crc.update(uncompressedData)
                Triple(deflatedBytes, 8, crc.value)
            }

            val localHeaderOffset = out.size()
            val baseHeaderSize = 30 + fileNameBytes.size + entry.extra.size
            val currentDataOffset = localHeaderOffset + baseHeaderSize

            // For uncompressed (STORED) entries, calculate 4-byte alignment padding
            val padding = if (methodToUse == 0) {
                (4 - (currentDataOffset % 4)) % 4
            } else {
                0
            }

            val finalExtra = if (padding > 0) {
                entry.extra + ByteArray(padding)
            } else {
                entry.extra
            }

            // Write Local File Header (30 bytes + name + extra)
            val lfh = ByteBuffer.allocate(30).order(ByteOrder.LITTLE_ENDIAN)
            lfh.putInt(LOCAL_FILE_HEADER_SIG)
            lfh.putShort(20) // version needed
            lfh.putShort(0)  // flags
            lfh.putShort(methodToUse.toShort()) // method
            lfh.putInt(entry.time.toInt()) // mod time & date
            lfh.putInt(crcValue.toInt())
            lfh.putInt(compressedData.size) // compressed size
            lfh.putInt(uncompressedData.size) // uncompressed size
            lfh.putShort(fileNameBytes.size.toShort())
            lfh.putShort(finalExtra.size.toShort())

            out.write(lfh.array())
            out.write(fileNameBytes)
            if (finalExtra.isNotEmpty()) {
                out.write(finalExtra)
            }
            out.write(compressedData)

            // Write Central Directory Header for this entry
            val cdh = ByteBuffer.allocate(46).order(ByteOrder.LITTLE_ENDIAN)
            cdh.putInt(CENTRAL_DIR_HEADER_SIG)
            cdh.putShort(20) // version made by
            cdh.putShort(20) // version needed
            cdh.putShort(0)  // flags
            cdh.putShort(methodToUse.toShort())
            cdh.putInt(entry.time.toInt())
            cdh.putInt(crcValue.toInt())
            cdh.putInt(compressedData.size)
            cdh.putInt(uncompressedData.size)
            cdh.putShort(fileNameBytes.size.toShort())
            cdh.putShort(entry.extra.size.toShort()) // CD extra doesn't need LFH padding
            cdh.putShort(0) // comment len
            cdh.putShort(0) // disk number
            cdh.putShort(0) // internal file attrs
            cdh.putInt(0)   // external file attrs
            cdh.putInt(localHeaderOffset) // relative offset of LFH

            cdOut.write(cdh.array())
            cdOut.write(fileNameBytes)
            if (entry.extra.isNotEmpty()) {
                cdOut.write(entry.extra)
            }
            centralDirEntriesCount++
        }

        val cdOffset = out.size()
        val cdBytes = cdOut.toByteArray()
        out.write(cdBytes)

        // Write End of Central Directory (EOCD)
        val eocd = ByteBuffer.allocate(22).order(ByteOrder.LITTLE_ENDIAN)
        eocd.putInt(EOCD_SIG)
        eocd.putShort(0) // disk number
        eocd.putShort(0) // cd disk number
        eocd.putShort(centralDirEntriesCount.toShort()) // cd records on disk
        eocd.putShort(centralDirEntriesCount.toShort()) // total cd records
        eocd.putInt(cdBytes.size) // size of CD
        eocd.putInt(cdOffset) // offset of CD
        eocd.putShort(0) // comment length

        out.write(eocd.array())
        return out.toByteArray()
    }

    /**
     * Extracts non-signature entries from an existing APK file byte array.
     */
    fun extractCleanEntriesFromApk(apkBytes: ByteArray): List<RawZipEntry> {
        val entries = mutableListOf<RawZipEntry>()
        val zis = java.util.zip.ZipInputStream(java.io.ByteArrayInputStream(apkBytes))
        var ze = zis.nextEntry

        while (ze != null) {
            val name = ze.name
            val upper = name.uppercase()

            // Skip previous signatures and manifests to allow clean re-signing
            val isSignature = upper.startsWith("META-INF/") && (
                upper.endsWith(".SF") ||
                upper.endsWith(".RSA") ||
                upper.endsWith(".DSA") ||
                upper.endsWith(".EC") ||
                upper == "META-INF/MANIFEST.MF" ||
                upper.startsWith("META-INF/SIG-")
            )

            if (!isSignature && !ze.isDirectory) {
                val data = zis.readBytes()
                // Keep .so, resources.arsc or uncompressed assets as STORED if appropriate
                val shouldBeStored = upper.endsWith(".SO") || upper.endsWith(".PNG") || upper.endsWith(".JPG") || ze.method == ZipEntry.STORED
                entries.add(
                    RawZipEntry(
                        name = name,
                        data = data,
                        compressionMethod = if (shouldBeStored) ZipEntry.STORED else ZipEntry.DEFLATED,
                        time = ze.time
                    )
                )
            }
            ze = zis.nextEntry
        }
        zis.close()
        return entries
    }
}
