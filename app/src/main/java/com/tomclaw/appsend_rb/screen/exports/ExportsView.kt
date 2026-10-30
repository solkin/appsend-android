package com.tomclaw.appsend_rb.screen.exports

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.listitem.ListItemViewHolder
import com.google.android.material.snackbar.Snackbar
import com.jakewharton.rxrelay3.PublishRelay
import com.tomclaw.appsend_rb.R
import com.tomclaw.appsend_rb.databinding.FragmentExportsBinding
import com.tomclaw.appsend_rb.databinding.ItemExportFileBinding
import com.tomclaw.appsend_rb.databinding.ItemSectionBinding
import com.tomclaw.appsend_rb.domain.ExportedFile
import com.tomclaw.appsend_rb.screen.main.BottomBarHost
import com.tomclaw.appsend_rb.util.AppIcons
import com.tomclaw.appsend_rb.util.applySystemBarInsets
import io.reactivex.rxjava3.core.Observable

interface ExportsView {

    fun render(model: ExportsViewModel)

    fun showFileMenu(row: ExportsRow.File)

    fun showDeleteFailed()

    fun showNothingSelected()

    fun showDeleteSelectedConfirmation(count: Int)

    fun filterChanges(): Observable<ExportsFilter>

    fun rowClicks(): Observable<ExportsRow.File>

    fun rowLongClicks(): Observable<ExportsRow.File>

    fun installClicks(): Observable<ExportedFile>

    fun shareClicks(): Observable<ExportedFile>

    fun deleteConfirmations(): Observable<ExportedFile>

    fun clearConfirmations(): Observable<Unit>

    fun changeFolderClicks(): Observable<Unit>

    fun installFromFileClicks(): Observable<Unit>

    fun selectClicks(): Observable<Unit>

    fun cancelSelectionClicks(): Observable<Unit>

    fun selectAllClicks(): Observable<Unit>

    fun shareSelectedClicks(): Observable<Unit>

    fun deleteSelectedClicks(): Observable<Unit>

    fun deleteSelectedConfirmations(): Observable<Unit>

}

class ExportsViewImpl(
    private val binding: FragmentExportsBinding,
    lifecycleOwner: LifecycleOwner,
    backDispatcher: OnBackPressedDispatcher,
    private val bottomBar: BottomBarHost?,
) : ExportsView {

    private val context = binding.root.context
    private val filterRelay = PublishRelay.create<ExportsFilter>()
    private val rowClickRelay = PublishRelay.create<ExportsRow.File>()
    private val rowLongClickRelay = PublishRelay.create<ExportsRow.File>()
    private val installRelay = PublishRelay.create<ExportedFile>()
    private val shareRelay = PublishRelay.create<ExportedFile>()
    private val deleteRelay = PublishRelay.create<ExportedFile>()
    private val clearRelay = PublishRelay.create<Unit>()
    private val changeFolderRelay = PublishRelay.create<Unit>()
    private val installFromFileRelay = PublishRelay.create<Unit>()
    private val selectRelay = PublishRelay.create<Unit>()
    private val cancelSelectionRelay = PublishRelay.create<Unit>()
    private val selectAllRelay = PublishRelay.create<Unit>()
    private val shareSelectedRelay = PublishRelay.create<Unit>()
    private val deleteSelectedRelay = PublishRelay.create<Unit>()
    private val deleteSelectedConfirmRelay = PublishRelay.create<Unit>()
    private var folder = ""
    private var menuAnchor: View? = null
    private val adapter = ExportsAdapter(
        onClick = { row, anchor ->
            menuAnchor = anchor
            rowClickRelay.accept(row)
        },
        onLongClick = rowLongClickRelay::accept,
        onInstall = installRelay::accept,
        onShare = shareRelay::accept,
    )
    private val selectionBack = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() = cancelSelectionRelay.accept(Unit)
    }

    init {
        backDispatcher.addCallback(lifecycleOwner, selectionBack)
        binding.appBar.applySystemBarInsets(top = true)
        binding.list.layoutManager = LinearLayoutManager(context)
        binding.list.adapter = adapter
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_select -> selectRelay.accept(Unit)
                R.id.action_change_folder -> changeFolderRelay.accept(Unit)
                R.id.action_clear -> confirmClear()
            }
            true
        }
        binding.selectionBar.setNavigationOnClickListener { cancelSelectionRelay.accept(Unit) }
        binding.selectionBar.setOnMenuItemClickListener {
            selectAllRelay.accept(Unit)
            true
        }
        binding.filters.setOnCheckedStateChangeListener { _, ids ->
            filterRelay.accept(
                when (ids.firstOrNull()) {
                    R.id.filter_installed -> ExportsFilter.INSTALLED
                    R.id.filter_outdated -> ExportsFilter.OUTDATED
                    else -> ExportsFilter.ALL
                }
            )
        }
        binding.installFromFile.setOnClickListener { installFromFileRelay.accept(Unit) }
        binding.shareButton.setOnClickListener { shareSelectedRelay.accept(Unit) }
        binding.deleteButton.setOnClickListener { deleteSelectedRelay.accept(Unit) }
    }

    override fun render(model: ExportsViewModel) {
        folder = model.folder
        binding.toolbar.subtitle = context.resources.getQuantityString(
            R.plurals.exports_summary,
            model.count,
            model.folder,
            model.count,
            model.totalSize,
        )
        binding.empty.isVisible = !model.loading && model.rows.isEmpty()
        adapter.submit(model.rows)

        val selecting = model.selectionMode
        selectionBack.isEnabled = selecting
        bottomBar?.setBottomBarHidden(BAR_OWNER, selecting)
        binding.toolbar.isVisible = !selecting
        binding.filters.isVisible = !selecting
        binding.selectionBar.isVisible = selecting
        binding.floatingToolbar.isVisible = selecting
        binding.installFromFile.isVisible = !selecting
        binding.selectionBar.title = context.getString(R.string.selected_apps_count, model.selectedCount)
        binding.selectionBar.subtitle = model.selectedSize
    }

    override fun showFileMenu(row: ExportsRow.File) {
        val menu = PopupMenu(context, menuAnchor ?: binding.list)
        menu.menu.add(R.string.remove).setOnMenuItemClickListener {
            deleteRelay.accept(row.entry.file)
            true
        }
        menu.show()
    }

    private fun confirmClear() {
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.clear_exports)
            .setMessage(context.getString(R.string.clear_exports_message, folder))
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.remove) { _, _ -> clearRelay.accept(Unit) }
            .show()
    }

    override fun showDeleteSelectedConfirmation(count: Int) {
        MaterialAlertDialogBuilder(context)
            .setMessage(context.resources.getQuantityString(R.plurals.delete_selected_exports, count, count))
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.remove) { _, _ -> deleteSelectedConfirmRelay.accept(Unit) }
            .show()
    }

    override fun showDeleteFailed() {
        Snackbar.make(binding.root, R.string.cache_clearing_failed, Snackbar.LENGTH_SHORT).show()
    }

    override fun showNothingSelected() {
        Snackbar.make(binding.root, R.string.no_files_selected, Snackbar.LENGTH_SHORT)
            .setAnchorView(binding.floatingToolbar)
            .show()
    }

    override fun filterChanges(): Observable<ExportsFilter> = filterRelay

    override fun rowClicks(): Observable<ExportsRow.File> = rowClickRelay

    override fun rowLongClicks(): Observable<ExportsRow.File> = rowLongClickRelay

    override fun installClicks(): Observable<ExportedFile> = installRelay

    override fun shareClicks(): Observable<ExportedFile> = shareRelay

    override fun deleteConfirmations(): Observable<ExportedFile> = deleteRelay

    override fun clearConfirmations(): Observable<Unit> = clearRelay

    override fun changeFolderClicks(): Observable<Unit> = changeFolderRelay

    override fun installFromFileClicks(): Observable<Unit> = installFromFileRelay

    override fun selectClicks(): Observable<Unit> = selectRelay

    override fun cancelSelectionClicks(): Observable<Unit> = cancelSelectionRelay

    override fun selectAllClicks(): Observable<Unit> = selectAllRelay

    override fun shareSelectedClicks(): Observable<Unit> = shareSelectedRelay

    override fun deleteSelectedClicks(): Observable<Unit> = deleteSelectedRelay

    override fun deleteSelectedConfirmations(): Observable<Unit> = deleteSelectedConfirmRelay

}

private class ExportsAdapter(
    private val onClick: (ExportsRow.File, View) -> Unit,
    private val onLongClick: (ExportsRow.File) -> Unit,
    private val onInstall: (ExportedFile) -> Unit,
    private val onShare: (ExportedFile) -> Unit,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var rows: List<ExportsRow> = emptyList()

    @SuppressLint("NotifyDataSetChanged")
    fun submit(rows: List<ExportsRow>) {
        if (this.rows == rows) return
        this.rows = rows
        notifyDataSetChanged()
    }

    override fun getItemCount() = rows.size

    override fun getItemViewType(position: Int) =
        if (rows[position] is ExportsRow.Section) TYPE_SECTION else TYPE_FILE

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_SECTION) {
            SectionHolder(ItemSectionBinding.inflate(inflater, parent, false))
        } else {
            FileHolder(ItemExportFileBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = rows[position]) {
            is ExportsRow.Section -> (holder as SectionHolder).bind(row)
            is ExportsRow.File -> (holder as FileHolder).bind(row)
        }
    }

    private class SectionHolder(private val binding: ItemSectionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(row: ExportsRow.Section) {
            binding.title.setText(
                when (row.age) {
                    ExportAge.TODAY -> R.string.exports_today
                    ExportAge.THIS_WEEK -> R.string.exports_this_week
                    ExportAge.EARLIER -> R.string.exports_earlier
                }
            )
        }

    }

    private inner class FileHolder(private val binding: ItemExportFileBinding) :
        ListItemViewHolder(binding.root) {

        private val iconSize = binding.root.resources.getDimensionPixelSize(R.dimen.app_icon_size)

        fun bind(row: ExportsRow.File) {
            bind(row.position, row.groupSize)
            val context = binding.root.context
            val entry = row.entry
            binding.title.text = entry.file.name
            val state = when (entry.state) {
                InstallState.UNKNOWN -> null
                InstallState.NOT_INSTALLED -> context.getString(R.string.export_state_not_installed)
                InstallState.CURRENT -> context.getString(R.string.export_state_current)
                InstallState.NEWER_INSTALLED -> context.getString(R.string.export_state_newer)
                InstallState.OLDER_INSTALLED -> context.getString(R.string.export_state_older)
            }
            binding.meta.text = listOfNotNull(row.size, row.time, state).joinToString(" · ")
            binding.card.setOnClickListener { onClick(row, binding.card) }
            binding.card.setOnLongClickListener {
                onLongClick(row)
                true
            }
            binding.card.isChecked = row.selected
            binding.install.isVisible = !row.selectable
            binding.share.isVisible = !row.selectable
            binding.install.setOnClickListener { onInstall(entry.file) }
            binding.share.setOnClickListener { onShare(entry.file) }
            binding.check.isVisible = row.selected
            binding.icon.isVisible = !row.selected
            val packageName = entry.info?.packageName
            binding.icon.tag = packageName
            binding.icon.setImageResource(R.drawable.ic_file)
            if (packageName != null && entry.state != InstallState.NOT_INSTALLED) {
                AppIcons.load(context, packageName, iconSize) { loaded, bitmap ->
                    if (binding.icon.tag == loaded) binding.icon.setImageBitmap(bitmap)
                }
            }
        }

    }

}

private const val TYPE_SECTION = 0
private const val TYPE_FILE = 1
private const val BAR_OWNER = "exports"
