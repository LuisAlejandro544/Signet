# 🗺️ Roadmap de Desarrollo: Signet

Plan estratégico de evolución técnica y funcionalidades para el proyecto **Signet**.

---

## 📍 Fase 1: Fundamentos, Seguridad & Modularización (Completado ✅)
- [x] Establecimiento de identidad y nombre oficial: **Signet**.
- [x] Motor de generación criptográfica X.509 con BouncyCastle (RSA 2048, RSA 4096, EC P-256).
- [x] **Modo Efímero / Sin Rastro (Zero-Footprint Mode)**: Generación volátil 100% en memoria RAM sin persistencia en disco ni base de datos Room, con exportación SAF (.jks), descarga de ZIP de respaldo y soporte para compartir seguro vía caché temporal.
- [x] Soporte para extensiones `.jks`, `.keystore`, `.p12` (PKCS#12 Multiplataforma) y `.pfx` (Microsoft Authenticode / Windows / PC).
- [x] **Extensiones X.509 de Firma de Código (Code Signing)**: Incorporación nativa de `KeyUsage` y `ExtendedKeyUsage` (`id_kp_codeSigning`, `id_kp_clientAuth`) para permitir firmas nativas en Windows (`signtool.exe`, PowerShell) y multiplataforma.
- [x] **Generador y Visualizador Interactivo de Snippets**:
  - [x] Visualizador interactivo de bloques `signingConfigs` para `build.gradle.kts` (Kotlin DSL) y `build.gradle` (Groovy).
  - [x] Generador de comandos para **Microsoft SignTool & PowerShell** (.pfx / Authenticode) y utilidades **OpenSSL**.
  - [x] Generador de workflows automatizados para GitHub Actions (`.github/workflows/android-build-and-sign.yml`) con decodificación de `KEYSTORE_BASE64` y firma en runners.
  - [x] Comandos CLI para `apksigner` y `zipalign`.
- [x] Limpieza de dependencias innecesarias de Google Play para distribución universal (Uptodown, GitHub Releases, APKs).
- [x] Publicación bajo licencia **GNU General Public License v3.0 (GPL v3)** y definición de directrices de contribución en `CONTRIBUTING.md`.
- [x] Pipeline CI/CD en GitHub Actions manual (`build-debug-apk.yml` con `workflow_dispatch`) con descarga de código, caché de Gradle, generación de clave en runner y compilación de APK Debug.
- [x] **Auditoría Automatizada de Seguridad en GitHub Actions (`security-scan.yml`)**:
  - [x] Escaneo confidencial en modo Stealth de vulnerabilidades en código Kotlin, Manifest, secretos y dependencias.
  - [x] Generación de reporte estructurado `vulnerabilities-report.json` y despacho privado a Telegram para consumo del asistente IA.
- [x] **Pruebas E2E en Emulador Real KVM en GitHub Actions (`emulator-e2e-test.yml`)**:
  - [x] Emulador Android nativo acelerado por hardware KVM (Pixel 6 / API 34).
  - [x] Verificación de importación de paquetes ZIP legítimos vs rechazo estricto de paquetes adulterados (Anti-Tampering HMAC).
  - [x] Generación de reporte JSON `emulator-e2e-report.json`, captura de pantalla del emulador y despacho confidencial a Telegram.
- [x] **Validación Cruzada CLI con Herramientas Oficiales (`cli-interoperability-test.yml`)**:
  - [x] Prueba de compatibilidad e interoperabilidad de Keystores de Signet con `keytool` y `apksigner` oficial de Google.
  - [x] Verificación estricta de firmas Android v1, v2 y v3, reporte `cli-interop-report.json` y despacho privado a Telegram.
- [x] Tests unitarios con Robolectric y soporte de CI/CD para repositorios.
- [x] **Portal Web Oficial, Términos & Privacidad para Cloudflare Pages (`web/`)**:
  - [x] Sitio estático de alto rendimiento en **Astro 5 + Tailwind CSS** con tema OLED y Emerald.
  - [x] Declaración explícita de Política de Privacidad (`/privacy`) con fecha de entrada en vigor al **16 de agosto de 2026**, **Cero Recolección de Datos** y Cero Telemetría.
  - [x] Términos y Condiciones de Uso (`/terms`) ampliados al **16 de agosto de 2026** con 12 secciones jurídicas y técnicas bajo licencia GPL v3, soberanía de claves, custodia, CI/CD, APK Matcher, anti-tampering HMAC, tiendas de terceros y exención de garantías "AS IS".
  - [x] Configuración lista para despliegue global en Cloudflare Pages (`wrangler.toml`).
- [x] **Flujo de Bienvenida Interactivo (Onboarding) & Integración Legal en la App**:
  - [x] Pantallas de bienvenida (`WelcomeScreen`) de 4 pasos explicando las capacidades del sistema y la privacidad 100% offline.
  - [x] Consentimiento informado y aceptación de Términos y Privacidad en el primer arranque.
  - [x] Acceso directo en `SettingsScreen` a las URLs de Términos y Privacidad del portal web y botón para repasar la bienvenida.
- [x] **Estrategia Multi-Canal de Distribución y Versionado Semántico**:
  - [x] Canal **.debug** (`com.signet.app.debug` / `1.0.0-D`): Compilación y pruebas internas del desarrollador vía workflow `build-debug-apk.yml`.
  - [x] Canal **.dev** (`com.signet.app.dev` / `1.0.0.dev`): Pre-alpha para pruebas tempranas de nuevas funciones experimentales.
  - [x] Canal **.beta** (`com.signet.app.beta` / `1.0.0-B`): Beta con herramientas pulidas candidatas a definitivas.
  - [x] Canal **.estable** (`com.signet.app` / `1.0.0-E`): Versión final sólida y libre de errores críticos para tiendas de terceros (Uptodown, GitHub Releases).
  - [x] Workflow automatizado de CI/CD para compilación, ofuscación R8/ProGuard, firma y publicación de APKs en GitHub Pre-Releases (`build-release-apk.yml`).
  - [x] Esquema preparado para gestión soberana de firmas criptográficas mediante GitHub Secrets.

---

## 📍 Fase 2: Firmador Integrado de APKs & Utilidades (En Progreso 🚀)
- [x] **Firmador Profesional de APKs en el Dispositivo (APK Signer & Zipalign)**:
  - [x] Implementación nativa de **APK Signature Scheme v1 (JAR signing)** generando `MANIFEST.MF`, `CERT.SF` y bloque PKCS#7 (`CERT.RSA`/`CERT.EC`).
  - [x] Implementación nativa de **APK Signature Scheme v2 (APK Signing Block)** inyectando el bloque de firma criptográfica con ID `0x7109871a` antes del Central Directory.
  - [x] Implementación nativa de **APK Signature Scheme v3 (APK Signature Scheme v3)** con ID `0xf05368c0`, soporte para rotación de claves criptográficas y compatibilidad completa para Android 9.0+.
  - [x] Inyección combinada e independiente de múltiples esquemas en el APK Signing Block.
  - [x] Motor nativo de **Zipalign a 4 bytes** para optimización de entradas `STORED` (sin comprimir) garantizando soporte `mmap` e instalación inmediata.
  - [x] Soporte para firma con Keystores de la bóveda interna (con descifrado automático AES-256-GCM) o archivos externos `.jks`/`.keystore`/`.p12`.
  - [x] Interfaz de usuario interactiva en Jetpack Compose (`SignApkScreen`) con selección de APK, Keystore, opciones personalizadas (v1, v2, v3 y zipalign), progreso, instalación directa e integración con APK Matcher.
- [ ] **Conversión de Formatos**:
  - Conversión bidireccional entre JKS/PKCS12 y PEM/CRT/KEY.
- [x] **Arquitectura Multiplataforma & Signet Desktop**:
  - [x] Sustitución de las 4 dependencias exclusivas de Android por equivalentes Java/Desktop:
    * `android.util.Base64` -> `com.example.crypto.Base64Compat` (respaldado por `java.util.Base64`).
    * `AndroidKeyStore` -> Arquitectura híbrida en `KeystoreEncryptionManager` (AES-256 en `%APPDATA%/Signet/signet_master.key` en Windows/Desktop).
    * `android.content.SharedPreferences` -> `PreferencesDataSource` y `DesktopPreferencesDataSource` (`%APPDATA%/Signet/signet_preferences.properties`).
    * `androidx.room` -> `KeystoreDataSource` y `DesktopKeystoreDataSource` (`%APPDATA%/Signet/vault_index.json`).
  - [x] Desacoplamiento del parámetro `android.content.Context` en `KeystoreGenerator`, `SignetBackupManager` y `KeystoreRepository` a `outputDir: File`.
  - [x] Motor de resolución de almacenamiento de escritorio `DesktopStorageUtils` para Windows (%APPDATA%), Linux/Unix y macOS.
  - [x] Suite de tests unitarios `DesktopMultiplatformTest` verificando compatibilidad en JVM/Escritorio.
  - [x] Módulo Gradle de Escritorio (`:desktop`) vinculado oficialmente en `settings.gradle.kts` con plugin Compose Multiplatform (`desktop/build.gradle.kts`), dependencias de Compose Desktop JVM runtime (UI, Material 3, Material Icons Extended), target JVM y empaquetado nativo jpackage (`.exe` / `.msi`).
  - [x] Punto de entrada nativo JVM `DesktopLauncher` (`app/src/main/java/com/example/desktop/Main.kt`) con bucle de ventana gráfica Swing/Compose, soporte para ejecución interactiva y suite completa de comandos CLI / Headless para Windows Terminal y PowerShell (`sign`, `generate`, `inspect`, `match`, `base64`, `backup-create`, `vault`).
  - [x] Contenedor UI `SignetDesktopApp` e inyección de servicios de plataforma (`DesktopPlatformServices` con `FileDialog` nativo y portapapeles AWT).
  - [x] Ergonomía y UX Adaptativo en `MainScreen` (`NavigationRail` en pantallas de escritorio con acceso directo a la carpeta de la bóveda y `NavigationBar` en móvil).
  - [x] **Estructura del Módulo Compartido (KMP / Shared UI)**:
    * Abstracción unificada de servicios del sistema operativo `PlatformServices` y `LocalPlatformServices` (inyección desacoplada de selección/guardado de archivos, portapapeles, explorador nativo e instalación).
    * Capa de UI compartida 100% desacoplada en Jetpack Compose / Compose Multiplatform (pantallas de Generación, Firma de APKs, Inspección forense, Historial, Bienvenida y Ajustes).
    * Adaptación y centralización de recursos y textos de interfaz en `SignetStrings` (`com.example.ui.res.SignetStrings`) eliminando dependencias de `android.R` y `stringResource`.
    * Desacoplamiento de `LocalContext` en pantallas (`GenerateScreen`, `SignApkScreen`, `LegalLinksSection`, `SettingsScreen`) delegando en `LocalPlatformServices`.
    * Configuración de fuentes compartidas de UI y datos en `desktop/build.gradle.kts` (`sourceSets`).
    * `KeystoreViewModel` reactivo gobernado puramente por Kotlin Coroutines `StateFlow` con sobrecargas agnósticas de plataforma para generación y firma.
    * Núcleo criptográfico común de alta seguridad (`com.example.crypto`) e inmutabilidad en modelos de dominio (`com.example.data.model`).
- [x] **Distribución y CI/CD de Signet Desktop**:
  - [x] Pipeline de CI/CD en GitHub Actions (`build-release-desktop.yml`) para compilación y empaquetado automatizado de ejecutables nativos de Windows (`.exe`), instaladores MSI (`.msi`) y paquetes portables (`.zip`), con hashes SHA-256, publicación en GitHub Releases (en tags/releases), ejecución manual para pruebas internas (`workflow_dispatch`) y notificación a Telegram.
  - [ ] Compilación empaquetada para Linux (.deb / AppImage) y macOS (.dmg).




