package com.tomclaw.appsend_rb.di

import android.app.Application
import android.content.Context
import com.tomclaw.appsend_rb.data.PackageArchiveInspector
import com.tomclaw.appsend_rb.data.PackageManagerAppsRepository
import com.tomclaw.appsend_rb.data.SelectableExportStorage
import com.tomclaw.appsend_rb.data.ShareCacheStorage
import com.tomclaw.appsend_rb.data.SharedAppPreferences
import com.tomclaw.appsend_rb.data.bundleMetadata
import com.tomclaw.appsend_rb.domain.ApkPackager
import com.tomclaw.appsend_rb.domain.AppPreferences
import com.tomclaw.appsend_rb.domain.ArchiveInspector
import com.tomclaw.appsend_rb.domain.ExportChanges
import com.tomclaw.appsend_rb.domain.ExportManager
import com.tomclaw.appsend_rb.domain.ExportManagerImpl
import com.tomclaw.appsend_rb.domain.ExportStorage
import com.tomclaw.appsend_rb.domain.InstalledAppsRepository
import com.tomclaw.appsend_rb.domain.UnseenExports
import com.tomclaw.appsend_rb.domain.Analytics
import com.tomclaw.appsend_rb.screen.exportprogress.ExportServiceGuard
import com.tomclaw.appsend_rb.data.AnalyticsImpl
import com.tomclaw.appsend_rb.domain.SchedulersFactory
import com.tomclaw.appsend_rb.data.SchedulersFactoryImpl
import dagger.Module
import dagger.Provides
import java.util.Locale
import javax.inject.Singleton

@Module
class AppModule(private val app: Application) {

    @Provides
    @Singleton
    internal fun provideContext(): Context = app

    @Provides
    @Singleton
    internal fun provideSchedulersFactory(): SchedulersFactory = SchedulersFactoryImpl()

    @Provides
    internal fun provideLocale(): Locale = Locale.getDefault()

    @Provides
    @Singleton
    internal fun providePreferences(context: Context): AppPreferences = SharedAppPreferences(context)

    @Provides
    @Singleton
    internal fun provideAnalytics(preferences: AppPreferences): Analytics =
        AnalyticsImpl(app) { preferences.analyticsEnabled }

    @Provides
    @Singleton
    internal fun provideRepository(
        context: Context,
        schedulers: SchedulersFactory,
    ): PackageManagerAppsRepository = PackageManagerAppsRepository(context, schedulers)

    @Provides
    internal fun provideAppsRepository(
        repository: PackageManagerAppsRepository,
    ): InstalledAppsRepository = repository

    @Provides
    @Singleton
    internal fun provideExportStorage(
        context: Context,
        preferences: AppPreferences,
    ): ExportStorage = SelectableExportStorage(context, preferences)

    @Provides
    @Singleton
    internal fun provideArchiveInspector(
        context: Context,
        storage: ExportStorage,
    ): ArchiveInspector = PackageArchiveInspector(context, storage)

    @Provides
    @Singleton
    internal fun provideUnseenExports(manager: ExportManager): UnseenExports = UnseenExports(manager)

    @Provides
    @Singleton
    internal fun provideExportChanges(): ExportChanges = ExportChanges()

    @Provides
    @Singleton
    internal fun provideExportManager(
        repository: PackageManagerAppsRepository,
        storage: ExportStorage,
        inspector: ArchiveInspector,
        preferences: AppPreferences,
        context: Context,
        schedulers: SchedulersFactory,
        analytics: Analytics,
    ): ExportManager = ExportManagerImpl(
        sources = repository,
        storage = storage,
        shareStorage = ShareCacheStorage(context),
        packager = ApkPackager(),
        inspector = inspector,
        metadata = ::bundleMetadata,
        preferences = preferences,
        guard = ExportServiceGuard(context),
        schedulers = schedulers,
        analytics = analytics,
    )

}
