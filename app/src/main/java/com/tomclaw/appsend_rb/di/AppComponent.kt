package com.tomclaw.appsend_rb.di

import com.tomclaw.appsend_rb.screen.apps.di.AppsComponent
import com.tomclaw.appsend_rb.screen.apps.di.AppsModule
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [AppModule::class])
interface AppComponent {

    fun appsComponent(module: AppsModule): AppsComponent

}