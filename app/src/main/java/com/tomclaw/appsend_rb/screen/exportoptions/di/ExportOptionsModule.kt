package com.tomclaw.appsend_rb.screen.exportoptions.di

import android.content.Context
import com.tomclaw.appsend_rb.data.AndroidValueFormatter
import com.tomclaw.appsend_rb.domain.AppPreferences
import com.tomclaw.appsend_rb.domain.ExportManager
import com.tomclaw.appsend_rb.domain.ExportStorage
import com.tomclaw.appsend_rb.domain.InstalledAppsRepository
import com.tomclaw.appsend_rb.screen.exportoptions.ExportOptionsFragment
import com.tomclaw.appsend_rb.screen.exportoptions.ExportOptionsInteractorImpl
import com.tomclaw.appsend_rb.screen.exportoptions.ExportOptionsPresenter
import com.tomclaw.appsend_rb.screen.exportoptions.ExportOptionsPresenterImpl
import com.tomclaw.appsend_rb.screen.exportoptions.ExportOptionsResourcesImpl
import com.tomclaw.appsend_rb.screen.exportoptions.ExportOptionsState
import com.tomclaw.appsend_rb.util.PerActivity
import com.tomclaw.appsend_rb.domain.SchedulersFactory
import dagger.Module
import dagger.Provides
import dagger.Subcomponent

@PerActivity
@Subcomponent(modules = [ExportOptionsModule::class])
interface ExportOptionsComponent {

    fun inject(fragment: ExportOptionsFragment)

}

@Module
class ExportOptionsModule(
    private val context: Context,
    private val packages: List<String>,
    private val state: ExportOptionsState?,
) {

    @Provides
    @PerActivity
    internal fun providePresenter(
        repository: InstalledAppsRepository,
        preferences: AppPreferences,
        storage: ExportStorage,
        exports: ExportManager,
        schedulers: SchedulersFactory,
    ): ExportOptionsPresenter = ExportOptionsPresenterImpl(
        ExportOptionsInteractorImpl(packages, repository, preferences, storage, exports),
        ExportOptionsResourcesImpl(context),
        AndroidValueFormatter(context),
        schedulers,
        state,
    )

}
