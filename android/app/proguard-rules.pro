# Imtihan Pro — ProGuard / R8 Keep Rules for Release APK Builds
# Keep BuildConfig and application models/repositories intact under R8 minification.

-keep class com.imtihanpro.app.BuildConfig { *; }
-keep class com.imtihanpro.app.data.remote.** { *; }
-keep class com.imtihanpro.app.data.local.room.** { *; }
-keep class com.imtihanpro.app.domain.model.** { *; }

# Keep Room generated database implementation classes
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Keep DataStore preferences protobuf / runtime classes
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite {
    <fields>;
}

# Kotlin Coroutines keep rules
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.flow.**
