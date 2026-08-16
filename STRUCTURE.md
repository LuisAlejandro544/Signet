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
│   │   │   │   ├── KeystoreGenerator.kt         # Motor criptográfico: X.509, BouncyCastle, hashes, PEM y Base64
│   │   │   │   ├── PasswordGenerator.kt         # Generador criptográfico CSPRNG de contraseñas ultra seguras y cálculo de entropía
│   │   │   │   ├── SignetBackupManager.kt       # Gestor de paquetes ZIP: exportación, firma HMAC anti-manipulación y restauración
│   │   │   │   └── SnippetGenerator.kt          # Generador modular de snippets: Gradle KTS, Groovy, GitHub Actions & apksigner
│   │   │   ├── data/
│   │   │   │   ├── KeystoreRepository.kt        # Repositorio que orquesta base de datos y operaciones
│   │   │   │   ├── local/
│   │   │   │   │   ├── AppDatabase.kt           # Definición de Room Database (v2) con migraciones
│   │   │   │   │   ├── KeystoreDao.kt            # Operaciones DAO (insert, query, delete)
│   │   │   │   │   └── KeystoreEntity.kt         # Entidad de persistencia de keystores con Base64 y credenciales
│   │   │   │   └── model/
│   │   │   │       └── KeystoreModels.kt        # Modelos de dominio (KeystoreConfig, KeystoreDetails, etc.)
│   │   │   └── ui/
│   │   │       ├── KeystoreViewModel.kt         # ViewModel central: StateFlows, tema, validaciones y UI state
│   │   │       ├── MainScreen.kt                # Barra de navegación de 4 pestañas y contenedor de vistas
│   │   │       ├── components/
│   │   │       │   ├── KeystoreDetailsSheet.kt  # BottomSheet orquestador de exportación SAF, compartir y detalles
│   │   │       │   └── details/
│   │   │       │       ├── DetailsActionUtils.kt         # Utilidades de portapapeles y compartir mediante FileProvider
│   │   │       │       ├── KeystoreHeaderSection.kt      # Encabezado, alias, botones de exportar y compartir
│   │   │       │       ├── KeystoreCredentialsCard.kt    # Tarjeta de credenciales con visibilidad y copia en un toque
│   │   │       │       ├── KeystoreBase64Card.kt         # Visualizador de Base64 para variables de entorno y CI/CD
│   │   │       │       ├── KeystoreFingerprintsCard.kt   # Huellas SHA-256, SHA-1, MD5 y datos X.509
│   │   │       │       └── KeystoreCodeSnippetsCard.kt   # Visor interactivo de código Gradle, Groovy, YAML y CLI
│   │   │       ├── screens/
│   │   │       │   ├── GenerateScreen.kt        # Pantalla principal de generación
│   │   │       │   ├── generate/
│   │   │       │   │   ├── GeneratePresetsSection.kt     # Chips de plantillas rápidas (Release, Upload, RSA 4096)
│   │   │       │   │   ├── KeystoreCredentialsForm.kt    # Formato, contraseñas, medidor de entropía y generador CSPRNG
│   │   │       │   │   ├── KeystoreValiditySection.kt    # Slider interactivo de años, chips y selector de algoritmo
│   │   │       │   │   └── KeystoreDnFields.kt           # Formulario X.500 con distinción de obligatorios Google y opcionales
│   │   │       │   ├── InspectScreen.kt         # Pantalla de análisis e inspección de archivos externos
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
│           └── ExampleRobolectricTest.kt        # Tests unitarios con Robolectric para generación, contraseñas y validación
├── .github/
│   └── workflows/
│       ├── build-debug-apk.yml                  # Compilación manual (workflow_dispatch) de APK Debug con generación de clave en runner y caché de Gradle
│       ├── security-scan.yml                    # Auditoría de seguridad silenciosa (modo stealth) y envío de reporte JSON a Telegram
│       └── sync-zip.yml                         # Automatización de sincronización desde zip
├── web/                                         # Portal web oficial, Términos y Política de Privacidad (Astro + Tailwind para Cloudflare Pages)
│   ├── public/                                  # Activos estáticos (favicon.svg, robots.txt)
│   ├── src/
│   │   ├── components/                          # Componentes UI (Navbar, Footer, Hero, Features, SecurityPillars, DownloadSection)
│   │   ├── layouts/                             # Layout base (Layout.astro)
│   │   ├── pages/                               # Rutas y páginas (index.astro, privacy.astro, terms.astro, 404.astro)
│   │   └── styles/                              # Estilos globales y utilidades Tailwind (global.css)
│   ├── astro.config.mjs                         # Configuración de Astro con Tailwind y Cloudflare Pages
│   ├── tailwind.config.mjs                      # Paleta de colores Dark/OLED y tipografía
│   ├── wrangler.toml                            # Configuración de despliegue Cloudflare Pages
│   └── package.json                             # Dependencias del frontend estático
├── zip/                                         # Carpeta para archivos comprimidos
├── LICENSE                                      # Licencia GNU General Public License v3.0 (GPL v3)
├── CONTRIBUTING.md                             # Directrices de contribución y políticas del repositorio
├── commit_message.txt                           # Mensaje de commit predeterminado
└── metadata.json                                # Metadatos de la plataforma AI Studio
```

---

## 🔄 Flujo de Datos

```
[UI Composables] (GenerateScreen, SavedKeystoresScreen, InspectScreen, SettingsScreen)
       │
       ▼ (Eventos / Formularios / Tema)
[KeystoreViewModel] (MutableStateFlow / UI States / ThemeState)
       │
       ├──► [PasswordGenerator] (CSPRNG, Entropía Shannon, Caracteres Seguros Terminal/Gradle)
       │
       ├──► [KeystoreGenerator & SnippetGenerator] (Criptografía X.509, BouncyCastle, Hashes, Base64, Templates CI/CD)
       │
       ├──► [SharedPreferences] (Persistencia de Modo de Pantalla y Paleta de Color)
       │
       ▼ (Persistencia)
[KeystoreRepository]
       │
       ▼
[AppDatabase / Room DAO] ◄──► [SQLite Local Storage]
```
