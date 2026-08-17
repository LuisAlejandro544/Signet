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
│   │   │   ├── MainActivity.kt                  # Punto de entrada de la actividad, Navigation y tema reactivo
│   │   │   ├── crypto/
│   │   │   │   ├── ApkMatcher.kt                # Fachada orquestadora forense APK vs Keystore (análisis y coincidencia)
│   │   │   │   ├── apk/
│   │   │   │   │   ├── ApkSigningBlockParser.kt # Parser binario de bajo nivel para esquemas v2 y v3 (APK Signing Block)
│   │   │   │   │   ├── ApkV1SignatureParser.kt  # Extractor de firmas PKCS#7 en META-INF mediante BouncyCastle CMS (v1 JAR)
│   │   │   │   │   └── AxmlManifestParser.kt    # Extractor de packageName desde el string pool binario de AndroidManifest.xml
│   │   │   │   ├── KeystoreGenerator.kt         # Motor criptográfico de generación de pares de claves y empaquetado PKCS#12
│   │   │   │   ├── PasswordGenerator.kt         # Generador criptográfico CSPRNG de contraseñas ultra seguras y cálculo de entropía
│   │   │   │   ├── PepkGenerator.kt             # Cifrado híbrido PEPK (RSA-OAEP + AES-GCM) para Google Play App Signing
│   │   │   │   ├── SignetBackupManager.kt       # Fachada orquestadora de exportación ZIP y restauración de respaldos
│   │   │   │   ├── backup/
│   │   │   │   │   ├── BackupTemplates.kt       # Generador de plantillas de texto (credentials.txt, key.properties, README-BACKUP.txt)
│   │   │   │   │   └── BackupIntegrityVerifier.kt # Cálculo de SHA-256, firma HMAC-SHA256 y verificación de integridad anti-tamper
│   │   │   │   ├── x509/
│   │   │   │   │   ├── X509CertificateInspector.kt # Inspección y lectura de certificados en almacenes PKCS12, JKS y BKS
│   │   │   │   │   └── X509CertificateUtils.kt    # Utilidades de cálculo de huellas digitales y formateo PEM estándar
│   │   │   │   └── SnippetGenerator.kt          # Generador modular de snippets: Gradle KTS, Groovy, GitHub Actions, apksigner & pepk
│   │   │   ├── data/
│   │   │   │   ├── KeystoreRepository.kt        # Repositorio que orquesta base de datos y operaciones
│   │   │   │   ├── local/
│   │   │   │   │   ├── AppDatabase.kt           # Definición de Room Database (v2) con migraciones
│   │   │   │   │   ├── KeystoreDao.kt            # Operaciones DAO (insert, query, delete)
│   │   │   │   │   └── KeystoreEntity.kt         # Entidad de persistencia de keystores con Base64 y credenciales
│   │   │   │   └── model/
│   │   │   │       └── KeystoreModels.kt        # Modelos de dominio (KeystoreConfig, KeystoreDetails, ApkInfo, ApkMatchResult, etc.)
│   │   │   └── ui/
│   │   │       ├── KeystoreViewModel.kt         # ViewModel central desacoplado: StateFlows, validaciones y orquestación
│   │   │       ├── MainScreen.kt                # Barra de navegación de 4 pestañas y contenedor de vistas
│   │   │       ├── components/
│   │   │       │   ├── KeystoreDetailsSheet.kt  # BottomSheet orquestador de exportación SAF, compartir y detalles
│   │   │       │   └── details/
│   │   │       │       ├── DetailsActionUtils.kt         # Utilidades de portapapeles y compartir mediante FileProvider
│   │   │       │       ├── KeystoreHeaderSection.kt      # Encabezado, alias, botones de exportar y compartir
│   │   │       │       ├── KeystoreCredentialsCard.kt    # Tarjeta de credenciales con visibilidad y copia en un toque
│   │   │       │       ├── KeystoreBase64Card.kt         # Visualizador de Base64 para variables de entorno y CI/CD
│   │   │       │       ├── KeystoreFingerprintsCard.kt   # Huellas SHA-256, SHA-1, MD5 y datos X.509
│   │   │       │       ├── KeystoreCodeSnippetsCard.kt   # Visor interactivo de código Gradle, Groovy, YAML, CLI y PEPK
│   │   │       │       └── PepkExportDialog.kt           # Diálogo de generación y exportación (.pepk individual o Bundle ZIP)
│   │   │       ├── preferences/
│   │   │       │   └── AppPreferencesManager.kt # Gestor modular de preferencias de usuario (tema, paleta y estado de bienvenida)
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
│   │   │       │   │   └── KeystoreDnFields.kt           # Formulario X.500 con distinción de obligatorios Google y opcionales
│   │   │       │   ├── InspectScreen.kt         # Orquestador con TabRow para inspección de archivos y validación APK
│   │   │       │   ├── inspect/
│   │   │       │   │   ├── KeystoreInspectorSection.kt   # Formulario de carga de archivo externo, contraseña y visor
│   │   │       │   │   ├── CertificateDetailsResultCard.kt # Tarjeta de visualización de certificados X.509 y huellas
│   │   │       │   │   └── ApkMatcherSection.kt          # Interfaz interactiva de validación forense APK vs Keystore
│   │   │       │   ├── SavedKeystoresScreen.kt  # Orquestador de historial de almacenes de claves guardados
│   │   │       │   ├── saved/
│   │   │       │   │   ├── SavedKeystoresHeader.kt       # Banner de restauración de respaldos ZIP
│   │   │       │   │   ├── KeystoreCardItem.kt           # Tarjeta de keystore con credenciales, huella SHA-256 y acciones
│   │   │       │   │   ├── EmptySavedKeystoresView.kt    # Vista vacía amigable con llamadas a la acción
│   │   │       │   │   └── SavedKeystoresDialogs.kt      # Diálogos de eliminación, progreso y errores de integridad
│   │   │       │   ├── SettingsScreen.kt        # Pantalla de configuración modular
│   │   │       │   └── settings/
│   │   │       │       ├── SettingsHeaderCard.kt         # Tarjeta de encabezado y resumen visual
│   │   │       │       ├── ThemeModeSection.kt           # Selector de temas (Claro, Oscuro, Negro 100% AMOLED, Sistema)
│   │   │       │       ├── ColorPaletteSection.kt        # Selector de paleta de acentos y Material You (Android 12+)
│   │   │       │       ├── LegalLinksSection.kt          # Accesos a Términos, Privacidad y guía de bienvenida
│   │   │       │       └── DistributionInfoSection.kt    # Resumen de criptografía offline, soporte de tiendas y GPL v3
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
│           │   ├── KeystoreGeneratorTest.kt     # Pruebas de generación RSA 2048, EC P256 e inspección
│           │   ├── PasswordGeneratorTest.kt     # Pruebas de contraseñas CSPRNG, cálculo de entropía y clasificación
│           │   ├── PepkGeneratorTest.kt         # Pruebas de cifrado híbrido PEPK y exportación en ZIP
│           │   ├── SignetBackupIntegrityTest.kt # Pruebas de integridad de respaldos ZIP y rechazo de manipulación HMAC
│           │   └── SnippetGeneratorTest.kt      # Pruebas de generación de código Gradle, Groovy, GitHub Actions y apksigner
│           └── ui/
│               └── KeystoreViewModelTest.kt     # Pruebas de estado de onboarding, endpoints legales y presets
├── Changelog-release.md                 # Historial detallado de notas de la versión y changelogs para releases
├── .github/
│   └── workflows/
│       ├── build-debug-apk.yml                  # Compilación y despacho confidencial de APK Debug (Canal .debug)
│       ├── build-release-apk.yml                # Compilación, R8 ProGuard, firma y publicación de APK Pre-Release (Canal .beta)
│       ├── security-scan.yml                    # Auditoría de seguridad estática en modo Stealth
│       ├── emulator-e2e-test.yml                # Pruebas E2E en emulador nativo Android KVM
│       ├── cli-interoperability-test.yml        # Validación cruzada CLI con keytool y Google apksigner
│       └── sync-zip.yml                         # Respaldo automático
└── web/                                         # Portal web estático (Astro 5 + Tailwind CSS para Cloudflare Pages)
    ├── src/
    │   ├── layouts/Layout.astro                 # Plantilla principal con metadata SEO y tema OLED
    │   ├── components/                          # Navbar, Hero, Features, SecurityPillars, DownloadSection, Footer
    │   └── pages/
    │       ├── index.astro                      # Página de inicio con descarga de APK e información del proyecto
    │       ├── privacy.astro                    # Política de privacidad (Cero recolección, offline, vigencia 16-08-2026)
    │       ├── terms.astro                      # Términos y condiciones detallados (13 secciones GPL v3, vigencia 16-08-2026)
    │       └── 404.astro                        # Página 404 personalizada
    ├── wrangler.toml                            # Configuración de despliegue en Cloudflare Pages
    └── astro.config.mjs                         # Configuración de Astro con integración Tailwind CSS
```

---

## 🔄 Flujo de Datos y Operaciones Clave

### 1. Validación Forense de APK vs Keystore (APK Matcher Modular)
1. **Selección del APK**: El usuario proporciona un archivo `.apk` mediante Android SAF.
2. **Extracción y Parsing Multi-Esquema**:
   - `ApkMatcher` coordina el análisis delegando en parsers especializados:
     * `ApkSigningBlockParser`: examina el bloque **APK Signing Block** (v2 y v3) buscando el ID `0x7109871a` y extrayendo los certificados X.509 de los signers.
     * `ApkV1SignatureParser`: examina los archivos PKCS#7 en `META-INF/*.RSA`, `META-INF/*.DSA` o `META-INF/*.EC` (v1 JAR Signature).
     * `AxmlManifestParser`: extrae metadatos del paquete (`packageName`, `versionName`, `versionCode`) desde el string pool binario.
3. **Cruce de Huellas Digitales**:
   - Se calculan las huellas SHA-256 de todos los certificados detectados en el APK y se contrastan con la huella del Keystore seleccionado (almacenado en Signet o externo).
4. **Diagnóstico**:
   - Se emite un dictamen visual inmediato: Coincidencia confirmada o Alerta de incompatibilidad de actualización.

### 2. Generación de Keystore
1. `GenerateScreen` recopila la configuración validada mediante sus submódulos (`GeneratePresetsSection`, `KeystoreCredentialsForm`, `KeystoreValiditySection`, `KeystoreDnFields`).
2. `KeystoreGenerator` genera el par de claves mediante BouncyCastle y construye el certificado X.509.
3. `X509CertificateUtils` calcula los hashes SHA-256, SHA-1, MD5 y formatea el certificado en PEM estándar.
4. `KeystoreRepository` persiste la entidad en la base de datos Room.

### 3. Inspección y Extracción de Certificados X.509
1. `X509CertificateInspector` procesa flujos de bytes de almacenes de claves soportando PKCS12, JKS y BKS.
2. Extrae cadenas de certificados X.509, fechas de validez, identificadores de emisor/sujeto y números de serie.
3. Utiliza `X509CertificateUtils` para el cálculo normalizado de huellas digitales.

### 4. Respaldos ZIP & Anti-Tampering Modular
1. `SignetBackupManager.createBackupZip`:
   - Delega la construcción de archivos textuales en `BackupTemplates` (`credentials.txt`, `key.properties`, `README-BACKUP.txt`).
   - Delega la generación y firma criptográfica HMAC-SHA256 del manifiesto `signet-backup.json` en `BackupIntegrityVerifier`.
2. `SignetBackupManager.restoreFromZip`:
   - Valida la integridad del archivo binario y del manifiesto contra la firma HMAC mediante `BackupIntegrityVerifier` antes de persistir o desbloquear.

### 5. Estrategia Multi-Canal y Versionado Semántico
- **`.debug` (`com.signet.app.debug` / `1.0.0-D`)**: Compilación interna y automática vía GitHub Actions (`build-debug-apk.yml`) firmada con `debug.keystore`.
- **`.dev` (`com.signet.app.dev` / `1.0.0.dev`)**: Pre-alpha para pruebas de funciones en desarrollo temprano firmadas por el desarrollador.
- **`.beta` (`com.signet.app.beta` / `1.0.0-B`)**: Versión beta con herramientas pulidas candidatas a definitivas.
- **`.estable` (`com.signet.app` / `1.0.0-E`)**: Versión definitiva de producción para tiendas de terceros (Uptodown, GitHub Releases).
