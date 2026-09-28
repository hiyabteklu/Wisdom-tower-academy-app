package com.example

import android.app.Application
import android.os.Build
import coil.Coil
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.memory.MemoryCache
import coil.request.ImageRequest

class WisdomTowerApplication : Application(), ImageLoaderFactory {

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
    }

    companion object {
        lateinit var instance: WisdomTowerApplication
            private set
    }
}
