package com.tomclaw.appsend_rb.screen.apps

import android.content.Context
import com.tomclaw.appsend_rb.R
import com.tomclaw.appsend_rb.util.getBooleanPreference
import com.tomclaw.appsend_rb.util.getStringPreference
import com.tomclaw.appsend_rb.util.setBooleanPreference
import com.tomclaw.appsend_rb.util.setStringPreference

interface PreferencesProvider {

    fun isDarkTheme(): Boolean

    fun isShowSystemApps(): Boolean

    fun isRunnableOnly(): Boolean

    fun getSortOrder(): Int

    /**
     * The filter chips on the apps screen are the only control for these two, so
     * their choice is what gets persisted — there is no separate setting to drift
     * out of sync with.
     */
    fun setShowSystemApps(value: Boolean)

    fun setSortOrder(order: Int)

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

    override fun setShowSystemApps(value: Boolean) {
        context.setBooleanPreference(R.string.pref_show_system, value)
    }

    override fun setSortOrder(order: Int) {
        val value = when (order) {
            NAME_DESCENDING -> R.string.sort_order_descending_value
            APP_SIZE -> R.string.sort_order_app_size_value
            INSTALL_TIME -> R.string.sort_order_install_time_value
            UPDATE_TIME -> R.string.sort_order_update_time_value
            else -> R.string.sort_order_ascending_value
        }
        context.setStringPreference(R.string.pref_sort_order, context.getString(value))
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
