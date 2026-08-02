# Maesylan — ProGuard rules
# Keep WebView JavaScript interfaces (if any are added later)
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep the BuildConfig so we can read WEBSITE_URL at runtime
-keep class com.maesylan.app.BuildConfig { *; }

# Standard Android / AndroidX rules
-keepattributes *Annotation*
-dontwarn javax.**
