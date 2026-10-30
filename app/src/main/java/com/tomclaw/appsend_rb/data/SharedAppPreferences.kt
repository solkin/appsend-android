package com.tomclaw.appsend_rb.data

import android.content.Context
import androidx.core.content.edit
import com.tomclaw.appsend_rb.domain.AppFilter
import com.tomclaw.appsend_rb.domain.AppPreferences
import com.tomclaw.appsend_rb.domain.DEFAULT_FILE_NAME_TEMPLATE
import com.tomclaw.appsend_rb.domain.ExportFormat
import com.tomclaw.appsend_rb.domain.SortOrder
import com.tomclaw.appsend_rb.domain.ThemeMode

class SharedAppPreferences(context: Context) : AppPreferences {

    private val prefs = context.getSharedPreferences(
        context.packageName + "_preferences",
        Context.MODE_PRIVATE,
    )

    override var themeMode: ThemeMode
        get() = prefs.getString(KEY_THEME_MODE, null)
            ?.let { stored -> ThemeMode.entries.firstOrNull { it.name == stored } }
            ?: legacyThemeMode()
        set(value) = prefs.edit { putString(KEY_THEME_MODE, value.name) }

    /** Older versions stored a plain on/off switch: an explicit choice must survive. */
    private fun legacyThemeMode(): ThemeMode = when {
        !prefs.contains(KEY_LEGACY_DARK_THEME) -> ThemeMode.SYSTEM
        prefs.getBoolean(KEY_LEGACY_DARK_THEME, false) -> ThemeMode.DARK
        else -> ThemeMode.LIGHT
    }

    override var dynamicColors: Boolean
        get() = prefs.getBoolean(KEY_DYNAMIC_COLORS, false)
        set(value) = prefs.edit { putBoolean(KEY_DYNAMIC_COLORS, value) }

    override var analyticsEnabled: Boolean
        get() = prefs.getBoolean(KEY_ANALYTICS, true)
        set(value) = prefs.edit { putBoolean(KEY_ANALYTICS, value) }

    override var appFilter: AppFilter
        get() = AppFilter(
            sort = when (prefs.getString(KEY_SORT, null)) {
                SORT_SIZE -> SortOrder.SIZE
                SORT_INSTALLED -> SortOrder.INSTALLED
                SORT_UPDATED -> SortOrder.UPDATED
                else -> SortOrder.NAME
            },
            showSystem = prefs.getBoolean(KEY_SHOW_SYSTEM, false),
            splitOnly = prefs.getBoolean(KEY_SPLIT_ONLY, false),
            runnableOnly = prefs.getBoolean(KEY_RUNNABLE, false),
        )
        set(value) = prefs.edit {
            putString(
                KEY_SORT,
                when (value.sort) {
                    SortOrder.NAME -> SORT_NAME
                    SortOrder.SIZE -> SORT_SIZE
                    SortOrder.INSTALLED -> SORT_INSTALLED
                    SortOrder.UPDATED -> SORT_UPDATED
                },
            )
            putBoolean(KEY_SHOW_SYSTEM, value.showSystem)
            putBoolean(KEY_SPLIT_ONLY, value.splitOnly)
            putBoolean(KEY_RUNNABLE, value.runnableOnly)
        }

    override var exportFormat: ExportFormat
        get() = prefs.getString(KEY_EXPORT_FORMAT, null)
            ?.let { stored -> ExportFormat.entries.firstOrNull { it.name == stored } }
            ?: ExportFormat.BUNDLE
        set(value) = prefs.edit { putString(KEY_EXPORT_FORMAT, value.name) }

    override var fileNameTemplate: String
        get() = prefs.getString(KEY_FILE_NAME_TEMPLATE, null) ?: DEFAULT_FILE_NAME_TEMPLATE
        set(value) = prefs.edit { putString(KEY_FILE_NAME_TEMPLATE, value) }

    override var exportFolder: String?
        get() = prefs.getString(KEY_EXPORT_FOLDER, null)
        set(value) = prefs.edit { putString(KEY_EXPORT_FOLDER, value) }

}

private const val KEY_THEME_MODE = "pref_theme_mode"
private const val KEY_LEGACY_DARK_THEME = "pref_dark_theme"
private const val KEY_DYNAMIC_COLORS = "pref_dynamic_colors"
private const val KEY_ANALYTICS = "pref_analytics"
private const val KEY_SHOW_SYSTEM = "pref_show_system"
private const val KEY_SPLIT_ONLY = "pref_split_only"
private const val KEY_RUNNABLE = "pref_runnable"
private const val KEY_SORT = "pref_sort_order"
private const val KEY_EXPORT_FORMAT = "pref_export_format"
private const val KEY_FILE_NAME_TEMPLATE = "pref_file_name_template"
private const val KEY_EXPORT_FOLDER = "pref_export_folder"

// Values written by earlier versions, so a stored sort order keeps working.
private const val SORT_NAME = "sort_order_ascending"
private const val SORT_SIZE = "sort_order_app_size"
private const val SORT_INSTALLED = "sort_order_install_time"
private const val SORT_UPDATED = "sort_order_update_time"
