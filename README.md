# 🔐 Signet (Android KeyStore & Certificate Tool)

Generador y administrador profesional de Keystores (`.jks` y `.keystore`) para Android en Jetpack Compose con arquitectura Material Design 3 y criptografía BouncyCastle. **Signet** permite crear, inspeccionar, validar coincidencia con APKs (APK Matcher), exportar y convertir certificados a Base64 para CI/CD sin depender de Android Studio ni comandos manuales de terminal.

---

## 🚀 Características Principales

- **Generación Criptográfica Completa**:
  - Algoritmos soportados: **RSA 2048**, **RSA 4096** y **Curva Elíptica (EC P-256)** con firmas SHA-256.
  - Soporte de formatos universales: **`.jks`** (estándar de Android Studio), **`.keystore`** (formato tradicional), **`.p12`** (PKCS#12 Multiplataforma / iOS / Java) y **`.pfx`** (Microsoft Authenticode / Windows / PC).
  - **Extensiones X.509 de Firma de Código (Code Signing)**: Incorpora automáticamente extensiones estándar (`KeyUsage` con `digitalSignature` / `keyEncipherment` y `ExtendedKeyUsage` con `id_kp_codeSigning`) para permitir la firma nativa de ejecutables de Windows (`.exe`, `.msi`, `.dll`), instaladores y binarios móviles sin rechazos del sistema operativo.
  - **Modo Efímero / Sin Rastro (Zero-Footprint Mode)**: Opción para generar el keystore exclusivamente en memoria RAM sin guardarlo en la base de datos de la app ni en el almacenamiento interno. Permite exportar el archivo `.jks`/`.pfx`/`.p12`, descargar el ZIP de respaldo o copiar el Base64, y al cerrar la hoja o la app, desaparece por completo sin dejar rastro en el dispositivo.
  - **Generación Aleatoria por Campo & Perfil de Prueba Rápida (CSPRNG)**:
    * Botones interactivos de dado (`Casino`) en el trailing icon de cada campo (`CN`, `O`, `OU`, `Ciudad`, `Estado`, `Código de País`, `Nombre de Archivo`, `Alias`) para generar valores realistas y válidos al instante con un solo toque.
    * Botón de acción rápida **"Autocompletar Identidad Aleatoria"** para rellenar automáticamente todos los campos del certificado X.500 de forma coherente.
    * Preset **"Prueba Rápida Completa 🎲"** que autocompleta el formulario completo (archivo, contraseñas de 20 caracteres ultra seguras, alias, validez e identidad DN) ideal para prototipos ágiles y entornos de pruebas sin fricción.
  - **Generador de Contraseñas Ultra Seguras (CSPRNG Nativo)**: Motor aleatorio basado en `SecureRandom` con longitud seleccionable (16, 20, 24, 32 caracteres), inclusión obligatoria de mayúsculas, minúsculas, dígitos y símbolos seguros para Gradle/Terminal, acompañado de medidor en tiempo real de bits de entropía y nivel de seguridad.
  - **Validez de Certificado Personalizada & Deslizable**: Control interactivo mediante slider (1 a 100 años), indicador de días totales y atajos rápidos (1, 5, 10, 25, 30, 50, 100 años).
  - **Campos de Certificado con Guía de Identidad**: Distinción clara entre campos obligatorios por estándar Google/Android (`CN`, `O`) y opcionales (`OU`, `L`, `ST`, `C`), con aviso de soporte para datos inventados o seudónimos artísticos para preservar la privacidad.
  - Presets rápidos de 1 clic: *Prueba Rápida Completa*, *Google Play Release*, *Windows (.pfx)*, *Multiplataforma (.p12)*, *Upload Key* y *Alta Seguridad*.

- **Firmador Profesional de APKs en el Dispositivo (APK Signer & Zipalign)**:
  - **Firma Multi-Esquema Nativa (v1, v2 y v3)**: Implementación criptográfica completa en el dispositivo de **Esquema v1 (JAR Signing con `META-INF/MANIFEST.MF`, `CERT.SF` y bloque PKCS#7 `CERT.RSA/EC`)**, **Esquema v2 (APK Signature Scheme v2 con APK Signing Block inyectado antes del Central Directory)** y **Esquema v3 (APK Signature Scheme v3 con ID `0xf05368c0`, soporte de rotación de claves criptográficas y compatibilidad nativa para Android 9.0+)**.
  - **Motor Nativo de Zipalign (Alineación a 4 Bytes)**: Alinea automáticamente todas las entradas `STORED` (sin comprimir) a límites múltiplos de 4 bytes antes de firmar, garantizando optimización de memoria `mmap` y compatibilidad universal con los instaladores de Android.
  - **Compatibilidad con Claves de la Bóveda y Externas**: Permite firmar aplicaciones utilizando cualquier Keystore guardado en la app (con desbloqueo transparente y descifrado AES-256-GCM) o importando archivos `.jks`/`.keystore`/`.p12` externos del almacenamiento.
  - **Instalación y Distribución Inmediata**: Botón de un toque para instalar directamente el APK recién firmado en el dispositivo móvil vía `Intent.ACTION_INSTALL_PACKAGE`, compartir mediante el menú nativo de Android o transferir directamente a la pestaña de Inspección (APK Matcher) para validación cruzada.
  - **Opciones de Salida Personalizables**: Configuración granular de nombre de archivo firmado (`app-release-signed.apk`), selección independiente de esquemas de firma v1, v2 y v3, y activación/desactivación de zipalign.

- **Validador Forense APK vs Keystore (APK Matcher)**:
  - **Detección Multi-Esquema**: Inspecciona y extrae automáticamente los certificados X.509 de archivos `.apk` firmados con **Esquema v1 (JAR / PKCS#7 en `META-INF`)**, **Esquema v2 (APK Signing Block)** y **Esquema v3**.
  - **Lectura de Metadatos del APK**: Extrae el *Package Name*, *Version Name* y *Version Code* del paquete sin requerir herramientas externas.
  - **Cruce Criptográfico Determinista**: Compara las huellas digitales SHA-256 de los certificados contenidos en el APK contra un Keystore seleccionado (guardado en Signet o archivo externo `.jks`).
  - **Diagnóstico Preventivo de Actualización**: Alerta de forma inmediata si un APK podrá actualizarse con el Keystore o si causará un error `INSTALL_FAILED_UPDATE_INCOMPATIBLE` en dispositivos de usuarios finales.

- **Flujo de Bienvenida Interactivo & Onboarding**:
  - **Experiencia de Primera Ejecución**: Guía visual paso a paso de 4 pantallas que presenta de forma estructurada las capacidades de la aplicación (Generación RSA/EC y CSPRNG, Respaldos ZIP Anti-Manipulación y CI/CD, Validador APK Matcher y Privacidad 100% Offline).
  - **Aceptación Explícita de Términos y Privacidad**: Bloque de consentimiento informado con enlaces directos al sitio oficial y verificación de aceptación para comenzar.
  - **Revisión Continua**: Opción disponible en la pestaña de Configuración para reabrir la guía explicativa en cualquier momento.

- **Personalización Visual & Pestaña de Configuración**:
  - **Material You**: Soporte completo para colores dinámicos sincronizados con el fondo de pantalla en Android 12+.
  - **Modos de Pantalla**: Modo Claro, Modo Oscuro y **Negro 100% AMOLED** (#000000 absoluto) para máxima eficiencia energética en pantallas OLED.
  - **Paletas de Color de Autor**: Azul Marino, Verde Esmeralda Terminal, Púrpura Profundo, Ámbar Cálido, Cian Tecnológico, Rojo Carmesí y Monocromo Minimalista.
  - **Acceso Directo al Portal Legal**: Enlaces directos para abrir en el navegador los [Términos y Condiciones](https://signet-web.luisalejandrososacamacho9.workers.dev/terms/) y la [Política de Privacidad](https://signet-web.luisalejandrososacamacho9.workers.dev/privacy/).
  - Persistencia automática de preferencias visuales y estado de aceptación en el dispositivo.

- **Exportación, Respaldos ZIP, Bóveda Completa & CI/CD**:
  - **Bóveda Completa Multi-Keystore (.zip) en Subcarpetas**: Permite exportar absolutamente todos los Keystores almacenados en una sola bóveda comprimida `.zip`. Cada clave se empaqueta en su propia subcarpeta aislada (`keystores/1_alias/`, `keystores/2_alias/`, etc.) acompañada de su binario, `credentials.txt`, `key.properties`, `base64.txt`, `README-BACKUP.txt` y manifiesto individual.
  - **Manifiesto Maestro Anti-Manipulación (`signet-vault-backup.json`) e Inventario (`VAULT-SUMMARY.txt`)**: La raíz del archivo ZIP incluye un manifiesto global firmado criptográficamente con HMAC-SHA256 que vincula las huellas SHA-256 de todos los binarios, previniendo cualquier alteración externa o inyección de archivos no autorizados.
  - **Paquetes de Respaldo Portables Individuales (.zip) con Firma Anti-Manipulación**: Exporta un archivo ZIP completo que contiene el binario del keystore (`.jks`/`.keystore`), credenciales en texto plano (`credentials.txt`), archivo `key.properties` para Gradle, `base64.txt` para pipelines de CI/CD, instrucciones y un manifiesto firmado (`signet-backup.json`).
  - **Mecanismo de Seguridad Criptográfica**: El manifiesto incluye una firma criptográfica HMAC-SHA256 y hash SHA-256 del binario. Si alguien altera las contraseñas, el alias o el archivo del keystore externamente, Signet rechazará automáticamente la restauración garantizando la integridad absoluta.
  - **Restauración Inteligente de Bóvedas y Respaldos**: Detecta y restaura automáticamente con un solo toque tanto paquetes individuales como bóvedas maestras completas con múltiples keystores.
  - **Visualizador Interactivo de Código Gradle & Scripts**: Genera y previsualiza directamente bloques listos para `app/build.gradle.kts` (Kotlin DSL), `build.gradle` (Groovy), comandos **Microsoft SignTool (Windows Authenticode)**, **PowerShell** y **OpenSSL**.
  - **Generador de Workflows para GitHub Actions**: Plantilla completa y optimizada `.github/workflows/android-build-and-sign.yml` para decodificar el keystore Base64 y firmar APKs de lanzamiento automáticamente en runners CI/CD.
  - Conversión instantánea a **Base64** para variables de entorno en pipelines de CI/CD (GitHub Actions `KEYSTORE_BASE64`, Fastlane, Bitrise, Codemagic).
  - Comandos CLI para **`apksigner`**, **`signtool`**, **`zipalign`** y **`openssl`** con marcas de tiempo y esquemas de firma v1, v2 y v3.
  - Exportación de archivo binario al almacenamiento del dispositivo mediante Android SAF (Storage Access Framework).
  - Compartir archivo directamente mediante el menú nativo de compartir de Android.

- **Arquitectura Standalone & Cero Dependencias Innecesarias de Google Play**:
  - Aplicación 100% autónoma y offline-first sin servicios de Google Play innecesarios empaquetados.
  - Diseñada para distribución universal sin restricciones: Uptodown, GitHub Releases y tiendas de terceros.

- **Inspección de Keystores Existentes**:
  - Lectura y extracción de certificados X.509 de archivos `.jks`, `.keystore` y `.p12`.
  - Cálculo instantáneo de huellas digitales (**SHA-256**, **SHA-1**, **MD5**) requeridas para Firebase, Google Sign-In, Facebook SDK y Maps API.
  - Visualización del certificado en formato estándar PEM (`-----BEGIN CERTIFICATE-----`).

- **Gestión Local Segura, Búsqueda Inteligente & Cifrado en Reposo (Android KeyStore + AES-256-GCM)**:
  - **Búsqueda Instantánea con Lupa**: Barra de búsqueda interactiva en la pantalla de claves guardadas para filtrar en tiempo real por nombre de archivo, alias, campos Distinguished Name (CN, OU, O), algoritmo criptográfico o huellas digitales SHA-256/SHA-1/MD5.
  - **Filtros y Modos de Ordenamiento Avanzados**: Selector rápido mediante chips de Material 3 con cuatro criterios de organización:
    * **Más nuevos**: Prioriza los keystores creados o importados más recientemente.
    * **Más viejos**: Muestra primero los keystores históricos o iniciales.
    * **Intermedio**: Agrupa y prioriza aquellos con fechas medias de creación para facilitar exploraciones balanceadas.
    * **Recién vistos**: Registro dinámico de consulta que sitúa al inicio las claves abiertas o consultadas recientemente por el usuario.
  - **Cifrado Automático en Importación y Creación**: Tanto al generar un nuevo keystore como al importar respaldos individuales o restaurar una bóveda completa (.zip), todas las contraseñas se cifran de forma transparente en la base de datos Room usando **AES-256-GCM** protegido por **Android KeyStore** (`enc:v1:`). El usuario accede y copia sus credenciales con normalidad desde la UI, pero el almacenamiento físico en SQLite queda 100% protegido contra accesos indebidos de apps maliciosas o usuarios con acceso root.
  - **Protección del Portapapeles (Android 13+ / API 33+)**: Integración de la bandera del sistema `ClipDescription.EXTRA_IS_SENSITIVE` al copiar contraseñas y binarios Base64 para prevenir previsualizaciones flotantes no deseadas, acompañado de un aviso contextual preventivo sobre acceso de terceros al portapapeles.
  - Almacenamiento seguro en base de datos local **Room** (100% offline, sin telemetría, sin analíticas ni servidores externos).
  - Visualización y copia rápida de credenciales (Alias, Contraseña del Keystore, Contraseña de la Clave).
  - Garantía de Cero Recolección de Datos: ninguna clave, contraseña o huella digital sale jamás del silicio de tu dispositivo.

- **🖥️ Arquitectura Multiplataforma & Soporte para Windows / Desktop**:
  - **Módulo de Escritorio Nativo (`:desktop`)**: Vinculado en `settings.gradle.kts` e integrado con Compose Multiplatform en `desktop/build.gradle.kts` para empaquetado nativo en Windows (`.exe` / `.msi` vía `jpackage`), ejecución JVM y tarea `runDesktop`.
  - **Estructura del Módulo Compartido (KMP / Shared UI)**:
    * Capa de presentación compartida en Jetpack Compose / Compose Multiplatform desacoplada de dependencias exclusivas de Android.
    * Catálogo centralizado de recursos y cadenas de texto en `SignetStrings` (`com.example.ui.res.SignetStrings`) eliminando dependencias de `android.R` y `stringResource`.
    * Inyección desacoplada de servicios de sistema mediante `LocalPlatformServices` en todas las pantallas (`GenerateScreen`, `SignApkScreen`, `LegalLinksSection`, `SettingsScreen`).
    * Orquestación de estado desacoplada en `KeystoreViewModel` con flujos reactivos puros `StateFlow`.
    * Núcleo criptográfico común de alta seguridad (`com.example.crypto`) respaldado por BouncyCastle puro.
    * Compatibilidad nativa en arquitecturas x86_64 y ARM64 (Windows on ARM / Snapdragon X / Apple Silicon / Linux aarch64).
  - **Punto de Entrada & Bucle de Ventana (`DesktopLauncher`)**: `Main.kt` con inicialización gráfica AWT/Swing/Compose, detección headless, compatibilidad HiDPI y comandos CLI (`--open-vault`, `--help`, `--version`).
  - **Ergonomía de Interfaz y UX Adaptativo**: `MainScreen` responsivo que detecta el ancho de pantalla (`maxWidth >= 700.dp`) para desplegar un `NavigationRail` vertical con acceso rápido al explorador de archivos nativo en escritorio, y una barra inferior `NavigationBar` en móviles.

  - **Capa de Abstracción de Plataforma (`PlatformServices`)**:
    * `DesktopPlatformServices`: Selección de archivos con `FileDialog` nativo de Windows/Desktop, portapapeles del sistema y apertura de la carpeta de la bóveda en el Explorador de Windows.
    * `AndroidPlatformServices`: Storage Access Framework (SAF), `FileProvider` y menús nativos de compartir de Android.
  - **Desacoplamiento de las 4 Librerías Exclusivas de Android**:
    * `android.util.Base64` sustituido universalmente por `Base64Compat` (respaldado nativamente por `java.util.Base64`).
    * `AndroidKeyStore` adaptado con arquitectura híbrida en `KeystoreEncryptionManager` (AES-256-GCM con clave maestra `signet_master.key` en `%APPDATA%/Signet/` para entornos de escritorio).
    * `android.content.SharedPreferences` abstraído mediante `PreferencesDataSource` y `DesktopPreferencesDataSource` (archivo `.properties` en disco).
    * `androidx.room` abstraído mediante `KeystoreDataSource` y `DesktopKeystoreDataSource` (índice en archivo `vault_index.json` con `StateFlow` reactivo).
  - **Resolución de Almacenamiento Desktop (`DesktopStorageUtils`)**: Compatible nativamente con Windows (`%APPDATA%/Signet`), Linux/Unix (`~/.config/signet`), macOS y emuladores Winlator.

- **Portal Web Oficial, Términos y Privacidad (`web/`)**:
  - Sitio web estático de alto rendimiento desarrollado en **Astro 5 + Tailwind CSS** optimizado para **Cloudflare Pages**.
  - Documentación pública y páginas legales actualizadas al **16 de agosto de 2026**:
    - **Política de Privacidad (`/privacy`)**: Declaración formal de **Cero Recolección de Datos**, arquitectura 100% offline, almacenamiento local exclusivo en Room, ausencia total de telemetría/analítica y principio de privilegio mínimo con Android SAF.
    - **Términos y Condiciones (`/terms`)**: Marco legal exhaustivo de 12 secciones bajo la licencia **GNU GPL v3**, detallando la soberanía criptográfica absoluta del usuario, deberes de custodia y respaldo, seguridad de variables Base64 en CI/CD, carácter diagnóstico del APK Matcher, integridad anti-manipulación HMAC en respaldos ZIP, distribución en plataformas de terceros (Uptodown, GitHub Releases, APKs directos), exención de garantías ("AS IS") y limitaciones de responsabilidad.

---

## 🏷️ Canales de Lanzamiento, Identificadores y Nomenclatura de Versiones

Signet adopta un sistema estructurado de canales de desarrollo y distribución que permite la coexistencia simultánea de compilaciones en el mismo dispositivo y una gestión estricta del ciclo de vida del software:

| Canal | Sufijo / Package ID | Etiqueta de Versión | Firma y Origen | Propósito y Estabilidad |
|---|---|---|---|---|
| **Exclusivo Debug (`.debug`)** | `com.signet.app.debug` | `1.0.0-D` | Dinámica en runner (`debug.keystore`) | **Canal interno de desarrollo**: Funciones en pruebas privadas por el desarrollador antes de llegar a `.dev`. Generado exclusivamente por el workflow de GitHub Actions (`build-debug-apk.yml`). |
| **Pre-Alpha (`.dev`)** | `com.signet.app.dev` | `1.0.0-dev` | Firma personalizada (GitHub Secrets) | **Canal de innovación activa**: Nuevas herramientas en desarrollo temprano. Pensada para evaluación comunitaria; puede ser inestable y las funciones pueden variar o eliminarse. |
| **Beta (`.beta`)** | `com.signet.app.beta` | `1.0.0-B` | Firma personalizada (GitHub Secrets) | **Canal de consolidación**: Funciones y herramientas ya pulidas y candidatas a definitivas. Evaluación de estabilidad previa al lanzamiento público final. |
| **Estable (`.estable`)** | `com.signet.app` / `com.signet.app.estable` | `1.0.0-E` | Firma de producción (GitHub Secrets) | **Canal definitivo de producción**: Versión final 100% pulida, optimizada y libre de errores críticos para tiendas de terceros (Uptodown, GitHub Releases). |

> **Nota Criptográfica sobre Firmas CI/CD**: Las claves de firma para los canales `.dev`, `.beta` y `.estable` se administran de forma soberana a través de **GitHub Secrets**, garantizando que los binarios públicos mantengan trazabilidad y seguridad inquebrantable.

---

## 🛠️ Stack Tecnológico

| Capa | Tecnología |
|---|---|
| **Lenguaje Android & JVM** | Kotlin 2.0+ con Coroutines & StateFlow |
| **UI Framework** | Jetpack Compose (Android) & Compose Multiplatform (Desktop) con Material Design 3 |
| **Persistencia** | Room Database (Android) / JSON Vault Index & Properties (Desktop/Windows) |
| **Criptografía** | BouncyCastle (SpongyCastle/BC) X.509 PKCS#12 JCA, AES-256-GCM & CSPRNG SecureRandom |
| **Análisis de APKs** | Parser nativo de APK Signing Block (v2/v3) & CMS PKCS#7 (v1) |
| **Arquitectura** | MVVM (Model - View - ViewModel) + Clean Architecture Multiplataforma |
| **Testing** | Robolectric 4.14+, JUnit 4 & Suite Multiplataforma |
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

### 5. Compilación, Empaquetado Nativo Eficiente y Firma de APK Release / Pre-Release (`.github/workflows/build-release-apk.yml`)
- **Pipeline Automatizado de Lanzamiento Beta**: Se activa automáticamente al publicar o crear un Pre-Release en GitHub, al empujar tags semánticos (ej. `v1.0.0-B`, `v*`), o de forma manual con `workflow_dispatch`.
- **Distribución Separada y Binarios Dedicados por Arquitectura**:
  * **Paquete para Móviles (`Signet-v*-release-signed.apk`)**: Compilado y optimizado específicamente para procesadores ARM de 64 bits (`arm64-v8a`) y 32 bits (`armeabi-v7a`), garantizando máxima compatibilidad y rendimiento tanto en teléfonos modernos como en dispositivos económicos/anteriores.
  * **Paquete para Emuladores PC (`Signet-v*-emulator-x86-release-signed.apk`)**: Compilado exclusivamente con binarios `x86_64` y `x86` para Android Studio Emulator, BlueStacks, LDPlayer y entornos CI/CD sin penalizaciones de traducción binaria ni incompatibilidades nativas.
- **Empaquetado Nativo sin Extracción (`extractNativeLibs = false` / `useLegacyPackaging = false`)**: Mapea directamente las librerías nativas `.so` (Room/SQLite y NDK) desde el archivo APK a memoria RAM mediante `mmap` en Android 6.0+ (API 23+), previniendo la duplicación física de archivos en `/data/app/` y reduciendo sustancialmente el peso de la app instalada en el almacenamiento interno del dispositivo.
- **Filtrado de Recursos de Idioma (`resourceConfigurations`)**: Descarte de recursos y traducciones no utilizadas de librerías AndroidX y Material 3 conservando exclusivamente español e inglés (`es`, `en`).
- **Optimización R8 Full Mode & ProGuard Avanzado**: Ofusca, aplica desvirtualización, inlining de lambdas de Compose y realiza un tree-shaking selectivo sobre BouncyCastle (`android.enableR8.fullMode=true`), eliminando metadatos redundantes (`.kotlin_module`, `DebugProbesKt`, prototipos) y reduciendo el peso del APK empaquetado manteniendo la funcionalidad criptográfica al 100%.
- **Hardening de Bytecode y Anti-Ingeniería Inversa**:
  * **Aplanamiento de Paquetes (`-repackageclasses ''` / `-allowaccessmodification`)**: Mueve todas las clases ofuscadas al paquete raíz eliminando la jerarquía de directorios en el DEX para impedir la deducción de arquitectura por descompiladores.
  * **Supresión de Nombres de Variables (`kotlin.jvm.internal.Intrinsics`)**: Elimina en Release llamadas internas que filtran nombres de variables en texto plano dentro del bytecode.
  * **Ocultación de Archivos Fuente (`-renamesourcefileattribute ""`)**: Borra referencias a nombres de archivos `.kt` originales.
- **Verificación de Integridad de Firma en Runtime (Anti-Tampering / Anti-Clon)**:
  * `SignatureVerifier.kt` inspecciona el certificado criptográfico de instalación (`signingInfo`) y valida la autenticidad frente a clonaciones o modificaciones no autorizadas por terceros.
- **Firma Criptográfica Segura**: Decodifica el almacén JKS/PKCS12 a partir de secretos en Base64 e inyecta las credenciales de firma en tiempo de compilación.
- **Publicación Automática**: Adjunta los binarios para móviles y emuladores junto a sus respectivos checksums `SHA-256` en los assets del Release de GitHub y como artefactos del workflow.
- **Secretos en GitHub Requeridos para el Canal Beta**:
  - `KEYSTORE_BETA_BASE64` (o `KEYSTORE_BASE64`): Contenido en Base64 del archivo Keystore de firma Beta (generado por la propia app Signet).
  - `KEYSTORE_BETA_PASSWORD` (o `KEYSTORE_PASSWORD`): Contraseña maestra del archivo Keystore Beta.
  - `KEY_ALIAS_BETA` (o `KEY_ALIAS`): Alias de la clave de firma (por defecto: `signet-beta`).
  - `KEY_PASSWORD_BETA` (o `KEY_PASSWORD`): Contraseña específica del alias Beta.
- **Registro de Versiones**: Consulta [Changelog-release.md](Changelog-release.md) para el historial detallado de notas de lanzamiento.

### 6. Compilación y Empaquetado Multi-Arquitectura de Windows Desktop (.EXE, .MSI, .ZIP) (`.github/workflows/build-release-desktop.yml`)
- **Pipeline Automatizado para Windows**:
  * **Activación por Releases / Tags (`v*`, `v*-B`, `v*-dev`, `v*-E`, `v*-desktop*`)**: Compila los paquetes de ambas arquitecturas, genera checksums SHA-256 y los adjunta automáticamente a los assets del Release en GitHub.
  * **Activación Manual (`workflow_dispatch`)**: Permite compilar y empaquetar en cualquier momento para pruebas internas; los binarios se suben exclusivamente como artefactos de workflow sin publicarse en GitHub Release assets.
- **Doble Arquitectura Soportada (Matriz de Compilación)**:
  1. **`windows-x64`**: Ejecutables nativos para computadoras y procesadores tradicionales de PC (Intel & AMD de 64 bits).
  2. **`windows-arm64`**: Ejecutables nativos optimizados para computadoras con procesadores de tecnología móvil / teléfono en Windows (ARM64 / Snapdragon / Qualcomm).
- **Empaquetado Nativo con `jpackage`**: Compila el Fat JAR del módulo `:desktop` con el runtime de Compose Desktop y genera para cada arquitectura:
  * **`.exe` (Signet.exe autónomo)** y **`.zip` portable** con runtime embebido (`Signet-v*-windows-x64-portable.zip` / `Signet-v*-windows-arm64-portable.zip`).
  * **`.msi` (Instalador oficial de Windows)** con accesos directos en el menú de inicio y escritorio (`Signet-v*-windows-x64-installer.msi` / `Signet-v*-windows-arm64-installer.msi`).
- **Suite CLI / Headless para Windows Terminal & PowerShell**:
  * Ejecución por terminal sin interfaz gráfica: `signet sign`, `signet generate`, `signet inspect`, `signet match`, `signet base64`, `signet backup-create` y `signet vault`.
- **Publicación y Entrega Segura**: Genera hashes `SHA-256`, adjunta todos los archivos a GitHub Releases (en eventos de tag/release) y envía una notificación confidencial con el archivo a Telegram.
- **Secretos en GitHub (reutiliza automáticamente los ya existentes)**:
  - `TELEGRAM_BOT_TOKEN_RELEASE_DESKTOP` (o `TELEGRAM_BOT_TOKEN_RELEASE_APK` / `TELEGRAM_BOT_TOKEN`).
  - `TELEGRAM_CHAT_ID_RELEASE_DESKTOP` (o `TELEGRAM_CHAT_ID_RELEASE_APK` / `TELEGRAM_CHAT_ID`).

---

## 📜 Licencia & Contribuciones

- **Licencia**: Este proyecto está publicado y licenciado bajo los términos de la **[GNU General Public License v3.0 (GPL v3)](LICENSE)**.
- **Contribuciones**: Consulta **[CONTRIBUTING.md](CONTRIBUTING.md)** para más detalles sobre las políticas actuales del repositorio (actualmente bajo desarrollo enfocado sin admisión de PRs de terceros).
