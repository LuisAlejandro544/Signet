# 🗺️ Roadmap de Desarrollo: Keystore Generator

Plan estratégico de evolución técnica y funcionalidades para el proyecto **Keystore Generator**.

---

## 📍 Fase 1: Fundamentos & Funcionalidad Central (Completado ✅)
- [x] Motor de generación criptográfica X.509 con BouncyCastle (RSA 2048, RSA 4096, EC P-256).
- [x] Soporte para extensiones `.jks` y `.keystore`.
- [x] Control deslizable interactivo (Slider) y personalización libre de validez de certificados en años.
- [x] Cálculo automático de huellas digitales SHA-256, SHA-1 y MD5.
- [x] Conversión y visualización en tiempo real a Base64.
- [x] Copia de credenciales y contraseñas con un solo toque.
- [x] Persistencia local con Room Database y exportación SAF.
- [x] Pestaña de Configuración con personalización de color, Material You, modo Negro 100% (AMOLED) y paletas de autor.
- [x] **Generador y Visualizador Interactivo de Snippets**:
  - [x] Visualizador interactivo de bloques `signingConfigs` para `build.gradle.kts` (Kotlin DSL) y `build.gradle` (Groovy).
  - [x] Generador de workflows automatizados para GitHub Actions (`.github/workflows/android-build-and-sign.yml`) con decodificación de `KEYSTORE_BASE64` y firma en runners.
  - [x] Comandos CLI para `apksigner` y `zipalign`.
- [x] Limpieza de dependencias innecesarias de Google Play para distribución universal (Uptodown, F-Droid, APKs).
- [x] Pipeline CI/CD en GitHub Actions manual (`build-debug-apk.yml` con `workflow_dispatch`) con descarga de código, caché de Gradle, generación de clave en runner y compilación de APK Debug.
- [x] Tests unitarios con Robolectric y soporte de CI/CD para repositorios.

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
