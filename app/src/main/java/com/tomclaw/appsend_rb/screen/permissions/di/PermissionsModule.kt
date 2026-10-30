package com.tomclaw.appsend_rb.screen.permissions.di

import com.tomclaw.appsend_rb.domain.InstalledAppsRepository
import com.tomclaw.appsend_rb.screen.permissions.PermissionsActivity
import com.tomclaw.appsend_rb.screen.permissions.PermissionsInteractorImpl
import com.tomclaw.appsend_rb.screen.permissions.PermissionsPresenter
import com.tomclaw.appsend_rb.screen.permissions.PermissionsPresenterImpl
import com.tomclaw.appsend_rb.domain.Analytics
import com.tomclaw.appsend_rb.util.PerActivity
import com.tomclaw.appsend_rb.domain.SchedulersFactory
import dagger.Module
import dagger.Provides
import dagger.Subcomponent

@PerActivity
@Subcomponent(modules = [PermissionsModule::class])
interface PermissionsComponent {

    fun inject(activity: PermissionsActivity)

}

@Module
class PermissionsModule(private val packageName: String) {

    @Provides
    @PerActivity
    internal fun providePresenter(
        repository: InstalledAppsRepository,
        schedulers: SchedulersFactory,
        analytics: Analytics,
    ): PermissionsPresenter = PermissionsPresenterImpl(
        PermissionsInteractorImpl(packageName, repository),
        schedulers,
        analytics,
    )

}
