package com.tomclaw.appsend_rb.util

import android.app.Activity
import com.jaeger.library.StatusBarUtil
import com.tomclaw.appsend_rb.R
import com.tomclaw.appsend_rb.screen.apps.PreferencesProvider

fun Activity.updateTheme(preferences: PreferencesProvider): Boolean {
    val isDarkTheme = preferences.isDarkTheme()
    setTheme(if (isDarkTheme) R.style.AppThemeBlack else R.style.AppTheme)
    return isDarkTheme
}

fun Activity.updateStatusBar() {
    val color = getAttributedColor(this, R.attr.toolbar_background)
    StatusBarUtil.setColor(this, color)
}
