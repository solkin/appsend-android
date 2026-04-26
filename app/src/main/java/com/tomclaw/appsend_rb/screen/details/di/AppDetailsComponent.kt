package com.tomclaw.appsend_rb.screen.details.di

import com.tomclaw.appsend_rb.screen.details.AppDetailsActivity
import com.tomclaw.appsend_rb.util.PerActivity
import dagger.Subcomponent

@PerActivity
@Subcomponent(modules = [AppDetailsModule::class])
interface AppDetailsComponent {

    fun inject(activity: AppDetailsActivity)

}
