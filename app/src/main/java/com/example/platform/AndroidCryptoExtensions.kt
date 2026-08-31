package com.example.crypto

import android.content.Context
import com.example.data.model.ApkInfo
import com.example.data.model.ApkSigningOptions
import com.example.data.model.ApkSigningResult
import com.example.data.model.KeystoreConfig
import com.example.data.model.KeystoreDetails
import java.io.File
import java.io.InputStream

/**
 * Extensiones y sobrecargas compatibles con Android (usando Context e InputStream)
 * aisladas para que el módulo desktop compile de forma 100% pura.
 */

fun KeystoreGenerator.generateKeystore(
    context: Context,
    config: KeystoreConfig,
    saveToFile: Boolean = true
): KeystoreDetails = generateKeystore(context.filesDir, config, saveToFile)

fun SignetBackupManager.restoreFromZip(
    context: Context,
    inputStream: InputStream
): KeystoreDetails = restoreFromZip(context.filesDir, inputStream.readBytes())

fun SignetBackupManager.restoreFromZip(
    context: Context,
    zipBytes: ByteArray
): KeystoreDetails = restoreFromZip(context.filesDir, zipBytes)

fun SignetBackupManager.restoreVaultFromZip(
    context: Context,
    zipBytes: ByteArray
): List<KeystoreDetails> = restoreVaultFromZip(context.filesDir, zipBytes)

fun SignetBackupManager.restoreVaultFromZip(
    context: Context,
    inputStream: InputStream
): List<KeystoreDetails> = restoreVaultFromZip(context.filesDir, inputStream.readBytes())

fun SignetBackupManager.restoreAnyFromZip(
    context: Context,
    zipBytes: ByteArray
): List<KeystoreDetails> = restoreAnyFromZip(context.filesDir, zipBytes)

fun SignetBackupManager.restoreAnyFromZip(
    context: Context,
    inputStream: InputStream
): List<KeystoreDetails> = restoreAnyFromZip(context.filesDir, inputStream.readBytes())

fun ApkMatcher.analyzeApk(context: Context, apkBytes: ByteArray, fileName: String = "app.apk"): ApkInfo =
    analyzeApk(apkBytes, fileName)

fun com.example.crypto.signer.ApkSigner.signApk(
    context: Context,
    apkBytes: ByteArray,
    keystoreBytes: ByteArray,
    storePassword: String,
    alias: String,
    keyPassword: String = storePassword,
    options: ApkSigningOptions = ApkSigningOptions()
): ApkSigningResult = signApk(
    apkBytes = apkBytes,
    keystoreBytes = keystoreBytes,
    storePassword = storePassword,
    alias = alias,
    keyPassword = keyPassword,
    options = options,
    outputDirectory = context.filesDir
)
