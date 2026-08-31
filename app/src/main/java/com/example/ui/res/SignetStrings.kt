package com.example.ui.res

/**
 * Catálogo centralizado y unificado de cadenas de texto, etiquetas y recursos de UI para Signet.
 * Garantiza total independencia de `android.R` y `stringResource`, permitiendo que la interfaz
 * Jetpack Compose / Compose Multiplatform se ejecute idénticamente en Android y Windows Desktop.
 */
object SignetStrings {
    // Aplicación y metadatos
    const val APP_NAME = "Signet"
    const val APP_TAGLINE = "Android Keystore Generator & APK Signer"
    const val APP_VERSION = "1.0.0-dev"

    // Navegación principal
    const val TAB_GENERATE = "Generar"
    const val TAB_VAULT = "Mis Keystores"
    const val TAB_INSPECT = "Inspeccionar"
    const val TAB_SIGN = "Firmar APK"
    const val TAB_SETTINGS = "Ajustes"

    // Pantalla de Generación
    const val GEN_TITLE = "Generador de Keystores"
    const val GEN_SUBTITLE = "Crea claves criptográficas estándar (.jks / .keystore / .pfx / .p12) para firmar tus APKs de Android, ejecutables y distribuirlas en Uptodown, web o tiendas."
    const val GEN_SUBMIT_BUTTON = "Generar Archivo Keystore"
    const val GEN_PROGRESS_TEXT = "Generando Claves y Certificado..."
    const val GEN_PRESETS_TITLE = "Plantillas Rápidas"
    const val GEN_EPHEMERAL_TITLE = "Modo Efímero / Sin Rastro (RAM)"
    const val GEN_EPHEMERAL_DESC = "Genera el almacén 100% en memoria volátil sin guardarlo en disco ni en base de datos local."

    // Pantalla de Firma de APKs
    const val SIGN_TITLE = "Firmar APK con Signet"
    const val SIGN_SUBTITLE = "Firma cualquier APK usando tus Keystores guardados o un archivo externo (.jks, .p12, .keystore) con soporte multi-esquema v1 + v2 + v3 y optimización Zipalign."
    const val SIGN_ACTION_BUTTON = "Firmar y Optimizar APK"
    const val SIGN_IN_PROGRESS = "Firmando APK..."
    const val SIGN_SUCCESS_TITLE = "¡APK Firmado y Optimizado con Éxito!"

    // Pantalla de Inspección
    const val INSPECT_TAB_KEYSTORE = "Inspeccionar Keystore"
    const val INSPECT_TAB_MATCHER = "APK vs Keystore"

    // Pantalla de Ajustes
    const val SETTINGS_TITLE = "Ajustes y Personalización"
    const val SETTINGS_THEME_TITLE = "Tema y Modo de Visualización"
    const val SETTINGS_PALETTE_TITLE = "Paleta Dinámica de Color"
    const val SETTINGS_LEGAL_TITLE = "Marco Legal y Políticas de Uso"
    const val SETTINGS_DISTRIBUTION_TITLE = "Distribución y Canales de Lanzamiento"

    // Textos Legales y Enlaces
    const val LEGAL_TERMS_TITLE = "Términos y Condiciones"
    const val LEGAL_TERMS_SUB = "Licencia GPL v3, soberanía de claves y custodia"
    const val LEGAL_PRIVACY_TITLE = "Política de Privacidad"
    const val LEGAL_PRIVACY_SUB = "Cero recolección, 100% offline y sandbox local"
    const val LEGAL_ONBOARDING_REOPEN = "Guía de Bienvenida e Introducción"

    // Formatos de exportación y respaldo
    const val VAULT_EXPORT_SUCCESS = "¡Bóveda completa exportada con éxito en archivo ZIP!"
    const val VAULT_RESTORE_SUCCESS = "¡Bóveda restaurada con éxito! Keystores verificados."
}
