package com.tomclaw.appsend_rb.screen.settings.di

import com.tomclaw.appsend_rb.domain.AppPreferences
import com.tomclaw.appsend_rb.domain.ExportStorage
import com.tomclaw.appsend_rb.screen.settings.SettingsEnvironment
import com.tomclaw.appsend_rb.screen.settings.SettingsFragment
import com.tomclaw.appsend_rb.screen.settings.SettingsInteractorImpl
import com.tomclaw.appsend_rb.screen.settings.SettingsPresenter
import com.tomclaw.appsend_rb.screen.settings.SettingsPresenterImpl
import com.tomclaw.appsend_rb.domain.Analytics
import com.tomclaw.appsend_rb.util.PerActivity
import dagger.Module
import dagger.Provides
import dagger.Subcomponent

@PerActivity
@Subcomponent(modules = [SettingsModule::class])
interface SettingsComponent {

    fun inject(fragment: SettingsFragment)

}

@Module
class SettingsModule(private val environment: SettingsEnvironment) {

    @Provides
    @PerActivity
    internal fun providePresenter(
        preferences: AppPreferences,
        storage: ExportStorage,
        analytics: Analytics,
    ): SettingsPresenter = SettingsPresenterImpl(
        SettingsInteractorImpl(preferences, storage, environment),
        analytics,
    )

}
