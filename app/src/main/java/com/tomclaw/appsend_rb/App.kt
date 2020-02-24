package com.tomclaw.appsend_rb

import android.app.Application
import com.tomclaw.appsend_rb.di.AppComponent
import com.tomclaw.appsend_rb.di.AppModule
import com.tomclaw.appsend_rb.di.DaggerAppComponent

class App : Application() {

    lateinit var component: AppComponent
        private set

    override fun onCreate() {
        super.onCreate()
        component = buildComponent()
    }

    private fun buildComponent(): AppComponent {
        return DaggerAppComponent.builder()
                .appModule(AppModule(this))
                .build()
    }

}

fun Application.getComponent(): AppComponent {
    return (this as App).component
}