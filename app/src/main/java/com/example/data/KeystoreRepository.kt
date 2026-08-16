package com.example.data

import android.content.Context
import com.example.crypto.KeystoreGenerator
import com.example.data.local.AppDatabase
import com.example.data.local.KeystoreEntity
import com.example.data.model.KeystoreConfig
import com.example.data.model.KeystoreDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

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
        withContext(Dispatchers.IO) {
            try {
                val list = KeystoreGenerator.inspectKeystore(inputStream, password)
                Result.success(list)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
