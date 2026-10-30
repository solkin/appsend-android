package com.tomclaw.appsend_rb.screen.exportoptions

import android.view.LayoutInflater
import androidx.core.view.isVisible
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.jakewharton.rxrelay3.PublishRelay
import com.tomclaw.appsend_rb.R
import com.tomclaw.appsend_rb.databinding.DialogTemplateBinding
import com.tomclaw.appsend_rb.databinding.FragmentExportOptionsBinding
import com.tomclaw.appsend_rb.domain.ExportFormat
import io.reactivex.rxjava3.core.Observable

interface ExportOptionsView {

    fun render(model: ExportOptionsViewModel)

    fun showStorageAccessRequired(canOpenSettings: Boolean)

    fun ownSettingsClicks(): Observable<Unit>

    fun formatChanges(): Observable<ExportFormat>

    fun folderClicks(): Observable<Unit>

    fun templateChanges(): Observable<String>

    fun extractClicks(): Observable<Unit>

    fun shareClicks(): Observable<Unit>

}

class ExportOptionsViewImpl(private val binding: FragmentExportOptionsBinding) : ExportOptionsView {

    private val context = binding.root.context
    private val formatRelay = PublishRelay.create<ExportFormat>()
    private val folderRelay = PublishRelay.create<Unit>()
    private val templateRelay = PublishRelay.create<String>()
    private val extractRelay = PublishRelay.create<Unit>()
    private val shareRelay = PublishRelay.create<Unit>()
    private val ownSettingsRelay = PublishRelay.create<Unit>()

    private var template = ""
    private var applyingModel = false

    init {
        binding.formats.addOnButtonCheckedListener { _, id, checked ->
            if (checked && !applyingModel) {
                formatRelay.accept(if (id == R.id.format_base) ExportFormat.BASE else ExportFormat.BUNDLE)
            }
        }
        binding.folderRow.title.setText(R.string.export_folder)
        binding.folderRow.icon.setImageResource(R.drawable.ic_folder)
        binding.folderRow.root.updateAppearance(0, 2)
        binding.folderRow.card.setOnClickListener { folderRelay.accept(Unit) }
        binding.nameRow.title.setText(R.string.export_file_name)
        binding.nameRow.icon.setImageResource(R.drawable.ic_file)
        binding.nameRow.root.updateAppearance(1, 2)
        binding.nameRow.card.setOnClickListener { showTemplateDialog() }
        binding.extractButton.setOnClickListener { extractRelay.accept(Unit) }
        binding.shareButton.setOnClickListener { shareRelay.accept(Unit) }
    }

    override fun render(model: ExportOptionsViewModel) {
        binding.title.text = model.title
        binding.subtitle.text = model.subtitle

        binding.formats.isVisible = model.showFormats
        binding.formatBundle.text = context.getString(R.string.export_format_bundle, model.bundleSize)
        binding.formatBase.text = context.getString(R.string.export_format_base, model.baseSize)
        applyingModel = true
        binding.formats.check(if (model.format == ExportFormat.BASE) R.id.format_base else R.id.format_bundle)
        applyingModel = false

        binding.notice.isVisible = model.notice != ExportNotice.NONE
        when (model.notice) {
            ExportNotice.BUNDLE_INSTALL -> binding.noticeText.setText(R.string.export_notice_bundle)
            ExportNotice.BASE_ONLY -> binding.noticeText.setText(R.string.export_notice_base)
            ExportNotice.SYSTEM_APP -> binding.noticeText.setText(R.string.backup_limitations_warning_message)
            ExportNotice.NONE -> binding.noticeText.text = null
        }

        binding.folderRow.subtitle.text = model.folder
        binding.nameRow.subtitle.text = model.fileName ?: model.template
        template = model.template
        binding.extractButton.isEnabled = model.ready
        binding.shareButton.isEnabled = model.ready
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

    override fun showStorageAccessRequired(canOpenSettings: Boolean) {
        val snackbar = Snackbar.make(binding.root, R.string.write_permission_extract, Snackbar.LENGTH_LONG)
        if (canOpenSettings) {
            snackbar.setAction(R.string.settings) { ownSettingsRelay.accept(Unit) }
        }
        snackbar.show()
    }

    override fun ownSettingsClicks(): Observable<Unit> = ownSettingsRelay

    override fun formatChanges(): Observable<ExportFormat> = formatRelay

    override fun folderClicks(): Observable<Unit> = folderRelay

    override fun templateChanges(): Observable<String> = templateRelay

    override fun extractClicks(): Observable<Unit> = extractRelay

    override fun shareClicks(): Observable<Unit> = shareRelay

}
