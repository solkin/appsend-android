package com.tomclaw.appsend_rb

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.tomclaw.appsend_rb.screen.apps.OutputWrapperImpl
import com.tomclaw.appsend_rb.util.getBooleanPreference
import com.tomclaw.appsend_rb.util.installKnopochki
import com.tomclaw.appsend_rb.util.setBooleanPreference
import com.tomclaw.knopochki.core.Alignment
import com.tomclaw.knopochki.core.Box
import com.tomclaw.knopochki.core.Column
import com.tomclaw.knopochki.core.Insets
import com.tomclaw.knopochki.core.KnopochkiView
import com.tomclaw.knopochki.core.Node
import com.tomclaw.knopochki.core.Row
import com.tomclaw.knopochki.core.Scroll
import com.tomclaw.knopochki.draw.Icons
import com.tomclaw.knopochki.theme.Role
import com.tomclaw.knopochki.widget.ListItem
import com.tomclaw.knopochki.widget.Snackbar
import com.tomclaw.knopochki.widget.Switch
import com.tomclaw.knopochki.widget.Text
import com.tomclaw.knopochki.widget.TextStyle
import com.tomclaw.knopochki.widget.TopAppBar

/**
 * Settings holds only what has no control on the apps screen itself.
 *
 * System apps and sort order used to live here as well, duplicating the filter
 * chips — and the two disagreed: the chips never wrote their choice back, so
 * changing either one here quietly did nothing until the next cold start. The
 * chips own those two now.
 */
class SettingsActivity : AppCompatActivity() {

    private val analytics by lazy { application.getComponent().analytics() }

    private val snackbar = Snackbar()

    private lateinit var view: KnopochkiView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        view = installKnopochki()
        analytics.trackEvent("screen_open", mapOf("screen" to "settings"))
        view.root = buildRoot()
    }

    private fun buildRoot(): Node {
        val bar = TopAppBar(
            title = getString(R.string.settings),
            navigationIcon = Icons.ARROW_BACK,
            onNavigationClick = { finish() },
        )

        val content = Column()
            .add(section(R.string.pref_base_title))
            .add(
                switchRow(
                    R.string.pref_title_dark_theme,
                    R.string.pref_dark_theme_system,
                    getBooleanPreference(R.string.pref_dark_theme, R.bool.pref_dark_theme_default),
                ) { onDarkThemeChanged(it) }
            )
            .add(
                switchRow(
                    R.string.pref_title_runnable,
                    R.string.pref_summary_runnable,
                    getBooleanPreference(R.string.pref_runnable, R.bool.pref_runnable_default),
                ) { onRunnableChanged(it) }
            )
            .add(
                clickRow(
                    getString(R.string.pref_title_clear_cache),
                    getString(R.string.pref_summary_clear_cache),
                ) { clearCache() }
            )

        val scroll = Scroll(Insets(content, applyBottom = true, applySides = true))
        scroll.onScroll = { bar.onContentScrolled(it) }

        return Box()
            .add(Column().add(bar).addFilling(scroll))
            .add(Insets(snackbar, applyBottom = true, applySides = true))
    }

    private fun section(titleRes: Int): Node =
        Column(paddingDp = 16f)
            .add(Text(getString(titleRes), style = TextStyle.TITLE_SMALL, role = Role.PRIMARY))

    private fun switchRow(
        titleRes: Int,
        summaryRes: Int,
        checked: Boolean,
        onChange: (Boolean) -> Unit,
    ): Node = Row(spacingDp = 16f, paddingDp = 16f)
        .add(
            Column(spacingDp = 2f)
                .add(Text(getString(titleRes), style = TextStyle.BODY_LARGE, role = Role.ON_SURFACE))
                .add(Text(getString(summaryRes), style = TextStyle.BODY_SMALL, role = Role.ON_SURFACE_VARIANT)),
            weight = 1f,
            align = Alignment.CENTER,
        )
        .add(Switch("", checked = checked, onCheckedChange = onChange), align = Alignment.CENTER)

    private fun clickRow(title: String, summary: String, onClick: () -> Unit): ListItem =
        ListItem(headline = title, supporting = summary, onClick = onClick)

    // ------------------------------------------------------------------ actions

    private fun onDarkThemeChanged(enabled: Boolean) {
        setBooleanPreference(R.string.pref_dark_theme, enabled)
        trackSettingChanged("dark_theme", enabled.toString())
        AppCompatDelegate.setDefaultNightMode(
            if (enabled) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    private fun onRunnableChanged(enabled: Boolean) {
        setBooleanPreference(R.string.pref_runnable, enabled)
        trackSettingChanged("runnable_only", enabled.toString())
        // The apps screen reads this one fresh on every load, so it only needs
        // to be told to reload.
        setResult(RESULT_UPDATE)
    }

    private fun clearCache() {
        analytics.trackEvent("export_cache_clear_started", emptyMap())
        val outputWrapper = OutputWrapperImpl(applicationContext, contentResolver)
        Thread {
            try {
                outputWrapper.clearExports()
                runOnUiThread {
                    analytics.trackEvent("export_cache_clear_succeeded", emptyMap())
                    snackbar.show(getString(R.string.cache_cleared_successfully))
                }
            } catch (error: Throwable) {
                runOnUiThread {
                    analytics.trackEvent("export_cache_clear_failed", emptyMap())
                    snackbar.show(getString(R.string.cache_clearing_failed))
                }
            }
        }.start()
    }

    private fun trackSettingChanged(setting: String, value: String) {
        analytics.trackEvent("settings_changed", mapOf("setting" to setting, "value" to value))
    }

    companion object {
        const val RESULT_UPDATE = 5
    }
}
