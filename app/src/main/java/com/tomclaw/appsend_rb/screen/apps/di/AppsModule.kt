package com.tomclaw.appsend_rb.screen.apps.di

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import com.avito.konveyor.ItemBinder
import com.avito.konveyor.adapter.AdapterPresenter
import com.avito.konveyor.adapter.SimpleAdapterPresenter
import com.avito.konveyor.blueprint.ItemBlueprint
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
import com.tomclaw.appsend_rb.screen.apps.adapter.app.AppItemBlueprint
import com.tomclaw.appsend_rb.screen.apps.adapter.app.AppItemPresenter
import com.tomclaw.appsend_rb.util.PerActivity
import com.tomclaw.appsend_rb.util.SchedulersFactory
import dagger.Module
import dagger.Provides
import dagger.Lazy
import dagger.multibindings.IntoSet
import java.text.DateFormat
import java.text.SimpleDateFormat

@Module
class AppsModule(
        private val context: Context,
        private val state: Bundle?
) {

    @Provides
    @PerActivity
    internal fun provideAdapterPresenter(binder: ItemBinder): AdapterPresenter {
        return SimpleAdapterPresenter(binder, binder)
    }

    @Provides
    @PerActivity
    internal fun providePresenter(
            interactor: AppsInteractor,
            adapterPresenter: Lazy<AdapterPresenter>,
            appEntityConverter: AppEntityConverter,
            preferences: PreferencesProvider,
            schedulers: SchedulersFactory
    ): AppsPresenter = AppsPresenterImpl(interactor, adapterPresenter, appEntityConverter, preferences, schedulers, state)

    @Provides
    @PerActivity
    internal fun provideInteractor(
            packageManager: PackageManagerWrapper,
            outputWrapper: OutputWrapper,
            schedulers: SchedulersFactory
    ): AppsInteractor = AppsInteractorImpl(packageManager, outputWrapper, schedulers)

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

    @Provides
    @PerActivity
    internal fun provideItemBinder(
            blueprintSet: Set<@JvmSuppressWildcards ItemBlueprint<*, *>>
    ): ItemBinder {
        return ItemBinder.Builder().apply {
            blueprintSet.forEach { registerItem(it) }
        }.build()
    }

    @Provides
    @IntoSet
    @PerActivity
    internal fun provideAppItemBlueprint(
            presenter: AppItemPresenter
    ): ItemBlueprint<*, *> = AppItemBlueprint(presenter)

    @Provides
    @PerActivity
    internal fun provideAppItemPresenter(presenter: AppsPresenter) =
            AppItemPresenter(presenter)

}