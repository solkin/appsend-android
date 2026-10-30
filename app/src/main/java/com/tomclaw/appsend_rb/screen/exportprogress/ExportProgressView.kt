package com.tomclaw.appsend_rb.screen.exportprogress

import android.view.LayoutInflater
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import com.google.android.material.listitem.ListItemViewHolder
import com.jakewharton.rxrelay3.PublishRelay
import com.tomclaw.appsend_rb.R
import com.tomclaw.appsend_rb.databinding.ActivityExportProgressBinding
import com.tomclaw.appsend_rb.databinding.ItemExportRowBinding
import com.tomclaw.appsend_rb.domain.ExportStatus
import com.tomclaw.appsend_rb.util.AppIcons
import com.tomclaw.appsend_rb.util.applySystemBarInsets
import io.reactivex.rxjava3.core.Observable

interface ExportProgressView {

    fun render(model: ExportProgressViewModel)

    fun cancelClicks(): Observable<Unit>

    fun minimizeClicks(): Observable<Unit>

    fun backClicks(): Observable<Unit>

    fun retryClicks(): Observable<String>

    fun doneClicks(): Observable<Unit>

    fun shareClicks(): Observable<Unit>

    fun openExportsClicks(): Observable<Unit>

}

class ExportProgressViewImpl(
    private val binding: ActivityExportProgressBinding,
    lifecycleOwner: LifecycleOwner,
    backDispatcher: OnBackPressedDispatcher,
) : ExportProgressView {

    private val context = binding.root.context
    private val cancelRelay = PublishRelay.create<Unit>()
    private val minimizeRelay = PublishRelay.create<Unit>()
    private val backRelay = PublishRelay.create<Unit>()
    private val retryRelay = PublishRelay.create<String>()
    private val doneRelay = PublishRelay.create<Unit>()
    private val shareRelay = PublishRelay.create<Unit>()
    private val openExportsRelay = PublishRelay.create<Unit>()
    private val adapter = ExportRowsAdapter(retryRelay::accept)

    init {
        backDispatcher.addCallback(lifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = backRelay.accept(Unit)
        })
        binding.root.applySystemBarInsets(top = true, bottom = true)
        binding.list.layoutManager = LinearLayoutManager(context)
        binding.list.itemAnimator = null
        binding.list.adapter = adapter
        binding.minimize.setOnClickListener { minimizeRelay.accept(Unit) }
        binding.cancelButton.setOnClickListener { cancelRelay.accept(Unit) }
        binding.minimizeButton.setOnClickListener { minimizeRelay.accept(Unit) }
        binding.doneButton.setOnClickListener { doneRelay.accept(Unit) }
        binding.shareButton.setOnClickListener { shareRelay.accept(Unit) }
        binding.openExportsButton.setOnClickListener { openExportsRelay.accept(Unit) }
    }

    override fun render(model: ExportProgressViewModel) {
        binding.title.text = if (model.running) {
            context.getString(R.string.export_progress_title, model.currentIndex, model.count)
        } else if (model.hasFailures) {
            context.getString(R.string.export_finished_with_failures)
        } else {
            context.getString(R.string.export_finished)
        }
        binding.subtitle.text = context.getString(R.string.export_progress_bytes, model.written, model.total)
        binding.progress.isVisible = model.running
        binding.progress.setProgressCompat(model.percent, true)
        binding.runningActions.isVisible = model.running
        binding.finishedActions.isVisible = !model.running
        binding.shareButton.isVisible = model.canShare
        binding.openExportsButton.isVisible = model.canOpenExports
        binding.minimize.isVisible = model.running
        adapter.submitList(model.rows)
    }

    override fun cancelClicks(): Observable<Unit> = cancelRelay

    override fun minimizeClicks(): Observable<Unit> = minimizeRelay

    override fun backClicks(): Observable<Unit> = backRelay

    override fun retryClicks(): Observable<String> = retryRelay

    override fun doneClicks(): Observable<Unit> = doneRelay

    override fun shareClicks(): Observable<Unit> = shareRelay

    override fun openExportsClicks(): Observable<Unit> = openExportsRelay

}

private class ExportRowsAdapter(
    private val onRetry: (String) -> Unit,
) : ListAdapter<ExportRow, ExportRowsAdapter.Holder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemExportRowBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(getItem(position))

    inner class Holder(private val binding: ItemExportRowBinding) : ListItemViewHolder(binding.root) {

        private val iconSize = binding.root.resources.getDimensionPixelSize(R.dimen.app_icon_size)

        fun bind(row: ExportRow) {
            bind(row.position, row.total)
            val context = binding.root.context
            binding.title.text = row.label
            binding.note.text = when (row.status) {
                ExportStatus.QUEUED -> context.getString(R.string.export_status_queued)
                ExportStatus.FAILED -> context.getString(R.string.export_status_failed)
                ExportStatus.CANCELLED -> context.getString(R.string.export_status_cancelled)
                ExportStatus.RUNNING, ExportStatus.DONE -> row.note
            }
            binding.done.isVisible = row.status == ExportStatus.DONE
            binding.progress.isVisible = row.status == ExportStatus.RUNNING
            binding.progress.setProgressCompat(row.percent, false)
            binding.retry.isVisible = row.status == ExportStatus.FAILED || row.status == ExportStatus.CANCELLED
            binding.retry.setOnClickListener { onRetry(row.packageName) }
            binding.icon.tag = row.packageName
            AppIcons.load(context, row.packageName, iconSize) { packageName, bitmap ->
                if (binding.icon.tag == packageName) binding.icon.setImageBitmap(bitmap)
            }
        }

    }

}

private val DIFF = object : DiffUtil.ItemCallback<ExportRow>() {

    override fun areItemsTheSame(oldItem: ExportRow, newItem: ExportRow) =
        oldItem.packageName == newItem.packageName

    override fun areContentsTheSame(oldItem: ExportRow, newItem: ExportRow) = oldItem == newItem

}
