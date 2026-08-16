# 🧠 Contexto para Modelos de IA & Asistentes de Código

Este documento proporciona contexto técnico, decisiones de arquitectura y directrices específicas para modelos de IA que trabajen en este repositorio.

---

## 🎯 Propósito del Proyecto
**Keystore Generator** es una aplicación nativa para Android desarrollada con **Jetpack Compose**, **Kotlin** y **BouncyCastle**. Su objetivo es brindar a los desarrolladores y creadores de software la capacidad de generar firmas digitales, certificados y archivos `.jks`/`.keystore` directamente en el dispositivo móvil, facilitando la exportación de huellas (SHA-256/SHA-1) y cadenas Base64 para entornos de integración continua (CI/CD).

---

## 🔑 Puntos Críticos y Reglas Criptográficas

1. **Proveedor BouncyCastle en Android**:
   - Android cuenta con un proveedor criptográfico interno limitado `"BC"`.
   - **Regla Obligatoria**: Siempre registrar `BouncyCastleProvider` al inicio de la lista de proveedores con `Security.insertProviderAt(BouncyCastleProvider(), 1)` y pasar explícitamente la instancia del proveedor a `JcaContentSignerBuilder`, `JcaX509CertificateConverter` y `KeyStore.getInstance("PKCS12", provider)`.

2. **Formatos Soportados**:
   - Generación: Estándar PKCS#12 (.jks y .keystore) compatible al 100% con `apksigner`, `jarsigner` y Android Studio Gradle Plugin.
   - Algoritmos: `RSA` (2048 y 4096 bits) con `SHA256WithRSAEncryption` y `EC` (Curva `secp256r1`/P-256) con `SHA256withECDSA`.

3. **Persistencia Local y Room**:
   - Toda información sensible (alias, huellas, contraseñas y Base64) se almacena localmente en SQLite mediante Room.
   - Las migraciones deben mantener compatibilidad o usar `fallbackToDestructiveMigration()` si la versión del schema se incrementa.

4. **Reglas de UI (Jetpack Compose & M3)**:
   - Utilizar componentes de Material 3 (`Scaffold`, `Card`, `FilterChip`, `Button`, `OutlinedTextField`).
   - Respetar áreas táctiles mínimas de 48dp y `testTag` en botones y acciones clave.
