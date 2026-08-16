package com.example.crypto

/**
 * Utility object for generating configuration snippets and CI/CD templates
 * for Gradle (KTS / Groovy), GitHub Actions, and CLI apksigner tools.
 */
object SnippetGenerator {

    /**
     * Generates Gradle build.gradle.kts (Kotlin DSL) signing config snippet
     */
    fun generateGradleKtsSnippet(fileName: String, alias: String): String {
        return """
            // ==========================================
            // app/build.gradle.kts (Kotlin DSL)
            // ==========================================
            android {
                signingConfigs {
                    create("release") {
                        // 1. Opcional: leer ruta desde variable de entorno o archivo local
                        val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${fileName}"
                        storeFile = file(keystorePath)
                        storePassword = System.getenv("KEYSTORE_PASSWORD") ?: "tu_contraseña_keystore"
                        keyAlias = System.getenv("KEY_ALIAS") ?: "${alias}"
                        keyPassword = System.getenv("KEY_PASSWORD") ?: "tu_contraseña_clave"
                    }
                }
                buildTypes {
                    release {
                        signingConfig = signingConfigs.getByName("release")
                        isMinifyEnabled = true
                        isShrinkResources = true
                        proguardFiles(
                            getDefaultProguardFile("proguard-android-optimize.txt"),
                            "proguard-rules.pro"
                        )
                    }
                }
            }
        """.trimIndent()
    }

    /**
     * Generates Gradle build.gradle (Groovy DSL) signing config snippet
     */
    fun generateGradleGroovySnippet(fileName: String, alias: String): String {
        return """
            // ==========================================
            // app/build.gradle (Groovy DSL)
            // ==========================================
            android {
                signingConfigs {
                    release {
                        def keystorePath = System.getenv("KEYSTORE_PATH") ?: "${fileName}"
                        storeFile file(keystorePath)
                        storePassword System.getenv("KEYSTORE_PASSWORD") ?: "tu_contraseña_keystore"
                        keyAlias System.getenv("KEY_ALIAS") ?: "${alias}"
                        keyPassword System.getenv("KEY_PASSWORD") ?: "tu_contraseña_clave"
                    }
                }
                buildTypes {
                    release {
                        signingConfig signingConfigs.release
                        minifyEnabled true
                        shrinkResources true
                        proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
                    }
                }
            }
        """.trimIndent()
    }

    /**
     * Generates a ready-to-use GitHub Actions workflow file for automated building and signing
     */
    fun generateGitHubActionsWorkflow(fileName: String, alias: String): String {
        return """
            # ==========================================
            # .github/workflows/android-build-and-sign.yml
            # ==========================================
            name: Build & Sign Android APK / Release

            on:
              push:
                branches: [ "main", "master" ]
                tags: [ "v*" ]
              workflow_dispatch:

            jobs:
              build:
                name: Build & Sign Release APK
                runs-on: ubuntu-latest

                steps:
                  - name: Checkout Code
                    uses: actions/checkout@v4

                  - name: Set up JDK 17
                    uses: actions/setup-java@v4
                    with:
                      distribution: 'temurin'
                      java-version: '17'
                      cache: 'gradle'

                  - name: Decode Base64 Keystore
                    env:
                      KEYSTORE_BASE64: ${'$'}{{ secrets.KEYSTORE_BASE64 }}
                    run: |
                      # Decodifica el secreto Base64 generado por la app al archivo ${fileName}
                      echo "${'$'}KEYSTORE_BASE64" | base64 --decode > app/${fileName}

                  - name: Ensure Gradle Wrapper & Permissions
                    run: |
                      if [ ! -f "./gradlew" ]; then
                        gradle wrapper --gradle-version 9.3.1
                      fi
                      chmod +x ./gradlew

                  - name: Build Release APK with Gradle
                    env:
                      KEYSTORE_PATH: ${fileName}
                      KEYSTORE_PASSWORD: ${'$'}{{ secrets.KEYSTORE_PASSWORD }}
                      KEY_ALIAS: ${'$'}{{ secrets.KEY_ALIAS || '${alias}' }}
                      KEY_PASSWORD: ${'$'}{{ secrets.KEY_PASSWORD }}
                    run: ./gradlew assembleRelease --stacktrace

                  - name: Upload Signed APK as Artifact
                    uses: actions/upload-artifact@v4
                    with:
                      name: signed-release-apk
                      path: app/build/outputs/apk/release/*.apk
                      if-no-files-found: error
        """.trimIndent()
    }

    /**
     * Generates CLI apksigner command snippet
     */
    fun generateApksignerSnippet(fileName: String, alias: String): String {
        return """
            # 1. Alinear el APK antes de firmar (si no fue compilado por Gradle):
            zipalign -v -p 4 app-unsigned.apk app-aligned.apk

            # 2. Firmar APK con apksigner (esquemas v1, v2 y v3):
            apksigner sign --ks ${fileName} \
              --ks-key-alias ${alias} \
              --ks-pass env:KEYSTORE_PASSWORD \
              --key-pass env:KEY_PASSWORD \
              --out app-release-signed.apk app-aligned.apk

            # 3. Verificar la firma del APK e imprimir huellas (SHA-256 / SHA-1):
            apksigner verify --verbose --print-certs app-release-signed.apk
        """.trimIndent()
    }

    /**
     * Generates Google Play PEPK tool command snippet
     */
    fun generatePepkSnippet(fileName: String, alias: String): String {
        return PepkGenerator.generatePepkCliCommand(fileName, alias)
    }
}
