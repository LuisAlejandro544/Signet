package com.example.data

import com.example.data.local.AppDatabase
import com.example.data.local.KeystoreDao
import com.example.data.local.KeystoreEntity
import com.example.data.model.KeystoreDetails
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Implementación de Room para Android.
 */
class RoomKeystoreDataSource(private val dao: KeystoreDao) : KeystoreDataSource {
    override fun getAllKeystores(): Flow<List<KeystoreDetails>> {
        return dao.getAllKeystores().map { list -> list.map { it.toDetails() } }
    }

    override suspend fun getKeystoreById(id: Long): KeystoreDetails? {
        return dao.getKeystoreById(id)?.toDetails()
    }

    override suspend fun insertKeystore(details: KeystoreDetails): Long {
        val entity = KeystoreEntity.fromDetails(details)
        return dao.insertKeystore(entity)
    }

    override suspend fun deleteKeystoreById(id: Long) {
        dao.deleteKeystoreById(id)
    }
}

/**
 * Factory extension para inicializar KeystoreRepository desde RoomDatabase (exclusivo de Android).
 */
fun createAndroidKeystoreRepository(database: AppDatabase): KeystoreRepository {
    return KeystoreRepository(RoomKeystoreDataSource(database.keystoreDao()))
}

