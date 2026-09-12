# Add project specific ProGuard rules here.
# Room
-keep class androidx.room.** { *; }
# Hilt
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.HiltAndroidApp
# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
