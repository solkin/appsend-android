package com.tomclaw.appsend_rb.util

import android.app.Activity
import android.os.Build
import com.jaeger.library.StatusBarUtil
import com.tomclaw.appsend_rb.R
import com.tomclaw.appsend_rb.screen.apps.PreferencesProvider

fun Activity.updateTheme(preferences: PreferencesProvider): Boolean {
    val isDarkTheme = preferences.isDarkTheme()
    setTheme(if (isDarkTheme) R.style.AppThemeBlack else R.style.AppTheme)
    return isDarkTheme
}

@Suppress("DEPRECATION")
fun Activity.updateStatusBar() {
    val color = getAttributedColor(this, R.attr.toolbar_background)
    StatusBarUtil.setColor(this, color)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        window.navigationBarColor = getAttributedColor(this, R.attr.bottom_bar_background)
    }
}
