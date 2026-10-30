package com.tomclaw.appsend_rb.screen.apps.di

import android.content.Context
import com.tomclaw.appsend_rb.data.AndroidValueFormatter
import com.tomclaw.appsend_rb.domain.AppPreferences
import com.tomclaw.appsend_rb.domain.ExportManager
import com.tomclaw.appsend_rb.domain.InstalledAppsRepository
import com.tomclaw.appsend_rb.screen.apps.AppsFragment
import com.tomclaw.appsend_rb.screen.apps.AppsInteractorImpl
import com.tomclaw.appsend_rb.screen.apps.AppsPresenter
import com.tomclaw.appsend_rb.screen.apps.AppsPresenterImpl
import com.tomclaw.appsend_rb.screen.apps.AppsState
import com.tomclaw.appsend_rb.domain.Analytics
import com.tomclaw.appsend_rb.util.PerActivity
import com.tomclaw.appsend_rb.domain.SchedulersFactory
import dagger.Module
import dagger.Provides
import dagger.Subcomponent
import java.util.Locale

@PerActivity
@Subcomponent(modules = [AppsModule::class])
interface AppsComponent {

    fun inject(fragment: AppsFragment)

}

@Module
class AppsModule(
    private val context: Context,
    private val state: AppsState?,
) {

    @Provides
    @PerActivity
    internal fun providePresenter(
        repository: InstalledAppsRepository,
        preferences: AppPreferences,
        exports: ExportManager,
        locale: Locale,
        schedulers: SchedulersFactory,
        analytics: Analytics,
    ): AppsPresenter = AppsPresenterImpl(
        AppsInteractorImpl(repository, preferences, exports, locale),
        AndroidValueFormatter(context),
        schedulers,
        analytics,
        state,
    )

}
