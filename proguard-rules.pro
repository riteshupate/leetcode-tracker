# Add project specific ProGuard rules here.

# Keep Gson classes
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Keep OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Keep API models
-keep class com.leetcode.tracker.api.** { *; }

# Keep widget and notification receivers
-keep class com.leetcode.tracker.widget.** { *; }
-keep class com.leetcode.tracker.notifications.** { *; }
