package com.tomclaw.appsend_rb.util

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

/** Pads the view by the system bars, on top of the padding it already has. */
fun View.applySystemBarInsets(top: Boolean = false, bottom: Boolean = false) {
    val initialTop = paddingTop
    val initialBottom = paddingBottom
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val bars = insets.getInsets(
            WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
        )
        view.updatePadding(
            left = bars.left,
            right = bars.right,
            top = if (top) initialTop + bars.top else initialTop,
            bottom = if (bottom) initialBottom + bars.bottom else initialBottom,
        )
        insets
    }
}
