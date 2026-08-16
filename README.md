# 🔐 Signet (Android KeyStore & Certificate Tool)

Generador y administrador profesional de Keystores (`.jks` y `.keystore`) para Android en Jetpack Compose con arquitectura Material Design 3 y criptografía BouncyCastle. **Signet** permite crear, inspeccionar, exportar y convertir certificados a Base64 para CI/CD sin depender de Android Studio ni comandos manuales de terminal.

---

## 🚀 Características Principales

- **Generación Criptográfica Completa**:
  - Algoritmos soportados: **RSA 2048**, **RSA 4096** y **Curva Elíptica (EC P-256)** con firmas SHA-256.
  - Soporte de extensiones estándar: `.jks` (estándar moderno de Android Studio) y `.keystore` (formato tradicional).
  - **Generador de Contraseñas Ultra Seguras (CSPRNG Nativo)**: Motor aleatorio basado en `SecureRandom` con longitud seleccionable (16, 20, 24, 32 caracteres), inclusión obligatoria de mayúsculas, minúsculas, dígitos y símbolos seguros para Gradle/Terminal, acompañado de medidor en tiempo real de bits de entropía y nivel de seguridad.
  - **Validez de Certificado Personalizada & Deslizable**: Control interactivo mediante slider (1 a 100 años), indicador de días totales y atajos rápidos (1, 5, 10, 25, 30, 50, 100 años).
  - **Campos de Certificado con Guía de Identidad**: Distinción clara entre campos obligatorios por estándar Google/Android (`CN`, `O`) y opcionales (`OU`, `L`, `ST`, `C`), con aviso de soporte para datos inventados o seudónimos artísticos para preservar la privacidad.
  - Presets rápidos de 1 clic: *Google Play Release*, *Upload Key* y *Alta Seguridad*.

- **Personalización Visual & Pestaña de Configuración**:
  - **Material You**: Soporte completo para colores dinámicos sincronizados con el fondo de pantalla en Android 12+.
  - **Modos de Pantalla**: Modo Claro, Modo Oscuro y **Negro 100% AMOLED** (#000000 absoluto) para máxima eficiencia energética en pantallas OLED.
  - **Paletas de Color de Autor**: Azul Marino, Verde Esmeralda Terminal, Púrpura Profundo, Ámbar Cálido, Cian Tecnológico, Rojo Carmesí y Monocromo Minimalista.
  - Persistencia automática de preferencias visuales en el dispositivo.

- **Exportación, Respaldos ZIP & CI/CD**:
  - **Paquetes de Respaldo Portables (.zip) con Firma Anti-Manipulación**: Exporta un archivo ZIP completo que contiene el binario del keystore (`.jks`/`.keystore`), credenciales en texto plano (`credentials.txt`), archivo `key.properties` para Gradle, `base64.txt` para pipelines de CI/CD y un manifiesto firmado (`signet-manifest.json`).
  - **Mecanismo de Seguridad Criptográfica**: El manifiesto incluye una firma criptográfica HMAC-SHA256 y hash SHA-256 del binario. Si alguien altera las contraseñas, el alias o el archivo del keystore externamente, Signet rechazará automáticamente la restauración garantizando la integridad absoluta.
  - **Restauración Instantánea**: Permite restaurar el paquete ZIP completo con un solo toque incluso si la app fue desinstalada o reinstalada.
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
| **Criptografía** | BouncyCastle (SpongyCastle/BC) X.509 PKCS#12 JCA & CSPRNG SecureRandom |
| **Arquitectura** | MVVM (Model - View - ViewModel) + Clean Architecture Modular |
| **Testing** | Robolectric 4.14+ & JUnit 4 |

---

## 📦 Cómo Compilar y Ejecutar

### Requisitos Previos
- Android SDK con `minSdk = 26` y `targetSdk = 36`
- Java Development Kit (JDK 17)
- Gradle 8.13+ / 9.x compatible con Kotlin 2.0+

### Comandos de Terminal
```bash
# Compilar el proyecto en modo depuración
gradle assembleDebug

# Ejecutar la suite completa de tests unitarios y Robolectric
gradle :app:testDebugUnitTest
```

---

## 🤖 CI/CD y Envío Privado a Telegram

El repositorio cuenta con un pipeline de GitHub Actions (`.github/workflows/build-debug-apk.yml`) que compila, firma y envía automáticamente el APK de depuración directamente a tu cuenta privada de **Telegram** mediante el protocolo nativo MTProto con **Telethon** (soportando archivos de hasta **2 GB**):

- **Sin exposición pública**: No se guardan artefactos ni binarios en los registros públicos de GitHub.
- **Configuración de Secretos en GitHub**:
  - `TELEGRAM_BOT_TOKEN_DEBUG_APK`: Token del bot proporcionado por `@BotFather`.
  - `TELEGRAM_API_ID_DEBUG_APK`: ID de la aplicación de desarrollo en `my.telegram.org`.
  - `TELEGRAM_API_HASH_DEBUG_APK`: Hash de la aplicación en `my.telegram.org`.
  - `TELEGRAM_CHAT_ID_DEBUG_APK`: Tu identificador personal de Telegram.

---

## 📜 Licencia & Contribuciones

- **Licencia**: Este proyecto está publicado y licenciado bajo los términos de la **[GNU General Public License v3.0 (GPL v3)](LICENSE)**.
- **Contribuciones**: Consulta **[CONTRIBUTING.md](CONTRIBUTING.md)** para más detalles sobre las políticas actuales del repositorio (actualmente bajo desarrollo enfocado sin admisión de PRs de terceros).

