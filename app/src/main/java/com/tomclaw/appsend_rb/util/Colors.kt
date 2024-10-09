package com.tomclaw.appsend_rb.util

import android.content.Context
import android.graphics.Color
import android.util.TypedValue

fun getAttributedColor(context: Context, attr: Int): Int {
    val set = intArrayOf(attr)
    val typedValue = TypedValue()
    val a = context.obtainStyledAttributes(typedValue.data, set)
    val color = a.getColor(0, Color.WHITE)
    a.recycle()
    return color
}
