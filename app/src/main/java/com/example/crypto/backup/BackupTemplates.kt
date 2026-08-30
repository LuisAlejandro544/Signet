package com.example.crypto.backup

import com.example.data.model.KeystoreDetails
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackupTemplates {

    /**
     * Builds a human-readable credentials and metadata text file for the ZIP bundle.
     */
    fun buildCredentialsText(
        details: KeystoreDetails,
        keystoreFileName: String
    ): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.ROOT)
        val validFromStr = dateFormat.format(Date(details.validFrom))
        val validUntilStr = dateFormat.format(Date(details.validUntil))

        return """
            ========================================================================
            SIGNET - CREDENCIALES Y METADATOS DE RESPALDO
            ========================================================================
            Archivo Keystore      : $keystoreFileName
            Alias de la Clave     : ${details.alias}
            Contraseña Keystore   : ${details.storePassword}
            Contraseña Clave      : ${details.keyPassword}
            Algoritmo             : ${details.algorithm}
            
            ------------------------------------------------------------------------
            HUELLAS DIGITALES DEL CERTIFICADO (FINGERPRINTS)
            ------------------------------------------------------------------------
            SHA-256 : ${details.sha256Fingerprint}
            SHA-1   : ${details.sha1Fingerprint}
            MD5     : ${details.md5Fingerprint}
            
            ------------------------------------------------------------------------
            VALIDEZ Y PROPIETARIO
            ------------------------------------------------------------------------
            Válido Desde : $validFromStr
            Válido Hasta : $validUntilStr
            Propietario  : ${details.subjectDn}
            Emisor       : ${details.issuerDn}
            Nº Serie     : ${details.serialNumber}
            
            ========================================================================
            INSTRUCCIONES DE RESTAURACIÓN:
            Este archivo ZIP puede restaurarse directamente en Signet (Android / PC)
            utilizando la función 'Restaurar Respaldo (.zip)'.
            
            ADVERTENCIA DE SEGURIDAD:
            Este archivo contiene credenciales sensibles. Guárdalo en un gestor seguro
            de contraseñas (KeePass, Bitwarden, 1Password) y no lo compartas públicamente.
            ========================================================================
        """.trimIndent()
    }

    /**
     * Builds a standard key.properties file for Gradle & Flutter builds.
     */
    fun buildKeyProperties(details: KeystoreDetails, keystoreFileName: String): String {
        return """
            # Generado automáticamente por Signet (Keystore Manager)
            # Coloca este archivo en la raíz o en la carpeta android/ de tu proyecto
            storePassword=${details.storePassword}
            keyPassword=${details.keyPassword}
            keyAlias=${details.alias}
            storeFile=../keystores/$keystoreFileName
        """.trimIndent()
    }

    /**
     * Builds a README-BACKUP.txt instruction file.
     */
    fun buildReadmeBackup(
        details: KeystoreDetails,
        keystoreFileName: String
    ): String {
        return """
            ========================================================================
            PAQUETE DE RESPALDO DE FIRMA ANDROID - SIGNET
            ========================================================================
            
            Contenido de este paquete:
            1. $keystoreFileName -> Archivo binario PKCS12 de firma Android.
            2. signet-backup.json -> Manifiesto firmado criptográficamente para restauración instantánea en Signet.
            3. credentials.txt -> Resumen legible con claves y huellas SHA-256.
            4. key.properties -> Archivo de configuración listo para Gradle / Flutter.
            5. base64.txt -> Llave codificada en Base64 para GitHub Actions / CI/CD secrets.
            
            ¿Cómo restaurar este respaldo si reinstalas la app o cambias de dispositivo?
            - Abre Signet.
            - Ve a la pestaña 'Mis Keystores' o 'Inspeccionar'.
            - Presiona 'Restaurar Respaldo (.zip)' y selecciona este archivo ZIP.
            - ¡Listo! Todo tu keystore, huellas y credenciales quedarán cargados.
            
            Nota: El archivo signet-backup.json cuenta con una firma anti-manipulación.
            Si el archivo es modificado manualmente, Signet rechazará la importación por seguridad.
        """.trimIndent()
    }
}
