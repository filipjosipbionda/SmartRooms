package com.benza.smartrooms

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.benza.smartrooms.data.di.dataModule
import com.benza.smartrooms.ui.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

/**
 * Application entry point that boots the Koin dependency graph.
 */
class SmartRoomsApplication :
    Application(),
    ImageLoaderFactory {
    /**
     * Initializes dependency injection once for the whole process.
     */
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@SmartRoomsApplication)
            modules(dataModule, viewModelModule)
        }
    }

    /**
     * Shares a bounded memory and disk cache across every avatar and attachment request.
     * Profile image URLs are immutable because each upload gets a new Storage object.
     */
    override fun newImageLoader(): ImageLoader =
        ImageLoader
            .Builder(this)
            .memoryCache {
                MemoryCache
                    .Builder(this)
                    .maxSizePercent(MEMORY_CACHE_PERCENT)
                    .build()
            }.diskCache {
                DiskCache
                    .Builder()
                    .directory(cacheDir.resolve(IMAGE_CACHE_DIRECTORY))
                    .maxSizeBytes(DISK_CACHE_SIZE_BYTES)
                    .build()
            }.respectCacheHeaders(false)
            .crossfade(true)
            .build()
}

private const val MEMORY_CACHE_PERCENT = 0.20
private const val DISK_CACHE_SIZE_BYTES = 96L * 1024L * 1024L
private const val IMAGE_CACHE_DIRECTORY = "coil_image_cache"
