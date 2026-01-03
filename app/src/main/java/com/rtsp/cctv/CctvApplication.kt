package com.rtsp.cctv

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.util.DebugLogger

/**
 * Custom Application class with optimized Coil image loading
 * - Disk cache: 50MB for offline/faster loading of snapshots
 * - Memory cache: 25% of available RAM
 * - Crossfade enabled globally
 */
class CctvApplication : Application(), ImageLoaderFactory {
    
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            // Memory cache - 25% of available memory
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            // Disk cache - 50MB for snapshots
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(50 * 1024 * 1024) // 50 MB
                    .build()
            }
            // Enable crossfade animation
            .crossfade(true)
            .crossfade(200)
            // Cache policies
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            // Respect cache headers from server
            .respectCacheHeaders(true)
            .build()
    }
}
