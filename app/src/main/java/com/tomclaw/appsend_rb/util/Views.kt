package com.tomclaw.appsend_rb.util

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewPropertyAnimator
import android.view.animation.AccelerateDecelerateInterpolator

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