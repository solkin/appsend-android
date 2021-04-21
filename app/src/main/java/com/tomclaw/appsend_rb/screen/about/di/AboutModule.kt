package com.tomclaw.appsend_rb.screen.about.di

import android.content.Context
import com.tomclaw.appsend_rb.screen.about.AboutPresenter
import com.tomclaw.appsend_rb.screen.about.AboutPresenterImpl
import com.tomclaw.appsend_rb.screen.apps.PreferencesProvider
import com.tomclaw.appsend_rb.screen.apps.PreferencesProviderImpl
import com.tomclaw.appsend_rb.util.PerActivity
import com.tomclaw.appsend_rb.util.SchedulersFactory
import dagger.Module
import dagger.Provides

@Module
class AboutModule(
    private val context: Context
) {

    @Provides
    @PerActivity
    internal fun providePresenter(
        schedulers: SchedulersFactory
    ): AboutPresenter = AboutPresenterImpl(schedulers)

    @Provides
    @PerActivity
    internal fun providePreferencesProvider(): PreferencesProvider {
        return PreferencesProviderImpl(context)
    }

}