package com.tomclaw.appsend_rb.util

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.Key
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.data.DataFetcher
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoader.LoadData
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.security.MessageDigest

/**
 * Created by ivsolkin on 23.12.16.
 */
class AppIconGlideLoader(private val packageManager: PackageManager) : ModelLoader<AppIconData, InputStream> {
    override fun buildLoadData(iconData: AppIconData,
                               width: Int,
                               height: Int,
                               options: Options): LoadData<InputStream>? {
        return LoadData(IconKey(iconData), object : DataFetcher<InputStream> {
            override fun loadData(priority: Priority, callback: DataFetcher.DataCallback<in InputStream>) {
                try {
                    val icon = packageManager.getApplicationIcon(iconData.packageName)
                    val bitmap = drawableToBitmap(icon)
                    val baos = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
                    callback.onDataReady(ByteArrayInputStream(baos.toByteArray()))
                } catch (ex: Exception) {
                    callback.onLoadFailed(ex)
                }
            }

            override fun cleanup() {}
            override fun cancel() {}
            override fun getDataClass(): Class<InputStream> {
                return InputStream::class.java
            }

            override fun getDataSource(): DataSource {
                return DataSource.LOCAL
            }
        })
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        val bitmap: Bitmap
        if (drawable is BitmapDrawable) {
            val bitmapDrawable = drawable
            if (bitmapDrawable.bitmap != null) {
                return bitmapDrawable.bitmap
            }
        }
        bitmap = if (drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0) {
            Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        } else {
            Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        }
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    override fun handles(iconData: AppIconData): Boolean {
        return true
    }

    private class IconKey internal constructor(private val iconData: AppIconData) : Key {
        override fun updateDiskCacheKey(messageDigest: MessageDigest) {
            messageDigest.update((iconData.packageName + iconData.versionCode).toByteArray())
        }

    }

}