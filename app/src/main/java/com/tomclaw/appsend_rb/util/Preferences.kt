package com.tomclaw.appsend_rb.util

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import androidx.annotation.BoolRes
import androidx.annotation.StringRes

fun Context.getBooleanPreference(
        @StringRes preferenceKey: Int,
        @BoolRes defaultValueKey: Int
): Boolean {
    return getSharedPreferences().getBoolean(
            resources.getString(preferenceKey),
            resources.getBoolean(defaultValueKey)
    )
}

fun Context.setBooleanPreference(@StringRes preferenceKey: Int, value: Boolean) {
    getSharedPreferences()
            .edit()
            .putBoolean(resources.getString(preferenceKey), value)
            .apply()
}

fun Context.getStringPreference(
        @StringRes preferenceKey: Int,
        @StringRes defaultValueKey: Int
): String? {
    return getSharedPreferences().getString(
            resources.getString(preferenceKey),
            resources.getString(defaultValueKey)
    )
}

fun Context.setStringPreference(@StringRes preferenceKey: Int, value: String) {
    getSharedPreferences()
            .edit()
            .putString(resources.getString(preferenceKey), value)
            .apply()
}

fun Context.getSharedPreferences(): SharedPreferences {
    return getSharedPreferences(getDefaultSharedPreferencesName(this), MODE_PRIVATE)
}

private fun getDefaultSharedPreferencesName(context: Context): String? {
    return context.packageName + "_preferences"
}
