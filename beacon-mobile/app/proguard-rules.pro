# Beacon ProGuard Rules

# Keep Beacon SDK classes
-keep class org.beacon.sdk.** { *; }
-keep class org.beacon.core.** { *; }
-keep class org.beacon.mesh.** { *; }
-keep class org.beacon.radio.** { *; }

# Keep Kotlin serialization
-keep class kotlinx.serialization.** { *; }
-keep @kotlinx.serialization.Serializable class * {
    <fields>;
}

# Keep Room entities
-keep class org.beacon.mobile.data.** { *; }

# Keep MapLibre
-keep class org.maplibre.** { *; }

# Keep Coil
-keep class io.coil.** { *; }

# Keep WorkManager
-keep class androidx.work.** { *; }

# Keep DataStore
-keep class androidx.datastore.** { *; }

# Keep SQLCipher
-keep class net.sqlcipher.** { *; }

# Keep Kotlin coroutines
-keep class kotlinx.coroutines.** { *; }

# Keep RxJava
-keep class io.reactivex.** { *; }

# Keep OkHttp
-keep class okhttp3.** { *; }
-keep class okio.** { *; }

# Keep Gson (if used)
-keep class com.google.gson.** { *; }

# Keep MapLibre native
-keep class org.maplibre.android.** { *; }

# Prevent obfuscation of annotation processors
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Keep native methods
-keepclasseswithmembers class * {
    native <methods>;
}

# Keep enum values
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}