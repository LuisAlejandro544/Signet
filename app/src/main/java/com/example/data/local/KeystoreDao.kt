package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface KeystoreDao {
    @Query("SELECT * FROM keystores ORDER BY createdAt DESC")
    fun getAllKeystores(): Flow<List<KeystoreEntity>>

    @Query("SELECT * FROM keystores WHERE id = :id")
    suspend fun getKeystoreById(id: Long): KeystoreEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKeystore(keystore: KeystoreEntity): Long

    @Query("DELETE FROM keystores WHERE id = :id")
    suspend fun deleteKeystoreById(id: Long)
}
