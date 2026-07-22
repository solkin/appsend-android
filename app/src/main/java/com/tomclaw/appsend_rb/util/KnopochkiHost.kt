package com.tomclaw.appsend_rb.util

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import com.tomclaw.knopochki.core.KnopochkiView
import com.tomclaw.knopochki.theme.Palette
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Brand accent seed — the calm "technical" green (#1e695c). It colours the real
 * accents only (primary / secondary / tertiary): buttons, chips, switches, section
 * headers, badges.
 */
private const val BRAND_SEED: Int = 0xFF1E695C.toInt()

/**
 * The app's palette: brand accents on **neutral** surfaces.
 *
 * `Palette.fromSeed` also tints the two neutral rows with the seed hue, which paints
 * every surface, container and outline a faint green. We overwrite those rows with
 * pure greys so light is plainly light and dark is plainly dark, and the accent
 * shows up only where it is actually an accent.
 */
fun appPalette(): Palette {
    val tones = Palette.fromSeed(BRAND_SEED).tones.copyOf()
    for (index in 0 until Palette.TONE_COUNT) {
        val grey = neutralTone(Palette.TONE_VALUES[index].toDouble())
        tones[Palette.NEUTRAL * Palette.TONE_COUNT + index] = grey
        tones[Palette.NEUTRAL_VARIANT * Palette.TONE_COUNT + index] = grey
    }
    return Palette(tones)
}

/** CIELAB lightness (0..100) → neutral sRGB grey, i.e. the same tone at chroma 0. */
private fun neutralTone(lightness: Double): Int {
    val y = if (lightness > 8.0) {
        val f = (lightness + 16.0) / 116.0
        f * f * f
    } else {
        lightness / 903.2962962962963
    }
    val channel = if (y <= 0.0031308) 12.92 * y else 1.055 * y.pow(1.0 / 2.4) - 0.055
    val value = (channel.coerceIn(0.0, 1.0) * 255.0).roundToInt()
    return Color.rgb(value, value, value)
}

/** Whether the (pref-driven, AppCompat-applied) configuration is currently dark. */
fun Context.isNightMode(): Boolean =
    (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
        Configuration.UI_MODE_NIGHT_YES

/**
 * Creates the single [KnopochkiView] that hosts a screen, skins it with the brand
 * palette for the current light/dark mode, installs it as the content view and
 * draws edge-to-edge. The caller sets `view.root` afterwards (its `*ViewImpl`
 * builds the node tree). Knopochki itself dispatches the system-bar insets down
 * the node tree, so screens only wrap content in `Insets` where needed.
 */
fun AppCompatActivity.installKnopochki(): KnopochkiView {
    val dark = isNightMode()
    val view = KnopochkiView(this)
    view.isDark = dark
    view.setSkin(appPalette(), animated = false)
    setContentView(view)
    applyEdgeToEdge(dark)
    return view
}

/** Enables edge-to-edge and matches system-bar icon contrast to the theme. */
fun AppCompatActivity.applyEdgeToEdge(dark: Boolean) {
    WindowCompat.setDecorFitsSystemWindows(window, false)
    @Suppress("DEPRECATION")
    window.statusBarColor = Color.TRANSPARENT
    @Suppress("DEPRECATION")
    window.navigationBarColor = Color.TRANSPARENT
    WindowCompat.getInsetsController(window, window.decorView).apply {
        isAppearanceLightStatusBars = !dark
        isAppearanceLightNavigationBars = !dark
    }
    ViewCompat.requestApplyInsets(window.decorView)
}
