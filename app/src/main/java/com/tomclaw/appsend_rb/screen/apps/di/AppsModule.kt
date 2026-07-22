package com.tomclaw.appsend_rb.screen.apps.di

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import com.tomclaw.appsend.util.Analytics
import com.tomclaw.appsend_rb.screen.apps.AppEntityConverter
import com.tomclaw.appsend_rb.screen.apps.AppEntityConverterImpl
import com.tomclaw.appsend_rb.screen.apps.AppsInteractor
import com.tomclaw.appsend_rb.screen.apps.AppsInteractorImpl
import com.tomclaw.appsend_rb.screen.apps.AppsPresenter
import com.tomclaw.appsend_rb.screen.apps.AppsPresenterImpl
import com.tomclaw.appsend_rb.screen.apps.OutputWrapper
import com.tomclaw.appsend_rb.screen.apps.OutputWrapperImpl
import com.tomclaw.appsend_rb.screen.apps.PackageManagerWrapper
import com.tomclaw.appsend_rb.screen.apps.PackageManagerWrapperImpl
import com.tomclaw.appsend_rb.screen.apps.PreferencesProvider
import com.tomclaw.appsend_rb.screen.apps.PreferencesProviderImpl
import com.tomclaw.appsend_rb.screen.apps.ResourceProvider
import com.tomclaw.appsend_rb.screen.apps.ResourceProviderImpl
import com.tomclaw.appsend_rb.util.PerActivity
import com.tomclaw.appsend_rb.util.SchedulersFactory
import dagger.Module
import dagger.Provides
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Locale

@Module
class AppsModule(
    private val context: Context,
    private val state: Bundle?
) {

    @Provides
    @PerActivity
    internal fun providePresenter(
        interactor: AppsInteractor,
        appEntityConverter: AppEntityConverter,
        preferences: PreferencesProvider,
        schedulers: SchedulersFactory,
        analytics: Analytics
    ): AppsPresenter = AppsPresenterImpl(
        interactor,
        appEntityConverter,
        preferences,
        schedulers,
        analytics,
        state
    )

    @Provides
    @PerActivity
    internal fun provideInteractor(
        packageManager: PackageManagerWrapper,
        outputWrapper: OutputWrapper,
        locale: Locale,
        schedulers: SchedulersFactory
    ): AppsInteractor = AppsInteractorImpl(packageManager, outputWrapper, locale, schedulers)

    @Provides
    @PerActivity
    internal fun provideAppInfoConverter(resourceProvider: ResourceProvider): AppEntityConverter {
        return AppEntityConverterImpl(resourceProvider)
    }

    @Provides
    @PerActivity
    internal fun provideResourceProvider(dateFormat: DateFormat): ResourceProvider {
        return ResourceProviderImpl(context, dateFormat)
    }

    @SuppressLint("SimpleDateFormat")
    @Provides
    @PerActivity
    internal fun provideDateFormat(): DateFormat {
        return SimpleDateFormat("dd.MM.yy")
    }

    @Provides
    @PerActivity
    internal fun providePreferencesProvider(): PreferencesProvider {
        return PreferencesProviderImpl(context)
    }

    @Provides
    @PerActivity
    internal fun providePackageManagerWrapper(): PackageManagerWrapper {
        return PackageManagerWrapperImpl(context.packageManager)
    }

    @Provides
    @PerActivity
    internal fun provideOutputWrapper(): OutputWrapper {
        return OutputWrapperImpl(context, context.contentResolver)
    }

}
