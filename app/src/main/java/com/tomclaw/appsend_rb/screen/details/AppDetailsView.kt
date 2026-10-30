package com.tomclaw.appsend_rb.screen.details

import android.view.LayoutInflater
import androidx.core.view.isVisible
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.jakewharton.rxrelay3.PublishRelay
import com.tomclaw.appsend_rb.R
import com.tomclaw.appsend_rb.databinding.ActivityDetailsBinding
import com.tomclaw.appsend_rb.databinding.ItemInfoRowBinding
import com.tomclaw.appsend_rb.util.AppIcons
import com.tomclaw.appsend_rb.util.applySystemBarInsets
import io.reactivex.rxjava3.core.Observable

interface AppDetailsView {

    fun render(model: AppDetailsViewModel)

    fun showPackageCopied()

    fun showNotRunnable()

    fun showBackupWarning()

    fun backClicks(): Observable<Unit>

    fun openClicks(): Observable<Unit>

    fun extractClicks(): Observable<Unit>

    fun shareClicks(): Observable<Unit>

    fun copyPackageClicks(): Observable<Unit>

    fun permissionsClicks(): Observable<Unit>

    fun storeClicks(): Observable<Unit>

    fun systemDetailsClicks(): Observable<Unit>

    fun removeClicks(): Observable<Unit>

    fun backupWarningConfirmations(): Observable<Unit>

}

class AppDetailsViewImpl(private val binding: ActivityDetailsBinding) : AppDetailsView {

    private val context = binding.root.context
    private val iconSize = context.resources.getDimensionPixelSize(R.dimen.app_icon_size_large)

    private val backRelay = PublishRelay.create<Unit>()
    private val openRelay = PublishRelay.create<Unit>()
    private val extractRelay = PublishRelay.create<Unit>()
    private val shareRelay = PublishRelay.create<Unit>()
    private val copyRelay = PublishRelay.create<Unit>()
    private val permissionsRelay = PublishRelay.create<Unit>()
    private val storeRelay = PublishRelay.create<Unit>()
    private val systemDetailsRelay = PublishRelay.create<Unit>()
    private val removeRelay = PublishRelay.create<Unit>()
    private val backupWarningRelay = PublishRelay.create<Unit>()

    init {
        binding.appBar.applySystemBarInsets(top = true)
        binding.content.applySystemBarInsets(bottom = true)
        binding.toolbar.setNavigationOnClickListener { backRelay.accept(Unit) }
        binding.packageRow.setOnClickListener { copyRelay.accept(Unit) }
        binding.openButton.setOnClickListener { openRelay.accept(Unit) }
        binding.extractButton.setOnClickListener { extractRelay.accept(Unit) }
        binding.shareButton.setOnClickListener { shareRelay.accept(Unit) }

        val actions = binding.actionRows
        actions.permissionsRow.title.setText(R.string.app_details_permissions)
        actions.permissionsRow.icon.setImageResource(R.drawable.ic_shield)
        actions.permissionsRow.card.setOnClickListener { permissionsRelay.accept(Unit) }
        actions.storeRow.title.setText(R.string.find_on_gp)
        actions.storeRow.icon.setImageResource(R.drawable.ic_store)
        actions.storeRow.card.setOnClickListener { storeRelay.accept(Unit) }
        actions.systemRow.title.setText(R.string.app_details_open_system_details)
        actions.systemRow.icon.setImageResource(R.drawable.ic_info)
        actions.systemRow.card.setOnClickListener { systemDetailsRelay.accept(Unit) }
        actions.removeRow.title.setText(R.string.remove)
        actions.removeRow.icon.setImageResource(R.drawable.ic_delete)
        actions.removeRow.card.setOnClickListener { removeRelay.accept(Unit) }
        listOf(actions.permissionsRow, actions.storeRow, actions.systemRow, actions.removeRow)
            .forEachIndexed { index, row ->
                row.root.updateAppearance(index, ACTION_ROW_COUNT)
                row.chevron.isVisible = index == 0
                row.subtitle.isVisible = index == 0
            }
    }

    override fun render(model: AppDetailsViewModel) {
        binding.title.text = model.label
        binding.packageName.text = model.packageName
        binding.openButton.isEnabled = model.canOpen
        AppIcons.load(context, model.packageName, iconSize) { _, bitmap ->
            binding.icon.setImageBitmap(bitmap)
        }

        val rows = binding.infoRows
        rows.removeAllViews()
        val inflater = LayoutInflater.from(context)
        model.rows.forEachIndexed { index, row ->
            val item = ItemInfoRowBinding.inflate(inflater, rows, true)
            item.root.updateAppearance(index, model.rows.size)
            item.label.text = row.label
            item.value.text = row.value
        }

        binding.actionRows.permissionsRow.subtitle.text = model.permissions
        binding.actionRows.permissionsRow.card.isEnabled = model.hasPermissions
    }

    override fun showPackageCopied() {
        Snackbar.make(binding.root, R.string.app_details_package_copied, Snackbar.LENGTH_SHORT).show()
    }

    override fun showNotRunnable() {
        Snackbar.make(binding.root, R.string.non_launchable_package, Snackbar.LENGTH_SHORT).show()
    }

    override fun showBackupWarning() {
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.backup_limitations_warning_title)
            .setMessage(R.string.backup_limitations_warning_message)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.continue_anyway) { _, _ -> backupWarningRelay.accept(Unit) }
            .show()
    }

    override fun backClicks(): Observable<Unit> = backRelay

    override fun openClicks(): Observable<Unit> = openRelay

    override fun extractClicks(): Observable<Unit> = extractRelay

    override fun shareClicks(): Observable<Unit> = shareRelay

    override fun copyPackageClicks(): Observable<Unit> = copyRelay

    override fun permissionsClicks(): Observable<Unit> = permissionsRelay

    override fun storeClicks(): Observable<Unit> = storeRelay

    override fun systemDetailsClicks(): Observable<Unit> = systemDetailsRelay

    override fun removeClicks(): Observable<Unit> = removeRelay

    override fun backupWarningConfirmations(): Observable<Unit> = backupWarningRelay

}

private const val ACTION_ROW_COUNT = 4
