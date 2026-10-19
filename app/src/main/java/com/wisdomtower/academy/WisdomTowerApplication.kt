package com.wisdomtower.academy

import android.app.Activity
import android.app.Application
import android.os.Build
import android.os.Bundle
import coil.Coil
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.memory.MemoryCache
import coil.request.ImageRequest
import com.wisdomtower.academy.data.auth.AuthStateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class WisdomTowerApplication : Application(), ImageLoaderFactory {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .respectCacheHeaders(false)
            .allowHardware(true)
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        val imageLoader = newImageLoader()
        Coil.setImageLoader(imageLoader)

        // Pre-warm GIF bytes directly in memory
        BrandBytes.preload(this)

        // Initialize Web Cache Vault for instant offline availability of all visited pages
        WebCacheVault.init(this)

        // Pre-decode GIF animation directly into Coil's memory cache
        val preWarmAssetRequest = ImageRequest.Builder(this)
            .data("file:///android_asset/brand/animation.gif")
            .memoryCacheKey("brand_animation_gif")
            .allowHardware(true)
            .build()
        imageLoader.enqueue(preWarmAssetRequest)

        val preWarmBytesRequest = ImageRequest.Builder(this)
            .data(BrandBytes.gif(this))
            .memoryCacheKey("brand_animation_gif_bytes")
            .allowHardware(true)
            .build()
        imageLoader.enqueue(preWarmBytesRequest)

        // Quiet background FCM token registration
        com.wisdomtower.academy.fcm.FcmTokenRegistrar.checkAndRegisterToken(this)

        // Load the saved (encrypted) sign-in session, then keep it fresh. The NATIVE side is the only
        // one that renews the Supabase login (the website page inside the WebView is blocked from
        // doing it, see src/lib/supabase.ts in the website repo): on every return to the foreground,
        // and every 45 seconds while the app is visible (AuthStateManager.onAppForeground), the
        // session is renewed when it is close to expiry and pushed into every open website WebView.
        AuthStateManager.init(this)
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityResumed(activity: Activity) {
                AuthStateManager.onAppForeground(activity)
                appScope.launch {
                    AuthStateManager.refreshIfNeeded()
                    // Also covers the case where nothing needed renewing but a page was restored
                    // from the background holding an older copy of the session.
                    AuthStateManager.injectIntoAllWebViews()
                }
            }
            override fun onActivityPaused(activity: Activity) {
                AuthStateManager.onAppBackground(activity)
            }
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }

    companion object {
        lateinit var instance: WisdomTowerApplication
            private set
    }
}
