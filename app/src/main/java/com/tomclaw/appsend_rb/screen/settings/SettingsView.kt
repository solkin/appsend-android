package com.tomclaw.appsend_rb.screen.settings

import android.view.LayoutInflater
import androidx.core.view.isVisible
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jakewharton.rxrelay3.PublishRelay
import com.tomclaw.appsend_rb.R
import com.tomclaw.appsend_rb.databinding.DialogTemplateBinding
import com.tomclaw.appsend_rb.databinding.FragmentSettingsBinding
import com.tomclaw.appsend_rb.domain.ExportFormat
import com.tomclaw.appsend_rb.domain.ThemeMode
import com.tomclaw.appsend_rb.util.applySystemBarInsets
import io.reactivex.rxjava3.core.Observable

interface SettingsView {

    fun render(model: SettingsViewModel)

    fun themeChanges(): Observable<ThemeMode>

    fun dynamicColorChanges(): Observable<Boolean>

    fun analyticsChanges(): Observable<Boolean>

    fun formatChanges(): Observable<ExportFormat>

    fun templateChanges(): Observable<String>

    fun folderClicks(): Observable<Unit>

    fun rateClicks(): Observable<Unit>

    fun developerAppsClicks(): Observable<Unit>

}

class SettingsViewImpl(private val binding: FragmentSettingsBinding) : SettingsView {

    private val context = binding.root.context
    private val themeRelay = PublishRelay.create<ThemeMode>()
    private val dynamicColorRelay = PublishRelay.create<Boolean>()
    private val analyticsRelay = PublishRelay.create<Boolean>()
    private val formatRelay = PublishRelay.create<ExportFormat>()
    private val templateRelay = PublishRelay.create<String>()
    private val folderRelay = PublishRelay.create<Unit>()
    private val rateRelay = PublishRelay.create<Unit>()
    private val developerAppsRelay = PublishRelay.create<Unit>()

    private var format = ExportFormat.BUNDLE
    private var template = ""
    private var applyingModel = false

    init {
        binding.scroll.applySystemBarInsets(top = true)

        binding.themes.addOnButtonCheckedListener { _, id, checked ->
            if (checked && !applyingModel) {
                themeRelay.accept(
                    when (id) {
                        R.id.theme_light -> ThemeMode.LIGHT
                        R.id.theme_dark -> ThemeMode.DARK
                        else -> ThemeMode.SYSTEM
                    }
                )
            }
        }
        binding.dynamicColors.title.setText(R.string.settings_dynamic_colors)
        binding.dynamicColors.subtitle.setText(R.string.settings_dynamic_colors_summary)
        // Both toggles share one id, so restored state would mix them up: render() sets them instead.
        binding.dynamicColors.toggle.isSaveEnabled = false
        binding.dynamicColors.toggle.setOnCheckedChangeListener { _, checked ->
            if (!applyingModel) dynamicColorRelay.accept(checked)
        }
        binding.analytics.title.setText(R.string.settings_analytics)
        binding.analytics.subtitle.setText(R.string.settings_analytics_summary)
        binding.analytics.root.updateAppearance(0, 1)
        binding.analytics.toggle.isSaveEnabled = false
        binding.analytics.toggle.setOnCheckedChangeListener { _, checked ->
            if (!applyingModel) analyticsRelay.accept(checked)
        }

        action(binding.formatRow, R.string.settings_export_format, 0, 3) { showFormatDialog() }
        action(binding.folderRow, R.string.export_folder, 1, 3) { folderRelay.accept(Unit) }
        action(binding.templateRow, R.string.export_file_name, 2, 3) { showTemplateDialog() }

        action(binding.versionRow, R.string.settings_version, 0, 3) {}
        binding.versionRow.icon.setImageResource(R.drawable.ic_info)
        binding.versionRow.icon.isVisible = true
        binding.versionRow.chevron.isVisible = false
        action(binding.rateRow, R.string.rate_app, 1, 3) { rateRelay.accept(Unit) }
        binding.rateRow.icon.setImageResource(R.drawable.ic_star)
        binding.rateRow.icon.isVisible = true
        binding.rateRow.subtitle.isVisible = false
        binding.rateRow.chevron.isVisible = false
        action(binding.developerRow, R.string.all_apps, 2, 3) { developerAppsRelay.accept(Unit) }
        binding.developerRow.icon.setImageResource(R.drawable.ic_person)
        binding.developerRow.icon.isVisible = true
        binding.developerRow.subtitle.setText(R.string.tomclaw_software)
        binding.developerRow.chevron.isVisible = false
    }

    private fun action(
        row: com.tomclaw.appsend_rb.databinding.ItemActionRowBinding,
        title: Int,
        position: Int,
        count: Int,
        onClick: () -> Unit,
    ) {
        row.title.setText(title)
        row.icon.isVisible = false
        row.root.updateAppearance(position, count)
        row.card.setOnClickListener { onClick() }
    }

    override fun render(model: SettingsViewModel) {
        applyingModel = true
        binding.themes.check(
            when (model.themeMode) {
                ThemeMode.SYSTEM -> R.id.theme_system
                ThemeMode.LIGHT -> R.id.theme_light
                ThemeMode.DARK -> R.id.theme_dark
            }
        )
        binding.dynamicColors.root.isVisible = model.dynamicColorsAvailable
        binding.dynamicColors.toggle.isChecked = model.dynamicColors
        binding.analytics.toggle.isChecked = model.analytics
        applyingModel = false

        format = model.exportFormat
        template = model.template
        binding.formatRow.subtitle.setText(
            if (model.exportFormat == ExportFormat.BASE) {
                R.string.settings_export_format_base
            } else {
                R.string.settings_export_format_bundle
            }
        )
        binding.folderRow.subtitle.text = model.folder
        binding.templateRow.subtitle.text = model.template
        binding.versionRow.subtitle.text = model.version
    }

    private fun showFormatDialog() {
        val formats = ExportFormat.entries
        val labels = formats.map {
            context.getString(
                if (it == ExportFormat.BASE) R.string.settings_export_format_base else R.string.settings_export_format_bundle
            )
        }.toTypedArray()
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.settings_export_format)
            .setSingleChoiceItems(labels, formats.indexOf(format)) { dialog, index ->
                formatRelay.accept(formats[index])
                dialog.dismiss()
            }
            .show()
    }

    private fun showTemplateDialog() {
        val dialog = DialogTemplateBinding.inflate(LayoutInflater.from(context))
        dialog.input.setText(template)
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.export_file_name)
            .setView(dialog.root)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                dialog.input.text?.toString()?.takeIf { it.isNotBlank() }?.let(templateRelay::accept)
            }
            .show()
    }

    override fun themeChanges(): Observable<ThemeMode> = themeRelay

    override fun dynamicColorChanges(): Observable<Boolean> = dynamicColorRelay

    override fun analyticsChanges(): Observable<Boolean> = analyticsRelay

    override fun formatChanges(): Observable<ExportFormat> = formatRelay

    override fun templateChanges(): Observable<String> = templateRelay

    override fun folderClicks(): Observable<Unit> = folderRelay

    override fun rateClicks(): Observable<Unit> = rateRelay

    override fun developerAppsClicks(): Observable<Unit> = developerAppsRelay

}
