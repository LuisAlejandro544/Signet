# 🧠 Contexto para Modelos de IA & Asistentes de Código

Este documento proporciona contexto técnico, decisiones de arquitectura y directrices específicas para modelos de IA que trabajen en este repositorio.

---

## 🎯 Propósito del Proyecto
**Signet** es una aplicación nativa para Android desarrollada con **Jetpack Compose**, **Kotlin** y **BouncyCastle**. Su objetivo es brindar a los desarrolladores y creadores de software la capacidad de generar firmas digitales, certificados, archivos `.jks`/`.keystore`, exportar paquetes `.pepk` para Google Play y validar la correspondencia forense entre APKs y Keystores (APK Matcher) directamente en el dispositivo móvil.

---

## 🔑 Puntos Críticos y Reglas Criptográficas & Arquitectura

1. **Identidad del Proyecto**:
   - Nombre de la aplicación: **Signet**.
   - Nombre en launcher (`app_name`): `Signet`.
   - Identificador de aplicación (`applicationId`): `com.signet.app`.
   - Propósito: Suite criptográfica de firma digital de APKs, creación de Keystores, certificados X.509, validación forense APK vs Keystore y generación de variables para CI/CD.

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

5. **Validador Forense APK vs Keystore (`ApkMatcher`)**:
   - Analiza binarios de paquetes `.apk` mediante análisis forense multi-esquema:
     - **Esquema v1 (JAR Signing)**: Lee archivos PKCS#7 en `META-INF/*.RSA`, `*.DSA` o `*.EC` usando `CMSSignedData` de BouncyCastle.
     - **Esquema v2 / v3 (APK Signing Block)**: Localiza el bloque de firma de Android buscando el magic `APK Sig Block 42` antes del EOCD del archivo ZIP y extrae certificados del ID de bloque `0x7109871a` y `0xf05368c0`.
   - Realiza la comparación determinista de huellas SHA-256 contra Keystores guardados en la base de datos Room o cargados externamente.

6. **Sistema de Temas Dinámico & Negro 100% (AMOLED)**:
   - Configuración centralizada mediante `ThemeState`, `ThemeMode` y `ColorPalette`.
   - Soporte para **Material You** (Android 12+), paletas personalizadas (Navy, Emerald, Purple, Amber, Teal, Crimson, Monochrome) y modo **Negro Puro 100%** (#000000 para pantallas OLED).
   - Preferencias del usuario persistidas en `SharedPreferences`.

7. **Persistencia Local y Room**:
   - Toda información sensible (alias, huellas, contraseñas y Base64) se almacena localmente en SQLite mediante Room.

8. **Reglas de UI (Jetpack Compose & M3)**:
   - Utilizar componentes de Material 3 (`Scaffold`, `Card`, `FilterChip`, `Button`, `OutlinedTextField`, `Slider`, `TabRow`, `LinearProgressIndicator`).
   - Respetar áreas táctiles mínimas de 48dp y `testTag` en botones y acciones clave.
   - Modularización de componentes en paquetes especializados (`ui/screens/generate/`, `ui/screens/inspect/` y `ui/components/details/`).

9. **Snippets Gradle & Workflows GitHub Actions**:
   - `SnippetGenerator` expone funciones para generar configuración `build.gradle.kts` (Kotlin DSL), `build.gradle` (Groovy), workflows de GitHub Actions (`.github/workflows/android-build-and-sign.yml`) y comandos CLI `apksigner`.
   - Las configuraciones de Gradle generadas deben seguir buenas prácticas de seguridad leyendo secretos desde variables de entorno (`KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, etc.).

10. **Distribución Fuera de Google Play (Uptodown, GitHub Releases, APKs)**:
    - La aplicación no requiere ni incluye servicios innecesarios de Google Play (`play-services-*`, `firebase-*`, etc.). Es 100% autónoma, funcional offline y compatible con cualquier dispositivo Android / ROM personalizada (LineageOS, GrapheneOS, microG).

11. **Paquetes de Respaldo ZIP & Mecanismo Anti-Manipulación (`SignetBackupManager`)**:
    - Generación y descompresión de paquetes ZIP con `signet-backup.json`, firma HMAC-SHA256, hash de contenido y credenciales.
    - Se garantiza la lectura atómica de entradas ZIP sin cierres prematuros de streams.

12. **Portal Web Oficial, Términos y Privacidad (`web/`)**:
    - Sitio web en Astro 5 + Tailwind CSS para Cloudflare Pages.
    - Términos y Condiciones (`/terms`) y Política de Privacidad (`/privacy`) formalizados con fecha **16 de agosto de 2026**.
    - Cobertura legal integral: GPL v3, soberanía total de claves del usuario, custodia sin servidores remotos, buenas prácticas en CI/CD, disclaimer del APK Matcher, compatibilidad PEPK, respaldos ZIP con firma HMAC y distribución en plataformas de terceros (Uptodown, GitHub Releases, APKs directos).

