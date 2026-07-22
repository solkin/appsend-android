package com.tomclaw.appsend_rb.screen.permissions.di

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import com.tomclaw.appsend.util.Analytics
import com.tomclaw.appsend_rb.screen.permissions.PermissionInfoProvider
import com.tomclaw.appsend_rb.screen.permissions.PermissionInfoProviderImpl
import com.tomclaw.appsend_rb.screen.permissions.PermissionsConverter
import com.tomclaw.appsend_rb.screen.permissions.PermissionsConverterImpl
import com.tomclaw.appsend_rb.screen.permissions.PermissionsPresenter
import com.tomclaw.appsend_rb.screen.permissions.PermissionsPresenterImpl
import com.tomclaw.appsend_rb.util.PerActivity
import com.tomclaw.appsend_rb.util.SchedulersFactory
import dagger.Module
import dagger.Provides
import java.util.Locale

@Module
class PermissionsModule(
    private val context: Context,
    private val permissions: List<String>,
    private val state: Bundle?
) {

    @Provides
    @PerActivity
    internal fun providePresenter(
        converter: PermissionsConverter,
        schedulers: SchedulersFactory,
        analytics: Analytics
    ): PermissionsPresenter =
        PermissionsPresenterImpl(permissions, converter, schedulers, analytics, state)

    @Provides
    @PerActivity
    internal fun providePermissionInfoProvider(
        packageManager: PackageManager,
        locale: Locale
    ): PermissionInfoProvider {
        return PermissionInfoProviderImpl(packageManager, locale)
    }

    @Provides
    @PerActivity
    internal fun providePermissionsConverter(
        permissionInfoProvider: PermissionInfoProvider
    ): PermissionsConverter {
        return PermissionsConverterImpl(permissionInfoProvider)
    }

}
