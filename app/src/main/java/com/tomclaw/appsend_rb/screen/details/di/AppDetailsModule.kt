package com.tomclaw.appsend_rb.screen.details.di

import android.content.Context
import com.tomclaw.appsend_rb.data.AndroidValueFormatter
import com.tomclaw.appsend_rb.data.SystemClipboard
import com.tomclaw.appsend_rb.domain.AppPreferences
import com.tomclaw.appsend_rb.domain.ExportManager
import com.tomclaw.appsend_rb.domain.InstalledAppsRepository
import com.tomclaw.appsend_rb.screen.details.AppDetailsActivity
import com.tomclaw.appsend_rb.screen.details.AppDetailsInteractorImpl
import com.tomclaw.appsend_rb.screen.details.AppDetailsPresenter
import com.tomclaw.appsend_rb.screen.details.AppDetailsPresenterImpl
import com.tomclaw.appsend_rb.screen.details.AppDetailsResourcesImpl
import com.tomclaw.appsend_rb.domain.Analytics
import com.tomclaw.appsend_rb.util.PerActivity
import com.tomclaw.appsend_rb.domain.SchedulersFactory
import dagger.Module
import dagger.Provides
import dagger.Subcomponent

@PerActivity
@Subcomponent(modules = [AppDetailsModule::class])
interface AppDetailsComponent {

    fun inject(activity: AppDetailsActivity)

}

@Module
class AppDetailsModule(
    private val context: Context,
    private val packageName: String,
) {

    @Provides
    @PerActivity
    internal fun providePresenter(
        repository: InstalledAppsRepository,
        preferences: AppPreferences,
        exports: ExportManager,
        schedulers: SchedulersFactory,
        analytics: Analytics,
    ): AppDetailsPresenter = AppDetailsPresenterImpl(
        packageName,
        AppDetailsInteractorImpl(repository, preferences, exports, SystemClipboard(context)),
        AppDetailsResourcesImpl(context, AndroidValueFormatter(context)),
        schedulers,
        analytics,
    )

}
