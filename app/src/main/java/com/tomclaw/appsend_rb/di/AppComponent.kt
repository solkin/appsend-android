package com.tomclaw.appsend_rb.di

import com.tomclaw.appsend_rb.domain.ExportManager
import com.tomclaw.appsend_rb.domain.Analytics
import com.tomclaw.appsend_rb.screen.apps.di.AppsComponent
import com.tomclaw.appsend_rb.screen.apps.di.AppsModule
import com.tomclaw.appsend_rb.screen.details.di.AppDetailsComponent
import com.tomclaw.appsend_rb.screen.details.di.AppDetailsModule
import com.tomclaw.appsend_rb.screen.exportoptions.di.ExportOptionsComponent
import com.tomclaw.appsend_rb.screen.exportoptions.di.ExportOptionsModule
import com.tomclaw.appsend_rb.screen.exportprogress.di.ExportProgressComponent
import com.tomclaw.appsend_rb.screen.exportprogress.di.ExportProgressModule
import com.tomclaw.appsend_rb.screen.exports.di.ExportsComponent
import com.tomclaw.appsend_rb.screen.exports.di.ExportsModule
import com.tomclaw.appsend_rb.screen.main.di.MainComponent
import com.tomclaw.appsend_rb.screen.main.di.MainModule
import com.tomclaw.appsend_rb.screen.permissions.di.PermissionsComponent
import com.tomclaw.appsend_rb.screen.permissions.di.PermissionsModule
import com.tomclaw.appsend_rb.screen.settings.di.SettingsComponent
import com.tomclaw.appsend_rb.screen.settings.di.SettingsModule
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [AppModule::class])
interface AppComponent {

    fun analytics(): Analytics

    fun exportManager(): ExportManager

    fun mainComponent(module: MainModule): MainComponent

    fun appsComponent(module: AppsModule): AppsComponent

    fun appDetailsComponent(module: AppDetailsModule): AppDetailsComponent

    fun permissionsComponent(module: PermissionsModule): PermissionsComponent

    fun exportOptionsComponent(module: ExportOptionsModule): ExportOptionsComponent

    fun exportProgressComponent(module: ExportProgressModule): ExportProgressComponent

    fun exportsComponent(module: ExportsModule): ExportsComponent

    fun settingsComponent(module: SettingsModule): SettingsComponent

}
