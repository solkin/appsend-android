package com.tomclaw.appsend_rb.screen.apps.di

import com.tomclaw.appsend_rb.screen.apps.AppsActivity
import com.tomclaw.appsend_rb.util.PerActivity
import dagger.Subcomponent

@PerActivity
@Subcomponent(modules = [AppsModule::class])
interface AppsComponent {

    fun inject(activity: AppsActivity)

}