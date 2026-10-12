# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.

# Keep Room entities and relations
-keep class com.example.phototag.data.entity.** { *; }
-keep class com.example.phototag.data.relation.** { *; }
-keep class com.example.phototag.data.model.** { *; }
-keep class com.example.phototag.data.dao.** { *; }
-keep class * extends androidx.room.RoomDatabase

# Keep Coil Image loaders, Okio and OkHttp
-keep class coil3.** { *; }
-dontwarn coil3.**
-keep class okio.** { *; }
-dontwarn okio.**
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**

# Keep AndroidX WorkManager & MediaStore
-keep class androidx.work.** { *; }
-dontwarn androidx.work.**

# Keep Media3 ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**
