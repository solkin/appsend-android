package com.tomclaw.appsend_rb.screen.permissions

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import com.google.android.material.listitem.ListItemViewHolder
import com.jakewharton.rxrelay3.PublishRelay
import com.tomclaw.appsend_rb.R
import com.tomclaw.appsend_rb.databinding.ActivityPermissionsBinding
import com.tomclaw.appsend_rb.databinding.ItemPermissionBinding
import com.tomclaw.appsend_rb.databinding.ItemSectionBinding
import com.tomclaw.appsend_rb.util.applySystemBarInsets
import io.reactivex.rxjava3.core.Observable

interface PermissionsView {

    fun render(model: PermissionsViewModel)

    fun backClicks(): Observable<Unit>

    fun filterChanges(): Observable<PermissionFilter>

}

class PermissionsViewImpl(private val binding: ActivityPermissionsBinding) : PermissionsView {

    private val context = binding.root.context
    private val backRelay = PublishRelay.create<Unit>()
    private val filterRelay = PublishRelay.create<PermissionFilter>()
    private val adapter = PermissionsAdapter()

    init {
        binding.appBar.applySystemBarInsets(top = true)
        binding.list.applySystemBarInsets(bottom = true)
        binding.list.layoutManager = LinearLayoutManager(context)
        binding.list.adapter = adapter
        binding.toolbar.setNavigationOnClickListener { backRelay.accept(Unit) }
        binding.filters.setOnCheckedStateChangeListener { _, ids ->
            filterRelay.accept(
                when (ids.firstOrNull()) {
                    R.id.filter_dangerous -> PermissionFilter.DANGEROUS
                    R.id.filter_granted -> PermissionFilter.GRANTED
                    else -> PermissionFilter.ALL
                }
            )
        }
    }

    override fun render(model: PermissionsViewModel) {
        binding.toolbar.subtitle = model.appLabel
        binding.filterAll.text = context.getString(R.string.permissions_filter_all, model.total)
        binding.filterDangerous.text = context.getString(R.string.permissions_filter_dangerous, model.dangerous)
        binding.filterGranted.text = context.getString(R.string.permissions_filter_granted, model.granted)
        adapter.submit(model.rows)
    }

    override fun backClicks(): Observable<Unit> = backRelay

    override fun filterChanges(): Observable<PermissionFilter> = filterRelay

}

private class PermissionsAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var rows: List<PermissionRow> = emptyList()

    @android.annotation.SuppressLint("NotifyDataSetChanged")
    fun submit(rows: List<PermissionRow>) {
        this.rows = rows
        notifyDataSetChanged()
    }

    override fun getItemCount() = rows.size

    override fun getItemViewType(position: Int) =
        if (rows[position] is PermissionRow.Section) TYPE_SECTION else TYPE_ITEM

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_SECTION) {
            SectionHolder(ItemSectionBinding.inflate(inflater, parent, false))
        } else {
            ItemHolder(ItemPermissionBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = rows[position]) {
            is PermissionRow.Section -> (holder as SectionHolder).bind(row)
            is PermissionRow.Item -> (holder as ItemHolder).bind(row)
        }
    }

    private class SectionHolder(private val binding: ItemSectionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(row: PermissionRow.Section) {
            binding.title.setText(
                if (row.dangerous) R.string.permissions_section_dangerous else R.string.permissions_section_normal
            )
            binding.title.setTextColor(
                MaterialColors.getColor(
                    binding.root,
                    if (row.dangerous) androidx.appcompat.R.attr.colorError else com.google.android.material.R.attr.colorOnSurfaceVariant,
                )
            )
        }

    }

    private class ItemHolder(private val binding: ItemPermissionBinding) :
        ListItemViewHolder(binding.root) {

        fun bind(row: PermissionRow.Item) {
            bind(row.position, row.groupSize)
            val permission = row.permission
            binding.title.text = permission.description
                ?: binding.root.context.getString(R.string.unknown_permission_description)
            binding.name.text = permission.name
            binding.state.isVisible = permission.dangerous
            binding.state.setText(
                if (permission.granted) R.string.permission_granted else R.string.permission_not_granted
            )
            binding.state.setBackgroundResource(
                if (permission.granted) R.drawable.bg_badge else R.drawable.bg_badge_outlined
            )
        }

    }

}

private const val TYPE_SECTION = 0
private const val TYPE_ITEM = 1
