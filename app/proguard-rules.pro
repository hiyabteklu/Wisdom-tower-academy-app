# ProGuard and R8 rules for Wisdom Tower Academy (Google Play Production)

# 1. Preserve JavaScript Interface methods called from WebView JavaScript
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# 2. Keep WebViewClient and WebChromeClient callbacks
-keepclassmembers class * extends android.webkit.WebViewClient {
    public *;
}
-keepclassmembers class * extends android.webkit.WebChromeClient {
    public *;
}

# 3. Preserve source file and line numbers for de-obfuscated crash reports in Play Console
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# 4. Keep main components referenced by AndroidManifest.xml
-keep class com.wisdomtower.academy.WisdomTowerApplication { *; }
-keep class com.wisdomtower.academy.MainActivity { *; }

# 5. Keep data models and serialization structures
-keepclassmembers class com.wisdomtower.academy.OfflineVault$* { *; }
-keepclassmembers class com.wisdomtower.academy.WebCacheVault$* { *; }
-keep class com.wisdomtower.academy.OfflineVault$DownloadProgress { *; }
-keep class com.wisdomtower.academy.WebCacheVault$EntryMeta { *; }

# 6. Coil & Coil GIF decoder
-keep class coil.** { *; }
-dontwarn coil.**

# 7. (Slice S3, docs/SECURITY_PLAN_CONTENT_ACCESS.md) Annotations and generic signatures that libraries read at run time
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# 8. (Slice S3) Remove verbose, debug and info log lines from the release app so nothing useful is written to
# the phone log. Log.w and Log.e stay (they carry crash and failure information only, no tokens or content).
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# 9. (Slice S3) What was checked, so the next agent does not have to repeat it (2026-10-20):
# - The app code parses JSON with org.json (part of Android itself), NOT with Moshi, Retrofit or Gson. Moshi and
#   Retrofit are declared in app/build.gradle.kts but no source file uses them, so shrinking cannot break JSON
#   parsing and no model keep rules are needed. If someone starts using Moshi reflection or Gson later, add keep
#   rules for those model classes in the same commit.
# - Room generates its own code and ships its own keep rules; OkHttp, Coroutines and Firebase ship theirs.
# - Manifest components (activity, receivers, service) are kept automatically by the Android build tools.
