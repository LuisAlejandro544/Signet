// Module build file for Signet Desktop (Windows / macOS / Linux)
plugins {
    kotlin("jvm")
    // org.jetbrains.compose plugin se aplica en entornos con Compose Multiplatform
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation(project(":app"))
    implementation(libs.bouncycastle.bcprov)
    implementation(libs.bouncycastle.bcpkix)
    implementation(libs.kotlinx.coroutines.core)
}

// Configuración de distribución nativa para Windows (.exe / .msi) y otros SOs
tasks.register<JavaExec>("runDesktop") {
    group = "application"
    description = "Ejecuta Signet en entorno de escritorio nativo"
    mainClass.set("com.example.desktop.DesktopLauncher")
    classpath = sourceSets["main"].runtimeClasspath
}
