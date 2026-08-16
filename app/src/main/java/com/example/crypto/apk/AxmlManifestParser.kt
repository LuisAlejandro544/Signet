package com.example.crypto.apk

import java.io.ByteArrayInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

object AxmlManifestParser {

    /**
     * Attempts to find and extract AndroidManifest.xml from the APK ZIP stream.
     */
    fun extractPackageNameFromZip(apkBytes: ByteArray): String? {
        try {
            ZipInputStream(ByteArrayInputStream(apkBytes)).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
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

    /**
     * Quick extraction of package name from binary AXML String Pool.
     */
    fun parsePackageFromAxml(bytes: ByteArray): String? {
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
}
