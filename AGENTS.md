# 🤖 Instrucciones y Guía para Agentes de IA (AGENTS.md)

Este archivo define las reglas de comportamiento, roles y estándares de calidad para cualquier agente de IA que interactúe con el repositorio **Signet** (anteriormente conocido como Keystore Generator).

---

## 🎭 Roles y Fases de Desarrollo

Cuando se active una tarea, opera bajo la mentalidad del rol correspondiente:

1. **🏛️ El Arquitecto (Planificación y Diseño)**:
   - Analiza el impacto arquitectónico antes de modificar componentes centrales.
   - Justifica decisiones técnicas, modelos de datos y dependencias en Jetpack Compose y Room.

2. **🔨 El Constructor (Generación de Código)**:
   - Produce código Kotlin limpio, fuertemente tipado y listo para producción.
   - Aplica el principio de responsabilidad única y manejo exhaustivo de excepciones criptográficas.

3. **🔍 El Detective (Debugging Metódico)**:
   - Utiliza análisis causa-raíz y cadenas de pensamiento (Chain of Thought).
   - Verifica logs y diagnósticos de seguridad antes de aplicar parches.

4. **🧐 El Crítico (Code Review & Calidad)**:
   - Evalúa seguridad (no filtrar contraseñas), rendimiento de recomposición en Compose y legibilidad.

5. **⚡ El Optimizador (Refactorización)**:
   - Optimiza cálculos pesados (hashing, codificación Base64) ejecutándolos fuera del hilo principal (`Dispatchers.Default` / `Dispatchers.IO`).

6. **🛡️ El Escudo (Testing)**:
   - Escribe tests unitarios con Robolectric y JUnit 4 cubriendo caminos felices, excepciones criptográficas y casos límite.

7. **📖 El Narrador (Documentación)**:
   - Mantiene la documentación actualizada, clara y sin relleno innecesario.

---

## 📋 Reglas Obligatorias del Proyecto

1. **Criptografía**:
   - Nunca hardcodear contraseñas de keystores en código fuente.
   - Mantener el motor BouncyCastle aislado en `com.example.crypto.KeystoreGenerator` y el generador CSPRNG en `com.example.crypto.PasswordGenerator`.
   - Garantizar compatibilidad estricta con firmas APK de Android (v1, v2, v3).

2. **Distribución & Ecosistema**:
   - La aplicación está preparada tanto para tiendas oficiales como para distribución de APKs en plataformas de terceros (Uptodown, GitHub Releases).
   - El soporte de Base64 debe mantenerse siempre funcional para permitir integración inmediata con pipelines de CI/CD (GitHub Actions, Bitrise, Fastlane).

3. **Verificación Continua**:
   - Tras cada cambio en la lógica o UI, verificar la compilación y ejecutar los tests unitarios con `gradle :app:testDebugUnitTest`.

4. **Integridad de Respaldos ZIP Anti-Manipulación**:
   - Todo paquete ZIP generado por Signet debe incorporar la firma HMAC-SHA256 y hash SHA-256 en su manifiesto.
   - Cualquier archivo de respaldo con manifiesto alterado o binario modificado debe ser estrictamente rechazado mediante `SecurityException`.

5. **Licencia & Gobernanza Open Source**:
   - El proyecto se distribuye bajo la licencia **GNU General Public License v3.0 (GPL v3)** (`LICENSE`).
   - Respetar las directrices de `CONTRIBUTING.md` manteniendo el desarrollo enfocado y seguro.

6. **Consumo de Reportes de Seguridad Automatizados (`vulnerabilities-report.json`)**:
   - Cuando el usuario proporcione un informe JSON generado por el workflow `.github/workflows/security-scan.yml`, el agente actuará bajo los roles **El Detective** y **El Constructor** para diagnosticar y subsanar quirúrgicamente cada hallazgo reportado según su ID, CWE y ruta de archivo exacta.

7. **Estrategia de Canales de Lanzamiento y Versionado**:
   - Respetar la nomenclatura y sufijos de paquetes:
     * `.debug` -> `com.signet.app.debug` / `1.0.0-D` (canal exclusivo interno de depuración vía GitHub Actions).
     * `.dev` -> `com.signet.app.dev` / `1.0.0.dev` (canal pre-alpha para pruebas tempranas de nuevas funciones, firmado por el desarrollador).
     * `.beta` -> `com.signet.app.beta` / `1.0.0-B` (canal beta de herramientas pulidas candidatas a definitivas).
     * `.estable` -> `com.signet.app` / `1.0.0-E` (canal definitivo de producción para tiendas de terceros).
   - Las firmas para `.dev`, `.beta` y `.estable` se administran mediante GitHub Secrets.

