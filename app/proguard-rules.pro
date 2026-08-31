# ProGuard / R8 Configuration for Signet Release & Beta builds

# Preserve line numbers for stacktraces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# BouncyCastle Cryptography (Fine-tuned for Signet keystores and APK signing)
-keep class org.bouncycastle.jce.provider.** { *; }
-keep class org.bouncycastle.asn1.** { *; }
-keep class org.bouncycastle.cert.** { *; }
-keep class org.bouncycastle.operator.** { *; }
-keep class org.bouncycastle.pkcs.** { *; }
-keep class org.bouncycastle.crypto.** { *; }
-dontwarn org.bouncycastle.**
-dontnote org.bouncycastle.**

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
