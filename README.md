# 🔐 Keystore Generator (Android KeyStore & Certificate Tool)

Generador y administrador profesional de Keystores (`.jks` y `.keystore`) para Android en Jetpack Compose con arquitectura M3 y criptografía BouncyCastle. Permite crear, inspeccionar, exportar y convertir certificados a Base64 para CI/CD sin depender de Android Studio ni comandos manuales de terminal.

---

## 🚀 Características Principales

- **Generación Criptográfica Completa**:
  - Algoritmos soportados: **RSA 2048**, **RSA 4096** y **Curva Elíptica (EC P-256)** con firmas SHA-256.
  - Soporte de extensiones estándar: `.jks` (estándar moderno de Android Studio) y `.keystore` (formato tradicional).
  - Configuración detallada de Distinguished Name (CN, OU, O, L, ST, C) y período de validez en años.
  - Presets rápidos de 1 clic: *Google Play Release*, *Upload Key* y *Alta Seguridad*.

- **Exportación & CI/CD**:
  - Conversión instantánea a **Base64** para variables de entorno en pipelines de CI/CD (GitHub Actions `KEYSTORE_BASE64`, Fastlane, Bitrise, Codemagic).
  - Exportación de archivo binario al almacenamiento del dispositivo mediante Android SAF (Storage Access Framework).
  - Compartir archivo directamente mediante el menú de compartir de Android.

- **Inspección de Keystores Existentes**:
  - Lectura y extracción de certificados X.509 de archivos `.jks`, `.keystore` y `.p12`.
  - Cálculo instantáneo de huellas digitales (**SHA-256**, **SHA-1**, **MD5**) requeridas para Firebase, Google Sign-In, Facebook SDK y Maps API.
  - Visualización del certificado en formato estándar PEM (`-----BEGIN CERTIFICATE-----`).

- **Gestión Local Segura**:
  - Almacenamiento seguro en base de datos local **Room**.
  - Visualización y copia rápida de credenciales (Alias, Contraseña del Keystore, Contraseña de la Clave).

---

## 🛠️ Stack Tecnológico

| Capa | Tecnología |
|---|---|
| **Lenguaje** | Kotlin 2.0+ con Coroutines & StateFlow |
| **UI Framework** | Jetpack Compose con Material Design 3 |
| **Persistencia** | Room Database (SQLite local offline-first) |
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
El repositorio cuenta con un flujo en `.github/workflows/sync-zip.yml` que permite actualizar el código base subiendo archivos comprimidos (`.zip`, `.tar.gz`, etc.) a la carpeta `zip/`.
