# 🔐 Keystore Generator (Android KeyStore & Certificate Tool)

Generador y administrador profesional de Keystores (`.jks` y `.keystore`) para Android en Jetpack Compose con arquitectura Material Design 3 y criptografía BouncyCastle. Permite crear, inspeccionar, exportar y convertir certificados a Base64 para CI/CD sin depender de Android Studio ni comandos manuales de terminal.

---

## 🚀 Características Principales

- **Generación Criptográfica Completa**:
  - Algoritmos soportados: **RSA 2048**, **RSA 4096** y **Curva Elíptica (EC P-256)** con firmas SHA-256.
  - Soporte de extensiones estándar: `.jks` (estándar moderno de Android Studio) y `.keystore` (formato tradicional).
  - **Validez de Certificado Personalizada & Deslizable**: Control interactivo mediante slider (1 a 100 años), indicador de días totales y atajos rápidos (1, 5, 10, 25, 30, 50, 100 años).
  - Configuración detallada de Distinguished Name (CN, OU, O, L, ST, C) y validación de parámetros X.500.
  - Presets rápidos de 1 clic: *Google Play Release*, *Upload Key* y *Alta Seguridad*.

- **Personalización Visual & Pestaña de Configuración**:
  - **Material You**: Soporte completo para colores dinámicos sincronizados con el fondo de pantalla en Android 12+.
  - **Modos de Pantalla**: Modo Claro, Modo Oscuro y **Negro 100% AMOLED** (#000000 absoluto) para máxima eficiencia energética en pantallas OLED.
  - **Paletas de Color de Autor**: Azul Marino, Verde Esmeralda Terminal, Púrpura Profundo, Ámbar Cálido, Cian Tecnológico, Rojo Carmesí y Monocromo Minimalista.
  - Persistencia automática de preferencias visuales en el dispositivo.

- **Exportación, Snippets & CI/CD**:
  - **Visualizador Interactivo de Código Gradle**: Genera y previsualiza directamente bloques listos para `app/build.gradle.kts` (Kotlin DSL) y `build.gradle` (Groovy) con soporte de variables de entorno (`KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, etc.).
  - **Generador de Workflows para GitHub Actions**: Plantilla completa y optimizada `.github/workflows/android-build-and-sign.yml` para decodificar el keystore Base64 y firmar APKs de lanzamiento automáticamente en runners CI/CD.
  - Conversión instantánea a **Base64** para variables de entorno en pipelines de CI/CD (GitHub Actions `KEYSTORE_BASE64`, Fastlane, Bitrise, Codemagic).
  - Comandos CLI para **`apksigner`** y **`zipalign`** con esquemas de firma v1, v2 y v3.
  - Exportación de archivo binario al almacenamiento del dispositivo mediante Android SAF (Storage Access Framework).
  - Compartir archivo directamente mediante el menú nativo de compartir de Android.

- **Arquitectura Standalone & Cero Dependencias Innecesarias de Google Play**:
  - Aplicación 100% autónoma y offline-first sin servicios de Google Play innecesarios empaquetados.
  - Diseñada para distribución universal sin restricciones: Uptodown, F-Droid, GitHub Releases y tiendas de terceros.

- **Inspección de Keystores Existentes**:
  - Lectura y extracción de certificados X.509 de archivos `.jks`, `.keystore` y `.p12`.
  - Cálculo instantáneo de huellas digitales (**SHA-256**, **SHA-1**, **MD5**) requeridas para Firebase, Google Sign-In, Facebook SDK y Maps API.
  - Visualización del certificado en formato estándar PEM (`-----BEGIN CERTIFICATE-----`).

- **Gestión Local Segura**:
  - Almacenamiento seguro en base de datos local **Room** (100% offline, sin telemetría ni servidores externos).
  - Visualización y copia rápida de credenciales (Alias, Contraseña del Keystore, Contraseña de la Clave).

---

## 🛠️ Stack Tecnológico

| Capa | Tecnología |
|---|---|
| **Lenguaje** | Kotlin 2.0+ con Coroutines & StateFlow |
| **UI Framework** | Jetpack Compose con Material Design 3 & Material You |
| **Persistencia** | Room Database (SQLite local offline-first) & SharedPreferences |
| **Criptografía** | BouncyCastle (SpongyCastle/BC) X.509 PKCS#12 JCA |
| **Arquitectura** | MVVM (Model - View - ViewModel) + Clean Architecture |
| **Testing** | Robolectric 4.14+ & JUnit 4 |

---

## 📦 Cómo Compilar y Ejecutar

### Requisitos Previos
- Android Studio Ladybug / Koala o superior (o entorno Gradle compatible).
- JDK 17 o superior.
- Android SDK Min 26, Target 35.

### Comandos de Construcción
```bash
# Compilar en modo Debug
gradle assembleDebug

# Ejecutar la suite completa de tests unitarios
gradle :app:testDebugUnitTest
```

---

## 🧪 Estructura de Automatización (GitHub Actions)
- **`build-debug-apk.yml` (Manual / workflow_dispatch)**: Flujo de ejecución manual bajo demanda. Descarga todo el código fuente del repositorio (`actions/checkout@v4`), configura el entorno con JDK 17 y caché de Gradle (`setup-java` y `setup-gradle`), genera dinámicamente una clave criptográfica RSA 2048 directamente en el runner (`keytool`), compila el APK Debug (`./gradlew assembleDebug`) y publica el APK y el Keystore generado como artefactos descargables.
- **`sync-zip.yml`**: Flujo para actualizar el código base automáticamente subiendo archivos comprimidos (`.zip`, `.tar.gz`, etc.) a la carpeta `zip/`.
