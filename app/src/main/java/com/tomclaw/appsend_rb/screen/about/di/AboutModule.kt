package com.tomclaw.appsend_rb.screen.about.di

import android.content.Context
import com.tomclaw.appsend_rb.screen.about.AboutPresenter
import com.tomclaw.appsend_rb.screen.about.AboutPresenterImpl
import com.tomclaw.appsend_rb.screen.about.AboutResourceProvider
import com.tomclaw.appsend_rb.screen.about.AboutResourceProviderImpl
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
        resourceProvider: AboutResourceProvider,
        schedulers: SchedulersFactory
    ): AboutPresenter = AboutPresenterImpl(resourceProvider, schedulers)

    @Provides
    @PerActivity
    internal fun providePreferencesProvider(): PreferencesProvider {
        return PreferencesProviderImpl(context)
    }

    @Provides
    @PerActivity
    internal fun provideResourceProvider(): AboutResourceProvider {
        return AboutResourceProviderImpl(
            context.packageName,
            context.packageManager,
            context.resources
        )
    }

}