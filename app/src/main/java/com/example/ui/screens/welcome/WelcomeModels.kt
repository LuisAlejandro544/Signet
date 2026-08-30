package com.example.ui.screens.welcome

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class OnboardingPageData(
    val title: String,
    val subtitle: String,
    val description: String,
    val icon: ImageVector,
    val iconTintBg: Color,
    val highlights: List<Triple<ImageVector, String, String>>
)

val defaultOnboardingPages = listOf(
    OnboardingPageData(
        title = "Bienvenido a Signet",
        subtitle = "Suite Criptográfica de Firma para Android",
        description = "Crea y administra almacenes de claves (.jks y .keystore) profesionales con criptografía de estándar industrial directamente en tu dispositivo móvil.",
        icon = Icons.Default.Key,
        iconTintBg = Color(0xFF10B981),
        highlights = listOf(
            Triple(
                Icons.Default.Key,
                "Algoritmos RSA & Curva Elíptica",
                "Soporte completo para RSA 2048, RSA 4096 y EC P-256 con firmas SHA-256."
            ),
            Triple(
                Icons.Default.Lock,
                "Generador CSPRNG de Contraseñas",
                "Genera contraseñas ultra seguras con SecureRandom del sistema y medidor de entropía en tiempo real."
            ),
            Triple(
                Icons.Default.CheckCircle,
                "Validez de hasta 100 Años",
                "Control deslizante de validez y presets para Google Play Release y Upload Keys."
            )
        )
    ),
    OnboardingPageData(
        title = "Respaldos & CI/CD",
        subtitle = "Respaldos ZIP Anti-Manipulación y Automatización",
        description = "Automatiza tus pipelines de desarrollo y resguarda tus claves con mecanismos de seguridad de grado empresarial.",
        icon = Icons.Default.FolderZip,
        iconTintBg = Color(0xFF3B82F6),
        highlights = listOf(
            Triple(
                Icons.Default.FolderZip,
                "Respaldos ZIP Anti-Manipulación",
                "Paquetes portables con manifiesto firmado mediante HMAC-SHA256 para prevenir alteraciones externas."
            ),
            Triple(
                Icons.Default.Code,
                "Snippets para Gradle y GitHub Actions",
                "Conversión instantánea a Base64 y plantillas para automatizar firmas en CI/CD y apksigner."
            ),
            Triple(
                Icons.Default.Shield,
                "Integración Segura con Secrets",
                "Genera variables Base64 y propiedades listas para GitHub Actions, Bitrise y Fastlane."
            )
        )
    ),
    OnboardingPageData(
        title = "Validador APK Matcher",
        subtitle = "Análisis Forense y Verificación de Coincidencia",
        description = "Evita fallos críticos de instalación (INSTALL_FAILED_UPDATE_INCOMPATIBLE) comprobando la concordancia de firmas antes de publicar.",
        icon = Icons.Default.FactCheck,
        iconTintBg = Color(0xFF8B5CF6),
        highlights = listOf(
            Triple(
                Icons.Default.FactCheck,
                "Detección Multi-Esquema",
                "Inspección y extracción de certificados X.509 en esquemas de firma v1 (JAR), v2 y v3."
            ),
            Triple(
                Icons.Default.CheckCircle,
                "Cruce de Huellas SHA-256",
                "Contrasta determinísticamente los certificados del APK contra tus Keystores guardados o externos."
            ),
            Triple(
                Icons.Default.Security,
                "Lectura de Metadatos de APK",
                "Extrae Package Name, Version Name y Version Code del paquete de forma 100% local."
            )
        )
    ),
    OnboardingPageData(
        title = "100% Privado & Seguro",
        subtitle = "Cero Recolección, Cero Telemetría, Cero Servidores",
        description = "En Signet, tus claves privadas y contraseñas nunca salen de tu teléfono. Operamos exclusivamente de forma local y offline.",
        icon = Icons.Default.VerifiedUser,
        iconTintBg = Color(0xFF059669),
        highlights = listOf(
            Triple(
                Icons.Default.PrivacyTip,
                "Arquitectura 100% On-Device",
                "Almacenamiento protegido en SQLite local (Room) bajo el sandbox seguro de Android."
            ),
            Triple(
                Icons.Default.Security,
                "Sin Analíticas ni Rastreadores",
                "Sin Google Analytics, Firebase Tracking, anuncios ni reportes remotos de errores."
            ),
            Triple(
                Icons.Default.Shield,
                "Licencia Libre GNU GPL v3",
                "Código abierto transparente y verificable por cualquier desarrollador o auditor."
            )
        )
    )
)
