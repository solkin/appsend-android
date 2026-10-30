package com.tomclaw.appsend_rb.screen.apps

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.google.android.material.listitem.ListItemViewHolder
import com.tomclaw.appsend_rb.R
import com.tomclaw.appsend_rb.databinding.ItemAppBinding
import com.tomclaw.appsend_rb.util.AppIcons

class AppsAdapter(
    private val onClick: (AppItem) -> Unit,
    private val onLongClick: (AppItem) -> Unit,
) : ListAdapter<AppItem, AppsAdapter.Holder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
        Holder(ItemAppBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(getItem(position))

    inner class Holder(private val binding: ItemAppBinding) : ListItemViewHolder(binding.root) {

        private val iconSize = binding.root.resources.getDimensionPixelSize(R.dimen.app_icon_size)

        fun bind(item: AppItem) {
            binding.card.setOnClickListener { onClick(item) }
            binding.card.setOnLongClickListener {
                onLongClick(item)
                true
            }
            binding.card.isChecked = item.selected
            binding.title.text = item.name
            binding.meta.text = binding.root.context.getString(
                R.string.app_row_meta,
                item.version,
                item.size,
                item.updated,
            )
            binding.badgeNew.isVisible = item.fresh
            binding.badgeSplit.isVisible = item.split
            binding.check.isVisible = item.selected
            binding.icon.isVisible = !item.selected
            binding.icon.tag = item.packageName
            binding.icon.setImageDrawable(null)
            AppIcons.load(binding.root.context, item.packageName, iconSize) { packageName, bitmap ->
                if (binding.icon.tag == packageName) binding.icon.setImageBitmap(bitmap)
            }
        }

    }

}

private val DIFF = object : DiffUtil.ItemCallback<AppItem>() {

    override fun areItemsTheSame(oldItem: AppItem, newItem: AppItem) =
        oldItem.packageName == newItem.packageName

    override fun areContentsTheSame(oldItem: AppItem, newItem: AppItem) = oldItem == newItem

}
