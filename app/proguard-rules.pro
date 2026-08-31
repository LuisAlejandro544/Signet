# ProGuard / R8 Configuration for Signet Release & Beta builds

# Preserve line numbers for stacktraces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# BouncyCastle Cryptography (Afinado para generación de keystores, firmas X.509 y APK signing)
-keep class org.bouncycastle.jce.provider.BouncyCastleProvider { *; }
-keep class org.bouncycastle.jce.provider.** { <fields>; <methods>; }
-keep class org.bouncycastle.asn1.** { *; }
-keep class org.bouncycastle.cert.** { *; }
-keep class org.bouncycastle.operator.** { *; }
-keep class org.bouncycastle.pkcs.** { *; }
-keep class org.bouncycastle.jcajce.** { *; }
-keep class org.bouncycastle.crypto.digests.** { *; }
-keep class org.bouncycastle.crypto.macs.** { *; }
-keep class org.bouncycastle.crypto.params.** { *; }
-keep class org.bouncycastle.crypto.generators.** { *; }
-keep class org.bouncycastle.crypto.signers.** { *; }
-keep class org.bouncycastle.crypto.engines.** { *; }
-dontwarn org.bouncycastle.**
-dontnote org.bouncycastle.**

# Optimización de logs: descartar llamadas a android.util.Log en compilaciones de Release
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int println(...);
}

# Room Database & SQLite
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }

# Signet Data Models
-keep class com.example.data.model.** { *; }
-keep class com.example.data.local.** { *; }
-keep class com.example.ui.state.** { *; }

# Kotlin Coroutines & Reflection
-keepclassmembers class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Desktop / AWT APIs (referenced in cross-platform desktop service adapters)
-dontwarn java.awt.**
-dontwarn javax.swing.**
-dontwarn java.beans.**
-dontwarn javax.annotation.**
-dontwarn sun.misc.**
