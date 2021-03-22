package com.tomclaw.appsend_rb.screen.apps

import android.content.Context
import com.tomclaw.appsend_rb.R
import com.tomclaw.appsend_rb.util.getBooleanPreference
import com.tomclaw.appsend_rb.util.getStringPreference

interface PreferencesProvider {

    fun isDarkTheme(): Boolean

    fun isShowSystemApps(): Boolean

    fun isRunnableOnly(): Boolean

    fun getSortOrder(): Int

}

class PreferencesProviderImpl(private val context: Context) : PreferencesProvider {

    override fun isDarkTheme(): Boolean {
        return context.getBooleanPreference(
            R.string.pref_dark_theme,
            R.bool.pref_dark_theme_default
        )
    }

    override fun isShowSystemApps(): Boolean {
        return context.getBooleanPreference(
            R.string.pref_show_system,
            R.bool.pref_show_system_default
        )
    }

    override fun isRunnableOnly(): Boolean {
        return context.getBooleanPreference(
            R.string.pref_runnable,
            R.bool.pref_runnable_default
        )
    }

    override fun getSortOrder(): Int {
        return context.getStringPreference(
            R.string.pref_sort_order,
            R.string.pref_sort_order_default
        ).run {
            when (this) {
                context.getString(R.string.sort_order_ascending_value) -> NAME_ASCENDING
                context.getString(R.string.sort_order_descending_value) -> NAME_DESCENDING
                context.getString(R.string.sort_order_app_size_value) -> APP_SIZE
                context.getString(R.string.sort_order_install_time_value) -> INSTALL_TIME
                context.getString(R.string.sort_order_update_time_value) -> UPDATE_TIME
                else -> NAME_ASCENDING
            }
        }
    }

}
