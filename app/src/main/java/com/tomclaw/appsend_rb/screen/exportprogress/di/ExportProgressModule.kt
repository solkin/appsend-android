package com.tomclaw.appsend_rb.screen.exportprogress.di

import android.content.Context
import com.tomclaw.appsend_rb.data.AndroidValueFormatter
import com.tomclaw.appsend_rb.domain.ExportManager
import com.tomclaw.appsend_rb.screen.exportprogress.ExportProgressActivity
import com.tomclaw.appsend_rb.screen.exportprogress.ExportProgressPresenter
import com.tomclaw.appsend_rb.screen.exportprogress.ExportProgressPresenterImpl
import com.tomclaw.appsend_rb.util.PerActivity
import com.tomclaw.appsend_rb.domain.SchedulersFactory
import dagger.Module
import dagger.Provides
import dagger.Subcomponent

@PerActivity
@Subcomponent(modules = [ExportProgressModule::class])
interface ExportProgressComponent {

    fun inject(activity: ExportProgressActivity)

}

@Module
class ExportProgressModule(private val context: Context) {

    @Provides
    @PerActivity
    internal fun providePresenter(
        manager: ExportManager,
        schedulers: SchedulersFactory,
    ): ExportProgressPresenter = ExportProgressPresenterImpl(
        manager,
        AndroidValueFormatter(context),
        schedulers,
    )

}
