package com.tomclaw.appsend_rb.util

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.util.LruCache
import java.util.concurrent.Executors

/**
 * Lightweight package-icon → [Bitmap] loader for Knopochki's `Image` node.
 *
 * Icons are rasterised off the main thread, cached in memory by package name and
 * delivered back on the main thread. The callback echoes the package name so a
 * recycled list row can drop a result that arrived after it was rebound.
 */
object AppIcons {

    private val maxMemoryKb = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cache = object : LruCache<String, Bitmap>(maxMemoryKb / 16) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }
    private val executor = Executors.newFixedThreadPool(4)
    private val main = Handler(Looper.getMainLooper())

    /** Returns an already-decoded icon without touching disk, or null on a miss. */
    fun cached(packageName: String): Bitmap? = cache.get(packageName)

    /**
     * Loads the icon for [packageName], rasterised to [sizePx] square. On a cache
     * hit [onReady] fires synchronously; otherwise it fires on the main thread once
     * the bitmap is ready. [onReady] receives the package name it was loaded for.
     */
    fun load(
        context: Context,
        packageName: String,
        sizePx: Int,
        onReady: (packageName: String, bitmap: Bitmap) -> Unit,
    ) {
        cache.get(packageName)?.let { onReady(packageName, it); return }
        val pm = context.applicationContext.packageManager
        executor.execute {
            val bitmap = render(pm, packageName, sizePx) ?: return@execute
            cache.put(packageName, bitmap)
            main.post { onReady(packageName, bitmap) }
        }
    }

    private fun render(pm: PackageManager, packageName: String, sizePx: Int): Bitmap? = try {
        rasterize(pm.getApplicationIcon(packageName), sizePx)
    } catch (ignored: Throwable) {
        // Some packages refuse to hand over an icon at all. A row with a blank
        // square reads as a rendering bug, so fall back to the platform's own
        // stand-in — the same one the launcher shows.
        defaultIcon(pm, sizePx)
    }

    private fun defaultIcon(pm: PackageManager, sizePx: Int): Bitmap? = try {
        rasterize(pm.defaultActivityIcon, sizePx)
    } catch (ignored: Throwable) {
        null
    }

    private fun rasterize(drawable: Drawable, sizePx: Int): Bitmap {
        if (drawable is BitmapDrawable) {
            drawable.bitmap?.let { return it }
        }
        val size = if (sizePx > 0) sizePx else maxOf(drawable.intrinsicWidth, 1)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, size, size)
        drawable.draw(canvas)
        return bitmap
    }
}
