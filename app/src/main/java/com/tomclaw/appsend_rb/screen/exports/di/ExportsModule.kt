package com.tomclaw.appsend_rb.screen.exports.di

import android.content.Context
import com.tomclaw.appsend_rb.data.AndroidValueFormatter
import com.tomclaw.appsend_rb.domain.AppPreferences
import com.tomclaw.appsend_rb.domain.ArchiveInspector
import com.tomclaw.appsend_rb.domain.ExportChanges
import com.tomclaw.appsend_rb.domain.ExportManager
import com.tomclaw.appsend_rb.domain.ExportStorage
import com.tomclaw.appsend_rb.domain.InstalledAppsRepository
import com.tomclaw.appsend_rb.screen.exports.ExportsFragment
import com.tomclaw.appsend_rb.screen.exports.ExportsInteractorImpl
import com.tomclaw.appsend_rb.screen.exports.ExportsPresenter
import com.tomclaw.appsend_rb.screen.exports.ExportsPresenterImpl
import com.tomclaw.appsend_rb.screen.exports.ExportsState
import com.tomclaw.appsend_rb.domain.Analytics
import com.tomclaw.appsend_rb.util.PerActivity
import com.tomclaw.appsend_rb.domain.SchedulersFactory
import dagger.Module
import dagger.Provides
import dagger.Subcomponent

@PerActivity
@Subcomponent(modules = [ExportsModule::class])
interface ExportsComponent {

    fun inject(fragment: ExportsFragment)

}

@Module
class ExportsModule(
    private val context: Context,
    private val state: ExportsState?,
) {

    @Provides
    @PerActivity
    internal fun providePresenter(
        storage: ExportStorage,
        inspector: ArchiveInspector,
        repository: InstalledAppsRepository,
        manager: ExportManager,
        changes: ExportChanges,
        preferences: AppPreferences,
        schedulers: SchedulersFactory,
        analytics: Analytics,
    ): ExportsPresenter = ExportsPresenterImpl(
        ExportsInteractorImpl(storage, inspector, repository, manager, changes, preferences, schedulers),
        AndroidValueFormatter(context),
        schedulers,
        analytics,
        state,
    )

}
