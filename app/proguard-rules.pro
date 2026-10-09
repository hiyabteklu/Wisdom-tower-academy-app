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
-keep class com.wisdomtower.academy.TabWebViewHost* { *; }
-keep class com.wisdomtower.academy.SingleTabState { *; }
-keep class com.wisdomtower.academy.TabWebViewHostState { *; }

# 5. Keep data models and serialization structures
-keepclassmembers class com.wisdomtower.academy.OfflineVault$* { *; }
-keepclassmembers class com.wisdomtower.academy.WebCacheVault$* { *; }
-keep class com.wisdomtower.academy.OfflineVault$DownloadProgress { *; }
-keep class com.wisdomtower.academy.WebCacheVault$EntryMeta { *; }

# 6. Coil & Coil GIF decoder
-keep class coil.** { *; }
-dontwarn coil.**
