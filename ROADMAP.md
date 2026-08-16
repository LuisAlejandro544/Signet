# 🗺️ Roadmap de Desarrollo: Keystore Generator

Plan estratégico de evolución técnica y funcionalidades para el proyecto **Keystore Generator**.

---

## 📍 Fase 1: Fundamentos & Funcionalidad Central (Completado ✅)
- [x] Motor de generación criptográfica X.509 con BouncyCastle (RSA 2048, RSA 4096, EC P-256).
- [x] Soporte para extensiones `.jks` y `.keystore`.
- [x] Cálculo automático de huellas digitales SHA-256, SHA-1 y MD5.
- [x] Conversión y visualización en tiempo real a Base64.
- [x] Copia de credenciales y contraseñas con un solo toque.
- [x] Persistencia local con Room Database y exportación SAF.
- [x] Tests unitarios con Robolectric y soporte de CI/CD para repositorios.

---

## 📍 Fase 2: Firmador Integrado de APKs & Utilidades (Próximo 🚀)
- [ ] **Firmador de APKs en el Dispositivo**:
  - Implementación de firma APK Signature Scheme v1 (JAR signing) y v2 (APK Signature Scheme v2) directamente en el móvil.
  - Firma de APKs desalineados con proceso automático de `zipalign`.
- [ ] **Conversión de Formatos**:
  - Conversión bidireccional entre JKS/PKCS12 y PEM/CRT/KEY.
  - Generación de claves de subida (.pepk) para Google Play App Signing.
- [ ] **Generador de Snippets de Configuración**:
  - Copia rápida de bloques `signingConfigs` para `build.gradle.kts` y `build.gradle` (Groovy).
  - Generador de secretos para GitHub Actions (`secrets.KEYSTORE_BASE64`, `secrets.KEY_ALIAS`, etc.).

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
