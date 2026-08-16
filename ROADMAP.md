# 🗺️ Roadmap de Desarrollo: Signet

Plan estratégico de evolución técnica y funcionalidades para el proyecto **Signet**.

---

## 📍 Fase 1: Fundamentos, Seguridad & Modularización (Completado ✅)
- [x] Establecimiento de identidad y nombre oficial: **Signet**.
- [x] Motor de generación criptográfica X.509 con BouncyCastle (RSA 2048, RSA 4096, EC P-256).
- [x] Soporte para extensiones `.jks` y `.keystore`.
- [x] **Generador Criptográfico de Contraseñas (CSPRNG Nativo)**: Entropía calculada en tiempo real, selección de longitud (16, 20, 24, 32 caracteres), inclusión garantizada de mayúsculas, minúsculas, dígitos y símbolos seguros para Gradle/Terminal.
- [x] **Guía de Campos y Requisitos de Identidad Google**: Claridad entre campos obligatorios (`CN`, `O`) y opcionales (`OU`, `L`, `ST`, `C`) con soporte para seudónimos y nombres inventados para privacidad.
- [x] **Arquitectura Modular Desacoplada**: Submódulos en `screens/generate/` y `components/details/`, y separación de `SnippetGenerator`.
- [x] Control deslizable interactivo (Slider) y personalización libre de validez de certificados en años.
- [x] Cálculo automático de huellas digitales SHA-256, SHA-1 y MD5.
- [x] Conversión y visualización en tiempo real a Base64.
- [x] Copia de credenciales y contraseñas con un solo toque.
- [x] Persistencia local con Room Database y exportación SAF.
- [x] **Paquetes de Respaldo ZIP y Restauración con Firma Anti-Manipulación**:
  - [x] Generación de paquetes `.zip` con keystore, contraseñas, `key.properties`, `base64.txt` y manifiesto JSON firmado.
  - [x] Mecanismo de verificación criptográfica HMAC-SHA256 para prevenir restauraciones de archivos manipulados o apócrifos.
  - [x] Restauración instantánea hacia la base de datos Room.
- [x] Pestaña de Configuración con personalización de color, Material You, modo Negro 100% (AMOLED) y paletas de autor.
- [x] **Generador y Visualizador Interactivo de Snippets**:
  - [x] Visualizador interactivo de bloques `signingConfigs` para `build.gradle.kts` (Kotlin DSL) y `build.gradle` (Groovy).
  - [x] Generador de workflows automatizados para GitHub Actions (`.github/workflows/android-build-and-sign.yml`) con decodificación de `KEYSTORE_BASE64` y firma en runners.
  - [x] Comandos CLI para `apksigner` y `zipalign`.
- [x] Limpieza de dependencias innecesarias de Google Play para distribución universal (Uptodown, F-Droid, APKs).
- [x] Publicación bajo licencia **GNU General Public License v3.0 (GPL v3)** y definición de directrices de contribución en `CONTRIBUTING.md`.
- [x] Pipeline CI/CD en GitHub Actions manual (`build-debug-apk.yml` con `workflow_dispatch`) con descarga de código, caché de Gradle, generación de clave en runner y compilación de APK Debug.
- [x] Tests unitarios con Robolectric y soporte de CI/CD para repositorios.
- [x] **Portal Web Oficial, Términos & Privacidad para Cloudflare Pages (`web/`)**:
  - [x] Sitio estático de alto rendimiento en **Astro 5 + Tailwind CSS** con tema OLED y Emerald.
  - [x] Declaración explícita de Política de Privacidad (`/privacy`) con **Cero Recolección de Datos** y Cero Telemetría.
  - [x] Términos y Condiciones de Uso (`/terms`) bajo licencia GPL v3 y pautas de custodia.
  - [x] Configuración lista para despliegue global en Cloudflare Pages (`wrangler.toml`).

---

## 📍 Fase 2: Firmador Integrado de APKs & Utilidades (Próximo 🚀)
- [ ] **Firmador de APKs en el Dispositivo**:
  - Implementación de firma APK Signature Scheme v1 (JAR signing) y v2 (APK Signature Scheme v2) directamente en el móvil.
  - Firma de APKs desalineados con proceso automático de `zipalign`.
- [ ] **Conversión de Formatos**:
  - Conversión bidireccional entre JKS/PKCS12 y PEM/CRT/KEY.
  - Generación de claves de subida (.pepk) para Google Play App Signing.

---

## 📍 Fase 3: Seguridad Avanzada & Biometría 🔒
- [ ] **Bloqueo Biométrico de la App**:
  - Protección de la lista de keystores y contraseñas mediante Android BiometricPrompt (Huella / Reconocimiento facial).
- [ ] **Cifrado de la Base de Datos Room**:
  - Integración de SQLCipher / EncryptedSharedPreferences para cifrar en reposo las contraseñas almacenadas.
- [ ] **Verificador de Integridad de APKs**:
  - Arrastrar/seleccionar un APK para verificar con qué certificado fue firmado y contrastarlo con los almacenados en la app.

---

## 📍 Fase 4: Rendimiento & Soporte Multiplataforma ⚡
- [ ] Módulo nativo en Rust (mediante JNI/UniFFI) para acelerar la firma de paquetes APK gigantes (>500MB).
- [ ] Soporte para modo tablet / escritorio (Canonical Layouts, navegación por raíl lateral).
