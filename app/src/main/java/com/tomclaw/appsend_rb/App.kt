package com.tomclaw.appsend_rb

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.tomclaw.appsend_rb.di.AppComponent
import com.tomclaw.appsend_rb.di.AppModule
import com.tomclaw.appsend_rb.di.DaggerAppComponent
import com.tomclaw.appsend_rb.util.KnopochkiStrings
import com.tomclaw.appsend_rb.util.PreferenceHelper
import com.tomclaw.knopochki.core.Strings

class App : Application() {

    lateinit var component: AppComponent
        private set

    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(
            if (PreferenceHelper.isDarkTheme(this)) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
        // Must precede the first screen: TopAppBar resolves its navigation label
        // from Strings.current when it is constructed.
        Strings.current = KnopochkiStrings(resources)
        component = buildComponent()
        component.analytics().register()
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
