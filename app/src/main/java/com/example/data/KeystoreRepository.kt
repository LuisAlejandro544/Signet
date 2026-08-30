package com.example.data

import android.content.Context
import android.util.Base64
import com.example.crypto.KeystoreGenerator
import com.example.crypto.SignetBackupManager
import com.example.data.local.AppDatabase
import com.example.data.local.KeystoreEntity
import com.example.data.model.KeystoreConfig
import com.example.data.model.KeystoreDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream

class KeystoreRepository(private val database: AppDatabase) {

    val allKeystores: Flow<List<KeystoreDetails>> = database.keystoreDao()
        .getAllKeystores()
        .map { list -> list.map { it.toDetails() } }

    suspend fun generateAndSaveKeystore(
        context: Context,
        config: KeystoreConfig
    ): Result<KeystoreDetails> = withContext(Dispatchers.IO) {
        try {
            val details = KeystoreGenerator.generateKeystore(context, config)
            val entity = KeystoreEntity.fromDetails(details)
            val id = database.keystoreDao().insertKeystore(entity)
            Result.success(details.copy(id = id))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreAndSaveKeystoreFromZip(
        context: Context,
        inputStream: InputStream
    ): Result<KeystoreDetails> = restoreAndSaveKeystoreFromZip(context, inputStream.readBytes())

    suspend fun restoreAndSaveKeystoreFromZip(
        context: Context,
        zipBytes: ByteArray
    ): Result<KeystoreDetails> = withContext(Dispatchers.IO) {
        try {
            val restoredDetails = SignetBackupManager.restoreFromZip(context, zipBytes)
            val entity = KeystoreEntity.fromDetails(restoredDetails)
            val id = database.keystoreDao().insertKeystore(entity)
            Result.success(restoredDetails.copy(id = id))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreAndSaveAnyFromZip(
        context: Context,
        zipBytes: ByteArray
    ): Result<List<KeystoreDetails>> = withContext(Dispatchers.IO) {
        try {
            val list = SignetBackupManager.restoreAnyFromZip(context, zipBytes)
            val persistedList = list.map { details ->
                val entity = KeystoreEntity.fromDetails(details)
                val id = database.keystoreDao().insertKeystore(entity)
                details.copy(id = id)
            }
            Result.success(persistedList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createVaultBackupZip(keystores: List<KeystoreDetails>): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
            val items = keystores.map { details ->
                val bytes = if (details.filePath.isNotBlank() && File(details.filePath).exists()) {
                    File(details.filePath).readBytes()
                } else if (details.base64Content.isNotBlank()) {
                    Base64.decode(details.base64Content, Base64.DEFAULT)
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
                Base64.decode(details.base64Content, Base64.DEFAULT)
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
        database.keystoreDao().deleteKeystoreById(id)
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

