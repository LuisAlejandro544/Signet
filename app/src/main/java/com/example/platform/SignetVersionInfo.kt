package com.example.platform

import com.example.BuildConfig

/**
 * Representa los canales de lanzamiento oficiales de Signet,
 * sus identificadores de paquete, sufijos de versión e información descriptiva.
 */
enum class SignetChannel(
    val channelKey: String,
    val displayName: String,
    val tagSuffix: String,
    val packageSuffix: String,
    val badgeLabel: String,
    val description: String
) {
    DEBUG(
        channelKey = "debug",
        displayName = "Depuración Interna",
        tagSuffix = "-D",
        packageSuffix = ".debug",
        badgeLabel = "DEBUG (-D)",
        description = "Canal exclusivo interno de pruebas automáticas en CI y GitHub Actions."
    ),
    PRE_ALPHA(
        channelKey = "dev",
        displayName = "Pre-Alpha / Desarrollo",
        tagSuffix = "-dev",
        packageSuffix = ".dev",
        badgeLabel = "PRE-ALPHA (-dev)",
        description = "Canal pre-alpha para pruebas tempranas de nuevas funciones criptográficas."
    ),
    BETA(
        channelKey = "beta",
        displayName = "Beta Comunitaria",
        tagSuffix = "-B",
        packageSuffix = ".beta",
        badgeLabel = "BETA (-B)",
        description = "Canal beta con herramientas pulidas candidatas a definitivas."
    ),
    STABLE(
        channelKey = "estable",
        displayName = "Estable (Producción)",
        tagSuffix = "-E",
        packageSuffix = "",
        badgeLabel = "ESTABLE (-E)",
        description = "Canal definitivo de producción optimizado para Uptodown y GitHub Releases."
    );

    companion object {
        fun resolve(versionName: String, applicationId: String): SignetChannel {
            val v = versionName.lowercase()
            val id = applicationId.lowercase()
            return when {
                v.endsWith("-d") || id.endsWith(".debug") -> DEBUG
                v.contains("dev") || v.endsWith("-a") || id.endsWith(".dev") -> PRE_ALPHA
                v.endsWith("-b") || id.endsWith(".beta") -> BETA
                v.endsWith("-e") || (!id.contains("debug") && !id.contains("dev") && !id.contains("beta")) -> STABLE
                else -> PRE_ALPHA
            }
        }
    }
}

/**
 * Proveedor de metadatos y versionado en tiempo de ejecución para Signet.
 */
object SignetVersionInfo {
    val versionName: String get() = try {
        BuildConfig.VERSION_NAME
    } catch (e: Throwable) {
        "1.0.0-dev"
    }

    val applicationId: String get() = try {
        BuildConfig.APPLICATION_ID
    } catch (e: Throwable) {
        "com.signet.app.dev"
    }

    val versionCode: Int get() = try {
        BuildConfig.VERSION_CODE
    } catch (e: Throwable) {
        1
    }

    val currentChannel: SignetChannel get() = SignetChannel.resolve(versionName, applicationId)

    val formattedVersion: String get() = "v$versionName"

    val fullVersionDisplay: String get() = "Signet v$versionName (${currentChannel.displayName})"

    val tagReference: String get() = "v$versionName"
}
