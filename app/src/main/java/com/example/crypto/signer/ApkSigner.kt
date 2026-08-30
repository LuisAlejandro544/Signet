package com.example.crypto.signer

import android.content.Context
import com.example.crypto.apk.AxmlManifestParser
import com.example.data.model.ApkSigningOptions
import com.example.data.model.ApkSigningResult
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.security.KeyStore
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Security
import java.security.cert.X509Certificate
import java.util.Locale

/**
 * Sovereign APK Signing and Optimization (Zipalign) Engine.
 * Supports dual Scheme v1 (JAR) + Scheme v2 (Full APK Signature) and 4-byte memory alignment.
 */
object ApkSigner {

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

    fun signApk(
        apkBytes: ByteArray,
        keystoreBytes: ByteArray,
        storePassword: String,
        alias: String,
        keyPassword: String,
        options: ApkSigningOptions = ApkSigningOptions(),
        context: Context? = null,
        onProgress: ((step: String, progress: Float) -> Unit)? = null
    ): ApkSigningResult {
        val startTime = System.currentTimeMillis()
        try {
            onProgress?.invoke("Cargando almacén de claves (Keystore)...", 0.1f)

            // 1. Load Keystore (try JKS, PKCS12, BKS)
            val (keyStore, loadedAlias) = loadKeyStore(keystoreBytes, storePassword, alias)
            val effectiveAlias = loadedAlias ?: alias

            val cert = keyStore.getCertificate(effectiveAlias) as? X509Certificate
                ?: throw IllegalArgumentException("No se encontró ningún certificado X.509 bajo el alias '$effectiveAlias'.")

            val effectiveKeyPassword = if (keyPassword.isNotBlank()) keyPassword else storePassword
            val key = keyStore.getKey(effectiveAlias, effectiveKeyPassword.toCharArray()) as? PrivateKey
                ?: throw IllegalArgumentException("No se pudo obtener la clave privada para el alias '$effectiveAlias'. Verifica la contraseña de la clave.")

            val publicKey: PublicKey = cert.publicKey

            // 2. Extract and sanitize APK entries
            onProgress?.invoke("Limpiando firmas previas y analizando componentes...", 0.3f)
            val packageName = AxmlManifestParser.extractPackageNameFromZip(apkBytes)
            val cleanEntries = ApkZipalignEngine.extractCleanEntriesFromApk(apkBytes)

            if (cleanEntries.isEmpty()) {
                throw IllegalArgumentException("El archivo APK proporcionado está vacío o no contiene entradas válidas.")
            }

            val appliedSchemes = mutableListOf<String>()
            val mutableEntries = cleanEntries.toMutableList()

            // 3. Apply Scheme v1 (JAR Signing) if enabled
            if (options.signV1) {
                onProgress?.invoke("Generando manifiesto y firmas de Esquema v1 (JAR)...", 0.5f)
                val v1Entries = ApkV1Signer.generateV1Entries(cleanEntries, key, cert, bcProvider)
                // Place META-INF files at the beginning of the zip
                mutableEntries.addAll(0, v1Entries)
                appliedSchemes.add("v1 (JAR Signing)")
            }

            // 4. Build 4-byte aligned ZIP
            onProgress?.invoke("Aplicando optimización Zipalign (alineación a 4 bytes)...", 0.7f)
            var signedZipBytes = ApkZipalignEngine.buildAlignedZip(mutableEntries)

            // 5. Apply Scheme v2 (APK Signing Block) if enabled
            if (options.signV2) {
                onProgress?.invoke("Inyectando bloque de firma Esquema v2 (APK Signature Block)...", 0.85f)
                signedZipBytes = ApkV2Signer.injectV2Signature(signedZipBytes, key, publicKey, cert, bcProvider)
                appliedSchemes.add("v2 (APK Signature Scheme v2)")
            }

            // 6. Save signed APK to cache if context is available
            var outputFile: File? = null
            if (context != null) {
                val outputDir = File(context.cacheDir, "signed_apks").apply { mkdirs() }
                val cleanOutputName = if (options.outputFileName.endsWith(".apk", ignoreCase = true)) {
                    options.outputFileName
                } else {
                    "${options.outputFileName}.apk"
                }
                outputFile = File(outputDir, cleanOutputName)
                FileOutputStream(outputFile).use { it.write(signedZipBytes) }
            }

            val sha256Fingerprint = calculateSha256Fingerprint(cert.encoded)
            val duration = System.currentTimeMillis() - startTime

            onProgress?.invoke("¡APK firmado y optimizado con éxito!", 1.0f)

            return ApkSigningResult(
                isSuccess = true,
                signedApkBytes = signedZipBytes,
                signedApkFile = outputFile,
                outputFileName = options.outputFileName,
                outputFileSizeBytes = signedZipBytes.size.toLong(),
                packageName = packageName,
                versionName = null,
                versionCode = null,
                sha256Fingerprint = sha256Fingerprint,
                appliedSchemes = appliedSchemes,
                zipalignApplied = options.zipalign,
                durationMs = duration,
                errorMessage = null
            )
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            return ApkSigningResult(
                isSuccess = false,
                outputFileName = options.outputFileName,
                durationMs = duration,
                errorMessage = e.localizedMessage ?: "Error inesperado durante la firma del APK."
            )
        }
    }

    private fun loadKeyStore(bytes: ByteArray, password: String, targetAlias: String): Pair<KeyStore, String?> {
        val types = listOf("JKS", "PKCS12", "BKS")
        for (type in types) {
            try {
                val ks = if (type == "BKS") {
                    KeyStore.getInstance(type, bcProvider)
                } else {
                    KeyStore.getInstance(type)
                }
                ks.load(ByteArrayInputStream(bytes), password.toCharArray())

                // Check alias
                if (targetAlias.isNotBlank() && ks.containsAlias(targetAlias)) {
                    return Pair(ks, targetAlias)
                }

                // If targetAlias blank or not found, try finding first key alias
                val aliases = ks.aliases()
                while (aliases.hasMoreElements()) {
                    val a = aliases.nextElement()
                    if (ks.isKeyEntry(a)) {
                        return Pair(ks, a)
                    }
                }

                return Pair(ks, null)
            } catch (_: Exception) {
                // Try next type
            }
        }
        throw IllegalArgumentException("Contraseña incorrecta o formato de Keystore no compatible (.jks, .p12, .keystore).")
    }

    private fun calculateSha256Fingerprint(bytes: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.joinToString(":") { "%02X".format(it) }
    }
}
