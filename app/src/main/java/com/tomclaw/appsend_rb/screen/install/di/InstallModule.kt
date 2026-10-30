package com.tomclaw.appsend_rb.screen.install.di

import android.content.Context
import android.os.Build
import com.tomclaw.appsend_rb.data.AndroidValueFormatter
import com.tomclaw.appsend_rb.domain.PackageInstallerGateway
import com.tomclaw.appsend_rb.screen.install.InstallActivity
import com.tomclaw.appsend_rb.screen.install.InstallInteractorImpl
import com.tomclaw.appsend_rb.screen.install.InstallPresenter
import com.tomclaw.appsend_rb.screen.install.InstallPresenterImpl
import com.tomclaw.appsend_rb.screen.install.InstallState
import com.tomclaw.appsend_rb.domain.Analytics
import com.tomclaw.appsend_rb.util.PerActivity
import com.tomclaw.appsend_rb.domain.SchedulersFactory
import dagger.Module
import dagger.Provides
import dagger.Subcomponent

@PerActivity
@Subcomponent(modules = [InstallModule::class])
interface InstallComponent {

    fun inject(activity: InstallActivity)

}

@Module
class InstallModule(
    private val context: Context,
    private val uri: String,
    private val state: InstallState?,
) {

    @Provides
    @PerActivity
    internal fun providePresenter(
        installer: PackageInstallerGateway,
        schedulers: SchedulersFactory,
        analytics: Analytics,
    ): InstallPresenter = InstallPresenterImpl(
        InstallInteractorImpl(uri, installer, Build.VERSION.SDK_INT, Build.SUPPORTED_ABIS.toList()),
        AndroidValueFormatter(context),
        schedulers,
        analytics,
        state,
    )

}
