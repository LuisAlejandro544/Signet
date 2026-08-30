package com.example.data

import com.example.crypto.DesktopStorageUtils
import com.example.data.local.KeystoreDao
import com.example.data.local.KeystoreEntity
import com.example.data.model.KeystoreDetails
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Fuente de datos desacoplada para persistencia de Keystores.
 * Permite alternar de forma transparente entre Room (Android) y almacenamiento
 * persistente de escritorio en Windows (%APPDATA%/Signet/vault_index.json).
 */
interface KeystoreDataSource {
    fun getAllKeystores(): Flow<List<KeystoreDetails>>
    suspend fun getKeystoreById(id: Long): KeystoreDetails?
    suspend fun insertKeystore(details: KeystoreDetails): Long
    suspend fun deleteKeystoreById(id: Long)
}

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
 * Implementación para Windows / Desktop que guarda el índice en %APPDATA%/Signet/vault_index.json.
 */
class DesktopKeystoreDataSource(
    private val dataDir: File = DesktopStorageUtils.getDesktopDataDir(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : KeystoreDataSource {

    private val indexFile = File(dataDir, "vault_index.json")
    private val mutex = Mutex()
    private val _keystoresFlow = MutableStateFlow<List<KeystoreDetails>>(emptyList())

    init {
        loadFromDisk()
    }

    private fun loadFromDisk() {
        if (!indexFile.exists()) {
            _keystoresFlow.value = emptyList()
            return
        }
        try {
            val content = indexFile.readText(Charsets.UTF_8)
            val jsonArray = JSONArray(content)
            val list = mutableListOf<KeystoreDetails>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(jsonToDetails(obj))
            }
            _keystoresFlow.value = list.sortedByDescending { it.createdAt }
        } catch (_: Exception) {
            _keystoresFlow.value = emptyList()
        }
    }

    private fun saveToDisk(list: List<KeystoreDetails>) {
        try {
            dataDir.mkdirs()
            val jsonArray = JSONArray()
            list.forEach { details ->
                jsonArray.put(detailsToJson(details))
            }
            val tempFile = File(dataDir, "vault_index.json.tmp")
            tempFile.writeText(jsonArray.toString(2), Charsets.UTF_8)
            if (tempFile.renameTo(indexFile) || (indexFile.delete() && tempFile.renameTo(indexFile))) {
                // Escrito exitosamente
            } else {
                indexFile.writeText(jsonArray.toString(2), Charsets.UTF_8)
            }
        } catch (_: Exception) {
            // Ignorar errores de I/O
        }
    }

    override fun getAllKeystores(): Flow<List<KeystoreDetails>> = _keystoresFlow.asStateFlow()

    override suspend fun getKeystoreById(id: Long): KeystoreDetails? = withContext(dispatcher) {
        mutex.withLock {
            _keystoresFlow.value.firstOrNull { it.id == id }
        }
    }

    override suspend fun insertKeystore(details: KeystoreDetails): Long = withContext(dispatcher) {
        mutex.withLock {
            val current = _keystoresFlow.value.toMutableList()
            val assignedId = if (details.id > 0) details.id else (current.maxOfOrNull { it.id } ?: 0L) + 1L
            val newDetails = details.copy(id = assignedId)

            val existingIndex = current.indexOfFirst { it.id == assignedId }
            if (existingIndex >= 0) {
                current[existingIndex] = newDetails
            } else {
                current.add(newDetails)
            }

            val sorted = current.sortedByDescending { it.createdAt }
            _keystoresFlow.value = sorted
            saveToDisk(sorted)
            assignedId
        }
    }

    override suspend fun deleteKeystoreById(id: Long) = withContext(dispatcher) {
        mutex.withLock {
            val current = _keystoresFlow.value.toMutableList()
            val removed = current.removeAll { it.id == id }
            if (removed) {
                _keystoresFlow.value = current
                saveToDisk(current)
            }
        }
    }

    private fun detailsToJson(d: KeystoreDetails): JSONObject {
        return JSONObject().apply {
            put("id", d.id)
            put("fileName", d.fileName)
            put("alias", d.alias)
            put("filePath", d.filePath)
            put("fileSizeBytes", d.fileSizeBytes)
            put("storePassword", d.storePassword)
            put("keyPassword", d.keyPassword)
            put("base64Content", d.base64Content)
            put("sha256Fingerprint", d.sha256Fingerprint)
            put("sha1Fingerprint", d.sha1Fingerprint)
            put("md5Fingerprint", d.md5Fingerprint)
            put("validFrom", d.validFrom)
            put("validUntil", d.validUntil)
            put("algorithm", d.algorithm)
            put("subjectDn", d.subjectDn)
            put("issuerDn", d.issuerDn)
            put("serialNumber", d.serialNumber)
            put("certificatePem", d.certificatePem)
            put("createdAt", d.createdAt)
        }
    }

    private fun jsonToDetails(obj: JSONObject): KeystoreDetails {
        return KeystoreDetails(
            id = obj.optLong("id", 0L),
            fileName = obj.optString("fileName", ""),
            alias = obj.optString("alias", ""),
            filePath = obj.optString("filePath", ""),
            fileSizeBytes = obj.optLong("fileSizeBytes", 0L),
            storePassword = obj.optString("storePassword", ""),
            keyPassword = obj.optString("keyPassword", ""),
            base64Content = obj.optString("base64Content", ""),
            sha256Fingerprint = obj.optString("sha256Fingerprint", ""),
            sha1Fingerprint = obj.optString("sha1Fingerprint", ""),
            md5Fingerprint = obj.optString("md5Fingerprint", ""),
            validFrom = obj.optLong("validFrom", 0L),
            validUntil = obj.optLong("validUntil", 0L),
            algorithm = obj.optString("algorithm", ""),
            subjectDn = obj.optString("subjectDn", ""),
            issuerDn = obj.optString("issuerDn", ""),
            serialNumber = obj.optString("serialNumber", ""),
            certificatePem = obj.optString("certificatePem", ""),
            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
        )
    }
}
