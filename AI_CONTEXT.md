# 🧠 Contexto para Modelos de IA & Asistentes de Código

Este documento proporciona contexto técnico, decisiones de arquitectura y directrices específicas para modelos de IA que trabajen en este repositorio.

---

## 🎯 Propósito del Proyecto
**Keystore Generator** es una aplicación nativa para Android desarrollada con **Jetpack Compose**, **Kotlin** y **BouncyCastle**. Su objetivo es brindar a los desarrolladores y creadores de software la capacidad de generar firmas digitales, certificados y archivos `.jks`/`.keystore` directamente en el dispositivo móvil, facilitando la exportación de huellas (SHA-256/SHA-1) y cadenas Base64 para entornos de integración continua (CI/CD).

---

## 🔑 Puntos Críticos y Reglas Criptográficas & Arquitectura

1. **Proveedor BouncyCastle en Android**:
   - Android cuenta con un proveedor criptográfico interno limitado `"BC"`.
   - **Regla Obligatoria**: Siempre registrar `BouncyCastleProvider` al inicio de la lista de proveedores con `Security.insertProviderAt(BouncyCastleProvider(), 1)` y pasar explícitamente la instancia del proveedor a `JcaContentSignerBuilder`, `JcaX509CertificateConverter` y `KeyStore.getInstance("PKCS12", provider)`.

2. **Formatos Soportados & Validez**:
   - Generación: Estándar PKCS#12 (.jks y .keystore) compatible al 100% con `apksigner`, `jarsigner` y Android Studio Gradle Plugin.
   - Algoritmos: `RSA` (2048 y 4096 bits) con `SHA256WithRSAEncryption` y `EC` (Curva `secp256r1`/P-256) con `SHA256withECDSA`.
   - Validez interactiva: Control deslizable (1 a 100 años) con valor recomendado de 25+ años para cumplir requerimientos de actualización de APKs.

3. **Sistema de Temas Dinámico & Negro 100% (AMOLED)**:
   - Configuración centralizada mediante `ThemeState`, `ThemeMode` y `ColorPalette`.
   - Soporte para **Material You** (Android 12+), paletas personalizadas (Navy, Emerald, Purple, Amber, Teal, Crimson, Monochrome) y modo **Negro Puro 100%** (#000000 para pantallas OLED).
   - Preferencias del usuario persistidas en `SharedPreferences`.

4. **Persistencia Local y Room**:
   - Toda información sensible (alias, huellas, contraseñas y Base64) se almacena localmente en SQLite mediante Room.

5. **Reglas de UI (Jetpack Compose & M3)**:
   - Utilizar componentes de Material 3 (`Scaffold`, `Card`, `FilterChip`, `Button`, `OutlinedTextField`, `Slider`).
   - Respetar áreas táctiles mínimas de 48dp y `testTag` en botones y acciones clave.

6. **Snippets Gradle & Workflows GitHub Actions**:
   - `KeystoreGenerator` expone funciones para generar configuración `build.gradle.kts` (Kotlin DSL), `build.gradle` (Groovy), workflows de GitHub Actions (`.github/workflows/android-build-and-sign.yml`) y comandos CLI `apksigner`.
   - Las configuraciones de Gradle generadas deben seguir buenas prácticas de seguridad leyendo secretos desde variables de entorno (`KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, etc.).

7. **Distribución Fuera de Google Play (Uptodown, F-Droid, APKs)**:
   - La aplicación no requiere ni incluye servicios innecesarios de Google Play (`play-services-*`, `firebase-*`, etc.). Es 100% autónoma, funcional offline y compatible con cualquier dispositivo Android / ROM personalizada (LineageOS, GrapheneOS, microG).

8. **Automatización de Builds en GitHub Actions**:
   - `.github/workflows/build-debug-apk.yml` automatiza bajo activación manual (`workflow_dispatch`) la descarga completa del código, configuración de JDK 17, caché de Gradle (mediante `setup-java` y `gradle/actions/setup-gradle`), generación de keystore en el runner con `keytool`, compilación directa de APK Debug (`./gradlew assembleDebug`) y publicación de artefactos descargables.
