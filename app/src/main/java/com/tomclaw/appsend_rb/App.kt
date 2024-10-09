package com.tomclaw.appsend_rb

import android.app.Application
import com.tomclaw.appsend_rb.di.AppComponent
import com.tomclaw.appsend_rb.di.AppModule
import com.tomclaw.appsend_rb.di.DaggerAppComponent
import com.tomclaw.appsend_rb.util.AppIconLoader
import com.tomclaw.cache.DiskLruCache
import com.tomclaw.imageloader.SimpleImageLoader.initImageLoader
import com.tomclaw.imageloader.core.DiskCacheImpl
import com.tomclaw.imageloader.core.FileProvider
import com.tomclaw.imageloader.core.FileProviderImpl
import com.tomclaw.imageloader.core.MainExecutorImpl
import com.tomclaw.imageloader.core.MemoryCacheImpl
import com.tomclaw.imageloader.util.BitmapDecoder
import com.tomclaw.imageloader.util.loader.ContentLoader
import com.tomclaw.imageloader.util.loader.FileLoader
import com.tomclaw.imageloader.util.loader.UrlLoader
import java.io.IOException
import java.util.concurrent.Executors

class App : Application() {

    lateinit var component: AppComponent
        private set

    override fun onCreate() {
        super.onCreate()
        component = buildComponent()
        initImageLoader()
    }

    private fun buildComponent(): AppComponent {
        return DaggerAppComponent.builder()
            .appModule(AppModule(this))
            .build()
    }

    private fun initImageLoader() {
        try {
            val fileProvider: FileProvider = FileProviderImpl(
                cacheDir,
                DiskCacheImpl(DiskLruCache.create(cacheDir, 15728640L)),
                UrlLoader(),
                FileLoader(assets),
                ContentLoader(contentResolver),
                AppIconLoader(packageManager),
            )
            this.initImageLoader(
                listOf(BitmapDecoder()),
                fileProvider, MemoryCacheImpl(), MainExecutorImpl(),
                Executors.newFixedThreadPool(5)
            )
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

}

fun Application.getComponent(): AppComponent {
    return (this as App).component
}
