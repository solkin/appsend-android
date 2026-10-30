package com.tomclaw.appsend_rb

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.DynamicColorsOptions
import com.tomclaw.appsend_rb.data.SharedAppPreferences
import com.tomclaw.appsend_rb.di.AppComponent
import com.tomclaw.appsend_rb.di.AppModule
import com.tomclaw.appsend_rb.di.DaggerAppComponent
import com.tomclaw.appsend_rb.domain.ThemeMode
import io.reactivex.rxjava3.exceptions.UndeliverableException
import io.reactivex.rxjava3.plugins.RxJavaPlugins

class App : Application() {

    lateinit var component: AppComponent
        private set

    override fun onCreate() {
        super.onCreate()
        val preferences = SharedAppPreferences(this)
        applyThemeMode(preferences.themeMode)
        DynamicColors.applyToActivitiesIfAvailable(
            this,
            DynamicColorsOptions.Builder()
                .setPrecondition { _, _ -> preferences.dynamicColors }
                .build(),
        )
        component = DaggerAppComponent.builder()
            .appModule(AppModule(this))
            .build()
        component.analytics().register()
        installUndeliverableErrorHandler()
    }

    /**
     * A failure that arrives after its subscriber left has nowhere to go, and RxJava's
     * default is to crash on it. It is expected here: screens leave mid-read all the time.
     * Anything else is a bug, and has to crash like one.
     */
    private fun installUndeliverableErrorHandler() {
        RxJavaPlugins.setErrorHandler { error ->
            if (error is UndeliverableException) {
                component.analytics().trackException(error.cause ?: error, mapOf("event" to "undeliverable"))
            } else {
                val thread = Thread.currentThread()
                thread.uncaughtExceptionHandler?.uncaughtException(thread, error)
            }
        }
    }

}

fun applyThemeMode(mode: ThemeMode) {
    AppCompatDelegate.setDefaultNightMode(
        when (mode) {
            ThemeMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            ThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            ThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
        }
    )
}

fun Application.getComponent(): AppComponent = (this as App).component
