package com.tomclaw.appsend_rb.di

import android.app.Application
import android.content.Context
import com.tomclaw.appsend_rb.util.SchedulersFactory
import com.tomclaw.appsend_rb.util.SchedulersFactoryImpl
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class AppModule(private val app: Application) {

    @Provides
    @Singleton
    internal fun provideContext(): Context = app

    @Provides
    @Singleton
    internal fun provideSchedulersFactory(): SchedulersFactory = SchedulersFactoryImpl()

}
