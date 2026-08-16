# 🗺️ Roadmap de Desarrollo: Signet

Plan estratégico de evolución técnica y funcionalidades para el proyecto **Signet**.

---

## 📍 Fase 1: Fundamentos, Seguridad & Modularización (Completado ✅)
- [x] Establecimiento de identidad y nombre oficial: **Signet**.
- [x] Motor de generación criptográfica X.509 con BouncyCastle (RSA 2048, RSA 4096, EC P-256).
- [x] Soporte para extensiones `.jks` y `.keystore`.
- [x] **Generador Criptográfico de Contraseñas (CSPRNG Nativo)**: Entropía calculada en tiempo real, selección de longitud (16, 20, 24, 32 caracteres), inclusión garantizada de mayúsculas, minúsculas, dígitos y símbolos seguros para Gradle/Terminal.
- [x] **Guía de Campos y Requisitos de Identidad Google**: Claridad entre campos obligatorios (`CN`, `O`) y opcionales (`OU`, `L`, `ST`, `C`) con soporte para seudónimos y nombres inventados para privacidad.
- [x] **Arquitectura Modular Desacoplada**: Submódulos en `screens/generate/`, `screens/inspect/` y `components/details/`, y separación de `SnippetGenerator`.
- [x] Control deslizable interactivo (Slider) y personalización libre de validez de certificados en años.
- [x] Cálculo automático de huellas digitales SHA-256, SHA-1 y MD5.
- [x] Conversión y visualización en tiempo real a Base64.
- [x] Copia de credenciales y contraseñas con un solo toque.
- [x] Persistencia local con Room Database y exportación SAF.
- [x] **Paquetes de Respaldo ZIP y Restauración con Firma Anti-Manipulación**:
  - [x] Generación de paquetes `.zip` con keystore, contraseñas, `key.properties`, `base64.txt` y manifiesto JSON firmado.
  - [x] Mecanismo de verificación criptográfica HMAC-SHA256 para prevenir restauraciones de archivos manipulados o apócrifos.
  - [x] Corrección y robustecimiento del flujo de descompresión de streams ZIP (`SignetBackupManager`).
  - [x] Restauración instantánea hacia la base de datos Room.
- [x] Pestaña de Configuración con personalización de color, Material You, modo Negro 100% (AMOLED) y paletas de autor.
- [x] **Generación y Exportación de Claves Cifradas para Google Play (.pepk)**:
  - [x] Cifrado híbrido on-device con RSA-OAEP (SHA-256) + AES-256-GCM y paquete binario `.pepk`.
  - [x] Selector y parser de claves públicas `encryption_public_key.pem` de Google Play Console.
  - [x] Exportación dual: archivo binario `.pepk` suelto o **Paquete ZIP Completo** (Keystore + `.pepk` + credenciales + `key.properties` + `base64.txt`).
  - [x] Generador de comandos CLI oficiales para `pepk.jar`.
- [x] **Validador Forense de Coincidencia APK vs Keystore (APK Matcher)**:
  - [x] Extracción nativa de certificados X.509 de firmas v1 (JAR Signature / PKCS#7 en `META-INF`), v2 (APK Signing Block) y v3.
  - [x] Lectura de metadatos del paquete APK (Package Name, Version Name, Version Code).
  - [x] Validación y diagnóstico de compatibilidad de actualización de APKs (coincidencia de huella SHA-256 contra Keystores guardados o externos).
  - [x] Interfaz interactiva en pestaña Inspeccionar con selector visual y tarjetas de diagnóstico.
- [x] **Generador y Visualizador Interactivo de Snippets**:
  - [x] Visualizador interactivo de bloques `signingConfigs` para `build.gradle.kts` (Kotlin DSL) y `build.gradle` (Groovy).
  - [x] Generador de workflows automatizados para GitHub Actions (`.github/workflows/android-build-and-sign.yml`) con decodificación de `KEYSTORE_BASE64` y firma en runners.
  - [x] Comandos CLI para `apksigner`, `zipalign` y `pepk.jar`.
- [x] Limpieza de dependencias innecesarias de Google Play para distribución universal (Uptodown, GitHub Releases, APKs).
- [x] Publicación bajo licencia **GNU General Public License v3.0 (GPL v3)** y definición de directrices de contribución en `CONTRIBUTING.md`.
- [x] Pipeline CI/CD en GitHub Actions manual (`build-debug-apk.yml` con `workflow_dispatch`) con descarga de código, caché de Gradle, generación de clave en runner y compilación de APK Debug.
- [x] **Auditoría Automatizada de Seguridad en GitHub Actions (`security-scan.yml`)**:
  - [x] Escaneo confidencial en modo Stealth de vulnerabilidades en código Kotlin, Manifest, secretos y dependencias.
  - [x] Generación de reporte estructurado `vulnerabilities-report.json` y despacho privado a Telegram para consumo del asistente IA.
- [x] **Pruebas E2E en Emulador Real KVM en GitHub Actions (`emulator-e2e-test.yml`)**:
  - [x] Emulador Android nativo acelerado por hardware KVM (Pixel 6 / API 34).
  - [x] Verificación de importación de paquetes ZIP legítimos vs rechazo estricto de paquetes adulterados (Anti-Tampering HMAC).
  - [x] Generación de reporte JSON `emulator-e2e-report.json`, captura de pantalla del emulador y despacho confidencial a Telegram.
- [x] **Validación Cruzada CLI con Herramientas Oficiales (`cli-interoperability-test.yml`)**:
  - [x] Prueba de compatibilidad e interoperabilidad de Keystores de Signet con `keytool` y `apksigner` oficial de Google.
  - [x] Verificación estricta de firmas Android v1, v2 y v3, reporte `cli-interop-report.json` y despacho privado a Telegram.
- [x] Tests unitarios con Robolectric y soporte de CI/CD para repositorios.
- [x] **Portal Web Oficial, Términos & Privacidad para Cloudflare Pages (`web/`)**:
  - [x] Sitio estático de alto rendimiento en **Astro 5 + Tailwind CSS** con tema OLED y Emerald.
  - [x] Declaración explícita de Política de Privacidad (`/privacy`) con fecha de entrada en vigor al **16 de agosto de 2026**, **Cero Recolección de Datos** y Cero Telemetría.
  - [x] Términos y Condiciones de Uso (`/terms`) ampliados al **16 de agosto de 2026** con 13 secciones jurídicas y técnicas bajo licencia GPL v3, soberanía de claves, custodia, CI/CD, APK Matcher, PEPK, anti-tampering HMAC, tiendas de terceros y exención de garantías "AS IS".
  - [x] Configuración lista para despliegue global en Cloudflare Pages (`wrangler.toml`).
- [x] **Flujo de Bienvenida Interactivo (Onboarding) & Integración Legal en la App**:
  - [x] Pantallas de bienvenida (`WelcomeScreen`) de 4 pasos explicando las capacidades del sistema y la privacidad 100% offline.
  - [x] Consentimiento informado y aceptación de Términos y Privacidad en el primer arranque.
  - [x] Acceso directo en `SettingsScreen` a las URLs de Términos y Privacidad del portal web y botón para repasar la bienvenida.

---

## 📍 Fase 2: Firmador Integrado de APKs & Utilidades (Próximo 🚀)
- [ ] **Firmador de APKs en el Dispositivo**:
  - Implementación de firma APK Signature Scheme v1 (JAR signing) y v2 (APK Signature Scheme v2) directamente en el móvil.
  - Firma de APKs desalineados con proceso automático de `zipalign`.
- [ ] **Conversión de Formatos**:
  - Conversión bidireccional entre JKS/PKCS12 y PEM/CRT/KEY.
- [ ] **Herramienta Multiplataforma (Signet Desktop)**:
  - Versión Compose Multiplatform (Desktop JVM) para desarrolladores en Linux, macOS y Windows.
