package com.tomclaw.appsend_rb.screen.apps.di

import android.content.Context
import android.os.Bundle
import com.tomclaw.appsend_rb.screen.apps.AppsInteractor
import com.tomclaw.appsend_rb.screen.apps.AppsInteractorImpl
import com.tomclaw.appsend_rb.screen.apps.AppsPresenter
import com.tomclaw.appsend_rb.screen.apps.AppsPresenterImpl
import com.tomclaw.appsend_rb.util.PerActivity
import com.tomclaw.appsend_rb.util.SchedulersFactory
import dagger.Module
import dagger.Provides

@Module
class AppsModule(
        private val context: Context,
        private val state: Bundle?
) {

    @Provides
    @PerActivity
    internal fun providePresenter(
            interactor: AppsInteractor,
            schedulers: SchedulersFactory
    ): AppsPresenter = AppsPresenterImpl(interactor, schedulers, state)

    @Provides
    @PerActivity
    internal fun provideInteractor(
            schedulers: SchedulersFactory
    ): AppsInteractor = AppsInteractorImpl(schedulers)

}