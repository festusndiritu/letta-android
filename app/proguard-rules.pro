# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# ────────────────────────────────────────────────────────────────────────────
# Essential attributes
# ────────────────────────────────────────────────────────────────────────────
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations, AnnotationDefault
-keepattributes SourceFile, LineNumberTable

# ────────────────────────────────────────────────────────────────────────────
# Kotlin Serialization
# ────────────────────────────────────────────────────────────────────────────
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep all serializable classes
-keep,includedescriptorclasses class **$$serializer { *; }
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep @Serializable annotated classes
-keep @kotlinx.serialization.Serializable class ** { *; }

# ────────────────────────────────────────────────────────────────────────────
# Room Database
# ────────────────────────────────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Keep all entity classes
-keep class dev.mizzenmast.letta.data.local.entity.** { *; }

# ────────────────────────────────────────────────────────────────────────────
# Retrofit & OkHttp
# ────────────────────────────────────────────────────────────────────────────
# Retrofit service interfaces
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# Keep API service interfaces
-keep interface dev.mizzenmast.letta.data.remote.api.** { *; }

# OkHttp platform used only on JVM and when Conscrypt dependency is available.
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ────────────────────────────────────────────────────────────────────────────
# Hilt / Dagger
# ────────────────────────────────────────────────────────────────────────────
-dontwarn com.google.errorprone.annotations.**

# ────────────────────────────────────────────────────────────────────────────
# Coroutines
# ────────────────────────────────────────────────────────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ────────────────────────────────────────────────────────────────────────────
# Data Transfer Objects (DTOs)
# ────────────────────────────────────────────────────────────────────────────
-keep class dev.mizzenmast.letta.data.remote.dto.** { *; }

# ────────────────────────────────────────────────────────────────────────────
# Debugging (optional - comment out for release)
# ────────────────────────────────────────────────────────────────────────────
-renamesourcefileattribute SourceFile