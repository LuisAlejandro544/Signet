// Module build file for Signet Desktop (Windows / macOS / Linux)
plugins {
    kotlin("jvm")
    alias(libs.plugins.kotlin.compose)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation(libs.bouncycastle.bcprov)
    implementation(libs.bouncycastle.bcpkix)
    implementation(libs.kotlinx.coroutines.core)
}

// Configuración de distribución nativa para Windows (.exe / .msi) y ejecución Desktop
tasks.register<JavaExec>("runDesktop") {
    group = "application"
    description = "Ejecuta Signet en entorno de escritorio nativo (Windows / Linux / macOS)"
    mainClass.set("com.example.desktop.DesktopLauncher")
    classpath = sourceSets["main"].runtimeClasspath
    jvmArgs = listOf(
        "-Dfile.encoding=UTF-8",
        "-Dsun.java2d.dpiaware=true"
    )
}

