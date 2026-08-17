# 🔐 Signet (Android KeyStore & Certificate Tool)

Generador y administrador profesional de Keystores (`.jks` y `.keystore`) para Android en Jetpack Compose con arquitectura Material Design 3 y criptografía BouncyCastle. **Signet** permite crear, inspeccionar, validar coincidencia con APKs (APK Matcher), exportar y convertir certificados a Base64 para CI/CD sin depender de Android Studio ni comandos manuales de terminal.

---

## 🚀 Características Principales

- **Generación Criptográfica Completa**:
  - Algoritmos soportados: **RSA 2048**, **RSA 4096** y **Curva Elíptica (EC P-256)** con firmas SHA-256.
  - Soporte de extensiones estándar: `.jks` (estándar moderno de Android Studio) y `.keystore` (formato tradicional).
  - **Generador de Contraseñas Ultra Seguras (CSPRNG Nativo)**: Motor aleatorio basado en `SecureRandom` con longitud seleccionable (16, 20, 24, 32 caracteres), inclusión obligatoria de mayúsculas, minúsculas, dígitos y símbolos seguros para Gradle/Terminal, acompañado de medidor en tiempo real de bits de entropía y nivel de seguridad.
  - **Validez de Certificado Personalizada & Deslizable**: Control interactivo mediante slider (1 a 100 años), indicador de días totales y atajos rápidos (1, 5, 10, 25, 30, 50, 100 años).
  - **Campos de Certificado con Guía de Identidad**: Distinción clara entre campos obligatorios por estándar Google/Android (`CN`, `O`) y opcionales (`OU`, `L`, `ST`, `C`), con aviso de soporte para datos inventados o seudónimos artísticos para preservar la privacidad.
  - Presets rápidos de 1 clic: *Google Play Release*, *Upload Key* y *Alta Seguridad*.

- **Validador Forense APK vs Keystore (APK Matcher)**:
  - **Detección Multi-Esquema**: Inspecciona y extrae automáticamente los certificados X.509 de archivos `.apk` firmados con **Esquema v1 (JAR / PKCS#7 en `META-INF`)**, **Esquema v2 (APK Signing Block)** y **Esquema v3**.
  - **Lectura de Metadatos del APK**: Extrae el *Package Name*, *Version Name* y *Version Code* del paquete sin requerir herramientas externas.
  - **Cruce Criptográfico Determinista**: Compara las huellas digitales SHA-256 de los certificados contenidos en el APK contra un Keystore seleccionado (guardado en Signet o archivo externo `.jks`).
  - **Diagnóstico Preventivo de Actualización**: Alerta de forma inmediata si un APK podrá actualizarse con el Keystore o si causará un error `INSTALL_FAILED_UPDATE_INCOMPATIBLE` en dispositivos de usuarios finales.

- **Flujo de Bienvenida Interactivo & Onboarding**:
  - **Experiencia de Primera Ejecución**: Guía visual paso a paso de 4 pantallas que presenta de forma estructurada las capacidades de la aplicación (Generación RSA/EC y CSPRNG, Exportación PEPK/ZIP/CI-CD, Validador APK Matcher y Privacidad 100% Offline).
  - **Aceptación Explícita de Términos y Privacidad**: Bloque de consentimiento informado con enlaces directos al sitio oficial y verificación de aceptación para comenzar.
  - **Revisión Continua**: Opción disponible en la pestaña de Configuración para reabrir la guía explicativa en cualquier momento.

- **Personalización Visual & Pestaña de Configuración**:
  - **Material You**: Soporte completo para colores dinámicos sincronizados con el fondo de pantalla en Android 12+.
  - **Modos de Pantalla**: Modo Claro, Modo Oscuro y **Negro 100% AMOLED** (#000000 absoluto) para máxima eficiencia energética en pantallas OLED.
  - **Paletas de Color de Autor**: Azul Marino, Verde Esmeralda Terminal, Púrpura Profundo, Ámbar Cálido, Cian Tecnológico, Rojo Carmesí y Monocromo Minimalista.
  - **Acceso Directo al Portal Legal**: Enlaces directos para abrir en el navegador los [Términos y Condiciones](https://signet-web.luisalejandrososacamacho9.workers.dev/terms/) y la [Política de Privacidad](https://signet-web.luisalejandrososacamacho9.workers.dev/privacy/).
  - Persistencia automática de preferencias visuales y estado de aceptación en el dispositivo.

- **Exportación, Respaldos ZIP & CI/CD**:
  - **Generación y Exportación de Claves Cifradas para Google Play (.pepk)**:
    - Motor nativo de cifrado híbrido **PEPK (Play Encrypt Private Key)** mediante **RSA-OAEP (SHA-256) + AES-256-GCM** para transferir de forma 100% segura claves privadas a **Google Play App Signing**.
    - Soporte interactivo para cargar o pegar la clave pública `encryption_public_key.pem` de Google Play Console.
    - **Exportación Dual Flexible**: Opción para exportar el archivo binario `.pepk` de forma individual o empaquetar un **Bundle ZIP Completo** que contiene el Keystore original (`.jks`/`.keystore`), el archivo `.pepk` cifrado, credenciales, `key.properties`, `base64.txt` y manifiesto firmado.
    - Generador de comandos CLI oficiales para `pepk.jar` listos para copiar.
  - **Paquetes de Respaldo Portables (.zip) con Firma Anti-Manipulación**: Exporta un archivo ZIP completo que contiene el binario del keystore (`.jks`/`.keystore`), credenciales en texto plano (`credentials.txt`), archivo `key.properties` para Gradle, `base64.txt` para pipelines de CI/CD, instrucciones y un manifiesto firmado (`signet-backup.json`).
  - **Mecanismo de Seguridad Criptográfica**: El manifiesto incluye una firma criptográfica HMAC-SHA256 y hash SHA-256 del binario. Si alguien altera las contraseñas, el alias o el archivo del keystore externamente, Signet rechazará automáticamente la restauración garantizando la integridad absoluta.
  - **Restauración Instantánea**: Permite restaurar el paquete ZIP completo con un solo toque incluso si la app fue desinstalada o reinstalada.
  - **Visualizador Interactivo de Código Gradle**: Genera y previsualiza directamente bloques listos para `app/build.gradle.kts` (Kotlin DSL) y `build.gradle` (Groovy) con soporte de variables de entorno (`KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, etc.).
  - **Generador de Workflows para GitHub Actions**: Plantilla completa y optimizada `.github/workflows/android-build-and-sign.yml` para decodificar el keystore Base64 y firmar APKs de lanzamiento automáticamente en runners CI/CD.
  - Conversión instantánea a **Base64** para variables de entorno en pipelines de CI/CD (GitHub Actions `KEYSTORE_BASE64`, Fastlane, Bitrise, Codemagic).
  - Comandos CLI para **`apksigner`**, **`zipalign`** y **`pepk.jar`** con esquemas de firma v1, v2 y v3.
  - Exportación de archivo binario al almacenamiento del dispositivo mediante Android SAF (Storage Access Framework).
  - Compartir archivo directamente mediante el menú nativo de compartir de Android.

- **Arquitectura Standalone & Cero Dependencias Innecesarias de Google Play**:
  - Aplicación 100% autónoma y offline-first sin servicios de Google Play innecesarios empaquetados.
  - Diseñada para distribución universal sin restricciones: Uptodown, GitHub Releases y tiendas de terceros.

- **Inspección de Keystores Existentes**:
  - Lectura y extracción de certificados X.509 de archivos `.jks`, `.keystore` y `.p12`.
  - Cálculo instantáneo de huellas digitales (**SHA-256**, **SHA-1**, **MD5**) requeridas para Firebase, Google Sign-In, Facebook SDK y Maps API.
  - Visualización del certificado en formato estándar PEM (`-----BEGIN CERTIFICATE-----`).

- **Gestión Local Segura & Cero Recolección**:
  - Almacenamiento seguro en base de datos local **Room** (100% offline, sin telemetría, sin analíticas ni servidores externos).
  - Visualización y copia rápida de credenciales (Alias, Contraseña del Keystore, Contraseña de la Clave).
  - Garantía de Cero Recolección de Datos: ninguna clave, contraseña o huella digital sale jamás del silicio de tu dispositivo.

- **Portal Web Oficial, Términos y Privacidad (`web/`)**:
  - Sitio web estático de alto rendimiento desarrollado en **Astro 5 + Tailwind CSS** optimizado para **Cloudflare Pages**.
  - Documentación pública y páginas legales actualizadas al **16 de agosto de 2026**:
    - **Política de Privacidad (`/privacy`)**: Declaración formal de **Cero Recolección de Datos**, arquitectura 100% offline, almacenamiento local exclusivo en Room, ausencia total de telemetría/analítica y principio de privilegio mínimo con Android SAF.
    - **Términos y Condiciones (`/terms`)**: Marco legal exhaustivo de 13 secciones bajo la licencia **GNU GPL v3**, detallando la soberanía criptográfica absoluta del usuario, deberes de custodia y respaldo, seguridad de variables Base64 en CI/CD, carácter diagnóstico del APK Matcher, especificaciones de cifrado PEPK para Google Play, integridad anti-manipulación HMAC en respaldos ZIP, distribución en plataformas de terceros (Uptodown, GitHub Releases, APKs directos), exención de garantías ("AS IS") y limitaciones de responsabilidad.

---

## 🏷️ Canales de Lanzamiento, Identificadores y Nomenclatura de Versiones

Signet adopta un sistema estructurado de canales de desarrollo y distribución que permite la coexistencia simultánea de compilaciones en el mismo dispositivo y una gestión estricta del ciclo de vida del software:

| Canal | Sufijo / Package ID | Etiqueta de Versión | Firma y Origen | Propósito y Estabilidad |
|---|---|---|---|---|
| **Exclusivo Debug (`.debug`)** | `com.signet.app.debug` | `1.0.0-D` | Dinámica en runner (`debug.keystore`) | **Canal interno de desarrollo**: Funciones en pruebas privadas por el desarrollador antes de llegar a `.dev`. Generado exclusivamente por el workflow de GitHub Actions (`build-debug-apk.yml`). |
| **Pre-Alpha (`.dev`)** | `com.signet.app.dev` | `1.0.0.dev` | Firma personalizada (GitHub Secrets) | **Canal de innovación activa**: Nuevas herramientas en desarrollo temprano. Pensada para evaluación comunitaria; puede ser inestable y las funciones pueden variar o eliminarse. |
| **Beta (`.beta`)** | `com.signet.app.beta` | `1.0.0-B` | Firma personalizada (GitHub Secrets) | **Canal de consolidación**: Funciones y herramientas ya pulidas y candidatas a definitivas. Evaluación de estabilidad previa al lanzamiento público final. |
| **Estable (`.estable`)** | `com.signet.app` / `com.signet.app.estable` | `1.0.0-E` | Firma de producción (GitHub Secrets) | **Canal definitivo de producción**: Versión final 100% pulida, optimizada y libre de errores críticos para tiendas de terceros (Uptodown, GitHub Releases). |

> **Nota Criptográfica sobre Firmas CI/CD**: Las claves de firma para los canales `.dev`, `.beta` y `.estable` se administran de forma soberana a través de **GitHub Secrets**, garantizando que los binarios públicos mantengan trazabilidad y seguridad inquebrantable.

---

## 🛠️ Stack Tecnológico

| Capa | Tecnología |
|---|---|
| **Lenguaje Android** | Kotlin 2.0+ con Coroutines & StateFlow |
| **UI Framework Android** | Jetpack Compose con Material Design 3 & Material You |
| **Persistencia** | Room Database (SQLite local offline-first) & SharedPreferences |
| **Criptografía** | BouncyCastle (SpongyCastle/BC) X.509 PKCS#12 JCA & CSPRNG SecureRandom |
| **Análisis de APKs** | Parser nativo de APK Signing Block (v2/v3) & CMS PKCS#7 (v1) |
| **Arquitectura** | MVVM (Model - View - ViewModel) + Clean Architecture Modular |
| **Testing** | Robolectric 4.14+ & JUnit 4 |
| **Web & Legal (Cloudflare Pages)** | Astro 5, Tailwind CSS, TypeScript & Static HTML5 |

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

## 🤖 CI/CD, Seguridad y Envío Privado a Telegram

El repositorio cuenta con pipelines automatizados en GitHub Actions orientados a la privacidad y seguridad:

### 1. Compilación y Envío de APK Debug (`.github/workflows/build-debug-apk.yml`)
- Compila, firma y envía automáticamente el APK de depuración directamente a tu cuenta privada de **Telegram** mediante el protocolo nativo MTProto con **Telethon** (soportando archivos de hasta **2 GB**).
- **Sin exposición pública**: No se guardan binarios ni artefactos en los registros públicos de GitHub.
- **Secretos en GitHub**:
  - `TELEGRAM_BOT_TOKEN_DEBUG_APK`: Token del bot proporcionado por `@BotFather`.
  - `TELEGRAM_API_ID_DEBUG_APK`: ID de la aplicación de desarrollo en `my.telegram.org`.
  - `TELEGRAM_API_HASH_DEBUG_APK`: Hash de la aplicación en `my.telegram.org`.
  - `TELEGRAM_CHAT_ID_DEBUG_APK`: Tu identificador personal de Telegram.

### 2. Auditoría Silenciosa de Vulnerabilidades (`.github/workflows/security-scan.yml`)
- Activación **estrictamente manual (`workflow_dispatch`)** y ejecución en modo **Stealth (cero fugas en los logs del runner)** de seguridad en código Kotlin, Manifest, criptografía BouncyCastle, secretos y dependencias.
- Genera un reporte exhaustivo estructurado en `vulnerabilities-report.json` y lo despacha de forma privada a **Telegram** con un resumen ejecutivo en texto y el archivo `.json` descargable listo para alimentar directamente al asistente de IA.
- **Secretos en GitHub (con fallback automático a los secretos de Debug APK)**:
  - `TELEGRAM_BOT_TOKEN_SECURITY_SCAN`: Token del bot para auditorías de seguridad.
  - `TELEGRAM_CHAT_ID_SECURITY_SCAN`: ID de chat de Telegram para recibir los reportes.
  - `TELEGRAM_API_ID_SECURITY_SCAN` / `TELEGRAM_API_HASH_SECURITY_SCAN`: Credenciales de Telegram MTProto (opcionales).
  - *Nota*: Si no se configuran específicamente, el flujo reutiliza automáticamente `TELEGRAM_BOT_TOKEN_DEBUG_APK` y `TELEGRAM_CHAT_ID_DEBUG_APK`.

### 3. Pruebas E2E en Emulador Android Real KVM (`.github/workflows/emulator-e2e-test.yml`)
- **Ejecución en Emulador Real**: Levanta un emulador Android oficial con aceleración por hardware KVM (`reactivecircus/android-emulator-runner`) en GitHub Actions (API 34 / Pixel 6).
- **Validación Criptográfica & Anti-Tampering de ZIP**:
  - Prueba la importación de respaldos ZIP legítimos generados por la app verificando la concordancia de la firma HMAC-SHA256.
  - Evalúa la inyección de paquetes ZIP adulterados (tampered) comprobando que el motor de seguridad bloquea y rechaza inmediatamente cualquier archivo manipulado.
- **Reporte JSON & Evidencia Visual**: Genera `emulator-e2e-report.json`, captura de pantalla (`emulator_screenshot.png`) y despacha los resultados a **Telegram** con enmascaramiento estricto de credenciales en logs (`::add-mask::`).
- **Secretos en GitHub (reutilizables automáticamente)**:
  - `TELEGRAM_BOT_TOKEN_E2E_EMULATOR` (o fallback a `TELEGRAM_BOT_TOKEN_DEBUG_APK`)
  - `TELEGRAM_CHAT_ID_E2E_EMULATOR` (o fallback a `TELEGRAM_CHAT_ID_DEBUG_APK`)
  - `TELEGRAM_API_ID_E2E_EMULATOR` (o fallback a `TELEGRAM_API_ID_DEBUG_APK`)
  - `TELEGRAM_API_HASH_E2E_EMULATOR` (o fallback a `TELEGRAM_API_HASH_DEBUG_APK`)

### 4. Validación Cruzada con Herramientas Oficiales CLI (`.github/workflows/cli-interoperability-test.yml`)
- **Interoperabilidad Total con Terminal**: Valida que los Keystores y firmas generados por Signet sean 100% compatibles e intercambiables con `keytool` (Oracle) y `apksigner` (Android SDK / Google).
- **Firma Real y Verificación de Esquemas**: Firma un APK real con el keystore y valida exhaustivamente los esquemas de firma de Android **v1 (JAR signing), v2 (APK Signature Scheme v2) y v3**.
- **Reporte JSON & Despacho a Telegram**: Genera `cli-interop-report.json` con desglose paso a paso y lo despacha de forma confidencial a Telegram.
- **Secretos en GitHub**: Soporta secretos dedicados `TELEGRAM_BOT_TOKEN_CLI_INTEROP` / `TELEGRAM_CHAT_ID_CLI_INTEROP` o fallback automático a los secretos principales.

### 5. Compilación y Firma de APK Release / Pre-Release (`.github/workflows/build-release-apk.yml`)
- **Pipeline Automatizado de Lanzamiento Beta**: Se activa automáticamente al publicar o crear un Pre-Release en GitHub, al empujar tags semánticos (ej. `v1.0.0-B`, `v*`), o de forma manual con `workflow_dispatch`.
- **Optimización R8 / ProGuard**: Ofusca y comprime recursos (`isMinifyEnabled = true`, `isShrinkResources = true`) preservando BouncyCastle, Room y componentes de presentación.
- **Firma Criptográfica Segura**: Decodifica el almacén JKS/PKCS12 a partir de secretos en Base64 e inyecta las credenciales de firma en tiempo de compilación.
- **Publicación Automática**: Adjunta el binario `Signet-v*-release-signed.apk` junto a su checksum `SHA-256` en los assets del Pre-Release de GitHub y como artefacto del workflow.
- **Secretos en GitHub Requeridos para el Canal Beta**:
  - `KEYSTORE_BETA_BASE64` (o `KEYSTORE_BASE64`): Contenido en Base64 del archivo Keystore de firma Beta (generado por la propia app Signet).
  - `KEYSTORE_BETA_PASSWORD` (o `KEYSTORE_PASSWORD`): Contraseña maestra del archivo Keystore Beta.
  - `KEY_ALIAS_BETA` (o `KEY_ALIAS`): Alias de la clave de firma (por defecto: `signet-beta`).
  - `KEY_PASSWORD_BETA` (o `KEY_PASSWORD`): Contraseña específica del alias Beta.
- **Registro de Versiones**: Consulta [Changelog-release.md](Changelog-release.md) para el historial detallado de notas de lanzamiento.

---

## 📜 Licencia & Contribuciones

- **Licencia**: Este proyecto está publicado y licenciado bajo los términos de la **[GNU General Public License v3.0 (GPL v3)](LICENSE)**.
- **Contribuciones**: Consulta **[CONTRIBUTING.md](CONTRIBUTING.md)** para más detalles sobre las políticas actuales del repositorio (actualmente bajo desarrollo enfocado sin admisión de PRs de terceros).
