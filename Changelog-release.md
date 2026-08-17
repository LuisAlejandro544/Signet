# 📋 Changelog de Lanzamientos y Pre-Releases (Signet)

Este archivo registra el historial detallado de cambios, notas de la versión, características añadidas, mejoras técnicas y correcciones de seguridad para cada versión pública y pre-release de **Signet**.

---

## 🏷️ [v1.0.0-B] - 2026-08-16 (Canal Beta - Pre-Release)

### 🌟 Resumen de la Versión
Primer pre-lanzamiento público oficial del **Canal Beta (`.beta`)** de Signet (`com.signet.app.beta`). Esta versión consolida una suite criptográfica autónoma, offline y de alto rendimiento diseñada para desarrolladores de Android y creadores de software que distribuyen fuera de Google Play (Uptodown, GitHub Releases).

---

### ✨ Características Principales Incluidas
- 🔐 **Generador Criptográfico de Keystores**:
  * Creación de almacenes de claves en formatos estándares de la industria: **JKS (Java KeyStore)** y **PKCS12 (.p12 / .pfx)**.
  * Soporte completo para algoritmos asimétricos modernos: **RSA (2048, 3072, 4096 bits)** y **Elliptic Curve / Curvas Elípticas (EC secp256r1, secp384r1, secp521r1)**.
  * Generador de certificados digitales X.509 v3 auto-firmados con período de validez configurable (de 1 a 100 años) y metadatos estándar (CN, OU, O, L, ST, C).
  * Compatibilidad estricta con los esquemas de firma de Android **APK Signature Scheme v1, v2 y v3** (`apksigner`).

- ⚡ **Integración Inmediata con Pipelines de CI/CD**:
  * Conversión y exportación automática a **Base64** con un solo toque desde la interfaz, listo para copiar y pegar en **GitHub Secrets**, Bitrise, GitLab CI o Fastlane sin intermediarios.
  * Selector nativo Storage Access Framework (SAF) para exportar directamente archivos `.jks` o `.p12` al almacenamiento local.

- 🎲 **Generador de Contraseñas Criptográficas (CSPRNG)**:
  * Generación de contraseñas de alta entropía mediante `SecureRandom` con longitud personalizable (8 a 64 caracteres) y selección de conjuntos de caracteres (mayúsculas, minúsculas, dígitos y símbolos).

- 📦 **Sistema de Respaldo y Migración Anti-Manipulación**:
  * Exportación e importación de la base de datos completa en paquetes ZIP protegidos con firma criptográfica **HMAC-SHA256** y cálculo de hash **SHA-256**.
  * Detección y bloqueo automático de archivos ZIP alterados o corruptos mediante `SecurityException`.

- 🚀 **Presets Rápidos para Entornos**:
  * Plantillas de 1 toque para inicializar configuraciones de firma para **Desarrollo**, **Play Store / Distribución** y **Empresarial / Alta Seguridad**.

- 🛡️ **Privacidad y Soberanía 100% Offline**:
  * Cero rastreadores, cero analíticas y cero permisos de red (`android.permission.INTERNET` no requerido para la operativa criptográfica).
  * Flujo de bienvenida interactivo (`WelcomeScreen`) con términos de uso y políticas de privacidad transparentes.

---

### 🛠️ Mejoras Técnicas y de Rendimiento
- **Compilación Optimizada con R8 / ProGuard**: Ofuscación y reducción de recursos para un tamaño de APK ultraligero y rendimiento nativo fluido en Jetpack Compose con Material Design 3.
- **Arquitectura MVVM Desacoplada**: Gestión de estado reactivo mediante Kotlin StateFlow, Room Database 2.6+ con KSP y aislamiento criptográfico en BouncyCastle.
- **Pipeline Automatizado de CI/CD**: Workflow de GitHub Actions con firma desatendida mediante variables `KEYSTORE_BETA_BASE64` y publicación automática de assets.
