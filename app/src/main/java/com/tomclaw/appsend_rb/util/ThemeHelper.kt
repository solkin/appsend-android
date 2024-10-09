package com.tomclaw.appsend_rb.util

import android.app.Activity
import com.jaeger.library.StatusBarUtil
import com.tomclaw.appsend_rb.R

fun updateTheme(activity: Activity): Boolean {
    val isDarkTheme = PreferenceHelper.isDarkTheme(activity)
    activity.setTheme(if (isDarkTheme) R.style.AppThemeBlack else R.style.AppTheme)
    return isDarkTheme
}

fun updateStatusBar(activity: Activity) {
    val color = getAttributedColor(activity, R.attr.toolbar_background)
    StatusBarUtil.setColor(activity, color)
}
