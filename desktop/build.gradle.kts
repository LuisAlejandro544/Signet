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

    // Compose Desktop JVM runtime & UI
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
}

sourceSets {
    main {
        java.srcDirs(
            "src/main/java",
            "../app/src/main/java/com/example/crypto",
            "../app/src/main/java/com/example/desktop",
            "../app/src/main/java/com/example/platform",
            "../app/src/main/java/com/example/data/model",
            "../app/src/main/java/com/example/data",
            "../app/src/main/java/com/example/ui"
        )
    }
}

// Generación de JAR ejecutable autónomo con todas las dependencias
tasks.register<Jar>("fatJar") {
    group = "distribution"
    description = "Genera un Fat JAR ejecutable que contiene todas las dependencias de escritorio"
    archiveBaseName.set("Signet-Desktop")
    archiveClassifier.set("all")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes["Main-Class"] = "com.example.desktop.DesktopLauncher"
        attributes["Implementation-Title"] = "Signet Desktop"
        attributes["Implementation-Version"] = "1.0.0"
    }
    from(sourceSets["main"].output)
    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    })
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

