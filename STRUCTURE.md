# 🏗️ Arquitectura y Estructura del Proyecto

Este documento describe la organización de carpetas, responsabilidades de cada módulo y flujo de datos dentro de **Keystore Generator**.

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
│   │   │   │   └── KeystoreGenerator.kt         # Motor criptográfico: X.509, BouncyCastle, hashes, PEM, Base64, Gradle snippets & GitHub Actions workflow
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
│   │   │       │   ├── KeystoreCard.kt          # Tarjeta visual para listas de keystores
│   │   │       │   └── KeystoreDetailsSheet.kt  # Hoja modal con Base64, huellas, exportación y visualizador interactivo de snippets Gradle & GitHub Actions
│   │   │       ├── screens/
│   │   │       │   ├── GenerateScreen.kt        # Pantalla de creación con Slider de validez interactivo
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
│           └── ExampleRobolectricTest.kt        # Tests unitarios con Robolectric para generación y validación
├── .github/
│   └── workflows/
│       ├── build-debug-apk.yml                  # Compilación manual (workflow_dispatch) de APK Debug con generación de clave en runner y caché de Gradle
│       └── sync-zip.yml                         # Automatización de sincronización desde zip
├── zip/                                         # Carpeta para archivos comprimidos
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
       ├──► [KeystoreGenerator] (Criptografía X.509, BouncyCastle, Hashes, Base64)
       │
       ├──► [SharedPreferences] (Persistencia de Modo de Pantalla y Paleta de Color)
       │
       ▼ (Persistencia)
[KeystoreRepository]
       │
       ▼
[AppDatabase / Room DAO] ◄──► [SQLite Local Storage]
```
