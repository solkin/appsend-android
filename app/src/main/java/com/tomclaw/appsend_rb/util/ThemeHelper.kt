package com.tomclaw.appsend_rb.util

import android.app.Activity
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.jaeger.library.StatusBarUtil
import com.tomclaw.appsend_rb.R

fun updateTheme(activity: Activity): Boolean {
    val isDarkTheme = PreferenceHelper.isDarkTheme(activity)
    activity.setTheme(if (isDarkTheme) R.style.AppThemeBlack else R.style.AppTheme)
    return isDarkTheme
}

@Suppress("DEPRECATION")
fun updateStatusBar(activity: Activity) {
    val color = getAttributedColor(activity, R.attr.toolbar_background)
    StatusBarUtil.setColor(activity, color)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        activity.window.navigationBarColor = getAttributedColor(activity, R.attr.bottom_bar_background)
    }
}

fun applySystemBarsInsets(activity: Activity) {
    val content = activity.findViewById<View>(android.R.id.content) ?: return
    WindowCompat.setDecorFitsSystemWindows(activity.window, false)
    val statusBarScrim = ColorDrawable(getAttributedColor(activity, R.attr.toolbar_background))
    val navigationBarScrim = ColorDrawable(getAttributedColor(activity, R.attr.bottom_bar_background))
    val leftNavigationScrim = ColorDrawable(getAttributedColor(activity, R.attr.bottom_bar_background))
    val rightNavigationScrim = ColorDrawable(getAttributedColor(activity, R.attr.bottom_bar_background))
    content.overlay.add(statusBarScrim)
    content.overlay.add(navigationBarScrim)
    content.overlay.add(leftNavigationScrim)
    content.overlay.add(rightNavigationScrim)
    val initialPadding = InsetsPadding(
        left = content.paddingLeft,
        top = content.paddingTop,
        right = content.paddingRight,
        bottom = content.paddingBottom
    )
    ViewCompat.setOnApplyWindowInsetsListener(content) { view, windowInsets ->
        val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
        view.setPadding(
            initialPadding.left + insets.left,
            initialPadding.top + insets.top,
            initialPadding.right + insets.right,
            initialPadding.bottom + insets.bottom
        )
        view.post {
            val width = view.width
            val height = view.height
            statusBarScrim.setBounds(0, 0, width, insets.top)
            navigationBarScrim.setBounds(0, height - insets.bottom, width, height)
            leftNavigationScrim.setBounds(0, insets.top, insets.left, height - insets.bottom)
            rightNavigationScrim.setBounds(width - insets.right, insets.top, width, height - insets.bottom)
            view.invalidate()
        }
        windowInsets
    }
    ViewCompat.requestApplyInsets(content)
}

private data class InsetsPadding(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
)
