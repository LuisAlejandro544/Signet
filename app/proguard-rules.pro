# ProGuard / R8 Configuration for Signet Release & Beta builds

# Número de pasadas de optimización y análisis de código muerto en R8
-optimizationpasses 5

# Aplanamiento y Fusión de Paquetes (Flattening)
# Mueve todas las clases ofuscadas al paquete raíz por defecto eliminando pistas de carpetas
-repackageclasses ''
-allowaccessmodification

# Ocultación de Archivos Fuente y Pistas de Depuración
-keepattributes LineNumberTable
-renamesourcefileattribute ""

# BouncyCastle Cryptography (Afinado con R8 Full Mode y Tree-Shaking selectivo)
# Se conserva la instanciación de BouncyCastleProvider y servicios JCE requeridos
-keep class org.bouncycastle.jce.provider.BouncyCastleProvider {
    public <init>();
}
-keepclassmembers class org.bouncycastle.jce.provider.** {
    public <init>(...);
    public static ** getInstance(...);
}
-keep class org.bouncycastle.asn1.ASN1ObjectIdentifier { *; }
-keep class org.bouncycastle.asn1.ASN1Primitive { *; }
-keep class org.bouncycastle.asn1.ASN1Encodable { *; }
-keep class org.bouncycastle.asn1.x509.** { *; }
-keep class org.bouncycastle.asn1.pkcs.** { *; }
-keep class org.bouncycastle.asn1.x500.** { *; }
-keep class org.bouncycastle.cert.X509CertificateHolder { *; }
-keep class org.bouncycastle.cert.jcajce.** { *; }
-keep class org.bouncycastle.operator.jcajce.** { *; }
-keep class org.bouncycastle.pkcs.PKCS10CertificationRequest { *; }
-keep class org.bouncycastle.pkcs.jcajce.** { *; }
-keep class org.bouncycastle.jcajce.provider.asymmetric.** { *; }
-keep class org.bouncycastle.jcajce.provider.digest.** { *; }
-keep class org.bouncycastle.jcajce.provider.symmetric.** { *; }
-keep class org.bouncycastle.crypto.digests.** { public <init>(...); }
-keep class org.bouncycastle.crypto.macs.** { public <init>(...); }
-keep class org.bouncycastle.crypto.params.** { *; }
-keep class org.bouncycastle.crypto.generators.** { public <init>(...); }
-keep class org.bouncycastle.crypto.signers.** { public <init>(...); }
-keep class org.bouncycastle.crypto.engines.** { public <init>(...); }
-dontwarn org.bouncycastle.**
-dontnote org.bouncycastle.**

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
