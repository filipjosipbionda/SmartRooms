package com.benza.smartrooms.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.LruCache
import androidx.core.graphics.createBitmap
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.security.MessageDigest

internal object PdfThumbnailLoader {
    fun getCachedThumbnail(source: String): Bitmap? = memoryCache.get(source.sha1())

    suspend fun loadThumbnail(
        context: Context,
        source: String,
    ): Bitmap? {
        val cacheKey = source.sha1()
        memoryCache.get(cacheKey)?.let { return it }
        val loadLock =
            synchronized(loadLocks) {
                loadLocks.getOrPut(cacheKey) { Mutex() }
            }

        return loadLock.withLock {
            memoryCache.get(cacheKey)?.let { return@withLock it }
            withContext(Dispatchers.IO) {
                runCatching {
                    openPdfFileDescriptor(context, source)?.use { fileDescriptor ->
                        PdfRenderer(fileDescriptor).use { renderer ->
                            if (renderer.pageCount <= 0) return@withContext null
                            renderer.openPage(0).use { page ->
                                val width = PDF_THUMBNAIL_WIDTH
                                val height =
                                    (width * page.height / page.width.toFloat())
                                        .toInt()
                                        .coerceAtLeast(PDF_THUMBNAIL_MIN_HEIGHT)
                                val bitmap = createBitmap(width, height)
                                bitmap.eraseColor(android.graphics.Color.WHITE)
                                page.render(
                                    bitmap,
                                    null,
                                    null,
                                    PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY,
                                )
                                memoryCache.put(cacheKey, bitmap)
                                bitmap
                            }
                        }
                    }
                }.getOrNull()
            }
        }
    }

    private fun openPdfFileDescriptor(
        context: Context,
        source: String,
    ): ParcelFileDescriptor? =
        when {
            source.startsWith("content://") || source.startsWith("file://") -> {
                context.contentResolver.openFileDescriptor(source.toUri(), "r")
            }

            source.startsWith("http://") || source.startsWith("https://") -> {
                val cachedFile = context.cachePdfFile(source)
                ParcelFileDescriptor.open(cachedFile, ParcelFileDescriptor.MODE_READ_ONLY)
            }

            else -> {
                context.contentResolver.openFileDescriptor(source.toUri(), "r")
            }
        }

    private fun Context.cachePdfFile(source: String): File {
        val cacheDirectory = File(cacheDir, "pdf-previews").apply { mkdirs() }
        val cacheKey = source.sha1()
        val cachedFile = File(cacheDirectory, "$cacheKey.pdf")
        if (cachedFile.exists() && cachedFile.length() > 0L) return cachedFile

        val tempFile = File(cacheDirectory, "$cacheKey.tmp")
        if (tempFile.exists()) tempFile.delete()
        val connection =
            URL(source).openConnection().apply {
                connectTimeout = PDF_DOWNLOAD_TIMEOUT_MS
                readTimeout = PDF_DOWNLOAD_TIMEOUT_MS
            }
        connection.getInputStream().use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        }
        if (cachedFile.exists()) cachedFile.delete()
        if (!tempFile.renameTo(cachedFile)) {
            tempFile.copyTo(cachedFile, overwrite = true)
            tempFile.delete()
        }
        return cachedFile
    }

    private fun String.sha1(): String =
        MessageDigest
            .getInstance("SHA-1")
            .digest(toByteArray())
            .joinToString(separator = "") { byte -> "%02x".format(byte) }

    private val memoryCache =
        object : LruCache<String, Bitmap>(PDF_THUMBNAIL_MEMORY_CACHE_SIZE_KB) {
            override fun sizeOf(
                key: String,
                value: Bitmap,
            ): Int = value.allocationByteCount / 1024
        }
    private val loadLocks = mutableMapOf<String, Mutex>()
}

private const val PDF_THUMBNAIL_WIDTH = 160
private const val PDF_THUMBNAIL_MIN_HEIGHT = 208
private const val PDF_THUMBNAIL_MEMORY_CACHE_SIZE_KB = 6 * 1024
private const val PDF_DOWNLOAD_TIMEOUT_MS = 15_000
