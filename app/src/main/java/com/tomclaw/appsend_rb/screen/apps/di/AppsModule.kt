package com.tomclaw.appsend_rb.screen.apps.di

import android.content.Context
import android.os.Bundle
import com.avito.konveyor.ItemBinder
import com.avito.konveyor.adapter.AdapterPresenter
import com.avito.konveyor.adapter.SimpleAdapterPresenter
import com.avito.konveyor.blueprint.ItemBlueprint
import com.tomclaw.appsend_rb.screen.apps.AppInfoConverter
import com.tomclaw.appsend_rb.screen.apps.AppInfoConverterImpl
import com.tomclaw.appsend_rb.screen.apps.AppsInteractor
import com.tomclaw.appsend_rb.screen.apps.AppsInteractorImpl
import com.tomclaw.appsend_rb.screen.apps.AppsPresenter
import com.tomclaw.appsend_rb.screen.apps.AppsPresenterImpl
import com.tomclaw.appsend_rb.screen.apps.adapter.app.AppItemBlueprint
import com.tomclaw.appsend_rb.screen.apps.adapter.app.AppItemPresenter
import com.tomclaw.appsend_rb.util.PerActivity
import com.tomclaw.appsend_rb.util.SchedulersFactory
import dagger.Module
import dagger.Provides
import dagger.Lazy
import dagger.multibindings.IntoSet

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
            appInfoConverter: AppInfoConverter,
            schedulers: SchedulersFactory
    ): AppsPresenter = AppsPresenterImpl(interactor, adapterPresenter, appInfoConverter, schedulers, state)

    @Provides
    @PerActivity
    internal fun provideInteractor(
            schedulers: SchedulersFactory
    ): AppsInteractor = AppsInteractorImpl(schedulers)

    @Provides
    @PerActivity
    internal fun provideAppInfoConverter(): AppInfoConverter {
        return AppInfoConverterImpl()
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