# 📋 Changelog de Lanzamientos y Pre-Releases (Signet)

Este archivo registra el historial detallado de cambios, notas de la versión, características añadidas, mejoras técnicas y correcciones de seguridad para cada versión pública y pre-release de **Signet**.

---

## 🏷️ [v1.0.0-B] - 2026-08-16 (Canal Beta - Pre-Release)

### 🌟 Resumen de la Versión
Primer pre-lanzamiento público oficial del **Canal Beta (`.beta`)** de Signet (`com.signet.app.beta`). Esta versión consolida una suite criptográfica autónoma, offline y de alto rendimiento diseñada para desarrolladores de Android y creadores de software que distribuyen fuera de Google Play (Uptodown, GitHub Releases).

---

### ✨ Características Principales Incluidas
- 🔐 **Generador Criptográfico de Keystores**:
  * Creación de almacenes de claves en formatos estándares de la industria: **JKS (Java KeyStore)**, **.keystore**, **PKCS#12 (.p12)** y **Microsoft Authenticode (.pfx)**.
  * **Extensiones X.509 de Firma de Código (Code Signing)**: Incorporación nativa de extensiones X.509 (`KeyUsage` con `digitalSignature` / `keyEncipherment` y `ExtendedKeyUsage` con `id_kp_codeSigning`) para firma de ejecutables de Windows (.exe / .msi), librerías DLL y paquetes móviles sin alertas del sistema operativo.
  * **Modo Efímero / Sin Rastro (Zero-Footprint Mode)**: Generación opcional 100% en memoria RAM sin persistir en la base de datos Room ni almacenar archivos en disco local, con soporte para exportación SAF, descarga de ZIP y copia de Base64 antes de destruir la sesión.
  * Soporte completo para algoritmos asimétricos modernos: **RSA (2048, 3072, 4096 bits)** y **Elliptic Curve / Curvas Elípticas (EC secp256r1, secp384r1, secp521r1)**.
  * Generador de certificados digitales X.509 v3 auto-firmados con período de validez configurable (de 1 a 100 años) y metadatos estándar (CN, OU, O, L, ST, C).
  * Compatibilidad estricta con los esquemas de firma de Android **APK Signature Scheme v1, v2 y v3** (`apksigner`) y herramientas de firma de Windows (**Microsoft SignTool** y **PowerShell**).

- ✍️ **Firmador de APKs en el Dispositivo (APK Signer & Zipalign)**:
  * **Firma Multi-Esquema Nativa v1 + v2 + v3**: Generación nativa de firmas JAR (`META-INF/MANIFEST.MF`, `CERT.SF`, `CERT.RSA/EC`) e inyección de bloques de firma `APK Sig Block 42` antes del Central Directory soportando simultáneamente **Esquema v2** (`0x7109871a`) y **Esquema v3** (`0xf05368c0`) con soporte de rotación de claves criptográficas y compatibilidad completa para Android 9.0+.
  * **Motor Zipalign 4-Bytes**: Alineación automática de entradas sin comprimir (`STORED`) en múltiplos de 4 bytes para optimización `mmap` y soporte nativo de instalación.
  * **Firma con Keystores de la Bóveda o Externos**: Selección transparente de claves guardadas en SQLite (con descifrado AES-256-GCM) o carga directa de archivos `.jks`/`.keystore`/`.p12` externos.
  * **Instalación y Verificación Directa**: Botón para instalar el APK firmado directamente en el teléfono e integración con APK Matcher para validación de certificados.

- 🔍 **Búsqueda Instantánea y Filtros Inteligentes en la Bóveda de Claves**:
  * **Barra de Búsqueda con Lupa y Filtrado en Tiempo Real**: Campo de búsqueda reactivo en `SavedKeystoresScreen` que evalúa instantáneamente coincidencias sobre nombres de archivo, alias, campos X.500 Distinguished Name (CN, OU, O), algoritmo criptográfico (RSA, EC) y huellas digitales SHA-256, SHA-1 y MD5.
  * **Filtros por Chips de Material 3**:
    - **Más nuevos**: Ordena los almacenes de claves por fecha de creación descendente.
    - **Más viejos**: Prioriza las claves iniciales o históricas creadas en orden cronológico ascendente.
    - **Intermedio**: Agrupa y prioriza aquellos keystores con fechas medias de creación.
    - **Recién vistos**: Registra en tiempo de ejecución las claves visualizadas o inspeccionadas por el usuario para acceso prioritario inmediato.
  * **Vista Vacía Dinámica para Búsquedas**: Componente visual amigable con icono de advertencia, indicación de término de búsqueda no encontrado y botón de 1 toque para restablecer los filtros.

- 🏷️ **Nomenclatura de Tags de Versión y Visualización de Canal en Runtime**:
  * **Resolución Automática de Canales (`SignetVersionInfo` / `SignetChannel`)**: Detección dinámica en tiempo de ejecución de la versión instalada y el canal activo:
    - **Pre-Alpha (`.dev`)**: Sufijo `-dev` / Tag `v*-dev` (`com.signet.app.dev`).
    - **Beta Comunitaria (`.beta`)**: Sufijo `-B` / Tag `v*-B` (`com.signet.app.beta`).
    - **Estable (`.estable`)**: Sufijo `-E` / Tag `v*-E` (`com.signet.app`).
    - **Depuración Interna (`.debug`)**: Sufijo `-D` / Tag `v*-D` (`com.signet.app.debug`).
  * **Visualización Dinámica en Ajustes**: Integración en `SettingsScreen` (`DistributionInfoSection` y `SoftwareUpdateSection`) con badges de color semántico, visualización del Package ID y guía interactiva de la matriz de canales de distribución.

- 🗜️ **Optimización de Compilación, R8 Full Mode y Reducción de Peso de APK**:
  * **R8 Full Mode Habilitado**: Activación de `android.enableR8.fullMode=true` para inlining agresivo de lambdas de Compose, desvirtualización de clases y eliminación de métodos puente en runtime.
  * **Afinamiento de Reglas ProGuard sobre BouncyCastle**: Reemplazo de comodines masivos `{ *; }` por reglas selectivas para providers y serializadores ASN.1/X.509, permitiendo que R8 elimine algoritmos no invocados.
  * **Descarte de Metadatos Redundantes en Empaquetado**: Exclusión de archivos `.kotlin_module`, `DebugProbesKt.bin`, esquemas `.proto` y versionado de librerías en `packaging.resources`.

- 🛡️ **Hardening de DEX, Anti-Ingeniería Inversa y Verificación de Integridad**:
  * **Aplanamiento de Paquetes (`-repackageclasses ''`)**: Mueve todas las clases ofuscadas al paquete raíz eliminando la jerarquía de directorios en el DEX para dificultar el análisis estático con descompiladores.
  * **Supresión de Cadenas de Parámetros (`Intrinsics`)**: Purgado de llamadas internas de comprobación en Release que filtraban nombres de variables en texto plano.
  * **Ocultación de Archivos Fuente y Ofuscación de Estados de UI**: Eliminación de nombres de archivo `.kt` originales y ofuscación de modelos de pantalla para proteger la lógica de validación y formularios.
  * **Verificación de Integridad de Firma en Runtime**: Integración de `SignatureVerifier.kt` en tiempo de ejecución para inspeccionar certificados criptográficos del APK instalado y alertar ante binarios alterados o refirmados por terceros no autorizados.

- ⚡ **Integración Inmediata con Pipelines de CI/CD**:
  * Conversión y exportación automática a **Base64** con un solo toque desde la interfaz, listo para copiar y pegar en **GitHub Secrets**, Bitrise, GitLab CI o Fastlane sin intermediarios.
  * Selector nativo Storage Access Framework (SAF) para exportar directamente archivos `.jks` o `.p12` al almacenamiento local.

- 🎲 **Generador de Contraseñas Criptográficas (CSPRNG)**:
  * Generación de contraseñas de alta entropía mediante `SecureRandom` con longitud personalizable (8 a 64 caracteres) y selección de conjuntos de caracteres (mayúsculas, minúsculas, dígitos y símbolos).

- 📦 **Sistema de Respaldo y Migración Anti-Manipulación**:
  * **Bóveda Completa Multi-Keystore (.zip)**: Exportación masiva de todos los almacenes de claves guardados en un único archivo ZIP estructurado por carpetas independientes (`keystores/1_alias/`, `keystores/2_alias/`), con binarios, credenciales, `key.properties`, `base64.txt`, `README-BACKUP.txt`, inventario `VAULT-SUMMARY.txt` y manifiesto maestro anti-manipulación `signet-vault-backup.json`.
  * Exportación e importación de respaldos protegidos con firma criptográfica **HMAC-SHA256** y cálculo de hash **SHA-256** para integridad de datos.
  * Detección y bloqueo automático de archivos ZIP o manifiestos alterados mediante `SecurityException`.
  * Restauración inteligente polimórfica que detecta e importa paquetes individuales o bóvedas completas con un solo toque.

- 🚀 **Presets Rápidos para Entornos**:
  * Plantillas de 1 toque para inicializar configuraciones de firma para **Google Play Release**, **Windows Authenticode (.pfx)**, **Multiplataforma (.p12)**, **Upload Key** y **Empresarial / Alta Seguridad**.

- 💻 **Generador de Snippets CI/CD y Comandos CLI**:
  * Visualizador interactivo de bloques listos para `build.gradle.kts` (Kotlin DSL), `build.gradle` (Groovy), workflows de GitHub Actions (`android-build-and-sign.yml`), comandos CLI de `apksigner`, utilidades `openssl` y comandos oficiales para **Microsoft SignTool** y **PowerShell** con marcas de tiempo RFC 3161.

- 🛡️ **Privacidad, Cifrado en Reposo y Soberanía 100% Offline**:
  * **Cifrado en Reposo de Credenciales**: Cifrado automático de contraseñas de almacén y claves con **AES-256-GCM** respaldado por **Android KeyStore** en Room (`KeystoreEncryptionManager`), garantizando que ninguna contraseña resida en plano en el disco.
  * **Protección del Portapapeles (Android 13+ / API 33+)**: Implementación del atributo `ClipDescription.EXTRA_IS_SENSITIVE` al copiar credenciales/Base64 y avisos contextuales de seguridad.
  * Cero rastreadores, cero analíticas y cero permisos de red (`android.permission.INTERNET` no requerido para la operativa criptográfica).
  * Flujo de bienvenida interactivo (`WelcomeScreen`) con términos de uso y políticas de privacidad transparentes.

---

### 🛠️ Mejoras Técnicas y de Rendimiento
- **Compilación Optimizada con R8 / ProGuard**: Ofuscación y reducción de recursos para un tamaño de APK ultraligero y rendimiento nativo fluido en Jetpack Compose con Material Design 3.
- **Arquitectura MVVM Desacoplada & Multiplataforma**: Gestión de estado reactivo mediante Kotlin StateFlow, Room Database 2.6+ con KSP y aislamiento criptográfico en BouncyCastle.
- **Desacoplamiento de Librerías Exclusivas de Android (Java / Windows / Desktop)**:
  * Sustitución de `android.util.Base64` por `Base64Compat` (respaldado por `java.util.Base64`).
  * Desacoplamiento de `AndroidKeyStore` con arquitectura híbrida en `KeystoreEncryptionManager` (AES-256-GCM y clave maestra en archivo para entornos de escritorio).
  * Abstracción de `SharedPreferences` con `PreferencesDataSource` y persistencia en `.properties` para Desktop (`signet_preferences.properties`).
  * Abstracción de Room con `KeystoreDataSource` y persistencia en índice JSON para Desktop (`vault_index.json`).
  * Desacoplamiento de `android.content.Context` en el generador criptográfico, respaldos ZIP y repositorio.
  * Compatibilidad verificada con Windows (%APPDATA%/Signet), Linux y emulación en Winlator.
  * Módulo Gradle `:desktop` con target JVM, enlace de fuentes compartidas de UI (`sourceSets`) y empaquetado nativo para Windows (`.exe` / `.msi`).
  * Punto de entrada de escritorio `DesktopLauncher` (`Main.kt`) con soporte para ejecución interactiva y comandos CLI (`--open-vault`, `--version`, `--help`).
  * Catálogo centralizado de recursos y cadenas de texto en `SignetStrings` (`com.example.ui.res.SignetStrings`) eliminando dependencias de `android.R` y `stringResource`.
  * Desacoplamiento de `LocalContext` en componentes UI delegando en `LocalPlatformServices`.
  * UX Adaptativo y Ergonomía de Escritorio en Compose (`NavigationRail` en pantallas de escritorio y `NavigationBar` en móviles).
- **Aviso sobre Versiones de Escritorio / PC (Experimental & Llamado a la Comunidad)**:
  * Las versiones y ejecutables para PC (Windows/Linux/JVM) se proporcionan como experimentales y su funcionamiento no está plenamente garantizado en todos los entornos de escritorio.
  * Se extiende una invitación abierta a la comunidad de desarrolladores y colaboradores open source para contribuir, pulir o construir herramientas y clientes dedicados para PC basados en el núcleo criptográfico de Signet.
- **Pipeline Automatizado de CI/CD**: Workflow de GitHub Actions con firma desatendida mediante variables `KEYSTORE_BETA_BASE64` y publicación automática de assets.

