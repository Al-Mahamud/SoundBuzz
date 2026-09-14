# Add project specific ProGuard rules here.
-keep class com.example.musictube.data.remote.model.** { *; }
-keepclassmembers class * {
    @androidx.room.* <methods>;
    @androidx.room.* <fields>;
}
