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
import com.wisdomtower.academy.data.content.ContentAccess
import com.wisdomtower.academy.ui.study.NativePlannerState
import com.wisdomtower.academy.ui.study.PlannerSync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
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

        // Slice S3: release builds close themselves when opened up for inspection (see IntegrityGuard).
        if (IntegrityGuard.isTampered(this)) {
            android.os.Process.killProcess(android.os.Process.myPid())
            return
        }

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

        // S-A2 follow-up: ask the server which packages the student owns as soon as a student is signed in
        // (app start with a saved session, or a fresh sign-in), so the first course screen already knows.
        // Runs once per signed-in student id; ContentAccess.warmUp() is cached 30 s and never throws on network errors.
        appScope.launch {
            AuthStateManager.currentUser
                .map { it?.id }
                .distinctUntilChanged()
                .collect { userId ->
                    if (userId != null) ContentAccess.warmUp()
                }
        }

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
                    // Refresh ownership when the app comes to the front (a grant may have been added meanwhile).
                    ContentAccess.warmUp()
                }
                // Study planner: send unsent edits and take a newer server copy (an admin may have pushed a plan).
                appScope.launch {
                    NativePlannerState.ensureLoaded(this@WisdomTowerApplication)
                    PlannerSync.sync()
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
