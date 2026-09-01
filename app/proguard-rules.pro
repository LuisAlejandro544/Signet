# ProGuard / R8 Configuration for Signet Release & Beta builds

# Número de pasadas de optimización y análisis de código muerto en R8
-optimizationpasses 5

# Ocultación de Archivos Fuente y Pistas de Depuración
-keepattributes *Annotation*,Exceptions,InnerClasses,Signature,LineNumberTable,EnclosingMethod
-renamesourcefileattribute ""

# BouncyCastle Cryptography (Seguridad JCA/JCE y Serializadores PKCS12 / BKS / JKS)
# BouncyCastleProvider carga dinámicamente sus algoritmos y keystores vía reflexión interna
-keep class org.bouncycastle.** { *; }
-keep interface org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**
-dontnote org.bouncycastle.**

# Java Security Provider SPI y servicios de KeyStore
-keep class java.security.** { *; }
-keep interface java.security.** { *; }
-keep class javax.crypto.** { *; }
-keep interface javax.crypto.** { *; }

# Jetpack Compose & Kotlin Runtime
-dontwarn androidx.compose.**
-keepclassmembers class androidx.compose.runtime.Recomposer { *; }

# Optimización de logs: descartar llamadas a android.util.Log en compilaciones de Release
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int println(...);
}

# Eliminación de Nombres de Variables en Bytecode (Kotlin Intrinsics)
# Suprime llamadas que filtran nombres de variables reales en texto plano dentro del DEX
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    public static void checkNotNullParameter(java.lang.Object, java.lang.String);
    public static void checkNotNullExpressionValue(java.lang.Object, java.lang.String);
    public static void checkParameterIsNotNull(java.lang.Object, java.lang.String);
    public static void throwParameterIsNullException(java.lang.String);
}

# Room Database & SQLite (Solo mantener clases anotadas necesarias para SQLite)
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }

# Kotlin Coroutines & Reflection
-keepclassmembers class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Desktop / AWT APIs (referenced in cross-platform desktop service adapters)
-dontwarn java.awt.**
-dontwarn javax.swing.**
-dontwarn java.beans.**
-dontwarn javax.annotation.**
-dontwarn sun.misc.**
