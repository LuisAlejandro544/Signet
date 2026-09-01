# 🏗️ Arquitectura y Estructura del Proyecto: Signet

Este documento describe la organización de carpetas, responsabilidades de cada módulo y flujo de datos dentro de **Signet**.

---

## 🌳 Árbol de Archivos Principal

```
app/
├── src/
│   ├── main/
│   │   ├── AndroidManifest.xml                  # Declaración de permisos, providers y componentes
│   │   ├── java/com/example/
│   │   │   ├── MainActivity.kt                  # Punto de entrada de la actividad Android, Navigation y tema reactivo
│   │   │   ├── desktop/
│   │   │   │   ├── Main.kt                      # Punto de entrada ejecutable nativo para escritorio (Windows / macOS / Linux) y CLI
│   │   │   │   ├── DesktopWindowLauncher.kt     # Inicializador del contenedor de ventana gráfica Swing y configuración HiDPI
│   │   │   │   ├── SignetDesktopApp.kt          # Contenedor Compose Desktop con inyección de servicios de plataforma
│   │   │   │   └── cli/
│   │   │   │       └── DesktopCliHandler.kt     # Despachador y ejecutor de comandos CLI headless (sign, generate, inspect, match, base64, backup, vault)
│   │   │   ├── platform/
│   │   │   │   ├── PlatformServices.kt          # Interfaz agnóstica de servicios del sistema operativo (archivos, portapapeles, explorador)
│   │   │   │   ├── DesktopPlatformServices.kt   # Implementación para Windows/Desktop (Java AWT, FileDialog, Explorer nativo)
│   │   │   │   ├── AndroidPlatformServices.kt   # Implementación para Android (Context, FileProvider, Intents del sistema)
│   │   │   │   └── PlatformFileAdapters.kt      # Adaptadores y hooks reactivos Compose para selección y guardado de archivos
│   │   │   ├── crypto/
│   │   │   │   ├── ApkMatcher.kt                # Fachada orquestadora forense APK vs Keystore (análisis y coincidencia)
│   │   │   │   ├── Base64Compat.kt              # Wrapper universal de Base64 compatible con Android y JVM/Desktop (java.util.Base64)
│   │   │   │   ├── DesktopStorageUtils.kt       # Resolución de rutas de almacenamiento en escritorio (%APPDATA%/Signet, XDG)
│   │   │   │   ├── KeystoreEncryptionManager.kt # Gestor de cifrado en reposo AES-256-GCM (AndroidKeyStore en móvil / clave maestra en escritorio)
│   │   │   │   ├── signer/
│   │   │   │   │   ├── ApkSigner.kt             # Fachada orquestadora del firmador de APKs (v1, v2, v3, Zipalign)
│   │   │   │   │   ├── ApkV1Signer.kt           # Motor de firma JAR (v1) con MANIFEST.MF, CERT.SF y CMS PKCS#7
│   │   │   │   │   ├── ApkV2Signer.kt           # Motor de inyección de APK Signing Block (Esquema v2)
│   │   │   │   │   ├── ApkV3Signer.kt           # Motor de inyección de APK Signing Block v3 (ID 0xf05368c0, rotación de claves)
│   │   │   │   │   └── ApkZipalignEngine.kt     # Motor de alineación a 4-bytes para optimización mmap en APKs
│   │   │   │   ├── apk/
│   │   │   │   │   ├── ApkSigningBlockParser.kt # Parser binario de bajo nivel para esquemas v2 y v3 (APK Signing Block)
│   │   │   │   │   ├── ApkV1SignatureParser.kt  # Extractor de firmas PKCS#7 en META-INF mediante BouncyCastle CMS (v1 JAR)
│   │   │   │   │   └── AxmlManifestParser.kt    # Extractor de packageName desde el string pool binario de AndroidManifest.xml
│   │   │   │   ├── KeystoreGenerator.kt         # Fachada orquestadora de generación y exportación de almacenes de claves
│   │   │   │   ├── keys/
│   │   │   │   │   └── KeyPairFactory.kt        # Fábrica especializada en generación de pares de claves asimétricas (RSA, EC, DSA)
│   │   │   │   ├── keystore/
│   │   │   │   │   └── Pkcs12KeystoreSerializer.kt # Serializador de almacenes PKCS#12, JKS, PFX y persistencia en disco
│   │   │   │   ├── PasswordGenerator.kt         # Generador criptográfico CSPRNG de contraseñas ultra seguras y cálculo de entropía
│   │   │   │   ├── RandomIdentityGenerator.kt   # Generador CSPRNG de datos aleatorios para DN (X.500), país, nombres de archivo y alias
│   │   │   │   ├── SignetBackupManager.kt       # Fachada orquestadora de exportación/restauración de respaldos individuales y bóvedas
│   │   │   │   ├── backup/
│   │   │   │   │   ├── BackupTemplates.kt       # Generador de plantillas de texto (credentials.txt, key.properties, README-BACKUP.txt, VAULT-SUMMARY.txt)
│   │   │   │   │   ├── BackupIntegrityVerifier.kt # Fachada de verificación de integridad y enlace a submódulos especializados
│   │   │   │   │   ├── HmacSignatureEngine.kt   # Motor criptográfico centralizado para hashes SHA-256 y firmas HMAC-SHA256 con tiempo constante
│   │   │   │   │   ├── SignetManifestParser.kt  # Parser, generador y validador de manifiestos individuales (signet-backup.json)
│   │   │   │   │   ├── SignetVaultManifestParser.kt # Parser, generador y validador de manifiestos maestros de bóveda (signet-vault-backup.json)
│   │   │   │   │   ├── VaultRestorationCoordinator.kt # Coordinador modular de restauración, desbloqueo y resolución de nombres de archivo
│   │   │   │   │   ├── ZipPackageBuilder.kt     # Empaquetador de flujos binarios ZIP para respaldos individuales y bóvedas maestras
│   │   │   │   │   └── ZipPackageExtractor.kt   # Extractor y analizador de flujos ZIP en memoria
│   │   │   │   ├── x509/
│   │   │   │   │   ├── X509CertificateBuilder.kt # Constructor modular de certificados X.509 v3 con extensiones ASN.1 Code Signing
│   │   │   │   │   ├── X509CertificateInspector.kt # Inspección y lectura de certificados en almacenes PKCS12, JKS, PFX, P12 y BKS
│   │   │   │   │   └── X509CertificateUtils.kt    # Utilidades de cálculo de huellas digitales y formateo PEM estándar
│   │   │   │   └── SnippetGenerator.kt          # Generador modular de snippets: Gradle KTS, Groovy, GitHub Actions, Microsoft SignTool (.pfx), PowerShell y OpenSSL
│   │   │   ├── platform/
│   │   │   │   ├── PlatformServices.kt          # Interfaz unificada de servicios de plataforma (Android SAF / Desktop AWT)
│   │   │   │   ├── LocalPlatformServices.kt     # CompositionLocal para inyección de dependencias multiplataforma en Compose
│   │   │   │   ├── AndroidPlatformServices.kt   # Implementación Android con SAF y FileProvider
│   │   │   │   ├── DesktopPlatformServices.kt   # Implementación Desktop con FileDialog y portapapeles AWT
│   │   │   │   ├── PlatformFileAdapters.kt      # Adaptadores de flujos y descriptores de archivo multiplataforma
│   │   │   │   ├── AndroidCryptoExtensions.kt   # Extensiones criptográficas y compatibilidad Android KeyStore
│   │   │   │   └── SignetVersionInfo.kt         # Metadatos de versión en runtime, resolución de canales (.dev, .beta, .estable, .debug) y tags
│   │   │   ├── data/
│   │   │   │   ├── KeystoreDataSource.kt        # Abstracción desacoplada de persistencia (RoomKeystoreDataSource / DesktopKeystoreDataSource)
│   │   │   │   ├── KeystoreRepository.kt        # Repositorio que orquesta base de datos y operaciones
│   │   │   │   ├── local/
│   │   │   │   │   ├── AppDatabase.kt           # Definición de Room Database (v2) con migraciones
│   │   │   │   │   ├── KeystoreDao.kt            # Operaciones DAO (insert, query, delete)
│   │   │   │   │   └── KeystoreEntity.kt         # Entidad de persistencia de keystores con Base64 y credenciales
│   │   │   │   └── model/
│   │   │   │       └── KeystoreModels.kt        # Modelos de dominio (KeystoreConfig, KeystoreDetails, ApkInfo, ApkSigningOptions, ApkSigningResult, etc.)
│   │   │   └── ui/
│   │   │       ├── KeystoreViewModel.kt         # ViewModel orquestador central que delega en gestores especializados
│   │   │       ├── delegates/
│   │   │       │   ├── KeystoreFormDelegate.kt   # Delegado de formularios de creación, presets, validación y estados de generación
│   │   │       │   ├── KeystoreFilterDelegate.kt # Delegado modular de búsqueda en tiempo real, ordenamiento temporal y métricas de acceso
│   │   │       │   ├── SignApkDelegate.kt        # Delegado de selección de APK, análisis preliminar y ejecución de firmado v1/v2/v3
│   │   │       │   ├── ApkMatcherDelegate.kt     # Delegado de análisis forense y verificación de firmas APK vs Keystore
│   │   │       │   └── AppUpdateDelegate.kt      # Delegado de comprobación de versiones en GitHub Releases y descarga de binarios
│   │   │       ├── MainScreen.kt                # Contenedor principal adaptativo (Desktop/Tablet/Mobile) con scaffolding limpio
│   │   │       ├── navigation/
│   │   │       │   ├── MainAdaptiveNavigation.kt # Componentes NavigationRail, NavigationBar y TopAppBar adaptativos
│   │   │       │   └── MainTabContent.kt        # Conmutador modular de pantallas según la pestaña activa
│   │   │       ├── components/
│   │   │       │   ├── KeystoreDetailsSheet.kt  # BottomSheet orquestador de exportación SAF, compartir y detalles
│   │   │       │   ├── main/
│   │   │       │   │   └── MainGlobalSheets.kt  # Host modal global para hojas de detalles y diálogos de actualización
│   │   │       │   └── details/
│   │   │       │       ├── DetailsActionUtils.kt         # Utilidades de portapapeles y compartir mediante FileProvider
│   │   │       │       ├── KeystoreHeaderSection.kt      # Encabezado, alias, botones de exportar y compartir
│   │   │       │       ├── KeystoreCredentialsCard.kt    # Tarjeta de credenciales con visibilidad y copia en un toque
│   │   │       │       ├── KeystoreBase64Card.kt         # Visualizador de Base64 para variables de entorno y CI/CD
│   │   │       │       ├── KeystoreFingerprintsCard.kt   # Huellas SHA-256, SHA-1, MD5 y datos X.509
│   │   │       │       └── KeystoreCodeSnippetsCard.kt   # Visor interactivo de código Gradle, Groovy, YAML y CLI
│   │   │       ├── preferences/
│   │   │       │   ├── AppPreferencesManager.kt # Gestor modular de preferencias de usuario (tema, paleta y estado de bienvenida)
│   │   │       │   └── PreferencesDataSource.kt # Abstracción multiplataforma de preferencias (Android SharedPreferences / Desktop .properties)
│   │   │       ├── res/
│   │   │       │   └── SignetStrings.kt         # Catálogo centralizado de recursos y cadenas de texto agnósticas (sin dependencias de android.R)
│   │   │       ├── screens/
│   │   │       │   ├── WelcomeScreen.kt         # Orquestador ligero de bienvenida, explicación de capacidades y navegación
│   │   │       │   ├── welcome/
│   │   │       │   │   ├── WelcomeModels.kt              # Estructuras de datos y constantes de las pantallas de bienvenida
│   │   │       │   │   ├── OnboardingPageItem.kt         # Renderizado de tarjeta hero, beneficios e iconos temáticos
│   │   │       │   │   ├── LegalConsentCard.kt           # Tarjeta de enlaces a Términos/Privacidad y aceptación checkbox
│   │   │       │   │   └── OnboardingNavigationFooter.kt # Paginador de puntos y botones Atrás/Siguiente/Comenzar
│   │   │       │   ├── GenerateScreen.kt        # Pantalla principal de generación
│   │   │       │   ├── generate/
│   │   │       │   │   ├── GeneratePresetsSection.kt     # Chips de plantillas rápidas (Release, Upload, RSA 4096)
│   │   │       │   │   ├── KeystoreCredentialsForm.kt    # Formato, contraseñas, medidor de entropía y generador CSPRNG
│   │   │       │   │   ├── KeystoreValiditySection.kt    # Slider interactivo de años, chips y selector de algoritmo
│   │   │       │   │   ├── KeystoreDnFields.kt           # Formulario X.500 con distinción de obligatorios Google y opcionales
│   │   │       │   │   └── EphemeralModeSection.kt       # Selector interactivo del Modo Efímero (Zero-Footprint en memoria RAM)
│   │   │       │   ├── SignApkScreen.kt         # Pantalla principal de firmado de APKs en el dispositivo
│   │   │       │   ├── sign/
│   │   │       │   │   ├── SelectApkCard.kt              # Tarjeta de selección de APK a firmar
│   │   │       │   │   ├── SelectKeystoreForSigningCard.kt # Selector entre Keystores guardados y archivo externo con contraseñas
│   │   │       │   │   ├── SigningOptionsCard.kt         # Opciones de Schemes v1/v2/v3, Zipalign y nombre de salida
│   │   │       │   │   └── SigningResultCard.kt          # Tarjeta de resultado con detalles, botón de instalación e inspección
│   │   │       │   ├── InspectScreen.kt         # Orquestador con TabRow para inspección de archivos y validación APK
│   │   │       │   ├── inspect/
│   │   │       │   │   ├── KeystoreInspectorSection.kt   # Formulario de carga de archivo externo, contraseña y visor
│   │   │       │   │   ├── CertificateDetailsResultCard.kt # Tarjeta de visualización de certificados X.509 y huellas
│   │   │       │   │   ├── ApkMatcherSection.kt          # Orquestador y layout principal de validación forense APK vs Keystore
│   │   │       │   │   └── apk/
│   │   │       │   │       ├── ApkMatcherHeaderBanner.kt     # Banner informativo de presentación del validador de coincidencia
│   │   │       │   │       ├── ApkFileSelectorCard.kt        # Tarjeta de selección de archivo APK mediante SAF
│   │   │       │   │       ├── ApkKeystoreSelectorCard.kt    # Selector interactivo entre almacenes guardados y archivo JKS externo con validación
│   │   │       │   │       ├── ApkMatchResultCard.kt         # Banner de diagnóstico comparativo de huellas SHA-256 y dictamen
│   │   │       │   │       ├── ApkMetadataDetailsCard.kt     # Visualizador de metadatos del paquete, package name y certificados X.509
│   │   │       │   │       └── ApkErrorCard.kt               # Tarjeta de visualización de errores y advertencias de análisis
│   │   │       │   ├── SavedKeystoresScreen.kt  # Orquestador de historial de almacenes de claves guardados con búsqueda y filtros
│   │   │       │   ├── saved/
│   │   │       │   │   ├── SavedKeystoresHeader.kt       # Banner de exportación y restauración de respaldos ZIP
│   │   │       │   │   ├── SavedKeystoresSearchAndFilterSection.kt # Barra de búsqueda rápida con lupa y chips de filtro (Más nuevos, Más viejos, Intermedio, Recién vistos)
│   │   │       │   │   ├── KeystoreCardItem.kt           # Tarjeta de keystore con credenciales, huella SHA-256 y acciones
│   │   │       │   │   ├── EmptySavedKeystoresView.kt    # Vista vacía amigable con llamadas a la acción
│   │   │       │   │   └── SavedKeystoresDialogs.kt      # Diálogos de eliminación, progreso y errores de integridad
│   │   │       │   ├── SettingsScreen.kt        # Pantalla de configuración modular
│   │   │       │   └── settings/
│   │   │       │       ├── SettingsHeaderCard.kt         # Tarjeta de encabezado y resumen visual
│   │   │       │       ├── ThemeModeSection.kt           # Selector de temas (Claro, Oscuro, Negro 100% AMOLED, Sistema)
│   │   │       │       ├── ColorPaletteSection.kt        # Selector de paleta de acentos y Material You (Android 12+)
│   │   │       │       ├── LegalLinksSection.kt          # Accesos a Términos, Privacidad y guía de bienvenida
│   │   │       │       └── DistributionInfoSection.kt    # Visualizador de versión activa, badges de canal (.dev, .beta, .estable, .debug) y matriz de tags
│   │   │       ├── state/
│   │   │       │   ├── FormState.kt             # Modelo inmutable de estado del formulario de generación de keystore
│   │   │       │   └── KeystoreUiStates.kt      # Contratos sellados de estado de UI (Generation, Restore, Inspector, ApkMatcher)
│   │   │       └── theme/
│   │   │           ├── Color.kt                 # Paleta de colores M3 (Emerald, Purple, Amber, Teal, Crimson, Mono)
│   │   │           ├── ThemeConfig.kt           # Enums ThemeMode (Claro, Oscuro, Negro 100%) y ColorPalette
│   │   │           ├── Theme.kt                 # Proveedor de temas M3 con soporte dinámico Android 12+ y OLED
│   │   │           └── Type.kt                  # Tipografía Material 3
│   │   └── res/                                 # Recursos visuales, strings y drawables
│   └── test/
│       └── java/com/example/
│           ├── ExampleRobolectricTest.kt        # Suite de pruebas de humo e integración end-to-end
│           ├── crypto/
│           │   ├── ApkMatcherTest.kt            # Pruebas de análisis forense y detección de coincidencia de firmas
│           │   ├── ApkSignerTest.kt             # Pruebas de firmado multi-esquema (v1, v2, v3) y motor Zipalign
│           │   ├── DesktopMultiplatformTest.kt  # Pruebas de persistencia Desktop, Base64Compat y rutas del sistema operativo
│           │   ├── KeystoreEncryptionManagerTest.kt # Pruebas de cifrado en reposo AES-256-GCM y mapeo en Room
│           │   ├── KeystoreGeneratorTest.kt     # Pruebas de generación RSA 2048, EC P256 e inspección
│           │   ├── PasswordGeneratorTest.kt     # Pruebas de contraseñas CSPRNG, cálculo de entropía y clasificación
│           │   ├── SignetBackupIntegrityTest.kt # Pruebas de integridad de respaldos ZIP y rechazo de manipulación HMAC
│           │   └── SnippetGeneratorTest.kt      # Pruebas de generación de código Gradle, Groovy, GitHub Actions y apksigner
│           └── ui/
│               └── KeystoreViewModelTest.kt     # Pruebas de estado de onboarding, endpoints legales y presets
├── desktop/                                     # Módulo Gradle de Escritorio para empaquetado nativo Windows/Desktop
│   └── build.gradle.kts                         # Configuración de compilación JVM y empaquetado jpackage
├── scripts/                                     # Scripts de pruebas automatizadas y validación cruzada
│   ├── cli_interoperability_test.py             # Validación cruzada CLI con keytool y Google apksigner
│   ├── e2e_emulator_test.py                     # Script E2E en emulador nativo Android KVM
│   └── run_e2e_test.sh                          # Lanzador automatizado para pruebas E2E
├── Changelog-release.md                         # Historial detallado de notas de la versión y changelogs para releases
├── .github/
│   └── workflows/
│       ├── build-debug-apk.yml                  # Compilación y despacho confidencial de APK Debug (Canal .debug)
│       ├── build-release-apk.yml                # Compilación, R8 ProGuard, empaquetado nativo eficiente (useLegacyPackaging=false), firma y publicación de APKs para Móviles (ARM) y Emuladores (x86) (Canal .beta)
│       ├── build-release-desktop.yml            # Compilación, empaquetado jpackage nativo Windows (.exe, .msi, .zip) y publicación
│       ├── security-scan.yml                    # Auditoría de seguridad estática en modo Stealth
│       ├── emulator-e2e-test.yml                # Pruebas E2E en emulador nativo Android KVM
│       ├── cli-interoperability-test.yml        # Validación cruzada CLI con keytool y Google apksigner
│       ├── override-commit-message.yml          # Sincronización y reescritura del último mensaje de commit desde commit_message.txt
│       └── sync-zip.yml                         # Respaldo automático
└── web/                                         # Portal web estático (Astro 5 + Tailwind CSS para Cloudflare Pages)
    ├── src/
    │   ├── layouts/Layout.astro                 # Plantilla principal con metadata SEO y tema OLED
    │   ├── components/                          # Navbar, Hero, Features, SecurityPillars, DownloadSection, Footer
    │   └── pages/
    │       ├── index.astro                      # Página de inicio con descarga de APK e información del proyecto
    │       ├── privacy.astro                    # Política de privacidad (Cero recolección, offline, vigencia 16-08-2026)
    │       ├── terms.astro                      # Términos y condiciones detallados (12 secciones GPL v3, vigencia 16-08-2026)
    │       └── 404.astro                        # Página 404 personalizada
    ├── wrangler.toml                            # Configuración de despliegue en Cloudflare Pages
    └── astro.config.mjs                         # Configuración de Astro con integración Tailwind CSS
```

---

## 🔄 Flujo de Datos y Operaciones Clave

### 1. Generación Modular de Keystores (`KeystoreGenerator`)
1. **Delegación Arquitectónica**: `KeystoreGenerator` actúa como fachada delegando responsabilidades a:
   - `KeyPairFactory`: Generación de pares de claves asimétricas (RSA de 2048/4096 bits, Curvas Elípticas ECDSA P-256/P-384/P-521, DSA).
   - `X509CertificateBuilder`: Construcción de certificados X.509 v3 auto-firmados con extensiones ASN.1 de propósito general y firma de código (`KeyUsage`, `ExtendedKeyUsage`).
   - `Pkcs12KeystoreSerializer`: Empaquetado PKCS#12 (.jks, .keystore, .p12, .pfx) y persistencia segura en disco.
2. **Cálculo de Huellas y Metadatos**: `X509CertificateUtils` computa las huellas SHA-256, SHA-1, MD5 y exporta la clave pública a formato PEM estándar.

### 2. Respaldos y Restauración Modular (`SignetBackupManager` & `VaultRestorationCoordinator`)
1. **Creación de Respaldos**:
   - `ZipPackageBuilder` genera los paquetes ZIP individuales y de bóveda maestra.
   - `HmacSignatureEngine` calcula el hash SHA-256 y la firma HMAC-SHA256 anti-manipulación.
   - `SignetManifestParser` y `SignetVaultManifestParser` generan y firman los manifiestos JSON.
2. **Restauración Inteligente**:
   - `ZipPackageExtractor` analiza el archivo ZIP en memoria.
   - `BackupIntegrityVerifier` valida la firma HMAC y el hash SHA-256 de cada binario.
   - `VaultRestorationCoordinator` resuelve colisiones de nombres de archivo, desbloquea las claves y ensambla los registros `KeystoreDetails` para su persistencia en el repositorio.

### 3. Orquestación del ViewModel (`KeystoreViewModel` & Delegados)
1. **Desacoplamiento de Responsabilidades**:
   - `KeystoreFormDelegate`: Gestiona el estado de formulario, presets rápidos (Release, Upload, Codesign), validación de contraseñas y ciclo de vida de generación.
   - `SignApkDelegate`: Gestiona la selección de APK, análisis de firmas existentes, configuración de esquemas (v1, v2, v3), alineación Zipalign y ejecución del firmado.
   - `ApkMatcherDelegate`: Realiza el análisis forense de paquetes APK y el cruce de huellas digitales contra almacenes guardados o externos.
   - `AppUpdateDelegate`: Consulta GitHub Releases y gestiona la descarga progresiva de binarios de actualización.
2. **Compatibilidad Total**: `KeystoreViewModel` mantiene su API pública y `StateFlows` intactos hacia la capa de UI y tests unitarios.

### 4. Modularización de la Interfaz Principal (`MainScreen`)
1. **Navegación Adaptativa (`MainAdaptiveNavigation.kt`)**:
   - `SignetNavigationRail`: Panel lateral para pantallas anchas (Desktop / Tablet) con botón de acceso a la carpeta de bóveda.
   - `SignetNavigationBar`: Barra inferior compacta para dispositivos móviles.
   - `SignetTopAppBar`: Barra superior con títulos dinámicos adaptados a la plataforma y contexto.
2. **Conmutación de Contenido (`MainTabContent.kt`)**:
   - Enrutador que monta `GenerateScreen`, `SavedKeystoresScreen`, `SignApkScreen`, `InspectScreen` o `SettingsScreen`.
3. **Modales Globales (`MainGlobalSheets.kt`)**:
   - Host desacoplado para la hoja modal `KeystoreDetailsSheet` y el diálogo `UpdateDialog`.

### 5. Validación Forense de APK vs Keystore (APK Matcher)
1. **Extracción Multi-Esquema**:
   - `ApkSigningBlockParser`: Examina el bloque binario **APK Signing Block** (esquemas v2 y v3).
   - `ApkV1SignatureParser`: Extrae certificados PKCS#7 en `META-INF/*.RSA`, `META-INF/*.DSA` o `META-INF/*.EC`.
   - `AxmlManifestParser`: Extrae `packageName` y versiones desde el manifest binario.
2. **Diagnóstico**: `ApkMatcher` compara las huellas SHA-256 de los certificados del APK contra el keystore seleccionado y genera el dictamen forense.

### 6. Arquitectura Multiplataforma y Módulo de Escritorio (:desktop)
1. **Capa de Abstracción (`PlatformServices`)**:
   - `AndroidPlatformServices`: Manejo de SAF, FileProvider e Intents del sistema.
   - `DesktopPlatformServices`: AWT `FileDialog`, portapapeles del SO y explorador de archivos nativo.
2. **Módulo de Escritorio (`:desktop`)**:
   - Soporte nativo para CLI / Headless y modo gráfico mediante Compose Desktop.

### 7. Estrategia Multi-Canal y Versionado Semántico
- **`.debug` (`com.signet.app.debug` / `1.0.0-D`)**: Compilación interna y automática vía GitHub Actions (`build-debug-apk.yml`) firmada con `debug.keystore`.
- **`.dev` (`com.signet.app.dev` / `1.0.0-dev`)**: Pre-alpha para pruebas de funciones en desarrollo temprano firmadas por el desarrollador.
- **`.beta` (`com.signet.app.beta` / `1.0.0-B`)**: Versión beta con herramientas pulidas candidatas a definitivas.
- **`.estable` (`com.signet.app` / `1.0.0-E`)**: Versión definitiva de producción para tiendas de terceros (Uptodown, GitHub Releases).

### 8. Pipeline de Optimización, R8 Full Mode y Reducción de Huella en Disco
1. **Empaquetado Nativo sin Extracción (`useLegacyPackaging = false` / `extractNativeLibs = false`)**: Mapea directamente las librerías nativas `.so` (Room/SQLite y NDK) desde el archivo APK a memoria RAM mediante `mmap` en Android 6.0+ (API 23+), previniendo la duplicación física de archivos en `/data/app/` y reduciendo sustancialmente el espacio de almacenamiento consumido en el dispositivo tras la instalación.
2. **Filtrado de Recursos de Idioma (`resourceConfigurations`)**: Conservación exclusiva de localizaciones en español e inglés (`es`, `en`), eliminando recursos de traducción redundantes de dependencias AndroidX y Material 3.
3. **Distribución Separada para Móviles y Emuladores PC**:
   - `Signet-v*-release-signed.apk`: Artefacto principal para teléfonos con procesadores ARM de 64 bits (`arm64-v8a`) y 32 bits (`armeabi-v7a`).
   - `Signet-v*-emulator-x86-release-signed.apk`: Artefacto optimizado para emuladores de PC con arquitecturas `x86_64` y `x86`.
4. **R8 Full Mode (`gradle.properties`)**: `android.enableR8.fullMode=true` para inlining agresivo de lambdas de Compose, desvirtualización de clases y eliminación de bridges de Kotlin.
5. **Afinamiento ProGuard (`app/proguard-rules.pro`)**: Tree-shaking selectivo sobre BouncyCastle (`org.bouncycastle.*`) preservando instanciación de providers y suprimiendo clases no utilizadas.
6. **Descarte de Metadatos de Empaquetado (`app/build.gradle.kts`)**: Exclusión en `packaging.resources` de `.kotlin_module`, `DebugProbesKt.bin`, esquemas `.proto` y metadatos de versionado de Compose/AndroidX.

### 9. Hardening de Bytecode y Verificación de Firma en Runtime
1. **Aplanamiento de Paquetes (`-repackageclasses ''` / `-allowaccessmodification`)**: Elimina la jerarquía de paquetes en el DEX agrupando todas las clases en el paquete raíz para entorpecer el análisis estático.
2. **Supresión de Nombres de Variables (`Intrinsics`)**: Elimina las cadenas de texto de validación generadas por Kotlin en Release.
3. **Ocultación de Archivos Fuente (`-renamesourcefileattribute ""`)**: Suprime nombres de archivos `.kt` originales en el binario.
4. **Verificador de Integridad de Firma (`SignatureVerifier.kt`)**: Consulta `signingInfo` / certificados de firma en runtime para certificar la autenticidad del APK frente a clonaciones o redistribuciones maliciosas de terceros.


