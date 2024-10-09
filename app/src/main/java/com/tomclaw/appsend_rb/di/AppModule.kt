package com.tomclaw.appsend_rb.di

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import com.tomclaw.appsend.util.Analytics
import com.tomclaw.appsend.util.AnalyticsImpl
import com.tomclaw.appsend_rb.util.SchedulersFactory
import com.tomclaw.appsend_rb.util.SchedulersFactoryImpl
import dagger.Module
import dagger.Provides
import java.util.Locale
import javax.inject.Singleton

@Module
class AppModule(private val app: Application) {

    @Provides
    @Singleton
    internal fun provideContext(): Context = app

    @Provides
    @Singleton
    internal fun provideSchedulersFactory(): SchedulersFactory = SchedulersFactoryImpl()

    @Provides
    @Singleton
    internal fun provideLocale(): Locale = Locale.getDefault()

    @Provides
    @Singleton
    internal fun provideAnalytics(): Analytics = AnalyticsImpl(app)

    @Provides
    @Singleton
    internal fun provideManager(
        context: Context
    ): PackageManager = context.packageManager

}
