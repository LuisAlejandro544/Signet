package com.example.desktop.cli

import com.example.crypto.ApkMatcher
import com.example.crypto.Base64Compat
import com.example.crypto.DesktopStorageUtils
import com.example.crypto.KeystoreGenerator
import com.example.crypto.SignetBackupManager
import com.example.crypto.signer.ApkSigner
import com.example.crypto.x509.X509CertificateInspector
import com.example.data.model.ApkSigningOptions
import com.example.data.model.DistinguishedName
import com.example.data.model.KeyAlgorithm
import com.example.data.model.KeystoreConfig
import java.awt.Desktop
import java.io.File
import java.io.FileOutputStream

/**
 * Gestor de comandos de línea de órdenes (CLI) para Signet Desktop.
 * Proporciona soporte headless para scripts en Windows Terminal, PowerShell y CI/CD.
 */
object DesktopCliHandler {

    fun handle(args: Array<String>) {
        val command = args[0].lowercase()
        val params = parseNamedArgs(args.drop(1))

        when (command) {
            "sign" -> executeSignApk(params)
            "generate", "gen" -> executeGenerateKeystore(params)
            "inspect" -> executeInspect(params)
            "match" -> executeMatch(params)
            "base64" -> executeBase64(params)
            "backup-create" -> executeBackupCreate(params)
            "vault" -> executeVault(params)
            "--open-vault", "-v" -> {
                val dir = DesktopStorageUtils.getDesktopDataDir()
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(dir)
                    println("Bóveda abierta en el explorador: ${dir.absolutePath}")
                } else {
                    println("Bóveda ubicada en: ${dir.absolutePath}")
                }
            }
            "--version", "-V" -> {
                println("Signet Desktop v1.0.0 (Windows / Desktop Edition)")
                println("Android Keystore Generator & APK Sovereign Signer")
            }
            "--help", "-h", "help" -> printHelp()
            else -> {
                println("Comando o parámetro no reconocido: '$command'.")
                println("Ejecuta 'signet --help' para ver la lista de comandos disponibles.")
            }
        }
    }

    private fun parseNamedArgs(args: List<String>): Map<String, String> {
        val map = mutableMapOf<String, String>()
        var i = 0
        while (i < args.size) {
            val arg = args[i]
            if (arg.startsWith("--") || arg.startsWith("-")) {
                val key = arg.trimStart('-').lowercase()
                if (i + 1 < args.size && !args[i + 1].startsWith("-")) {
                    map[key] = args[i + 1]
                    i += 2
                } else {
                    map[key] = "true"
                    i += 1
                }
            } else {
                i += 1
            }
        }
        return map
    }

    private fun executeSignApk(params: Map<String, String>) {
        val apkPath = params["apk"] ?: params["a"] ?: params["input"]
        val keystorePath = params["keystore"] ?: params["k"] ?: params["ks"]
        val password = params["password"] ?: params["p"] ?: params["storepass"] ?: ""
        val alias = params["alias"] ?: params["al"] ?: ""
        val keyPass = params["keypass"] ?: params["kp"] ?: password
        val outputPath = params["output"] ?: params["o"]
        val v1 = params["v1"]?.toBooleanStrictOrNull() ?: true
        val v2 = params["v2"]?.toBooleanStrictOrNull() ?: true
        val v3 = params["v3"]?.toBooleanStrictOrNull() ?: true
        val zipalign = params["zipalign"]?.toBooleanStrictOrNull() ?: true

        if (apkPath == null || keystorePath == null) {
            System.err.println("Error: Faltan argumentos requeridos para firmar.")
            System.err.println("Uso: signet sign --apk <app.apk> --keystore <key.jks> --password <pwd> [--alias <alias>] [--keypass <pwd>] [--output <out.apk>]")
            return
        }

        val apkFile = File(apkPath)
        val keystoreFile = File(keystorePath)

        if (!apkFile.exists() || !apkFile.isFile) {
            System.err.println("Error: El archivo APK no existe: $apkPath")
            return
        }
        if (!keystoreFile.exists() || !keystoreFile.isFile) {
            System.err.println("Error: El archivo Keystore no existe: $keystorePath")
            return
        }

        println("Firmando APK: ${apkFile.name} (${apkFile.length()} bytes)...")
        println("Usando Keystore: ${keystoreFile.name} (Esquemas: v1=$v1, v2=$v2, v3=$v3, Zipalign=$zipalign)")

        val outDir = if (outputPath != null) File(outputPath).parentFile else apkFile.parentFile
        val outFileName = if (outputPath != null) File(outputPath).name else "${apkFile.nameWithoutExtension}-signed.apk"

        val options = ApkSigningOptions(
            signV1 = v1,
            signV2 = v2,
            signV3 = v3,
            zipalign = zipalign,
            outputFileName = outFileName
        )

        val result = ApkSigner.signApk(
            apkBytes = apkFile.readBytes(),
            keystoreBytes = keystoreFile.readBytes(),
            storePassword = password,
            alias = alias,
            keyPassword = keyPass,
            options = options,
            outputDirectory = outDir
        )

        if (result.isSuccess) {
            val destination = File(outDir, outFileName)
            if (result.signedApkBytes != null && (!destination.exists() || destination.length() == 0L)) {
                FileOutputStream(destination).use { it.write(result.signedApkBytes) }
            }
            println("==================================================")
            println("  [OK] APK Firmado Exitosamente!")
            println("  Archivo salida: ${destination.absolutePath}")
            println("  Tamaño final: ${destination.length()} bytes")
            println("  Esquemas aplicados: ${result.appliedSchemes.joinToString(", ")}")
            println("  Zipalign 4-byte: ${if (result.zipalignApplied) "Si" else "No"}")
            println("  Huella SHA-256: ${result.sha256Fingerprint ?: "N/A"}")
            println("  Duración: ${result.durationMs} ms")
            println("==================================================")
        } else {
            System.err.println("Error durante la firma del APK: ${result.errorMessage}")
        }
    }

    private fun executeGenerateKeystore(params: Map<String, String>) {
        val outputDirStr = params["output"] ?: params["o"] ?: "."
        val fileName = params["name"] ?: params["n"] ?: "release-key.jks"
        val alias = params["alias"] ?: params["a"] ?: "key0"
        val password = params["password"] ?: params["p"] ?: params["storepass"] ?: ""
        val keyPass = params["keypass"] ?: params["kp"] ?: password
        val algoStr = params["algorithm"] ?: params["algo"] ?: "RSA_2048"
        val validityYears = params["validity"]?.toIntOrNull() ?: 30

        if (password.length < 6) {
            System.err.println("Error: La contraseña debe tener al menos 6 caracteres.")
            return
        }

        val algorithm = when (algoStr.uppercase()) {
            "RSA_4096", "RSA4096" -> KeyAlgorithm.RSA_4096
            "EC_P256", "EC", "ECDSA" -> KeyAlgorithm.EC_P256
            else -> KeyAlgorithm.RSA_2048
        }

        val dn = DistinguishedName(
            commonName = params["cn"] ?: "Android Signer",
            organizationalUnit = params["ou"] ?: "Mobile",
            organization = params["o"] ?: "Development",
            locality = params["l"] ?: "",
            state = params["st"] ?: "",
            countryCode = params["c"] ?: "US"
        )

        val config = KeystoreConfig(
            fileName = fileName,
            storePassword = password,
            alias = alias,
            keyPassword = keyPass,
            validityYears = validityYears,
            distinguishedName = dn,
            algorithm = algorithm
        )

        val outDir = File(outputDirStr)
        if (!outDir.exists()) outDir.mkdirs()

        println("Generando Keystore PKCS#12 ($algorithm, $validityYears años)...")
        val details = KeystoreGenerator.generateKeystore(outDir, config, saveToFile = true)

        println("==================================================")
        println("  [OK] Keystore Generado Exitosamente!")
        println("  Ruta: ${details.filePath ?: File(outDir, fileName).absolutePath}")
        println("  Alias: ${details.alias}")
        println("  Algoritmo: ${details.algorithm}")
        println("  Huella SHA-256: ${details.sha256Fingerprint}")
        println("  Huella SHA-1:   ${details.sha1Fingerprint}")
        println("  Huella MD5:     ${details.md5Fingerprint}")
        println("==================================================")
    }

    private fun executeInspect(params: Map<String, String>) {
        val filePath = params["file"] ?: params["f"] ?: params["input"]
        val password = params["password"] ?: params["p"] ?: ""

        if (filePath == null) {
            System.err.println("Error: Especifica el archivo a inspeccionar con --file <ruta>")
            return
        }

        val file = File(filePath)
        if (!file.exists()) {
            System.err.println("Error: Archivo no encontrado: $filePath")
            return
        }

        val bytes = file.readBytes()
        println("Inspeccionando: ${file.name} (${bytes.size} bytes)...")

        if (file.extension.equals("apk", ignoreCase = true)) {
            val apkInfo = ApkMatcher.analyzeApk(null, bytes, file.name)
            println("==================================================")
            println("  Metadatos del APK:")
            println("  Paquete: ${apkInfo.packageName ?: "No detectado"}")
            println("  Versión: ${apkInfo.versionName ?: "N/A"} (Code: ${apkInfo.versionCode ?: "N/A"})")
            println("  Esquemas detectados: ${apkInfo.signatureSchemesFound.joinToString(", ")}")
            println("  Certificados (${apkInfo.certificates.size}):")
            apkInfo.certificates.forEachIndexed { idx, cert ->
                println("    [$idx] Sujeto: ${cert.subjectDn}")
                println("        Esquema: ${cert.signatureScheme}")
                println("        SHA-256: ${cert.sha256Fingerprint}")
                println("        SHA-1:   ${cert.sha1Fingerprint}")
            }
            println("==================================================")
        } else {
            try {
                val certs = X509CertificateInspector.inspectKeystore(bytes, password)
                println("==================================================")
                println("  Keystore X.509 (${certs.size} certificados encontrados):")
                certs.forEachIndexed { idx, ks ->
                    println("  [$idx] Alias: ${ks.alias}")
                    println("      Algoritmo: ${ks.algorithm}")
                    println("      Validez: ${ks.validFrom} hasta ${ks.validUntil}")
                    println("      SHA-256: ${ks.sha256Fingerprint}")
                    println("      SHA-1:   ${ks.sha1Fingerprint}")
                    println("      MD5:     ${ks.md5Fingerprint}")
                }
                println("==================================================")
            } catch (e: Exception) {
                System.err.println("Error al inspeccionar Keystore: ${e.message}")
            }
        }
    }

    private fun executeMatch(params: Map<String, String>) {
        val apkPath = params["apk"] ?: params["a"]
        val keystorePath = params["keystore"] ?: params["k"]
        val password = params["password"] ?: params["p"] ?: ""

        if (apkPath == null || keystorePath == null) {
            System.err.println("Uso: signet match --apk <app.apk> --keystore <key.jks> [--password <pwd>]")
            return
        }

        val apkFile = File(apkPath)
        val ksFile = File(keystorePath)
        if (!apkFile.exists() || !ksFile.exists()) {
            System.err.println("Error: Archivo APK o Keystore no encontrado.")
            return
        }

        println("Analizando coincidencia criptográfica entre APK y Keystore...")
        val apkInfo = ApkMatcher.analyzeApk(null, apkFile.readBytes(), apkFile.name)
        val ksCerts = X509CertificateInspector.inspectKeystore(ksFile.readBytes(), password)

        val match = ksCerts.any { ks ->
            apkInfo.certificates.any { ac ->
                ac.sha256Fingerprint.replace(":", "").equals(ks.sha256Fingerprint.replace(":", ""), ignoreCase = true)
            }
        }

        println("==================================================")
        if (match) {
            println("  [COINCIDENCIA EXACTA] El APK coincide con el Keystore!")
        } else {
            println("  [NO COINCIDE] Los certificados del APK y del Keystore son diferentes.")
        }
        println("==================================================")
    }

    private fun executeBase64(params: Map<String, String>) {
        val filePath = params["file"] ?: params["f"] ?: params["keystore"] ?: params["k"]
        if (filePath == null) {
            System.err.println("Uso: signet base64 --file <archivo.jks>")
            return
        }
        val file = File(filePath)
        if (!file.exists()) {
            System.err.println("Error: Archivo no encontrado: $filePath")
            return
        }
        val base64Str = Base64Compat.encodeToString(file.readBytes())
        println(base64Str)
    }

    private fun executeBackupCreate(params: Map<String, String>) {
        val ksPath = params["keystore"] ?: params["k"]
        val password = params["password"] ?: params["p"] ?: ""
        val outZip = params["output"] ?: params["o"] ?: "signet-backup.zip"

        if (ksPath == null) {
            System.err.println("Uso: signet backup-create --keystore <key.jks> --password <pwd> [--output <backup.zip>]")
            return
        }

        val file = File(ksPath)
        if (!file.exists()) {
            System.err.println("Error: Keystore no encontrado: $ksPath")
            return
        }

        val bytes = file.readBytes()
        val certs = X509CertificateInspector.inspectKeystore(bytes, password)
        if (certs.isEmpty()) {
            System.err.println("Error: No se pudieron extraer certificados del Keystore con la contraseña provista.")
            return
        }

        val details = certs.first()
        val zipBytes = SignetBackupManager.createBackupZip(details, bytes)
        val outFile = File(outZip)
        FileOutputStream(outFile).use { it.write(zipBytes) }

        println("==================================================")
        println("  [OK] Respaldo ZIP anti-manipulación creado: ${outFile.absolutePath}")
        println("  Tamaño: ${outFile.length()} bytes")
        println("  Firma HMAC-SHA256 y hash SHA-256 integrados.")
        println("==================================================")
    }

    private fun executeVault(params: Map<String, String>) {
        val vaultDir = DesktopStorageUtils.getDesktopDataDir()
        val list = params["list"]?.toBooleanStrictOrNull() ?: false
        val open = params["open"]?.toBooleanStrictOrNull() ?: true

        println("Bóveda Signet en: ${vaultDir.absolutePath}")
        if (vaultDir.exists() && list) {
            val files = vaultDir.listFiles() ?: emptyArray()
            println("Archivos en la bóveda (${files.size}):")
            files.forEach { println(" - ${it.name} (${it.length()} bytes)") }
        }

        if (open && Desktop.isDesktopSupported()) {
            Desktop.getDesktop().open(vaultDir)
        }
    }

    fun printHelp() {
        println("===================================================================")
        println("  Signet - Android Keystore Generator & APK Sovereign Signer (CLI)")
        println("===================================================================")
        println("Uso: signet <comando> [opciones]")
        println()
        println("Comandos disponibles:")
        println("  sign             Firma un archivo APK con esquemas v1, v2, v3 y zipalign")
        println("                   Opciones: --apk <path> --keystore <path> --password <pwd>")
        println("                             [--alias <alias>] [--keypass <pwd>] [--output <out.apk>]")
        println("                             [--v1 <true|false>] [--v2 <true|false>] [--v3 <true|false>] [--zipalign <true|false>]")
        println()
        println("  generate, gen    Genera un nuevo almacén de claves PKCS#12 (.jks / .keystore)")
        println("                   Opciones: --output <dir> --name <filename> --alias <alias> --password <pwd>")
        println("                             [--keypass <pwd>] [--algorithm <RSA_2048|RSA_4096|EC_P256>]")
        println("                             [--cn <name>] [--o <org>] [--ou <unit>] [--c <country>] [--validity <years>]")
        println()
        println("  inspect          Inspecciona certificados y huellas de un Keystore o APK")
        println("                   Opciones: --file <keystore_or_apk> [--password <pwd>]")
        println()
        println("  match            Compara si un APK fue firmado con un Keystore")
        println("                   Opciones: --apk <app.apk> --keystore <key.jks> [--password <pwd>]")
        println()
        println("  base64           Exporta el contenido de un Keystore a formato Base64 para CI/CD")
        println("                   Opciones: --file <key.jks>")
        println()
        println("  backup-create    Crea un respaldo ZIP firmado con HMAC-SHA256 anti-manipulación")
        println("                   Opciones: --keystore <key.jks> --password <pwd> [--output <backup.zip>]")
        println()
        println("  vault            Gestiona o abre la bóveda en %APPDATA%/Signet")
        println("                   Opciones: [--open] [--list]")
        println()
        println("  -v, --open-vault Abre la carpeta de datos en el Explorador de Windows")
        println("  -V, --version    Muestra la versión de Signet")
        println("  -h, --help       Muestra este mensaje de ayuda")
        println("===================================================================")
    }
}
