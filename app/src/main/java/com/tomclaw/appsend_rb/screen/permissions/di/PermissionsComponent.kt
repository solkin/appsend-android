package com.tomclaw.appsend_rb.screen.permissions.di

import com.tomclaw.appsend_rb.screen.permissions.PermissionsActivity
import com.tomclaw.appsend_rb.util.PerActivity
import dagger.Subcomponent

@PerActivity
@Subcomponent(modules = [PermissionsModule::class])
interface PermissionsComponent {

    fun inject(activity: PermissionsActivity)

}
