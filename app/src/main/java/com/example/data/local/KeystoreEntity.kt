package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.KeystoreDetails

@Entity(tableName = "keystores")
data class KeystoreEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fileName: String,
    val alias: String,
    val filePath: String,
    val fileSizeBytes: Long,
    val storePassword: String = "",
    val keyPassword: String = "",
    val base64Content: String = "",
    val sha256Fingerprint: String,
    val sha1Fingerprint: String,
    val md5Fingerprint: String,
    val validFrom: Long,
    val validUntil: Long,
    val algorithm: String,
    val subjectDn: String,
    val issuerDn: String,
    val serialNumber: String,
    val certificatePem: String,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDetails(): KeystoreDetails = KeystoreDetails(
        id = id,
        fileName = fileName,
        alias = alias,
        filePath = filePath,
        fileSizeBytes = fileSizeBytes,
        storePassword = storePassword,
        keyPassword = keyPassword,
        base64Content = base64Content,
        sha256Fingerprint = sha256Fingerprint,
        sha1Fingerprint = sha1Fingerprint,
        md5Fingerprint = md5Fingerprint,
        validFrom = validFrom,
        validUntil = validUntil,
        algorithm = algorithm,
        subjectDn = subjectDn,
        issuerDn = issuerDn,
        serialNumber = serialNumber,
        certificatePem = certificatePem,
        createdAt = createdAt
    )

    companion object {
        fun fromDetails(details: KeystoreDetails): KeystoreEntity = KeystoreEntity(
            id = details.id,
            fileName = details.fileName,
            alias = details.alias,
            filePath = details.filePath,
            fileSizeBytes = details.fileSizeBytes,
            storePassword = details.storePassword,
            keyPassword = details.keyPassword,
            base64Content = details.base64Content,
            sha256Fingerprint = details.sha256Fingerprint,
            sha1Fingerprint = details.sha1Fingerprint,
            md5Fingerprint = details.md5Fingerprint,
            validFrom = details.validFrom,
            validUntil = details.validUntil,
            algorithm = details.algorithm,
            subjectDn = details.subjectDn,
            issuerDn = details.issuerDn,
            serialNumber = details.serialNumber,
            certificatePem = details.certificatePem,
            createdAt = details.createdAt
        )
    }
}
