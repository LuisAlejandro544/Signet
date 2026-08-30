package com.example.data

import android.content.Context
import com.example.crypto.Base64Compat
import com.example.crypto.DesktopStorageUtils
import com.example.crypto.KeystoreGenerator
import com.example.crypto.SignetBackupManager
import com.example.data.local.AppDatabase
import com.example.data.model.KeystoreConfig
import com.example.data.model.KeystoreDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream

class KeystoreRepository(private val dataSource: KeystoreDataSource) {

    constructor(database: AppDatabase) : this(RoomKeystoreDataSource(database.keystoreDao()))

    constructor(dataDir: File = DesktopStorageUtils.getDesktopDataDir()) : this(DesktopKeystoreDataSource(dataDir))

    val allKeystores: Flow<List<KeystoreDetails>> = dataSource.getAllKeystores()

    suspend fun generateAndSaveKeystore(
        outputDir: File,
        config: KeystoreConfig
    ): Result<KeystoreDetails> = generateKeystore(outputDir, config, saveToDatabase = true)

    suspend fun generateAndSaveKeystore(
        context: Context,
        config: KeystoreConfig
    ): Result<KeystoreDetails> = generateKeystore(context.filesDir, config, saveToDatabase = true)

    suspend fun generateKeystore(
        context: Context,
        config: KeystoreConfig,
        saveToDatabase: Boolean = true
    ): Result<KeystoreDetails> = generateKeystore(context.filesDir, config, saveToDatabase)

    suspend fun generateKeystore(
        outputDir: File,
        config: KeystoreConfig,
        saveToDatabase: Boolean = true
    ): Result<KeystoreDetails> = withContext(Dispatchers.IO) {
        try {
            val details = KeystoreGenerator.generateKeystore(outputDir, config, saveToFile = saveToDatabase)
            if (saveToDatabase) {
                val id = dataSource.insertKeystore(details)
                Result.success(details.copy(id = id))
            } else {
                // Ephemeral zero-footprint mode: not saved in database or persistent files
                Result.success(details.copy(id = 0L))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreAndSaveKeystoreFromZip(
        outputDir: File,
        zipBytes: ByteArray
    ): Result<KeystoreDetails> = withContext(Dispatchers.IO) {
        try {
            val restoredDetails = SignetBackupManager.restoreFromZip(outputDir, zipBytes)
            val id = dataSource.insertKeystore(restoredDetails)
            Result.success(restoredDetails.copy(id = id))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreAndSaveKeystoreFromZip(
        context: Context,
        inputStream: InputStream
    ): Result<KeystoreDetails> = restoreAndSaveKeystoreFromZip(context.filesDir, inputStream.readBytes())

    suspend fun restoreAndSaveKeystoreFromZip(
        context: Context,
        zipBytes: ByteArray
    ): Result<KeystoreDetails> = restoreAndSaveKeystoreFromZip(context.filesDir, zipBytes)

    suspend fun restoreAndSaveAnyFromZip(
        outputDir: File,
        zipBytes: ByteArray
    ): Result<List<KeystoreDetails>> = withContext(Dispatchers.IO) {
        try {
            val list = SignetBackupManager.restoreAnyFromZip(outputDir, zipBytes)
            val persistedList = list.map { details ->
                val id = dataSource.insertKeystore(details)
                details.copy(id = id)
            }
            Result.success(persistedList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreAndSaveAnyFromZip(
        context: Context,
        zipBytes: ByteArray
    ): Result<List<KeystoreDetails>> = restoreAndSaveAnyFromZip(context.filesDir, zipBytes)

    suspend fun createVaultBackupZip(keystores: List<KeystoreDetails>): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
            val items = keystores.map { details ->
                val bytes = if (details.filePath.isNotBlank() && File(details.filePath).exists()) {
                    File(details.filePath).readBytes()
                } else if (details.base64Content.isNotBlank()) {
                    Base64Compat.decode(details.base64Content)
                } else {
                    throw IllegalStateException("No se encontraron los datos binarios del keystore '${details.fileName}'.")
                }
                Pair(details, bytes)
            }
            val zipBytes = SignetBackupManager.createVaultBackupZip(items)
            Result.success(zipBytes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createBackupZip(details: KeystoreDetails): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
            val bytes = if (details.filePath.isNotBlank() && File(details.filePath).exists()) {
                File(details.filePath).readBytes()
            } else if (details.base64Content.isNotBlank()) {
                Base64Compat.decode(details.base64Content)
            } else {
                throw IllegalStateException("No se encontraron los datos binarios del keystore para exportar.")
            }
            val zipBytes = SignetBackupManager.createBackupZip(details, bytes)
            Result.success(zipBytes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteKeystore(id: Long, filePath: String) = withContext(Dispatchers.IO) {
        try {
            if (filePath.isNotBlank()) {
                val file = File(filePath)
                if (file.exists()) {
                    file.delete()
                }
            }
        } catch (_: Exception) {
            // Ignore file deletion errors if already removed
        }
        dataSource.deleteKeystoreById(id)
    }

    suspend fun inspectKeystore(inputStream: java.io.InputStream, password: String): Result<List<KeystoreDetails>> =
        inspectKeystore(inputStream.readBytes(), password)

    suspend fun inspectKeystore(bytes: ByteArray, password: String): Result<List<KeystoreDetails>> =
        withContext(Dispatchers.IO) {
            try {
                val list = KeystoreGenerator.inspectKeystore(bytes, password)
                Result.success(list)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}

