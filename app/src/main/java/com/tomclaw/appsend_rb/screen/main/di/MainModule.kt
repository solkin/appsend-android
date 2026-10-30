package com.tomclaw.appsend_rb.screen.main.di

import com.tomclaw.appsend_rb.domain.ExportManager
import com.tomclaw.appsend_rb.domain.SchedulersFactory
import com.tomclaw.appsend_rb.domain.UnseenExports
import com.tomclaw.appsend_rb.screen.main.MainActivity
import com.tomclaw.appsend_rb.screen.main.MainInteractorImpl
import com.tomclaw.appsend_rb.screen.main.MainPresenter
import com.tomclaw.appsend_rb.screen.main.MainPresenterImpl
import com.tomclaw.appsend_rb.screen.main.MainState
import com.tomclaw.appsend_rb.util.PerActivity
import dagger.Module
import dagger.Provides
import dagger.Subcomponent

@PerActivity
@Subcomponent(modules = [MainModule::class])
interface MainComponent {

    fun inject(activity: MainActivity)

}

@Module
class MainModule(private val state: MainState?) {

    @Provides
    @PerActivity
    internal fun providePresenter(
        manager: ExportManager,
        unseen: UnseenExports,
        schedulers: SchedulersFactory,
    ): MainPresenter = MainPresenterImpl(MainInteractorImpl(manager, unseen), schedulers, state)

}
