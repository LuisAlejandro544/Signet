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
│   │   │   ├── MainActivity.kt                  # Punto de entrada de la actividad, Navigation y tema
│   │   │   ├── crypto/
│   │   │   │   └── KeystoreGenerator.kt         # Motor criptográfico: X.509, BouncyCastle, hashes, PEM y Base64
│   │   │   ├── data/
│   │   │   │   ├── KeystoreRepository.kt        # Repositorio que orquesta base de datos y operaciones
│   │   │   │   ├── local/
│   │   │   │   │   ├── AppDatabase.kt           # Definición de Room Database (v2) con migraciones
│   │   │   │   │   ├── KeystoreDao.kt            # Operaciones DAO (insert, query, delete)
│   │   │   │   │   └── KeystoreEntity.kt         # Entidad de persistencia de keystores con Base64 y credenciales
│   │   │   │   └── model/
│   │   │   │       └── KeystoreModels.kt        # Modelos de dominio (KeystoreConfig, KeystoreDetails, etc.)
│   │   │   └── ui/
│   │   │       ├── KeystoreViewModel.kt         # ViewModel central: StateFlows, validaciones y UI state
│   │   │       ├── components/
│   │   │       │   ├── KeystoreCard.kt          # Tarjeta visual para listas de keystores
│   │   │       │   └── KeystoreDetailsSheet.kt  # Hoja inferior modal con Base64, huellas y exportación
│   │   │       ├── screens/
│   │   │       │   ├── GenerateScreen.kt        # Pantalla de creación de claves (.jks / .keystore)
│   │   │       │   ├── InspectScreen.kt         # Pantalla de análisis e inspección de archivos externos
│   │   │       │   └── SavedKeystoresScreen.kt  # Pantalla de historial de keystores guardados
│   │   │       └── theme/
│   │   │           ├── Color.kt                 # Paleta de colores M3
│   │   │           ├── Theme.kt                 # Configuración de tema claro/oscuro dinámico
│   │   │           └── Type.kt                  # Tipografía Material 3
│   │   └── res/                                 # Recursos visuales, strings y drawables
│   └── test/
│       └── java/com/example/
│           └── ExampleRobolectricTest.kt        # Tests unitarios con Robolectric para generación y validación
├── .github/
│   └── workflows/
│       └── sync-zip.yml                         # Automatización de sincronización desde zip
├── zip/                                         # Carpeta para archivos comprimidos
├── commit_message.txt                           # Mensaje de commit predeterminado
└── metadata.json                                # Metadatos de la plataforma AI Studio
```

---

## 🔄 Flujo de Datos

```
[UI Composables] (GenerateScreen, KeystoreDetailsSheet)
       │
       ▼ (Eventos / Formularios)
[KeystoreViewModel] (MutableStateFlow / UI States)
       │
       ├──► [KeystoreGenerator] (Criptografía X.509, BouncyCastle, Hashes, Base64)
       │
       ▼ (Persistencia)
[KeystoreRepository]
       │
       ▼
[AppDatabase / Room DAO] ◄──► [SQLite Local Storage]
```
