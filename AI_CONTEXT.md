# 🧠 Contexto para Modelos de IA & Asistentes de Código

Este documento proporciona contexto técnico, decisiones de arquitectura y directrices específicas para modelos de IA que trabajen en este repositorio.

---

## 🎯 Propósito del Proyecto
**Signet** es una aplicación nativa para Android desarrollada con **Jetpack Compose**, **Kotlin** y **BouncyCastle**. Su objetivo es brindar a los desarrolladores y creadores de software la capacidad de generar firmas digitales, certificados y archivos `.jks`/`.keystore` directamente en el dispositivo móvil, facilitando la exportación de huellas (SHA-256/SHA-1) y cadenas Base64 para entornos de integración continua (CI/CD).

---

## 🔑 Puntos Críticos y Reglas Criptográficas & Arquitectura

1. **Identidad del Proyecto**:
   - Nombre de la aplicación: **Signet**.
   - Nombre en launcher (`app_name`): `Signet`.
   - Propósito: Suite criptográfica de firma digital de APKs, creación de Keystores, certificados X.509 y generación de variables para CI/CD.

2. **Proveedor BouncyCastle en Android**:
   - Android cuenta con un proveedor criptográfico interno limitado `"BC"`.
   - **Regla Obligatoria**: Siempre registrar `BouncyCastleProvider` al inicio de la lista de proveedores con `Security.insertProviderAt(BouncyCastleProvider(), 1)` y pasar explícitamente la instancia del proveedor a `JcaContentSignerBuilder`, `JcaX509CertificateConverter` y `KeyStore.getInstance("PKCS12", provider)`.

3. **Generación Criptográfica de Contraseñas (CSPRNG)**:
   - Utilizar `com.example.crypto.PasswordGenerator` con `SecureRandom` del sistema operativo.
   - Longitud parametrizada (16, 20, 24, 32 caracteres) garantizando inclusión de mayúsculas, minúsculas, números y símbolos seguros para terminal y Gradle.
   - Cálculo en tiempo real de entropía (bits) y fortaleza para retroalimentación visual en Compose.

4. **Formatos Soportados & Validez**:
   - Generación: Estándar PKCS#12 (.jks y .keystore) compatible al 100% con `apksigner`, `jarsigner` y Android Studio Gradle Plugin.
   - Algoritmos: `RSA` (2048 y 4096 bits) con `SHA256WithRSAEncryption` y `EC` (Curva `secp256r1`/P-256) con `SHA256withECDSA`.
   - Validez interactiva: Control deslizable (1 a 100 años) con valor recomendado de 25+ años para cumplir requerimientos de actualización de APKs.
   - Requisitos de identidad: Diferenciación clara entre obligatorios por estándar Google/Android (`CN`, `O`) y opcionales (`OU`, `L`, `ST`, `C`), con soporte para datos inventados o seudónimos para proteger la privacidad.

5. **Sistema de Temas Dinámico & Negro 100% (AMOLED)**:
   - Configuración centralizada mediante `ThemeState`, `ThemeMode` y `ColorPalette`.
   - Soporte para **Material You** (Android 12+), paletas personalizadas (Navy, Emerald, Purple, Amber, Teal, Crimson, Monochrome) y modo **Negro Puro 100%** (#000000 para pantallas OLED).
   - Preferencias del usuario persistidas en `SharedPreferences`.

6. **Persistencia Local y Room**:
   - Toda información sensible (alias, huellas, contraseñas y Base64) se almacena localmente en SQLite mediante Room.

7. **Reglas de UI (Jetpack Compose & M3)**:
   - Utilizar componentes de Material 3 (`Scaffold`, `Card`, `FilterChip`, `Button`, `OutlinedTextField`, `Slider`, `LinearProgressIndicator`).
   - Respetar áreas táctiles mínimas de 48dp y `testTag` en botones y acciones clave.
   - Modularización de componentes en paquetes especializados (`ui/screens/generate/` y `ui/components/details/`).

8. **Snippets Gradle & Workflows GitHub Actions**:
   - `SnippetGenerator` expone funciones para generar configuración `build.gradle.kts` (Kotlin DSL), `build.gradle` (Groovy), workflows de GitHub Actions (`.github/workflows/android-build-and-sign.yml`) y comandos CLI `apksigner`.
   - Las configuraciones de Gradle generadas deben seguir buenas prácticas de seguridad leyendo secretos desde variables de entorno (`KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, etc.).

9. **Distribución Fuera de Google Play (Uptodown, F-Droid, APKs)**:
   - La aplicación no requiere ni incluye servicios innecesarios de Google Play (`play-services-*`, `firebase-*`, etc.). Es 100% autónoma, funcional offline y compatible con cualquier dispositivo Android / ROM personalizada (LineageOS, GrapheneOS, microG).

10. **Automatización de Builds y Auditoría de Seguridad en GitHub Actions**:
   - `.github/workflows/build-debug-apk.yml` automatiza bajo activación manual (`workflow_dispatch`) o push la descarga completa del código, configuración de JDK 17, caché de Gradle (mediante `setup-java` y `gradle/actions/setup-gradle`), generación efímera de keystore en el runner con `keytool`, compilación directa de APK Debug (`./gradlew assembleDebug`) y envío privado a Telegram por protocolo nativo MTProto con **Telethon** (soporta hasta 2GB por archivo, evitando artefactos públicos).
   - `.github/workflows/security-scan.yml` ejecuta bajo activación manual (`workflow_dispatch`) escaneo estático confidencial y auditoría de código, manifest, criptografía y dependencias en modo **Stealth** (sin registrar hallazgos ni secretos en los logs del runner), generando un archivo estructurado `vulnerabilities-report.json` y enviándolo adjunto a Telegram junto con un resumen en texto.
   - Secretos configurados: `TELEGRAM_BOT_TOKEN_SECURITY_SCAN`, `TELEGRAM_CHAT_ID_SECURITY_SCAN`, `TELEGRAM_API_ID_SECURITY_SCAN`, `TELEGRAM_API_HASH_SECURITY_SCAN` (con fallback automático a `TELEGRAM_BOT_TOKEN_DEBUG_APK`, `TELEGRAM_CHAT_ID_DEBUG_APK`, etc.).

11. **Paquetes de Respaldo ZIP & Mecanismo Anti-Manipulación (`SignetBackupManager`)**:
   - Signet permite exportar respaldos completos portables en formato `.zip` que contienen: el binario `.jks`/`.keystore`, `credentials.txt`, `key.properties`, `base64.txt`, `README-BACKUP.txt` y `signet-manifest.json`.
   - **Firma Anti-Manipulación Obligatoria**: El manifiesto JSON contiene la firma criptográfica HMAC-SHA256 y hash SHA-256 del binario. Si el archivo `.json` es alterado, falsificado o si el binario del keystore fue modificado, Signet rechazará automáticamente la restauración con `SecurityException`.
   - Al restaurar, se valida además la apertura del archivo contra los certificados antes de persistir en Room Database.

12. **Licencia GPL v3 & Política de Contribuciones**:
   - El proyecto está licenciado bajo la **GNU General Public License v3.0 (GPL v3)**.
   - Como se detalla en `CONTRIBUTING.md`, el proyecto se desarrolla de forma centralizada y cerrada temporalmente respecto a Pull Requests externos para proteger la integridad de las primitivas criptográficas y del roadmap.

13. **Portal Web Oficial, Términos & Privacidad (`web/` en Cloudflare Pages)**:
   - Desarrollado con **Astro 5 + Tailwind CSS** como sitio estático sin JavaScript runtime innecesario.
   - Incluye Landing Page oficial (`/`), Política de Privacidad estricta (`/privacy`) y Términos y Condiciones (`/terms`).
   - **Postura Obligatoria de Cero Recolección**: Se garantiza de forma explícita que Signet no recopila contraseñas, no tiene servidores en la nube, no integra SDKs de analítica ni telemetría y funciona 100% offline.

