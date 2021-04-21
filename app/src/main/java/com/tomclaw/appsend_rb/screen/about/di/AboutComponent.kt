package com.tomclaw.appsend_rb.screen.about.di

import com.tomclaw.appsend_rb.screen.about.AboutActivity
import com.tomclaw.appsend_rb.util.PerActivity
import dagger.Subcomponent

@PerActivity
@Subcomponent(modules = [AboutModule::class])
interface AboutComponent {

    fun inject(activity: AboutActivity)

}
