package com.example.update

import com.example.ui.res.SignetStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class ReleaseAsset(
    val name: String,
    val downloadUrl: String,
    val sizeBytes: Long,
    val contentType: String
)

data class AppReleaseInfo(
    val tagName: String,
    val versionDisplay: String,
    val title: String,
    val changelog: String,
    val publishedAt: String,
    val isPrerelease: Boolean,
    val releaseUrl: String,
    val assets: List<ReleaseAsset>,
    val matchedAsset: ReleaseAsset?
)

sealed interface UpdateUiState {
    object Idle : UpdateUiState
    object Checking : UpdateUiState
    data class Available(
        val release: AppReleaseInfo,
        val isDownloading: Boolean = false,
        val progressPercent: Int = 0,
        val downloadedBytes: Long = 0L,
        val totalBytes: Long = 0L,
        val downloadedFile: File? = null,
        val errorMessage: String? = null
    ) : UpdateUiState
    data class UpToDate(val currentVersion: String) : UpdateUiState
    data class Error(val message: String) : UpdateUiState
}

object AppUpdateManager {
    private const val GITHUB_REPO_API = "https://api.github.com/repos/LuisAlejandro544/Signet/releases"
    const val GITHUB_RELEASES_PAGE = "https://github.com/LuisAlejandro544/Signet/releases"

    /**
     * Consulta las versiones publicadas en GitHub Releases (incluyendo Pre-Releases / Beta)
     * y determina si existe una actualización disponible para la plataforma actual.
     */
    suspend fun checkLatestRelease(
        currentVersion: String = SignetStrings.APP_VERSION,
        isDesktop: Boolean = false
    ): AppReleaseInfo? = withContext(Dispatchers.IO) {
        try {
            val url = URL(GITHUB_REPO_API)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "Signet-App-Updater")
                connectTimeout = 12000
                readTimeout = 15000
            }

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext null
            }

            val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(responseBody)
            if (jsonArray.length() == 0) return@withContext null

            // Recorrer las releases ordenadas por GitHub (la primera suele ser la más reciente)
            for (i in 0 until jsonArray.length()) {
                val releaseObj = jsonArray.getJSONObject(i)
                val tagName = releaseObj.optString("tag_name", "")
                val isDraft = releaseObj.optBoolean("draft", false)
                if (isDraft || tagName.isBlank()) continue

                val isPrerelease = releaseObj.optBoolean("prerelease", false)
                val name = releaseObj.optString("name", tagName)
                val body = releaseObj.optString("body", "Sin notas de versión disponibles.")
                val publishedAt = releaseObj.optString("published_at", "")
                val htmlUrl = releaseObj.optString("html_url", GITHUB_RELEASES_PAGE)

                val assetsJson = releaseObj.optJSONArray("assets") ?: JSONArray()
                val assetsList = mutableListOf<ReleaseAsset>()
                for (j in 0 until assetsJson.length()) {
                    val assetObj = assetsJson.getJSONObject(j)
                    val assetName = assetObj.optString("name", "")
                    val downloadUrl = assetObj.optString("browser_download_url", "")
                    val size = assetObj.optLong("size", 0L)
                    val contentType = assetObj.optString("content_type", "")
                    if (assetName.isNotBlank() && downloadUrl.isNotBlank()) {
                        assetsList.add(ReleaseAsset(assetName, downloadUrl, size, contentType))
                    }
                }

                val matchedAsset = findBestAssetForPlatform(assetsList, isDesktop)

                if (isNewerVersion(currentVersion, tagName)) {
                    return@withContext AppReleaseInfo(
                        tagName = tagName,
                        versionDisplay = cleanVersion(tagName),
                        title = name,
                        changelog = cleanChangelog(body),
                        publishedAt = publishedAt,
                        isPrerelease = isPrerelease,
                        releaseUrl = htmlUrl,
                        assets = assetsList,
                        matchedAsset = matchedAsset
                    )
                }
            }
            null
        } catch (e: Exception) {
            println("[AppUpdateManager] Error al verificar actualizaciones: ${e.message}")
            null
        }
    }

    /**
     * Selecciona el artefacto correspondiente para Android (.apk) o Windows (.exe / .msi / .zip).
     */
    fun findBestAssetForPlatform(assets: List<ReleaseAsset>, isDesktop: Boolean): ReleaseAsset? {
        if (assets.isEmpty()) return null

        return if (!isDesktop) {
            // Android: Priorizar APK firmado de release
            assets.firstOrNull { it.name.endsWith("-release-signed.apk", ignoreCase = true) }
                ?: assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
        } else {
            // Windows / Desktop PC: Priorizar instalador .exe, luego .msi, luego .zip portable, luego .jar
            assets.firstOrNull { it.name.endsWith(".exe", ignoreCase = true) }
                ?: assets.firstOrNull { it.name.endsWith(".msi", ignoreCase = true) }
                ?: assets.firstOrNull { it.name.endsWith(".zip", ignoreCase = true) }
                ?: assets.firstOrNull { it.name.endsWith(".jar", ignoreCase = true) }
        }
    }

    /**
     * Compara versiones semánticas y etiquetas de lanzamiento.
     * Soporta formatos: 1.0.0, v1.0.0-B, 1.0.1-B, 1.0.0.dev, 1.0.0-D, 1.0.0-E.
     */
    fun isNewerVersion(currentVersion: String, remoteTag: String): Boolean {
        val currClean = cleanVersion(currentVersion)
        val remClean = cleanVersion(remoteTag)

        val currParts = parseVersionNumbers(currClean)
        val remParts = parseVersionNumbers(remClean)

        for (i in 0 until maxOf(currParts.size, remParts.size)) {
            val c = currParts.getOrElse(i) { 0 }
            val r = remParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }

        // Si los números base son iguales, comparar sufijos o modificadores de canal
        val currNormalized = currentVersion.trim().lowercase().removePrefix("v")
        val remNormalized = remoteTag.trim().lowercase().removePrefix("v")

        if (currNormalized != remNormalized) {
            // Si el remoto es versión final y la actual es beta/dev, el remoto es más nuevo
            val currIsPre = currNormalized.contains("-b") || currNormalized.contains(".dev") || currNormalized.contains("-d")
            val remIsStable = !remNormalized.contains("-b") && !remNormalized.contains(".dev") && !remNormalized.contains("-d")
            if (currIsPre && remIsStable) return true

            // Comparar subversiones numéricas dentro de tags beta (ej: 1.0.0-B.2 vs 1.0.0-B.1)
            val currSub = extractSubNumber(currNormalized)
            val remSub = extractSubNumber(remNormalized)
            if (remSub > currSub) return true
        }

        return false
    }

    private fun parseVersionNumbers(version: String): List<Int> {
        return version.split(".", "-", "_")
            .mapNotNull { it.trim().toIntOrNull() }
    }

    private fun extractSubNumber(tag: String): Int {
        val match = Regex("""\d+""").findAll(tag).lastOrNull()
        return match?.value?.toIntOrNull() ?: 0
    }

    fun cleanVersion(tag: String): String {
        return tag.trim()
            .removePrefix("v")
            .removePrefix("V")
    }

    private fun cleanChangelog(body: String): String {
        return body.lines()
            .filterNot { it.contains("🔐 Verificación Criptográfica", ignoreCase = true) || it.contains("SHA-256:", ignoreCase = true) }
            .joinToString("\n")
            .trim()
    }

    /**
     * Descarga el archivo de actualización en el directorio destino con seguimiento del progreso en tiempo real.
     */
    suspend fun downloadUpdate(
        downloadUrl: String,
        targetFile: File,
        onProgress: (bytesDownloaded: Long, totalBytes: Long, percent: Int) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            targetFile.parentFile?.mkdirs()
            if (targetFile.exists()) {
                targetFile.delete()
            }

            var currentUrl = downloadUrl
            var connection: HttpURLConnection
            var redirects = 0
            val maxRedirects = 5

            // Manejo de redirecciones de GitHub CDN
            while (true) {
                val url = URL(currentUrl)
                connection = url.openConnection() as HttpURLConnection
                connection.instanceFollowRedirects = false
                connection.setRequestProperty("User-Agent", "Signet-App-Updater")
                connection.connectTimeout = 15000
                connection.readTimeout = 30000

                val status = connection.responseCode
                if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM || status == 307 || status == 308) {
                    val newUrl = connection.getHeaderField("Location")
                    if (newUrl != null && redirects < maxRedirects) {
                        currentUrl = newUrl
                        redirects++
                        continue
                    }
                }
                break
            }

            if (connection.responseCode !in 200..299) {
                return@withContext Result.failure(Exception("Error HTTP del servidor: ${connection.responseCode}"))
            }

            val totalBytes = connection.contentLengthLong
            connection.inputStream.use { input ->
                FileOutputStream(targetFile).use { output ->
                    val buffer = ByteArray(32 * 1024)
                    var bytesRead: Int
                    var totalRead = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead

                        val percent = if (totalBytes > 0) {
                            ((totalRead * 100) / totalBytes).toInt().coerceIn(0, 100)
                        } else {
                            -1
                        }
                        onProgress(totalRead, totalBytes, percent)
                    }
                    output.flush()
                }
            }

            if (targetFile.exists() && targetFile.length() > 0) {
                Result.success(targetFile)
            } else {
                Result.failure(Exception("El archivo descargado está vacío."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
