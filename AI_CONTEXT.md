# 🧠 Contexto para Modelos de IA & Asistentes de Código

Este documento proporciona contexto técnico, decisiones de arquitectura y directrices específicas para modelos de IA que trabajen en este repositorio.

---

## 🎯 Propósito del Proyecto
**Signet** es una aplicación nativa para Android desarrollada con **Jetpack Compose**, **Kotlin** y **BouncyCastle**. Su objetivo es brindar a los desarrolladores y creadores de software la capacidad de generar firmas digitales, certificados, archivos `.jks`/`.keystore`, respaldos ZIP anti-manipulación y validar la correspondencia forense entre APKs y Keystores (APK Matcher) directamente en el dispositivo móvil.

---

## 🔑 Puntos Críticos y Reglas Criptográficas & Arquitectura

1. **Identidad del Proyecto**:
   - Nombre de la aplicación: **Signet**.
   - Nombre en launcher (`app_name`): `Signet`.
   - Identificador de aplicación (`applicationId`): `com.signet.app`.
   - Propósito: Suite criptográfica de firma digital de APKs, creación de Keystores, certificados X.509, validación forense APK vs Keystore y generación de variables para CI/CD.

2. **Proveedor BouncyCastle en Android**:
   - Android cuenta con un proveedor criptográfico interno limitado `"BC"`.
   - **Regla Obligatoria**: Siempre registrar `BouncyCastleProvider` al inicio de la lista de proveedores con `Security.insertProviderAt(BouncyCastleProvider(), 1)` y pasar explícitamente la instancia del proveedor a `JcaContentSignerBuilder`, `JcaX509CertificateConverter` y `KeyStore.getInstance("PKCS12", provider)`.

3. **Generación Criptográfica de Contraseñas (CSPRNG)**:
   - Utilizar `com.example.crypto.PasswordGenerator` con `SecureRandom` del sistema operativo.
   - Longitud parametrizada (16, 20, 24, 32 caracteres) garantizando inclusión de mayúsculas, minúsculas, números y símbolos seguros para terminal y Gradle.
   - Cálculo en tiempo real de entropía (bits) y fortaleza para retroalimentación visual en Compose.

4. **Formatos Soportados, Validez & Modo Efímero**:
   - Generación: Estándar PKCS#12 (.jks y .keystore) compatible al 100% con `apksigner`, `jarsigner` y Android Studio Gradle Plugin.
   - Algoritmos: `RSA` (2048 y 4096 bits) con `SHA256WithRSAEncryption` y `EC` (Curva `secp256r1`/P-256) con `SHA256withECDSA`.
   - **Modo Efímero / Sin Rastro (Zero-Footprint)**: Generación 100% en memoria RAM sin persistir en base de datos Room (`AppDatabase`) ni crear archivos físicos en `context.filesDir`. Permite exportar vía SAF (escribiendo bytes decodificados de Base64), generar el ZIP de respaldo o compartir con archivo temporal de caché. Al cerrar la hoja o la app, los datos desaparecen sin dejar rastro en el dispositivo.
   - Validez interactiva: Control deslizable (1 a 100 años) con valor recomendado de 25+ años para cumplir requerimientos de actualización de APKs.
   - Requisitos de identidad: Diferenciación clara entre obligatorios por estándar Google/Android (`CN`, `O`) y opcionales (`OU`, `L`, `ST`, `C`), con soporte para datos inventados o seudónimos para proteger la privacidad.

5. **Validador Forense APK vs Keystore (`ApkMatcher`)**:
   - Analiza binarios de paquetes `.apk` mediante análisis forense multi-esquema:
     - **Esquema v1 (JAR Signing)**: Lee archivos PKCS#7 en `META-INF/*.RSA`, `*.DSA` o `*.EC` usando `CMSSignedData` de BouncyCastle.
     - **Esquema v2 / v3 (APK Signing Block)**: Localiza el bloque de firma de Android buscando el magic `APK Sig Block 42` antes del EOCD del archivo ZIP y extrae certificados del ID de bloque `0x7109871a` y `0xf05368c0`.
   - Realiza la comparación determinista de huellas SHA-256 contra Keystores guardados en la base de datos Room o cargados externamente.

6. **Sistema de Temas Dinámico & Negro 100% (AMOLED)**:
   - Configuración centralizada mediante `ThemeState`, `ThemeMode` y `ColorPalette`.
   - Soporte para **Material You** (Android 12+), paletas personalizadas (Navy, Emerald, Purple, Amber, Teal, Crimson, Monochrome) y modo **Negro Puro 100%** (#000000 para pantallas OLED).
   - Preferencias del usuario persistidas en `SharedPreferences`.

7. **Persistencia Local y Cifrado en Reposo (Android KeyStore + Room)**:
   - Toda información sensible se almacena localmente en SQLite mediante Room.
   - Las contraseñas del almacén y de claves se cifran en reposo con **AES-256-GCM** mediante `KeystoreEncryptionManager`, utilizando claves maestras de hardware en AndroidKeyStore y prefijo `enc:v1:`.

8. **Seguridad del Portapapeles (Android 13+ / API 33+)**:
   - `DetailsActionUtils` marca los datos confidenciales (contraseñas y Base64) con `ClipDescription.EXTRA_IS_SENSITIVE` para impedir previsualizaciones inseguras en el sistema operativo y despliega avisos contextuales de seguridad.

9. **Reglas de UI (Jetpack Compose & M3)**:
   - Utilizar componentes de Material 3 (`Scaffold`, `Card`, `FilterChip`, `Button`, `OutlinedTextField`, `Slider`, `TabRow`, `LinearProgressIndicator`).
   - Respetar áreas táctiles mínimas de 48dp y `testTag` en botones y acciones clave.
   - Modularización de componentes en paquetes especializados (`ui/screens/generate/`, `ui/screens/inspect/` y `ui/components/details/`).

9. **Snippets Gradle & Workflows GitHub Actions**:
   - `SnippetGenerator` expone funciones para generar configuración `build.gradle.kts` (Kotlin DSL), `build.gradle` (Groovy), workflows de GitHub Actions (`.github/workflows/android-build-and-sign.yml`) y comandos CLI `apksigner`.
   - Las configuraciones de Gradle generadas deben seguir buenas prácticas de seguridad leyendo secretos desde variables de entorno (`KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, etc.).

10. **Distribución Fuera de Google Play (Uptodown, GitHub Releases, APKs)**:
    - La aplicación no requiere ni incluye servicios innecesarios de Google Play (`play-services-*`, `firebase-*`, etc.). Es 100% autónoma, funcional offline y compatible con cualquier dispositivo Android / ROM personalizada (LineageOS, GrapheneOS, microG).

11. **Paquetes de Respaldo ZIP, Bóveda Completa & Mecanismo Anti-Manipulación (`SignetBackupManager`)**:
    - Generación y descompresión de paquetes ZIP individuales con `signet-backup.json`, firma HMAC-SHA256, hash de contenido y credenciales.
    - **Exportación de Bóveda Completa (`createVaultBackupZip`)**: Empaqueta todos los Keystores guardados en una estructura de subcarpetas (`keystores/1_alias/`, `keystores/2_alias/`) conteniendo cada una sus credenciales, properties, base64 y manifiesto local.
    - **Manifiesto Maestro de Bóveda (`signet-vault-backup.json`)**: Firma global HMAC-SHA256 generada mediante `BackupIntegrityVerifier.buildSignedVaultManifest`, vinculando de manera estricta los hashes SHA-256 de todos los binarios y sus rutas relativas.
    - **Restauración Polimórfica (`restoreAnyFromZip`) y Cifrado Automático**: Determina de forma automática e inteligente si el paquete ZIP proporcionado corresponde a un respaldo individual o a una bóveda multi-keystore. Tras verificar las firmas HMAC y validar los binarios, todas las credenciales importadas se cifran automáticamente en reposo mediante **AES-256-GCM** en Android KeyStore (`KeystoreEntity.fromDetails`), protegiendo la base de datos contra accesos no autorizados de malware o usuarios root mientras el usuario accede con total transparencia y normalidad.
    - Se garantiza la lectura atómica de entradas ZIP sin cierres prematuros de streams.

12. **Portal Web Oficial, Términos y Privacidad (`web/`)**:
    - Sitio web en Astro 5 + Tailwind CSS para Cloudflare Pages.
    - Términos y Condiciones (`/terms` -> `https://signet-web.luisalejandrososacamacho9.workers.dev/terms/`) y Política de Privacidad (`/privacy` -> `https://signet-web.luisalejandrososacamacho9.workers.dev/privacy/`) formalizados con fecha **16 de agosto de 2026**.
    - Cobertura legal integral: GPL v3, soberanía total de claves del usuario, custodia sin servidores remotos, buenas prácticas en CI/CD, disclaimer del APK Matcher, respaldos ZIP con firma HMAC y distribución en plataformas de terceros (Uptodown, GitHub Releases, APKs directos).

13. **Flujo de Bienvenida & Onboarding (`WelcomeScreen`)**:
    - `MainActivity` evalúa `isOnboardingCompleted` desde `KeystoreViewModel` (respaldado en `SharedPreferences` con la clave `onboarding_completed`).
    - En el primer inicio, presenta una experiencia guiada de 4 etapas que detalla las características de la suite, garantiza la privacidad offline y solicita la aceptación expresa de los Términos y la Privacidad antes de acceder a la aplicación principal.
    - `SettingsScreen` contiene accesos directos interactivos a los URLs legales y permite reiniciar la guía con `resetOnboarding()`.

14. **Canales de Lanzamiento, Sufijos de Package y Versionado**:
    - **Canal `.debug`**: ID `com.signet.app.debug`, versión `1.0.0-D`. Canal interno exclusivo de desarrollo; se compila mediante GitHub Actions (`build-debug-apk.yml`) con firma dinámica de depuración.
    - **Canal `.dev`**: ID `com.signet.app.dev`, versión `1.0.0.dev`. Canal Pre-Alpha para pruebas comunitarias de funciones en desarrollo temprano. Se firma mediante GitHub Secrets.
    - **Canal `.beta`**: ID `com.signet.app.beta`, versión `1.0.0-B`. Canal Beta para validación de funciones maduras y candidatas a definitivas. Se firma mediante GitHub Secrets.
    - **Canal `.estable`**: ID `com.signet.app` (o `.estable`), versión `1.0.0-E`. Canal de producción con herramientas 100% pulidas para distribución general en tiendas de terceros (Uptodown, GitHub Releases). Se firma mediante GitHub Secrets.

15. **Pipeline de Compilación y Publicación Beta (`build-release-apk.yml`)**:
    - Workflow activado por creación/publicación de Pre-Releases en GitHub o tags `v*` (especialmente `v1.0.0-B`).
    - Soporta secretos dedicados para el canal Beta: `KEYSTORE_BETA_BASE64` (o `KEYSTORE_BASE64`), `KEYSTORE_BETA_PASSWORD` (o `KEYSTORE_PASSWORD`), `KEY_ALIAS_BETA` (o `KEY_ALIAS`) y `KEY_PASSWORD_BETA` (o `KEY_PASSWORD`).
    - Decodifica el keystore en `app/signet-beta-key.jks`, compila con R8 (`isMinifyEnabled = true`, `isShrinkResources = true`) aplicando las reglas de `app/proguard-rules.pro`.
    - Genera el checksum SHA-256 y adjunta automáticamente el APK firmado a los assets del Pre-Release de GitHub.
    - Registro de versiones documentado en `Changelog-release.md`.

16. **Firmador Profesional de APKs & Motor Zipalign (`com.example.crypto.signer`)**:
    - `ApkSigner`: Orquestador central que extrae el par de claves/certificado del Keystore (interno de la bóveda o archivo externo), ejecuta `ApkZipalignEngine`, aplica `ApkV1Signer`, `ApkV2Signer` y/o `ApkV3Signer`, y genera el APK firmado con guardado/caché y opción de instalación nativa.
    - `ApkV1Signer`: Firma basada en JAR (`META-INF/MANIFEST.MF`, `CERT.SF`, `CERT.RSA`/`CERT.EC`) usando BouncyCastle CMS `CMSSignedDataGenerator`.
    - `ApkV2Signer`: Inyección del **APK Signing Block** con ID `0x7109871a` y magic `APK Sig Block 42` antes del Central Directory con cálculo de digesiones de 1 MB de las 3 secciones del APK (ZIP entries, Central Directory, EOCD ajustado).
    - `ApkV3Signer`: Inyección del **APK Signature Scheme v3** con ID `0xf05368c0` dentro del APK Signing Block, con rango de SDKs (`minSdkVersion = 28`, `maxSdkVersion = Integer.MAX_VALUE`), soporte de rotación de claves criptográficas e interoperabilidad nativa con v2.
    - Inyección coordinada en `ApkV3Signer.injectSignatureBlock`: Empaqueta bloques v2 y/o v3 en un único bloque APK Signing Block sin alterar el alineamiento ni la estructura de Central Directory.
    - `ApkZipalignEngine`: Alineación de entradas sin comprimir (`STORED`) en múltiplos exactos de 4 bytes para compatibilidad de mapeo en memoria `mmap`.
    - `SignApkScreen`: Interfaz en Compose con selector de APK, selector de Keystore, opciones configurables (switches independientes para v1, v2, v3 y zipalign) y tarjeta de resultados interactiva (con botón de instalación mediante `Intent.ACTION_INSTALL_PACKAGE` y navegación hacia `InspectScreen`).

17. **Arquitectura Multiplataforma & Desacoplamiento Desktop / Windows**:
    - **Sustitución de las 4 librerías exclusivas de Android**:
      1. `android.util.Base64` -> `com.example.crypto.Base64Compat`: Wrapper universal respaldado por `java.util.Base64` en tiempo de ejecución estándar JVM/Android.
      2. `AndroidKeyStore` (`KeyGenParameterSpec`) -> `KeystoreEncryptionManager`: Arquitectura híbrida que utiliza AndroidKeyStore en Android y en Desktop/Windows deriva a clave maestra AES-256 de 256 bits (`signet_master.key`) persistida en el directorio de la aplicación (`%APPDATA%/Signet/` en Windows).
      3. `android.content.SharedPreferences` -> `PreferencesDataSource`: Interfaz desacoplada con implementaciones `AndroidPreferencesDataSource` y `DesktopPreferencesDataSource` (persistencia en `signet_preferences.properties`).
      4. `androidx.room` -> `KeystoreDataSource`: Abstracción de repositorio con `RoomKeystoreDataSource` y `DesktopKeystoreDataSource` (almacenamiento en `vault_index.json` con `StateFlow` reactivo).
    - **Desacoplamiento de `android.content.Context` en el núcleo**: `SignetBackupManager`, `KeystoreGenerator` y `KeystoreRepository` aceptan directamente `outputDir: File`.
    - **Rutas del Sistema Operativo (`DesktopStorageUtils`)**: Resolución automática de `%APPDATA%/Signet` en Windows, `~/Library/Application Support/Signet` en macOS y `~/.config/signet` en Linux.
    - **Compatibilidad con Winlator / JVM Windows**: La arquitectura permite la ejecución sin dependencias del Android Framework para emuladores Winlator y ejecutables Desktop nativos.
    - **Pruebas de Compatibilidad Desktop**: Verificadas mediante `DesktopMultiplatformTest.kt` (32 tests unitarios pasando en total).



