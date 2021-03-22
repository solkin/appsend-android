package com.tomclaw.appsend_rb.util

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewPropertyAnimator
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.EditText
import android.widget.TextView
import com.jakewharton.rxrelay2.Relay

fun View?.toggle() {
    if (this?.visibility == VISIBLE) hide() else show()
}

fun View?.isVisible(): Boolean = this?.visibility == VISIBLE

fun View?.show() {
    this?.visibility = VISIBLE
}

fun View?.hide() {
    this?.visibility = GONE
}

fun TextView.bind(value: String?) {
    if (TextUtils.isEmpty(value)) {
        visibility = GONE
        text = ""
    } else {
        visibility = VISIBLE
        text = value
    }
}

fun View.clicks(relay: Relay<Unit>) {
    setOnClickListener { relay.accept(Unit) }
}

fun EditText.changes(handler: (String) -> Unit) {
    addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(s: Editable) {}

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            handler.invoke(s.toString())
        }
    })
}

fun View.showWithAlphaAnimation(
    duration: Long = ANIMATION_DURATION,
    animateFully: Boolean = true,
    endCallback: (() -> Unit)? = null
): ViewPropertyAnimator {
    if (animateFully) {
        alpha = 0.0f
    }
    show()
    return animate()
        .setDuration(duration)
        .alpha(1.0f)
        .setInterpolator(AccelerateDecelerateInterpolator())
        .setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                alpha = 1.0f
                show()
                endCallback?.invoke()
            }
        })
}

fun View.hideWithAlphaAnimation(
    duration: Long = ANIMATION_DURATION,
    animateFully: Boolean = true,
    endCallback: (() -> Unit)? = null
): ViewPropertyAnimator {
    if (animateFully) {
        alpha = 1.0f
    }
    return animate()
        .setDuration(duration)
        .alpha(0.0f)
        .setInterpolator(AccelerateDecelerateInterpolator())
        .setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                hide()
                alpha = 1.0f
                endCallback?.invoke()
            }
        })
}

const val ANIMATION_DURATION: Long = 250