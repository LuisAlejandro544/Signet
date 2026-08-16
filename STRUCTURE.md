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
│   │   │   │   ├── ApkMatcher.kt                # Validador forense de firmas APK vs Keystore (v1 JAR, v2/v3 Signing Block)
│   │   │   │   ├── KeystoreGenerator.kt         # Motor criptográfico: X.509, BouncyCastle, hashes, PEM y Base64
│   │   │   │   ├── PasswordGenerator.kt         # Generador criptográfico CSPRNG de contraseñas ultra seguras y cálculo de entropía
│   │   │   │   ├── PepkGenerator.kt             # Cifrado híbrido PEPK (RSA-OAEP + AES-GCM) para Google Play App Signing
│   │   │   │   ├── SignetBackupManager.kt       # Gestor de paquetes ZIP: exportación (.jks + .pepk bundle), firma HMAC anti-manipulación y restauración
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
│   │   │       ├── KeystoreViewModel.kt         # ViewModel central: StateFlows, tema, validaciones, APK Matcher y UI state
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
│   │   │       │       └── PepkExportDialog.kt           # Diálogo de generación y exportación (.pepk individual o Bundle ZIP con Keystore)
│   │   │       ├── screens/
│   │   │       │   ├── GenerateScreen.kt        # Pantalla principal de generación
│   │   │       │   ├── generate/
│   │   │       │   │   ├── GeneratePresetsSection.kt     # Chips de plantillas rápidas (Release, Upload, RSA 4096)
│   │   │       │   │   ├── KeystoreCredentialsForm.kt    # Formato, contraseñas, medidor de entropía y generador CSPRNG
│   │   │       │   │   ├── KeystoreValiditySection.kt    # Slider interactivo de años, chips y selector de algoritmo
│   │   │       │   │   └── KeystoreDnFields.kt           # Formulario X.500 con distinción de obligatorios Google y opcionales
│   │   │       │   ├── InspectScreen.kt         # Pantalla de análisis e inspección de archivos externos y subpestañas
│   │   │       │   ├── inspect/
│   │   │       │   │   └── ApkMatcherSection.kt     # Interfaz interactiva de validación forense APK vs Keystore
│   │   │       │   ├── SavedKeystoresScreen.kt  # Pantalla de historial de keystores guardados
│   │   │       │   └── SettingsScreen.kt        # Pantalla de configuración visual (Temas, Material You, OLED)
│   │   │       └── theme/
│   │   │           ├── Color.kt                 # Paleta de colores M3 (Emerald, Purple, Amber, Teal, Crimson, Mono)
│   │   │           ├── ThemeConfig.kt           # Enums ThemeMode (Claro, Oscuro, Negro 100%) y ColorPalette
│   │   │           ├── Theme.kt                 # Proveedor de temas M3 con soporte dinámico Android 12+ y OLED
│   │   │           └── Type.kt                  # Tipografía Material 3
│   │   └── res/                                 # Recursos visuales, strings y drawables
│   └── test/
│       └── java/com/example/
│           └── ExampleRobolectricTest.kt        # Suite de pruebas unitarias Robolectric (Keystore, ZIP restore, PEPK, APK Matcher)
├── .github/
│   └── workflows/
│       ├── build-debug-apk.yml                  # Compilación y despacho confidencial de APK Debug
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

### 1. Validación Forense de APK vs Keystore (APK Matcher)
1. **Selección del APK**: El usuario proporciona un archivo `.apk` mediante Android SAF.
2. **Extracción y Parsing Multi-Esquema**:
   - `ApkMatcher` examina primero el bloque **APK Signing Block** (v2 y v3) buscando el ID de bloque `0x7109871a` y extrayendo los certificados X.509 de los signers.
   - De forma concurrente o fallback, examina los archivos PKCS#7 en `META-INF/*.RSA`, `META-INF/*.DSA` o `META-INF/*.EC` (v1 JAR Signature).
   - Extrae metadatos del paquete (`packageName`, `versionName`, `versionCode`).
3. **Cruce de Huellas Digitales**:
   - Se calculan las huellas SHA-256 de todos los certificados detectados en el APK y se contrastan con la huella del Keystore seleccionado (almacenado en Signet o externo).
4. **Diagnóstico**:
   - Se emite un dictamen visual inmediato: Coincidencia confirmada o Alerta de incompatibilidad de actualización.

### 2. Generación de Keystore
1. `GenerateScreen` recopila la configuración validada.
2. `KeystoreGenerator` genera el par de claves mediante BouncyCastle y construye el certificado X.509.
3. Se calcula el hash SHA-256, SHA-1, MD5 y se codifica el binario en Base64.
4. `KeystoreRepository` persiste la entidad en la base de datos Room.

### 3. Respaldos ZIP & Anti-Tampering
1. `SignetBackupManager.createBackupZip` empaqueta el binario `.jks`/`.keystore`, credenciales, `key.properties`, `base64.txt` y genera el manifiesto `signet-backup.json` firmado con HMAC-SHA256.
2. `SignetBackupManager.restoreFromZip` valida exhaustivamente la integridad criptográfica antes de escribir o importar en Room.
