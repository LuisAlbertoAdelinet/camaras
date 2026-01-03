# ProGuard rules for CCTV App

# Keep Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# Moshi
-keepclassmembers class * {
    @com.squareup.moshi.* <fields>;
    @com.squareup.moshi.* <methods>;
}
-keep @com.squareup.moshi.JsonQualifier @interface *
-keepclassmembers @com.squareup.moshi.JsonClass class * extends java.lang.Enum {
    <fields>;
}
-keepclassmembers class com.squareup.moshi.internal.Util {
    private static java.lang.String getKotlinMetadataClassName();
}
-keep class com.squareup.moshi.kotlin.reflect.** { *; }

# Retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Keep data models
-keep class com.rtsp.cctv.data.** { *; }
-keep class com.rtsp.cctv.network.** { *; }

# ExoPlayer / Media3
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Coil
-keep class coil.** { *; }
-dontwarn coil.**
