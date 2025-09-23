# Media3/ExoPlayer
-keep class com.google.android.exoplayer2.** { *; }
-keep class androidx.media3.** { *; }

# Gson/Retrofit/OkHttp
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn okhttp3.**
-dontwarn okio.**

# CRÍTICO: Protección para modelos de datos (previene crashes en release builds)
-keep class com.ivanmarty.rovecast.model.** { *; }
-keepclassmembers class * { @com.google.gson.annotations.SerializedName <fields>; }

# Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.AppGlideModule
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** { *; }

# Cast (corregido: rovecast en lugar de radiola)
-keep class com.ivanmarty.rovecast.cast.** { *; }
-keep class com.ivanmarty.rovecast.App

