package com.tomclaw.appsend_rb.screen.apps

import android.view.Gravity
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.search.SearchView
import com.google.android.material.snackbar.Snackbar
import com.jakewharton.rxrelay3.PublishRelay
import com.tomclaw.appsend_rb.R
import com.tomclaw.appsend_rb.databinding.FragmentAppsBinding
import com.tomclaw.appsend_rb.domain.SortOrder
import com.tomclaw.appsend_rb.screen.main.BottomBarHost
import com.tomclaw.appsend_rb.util.applySystemBarInsets
import io.reactivex.rxjava3.core.Observable

interface AppsView {

    fun render(model: AppsViewModel)

    fun closeSearch()

    fun showNothingSelected()

    fun showBackupWarning()

    fun refreshRequests(): Observable<Unit>

    fun itemClicks(): Observable<AppItem>

    fun itemLongClicks(): Observable<AppItem>

    fun sortChanges(): Observable<SortOrder>

    fun systemFilterChanges(): Observable<Boolean>

    fun splitFilterChanges(): Observable<Boolean>

    fun runnableFilterChanges(): Observable<Boolean>

    fun queryChanges(): Observable<String>

    fun selectClicks(): Observable<Unit>

    fun cancelSelectionClicks(): Observable<Unit>

    fun selectAllClicks(): Observable<Unit>

    fun shareClicks(): Observable<Unit>

    fun extractClicks(): Observable<Unit>

    fun removeClicks(): Observable<Unit>

    fun backupWarningConfirmations(): Observable<Unit>

}

class AppsViewImpl(
    private val binding: FragmentAppsBinding,
    lifecycleOwner: LifecycleOwner,
    backDispatcher: OnBackPressedDispatcher,
    private val bottomBar: BottomBarHost?,
) : AppsView {

    private val context = binding.root.context

    private val refreshRelay = PublishRelay.create<Unit>()
    private val itemClickRelay = PublishRelay.create<AppItem>()
    private val itemLongClickRelay = PublishRelay.create<AppItem>()
    private val sortRelay = PublishRelay.create<SortOrder>()
    private val systemFilterRelay = PublishRelay.create<Boolean>()
    private val splitFilterRelay = PublishRelay.create<Boolean>()
    private val runnableFilterRelay = PublishRelay.create<Boolean>()
    private val queryRelay = PublishRelay.create<String>()
    private val selectRelay = PublishRelay.create<Unit>()
    private val cancelSelectionRelay = PublishRelay.create<Unit>()
    private val selectAllRelay = PublishRelay.create<Unit>()
    private val shareRelay = PublishRelay.create<Unit>()
    private val extractRelay = PublishRelay.create<Unit>()
    private val removeRelay = PublishRelay.create<Unit>()
    private val backupWarningRelay = PublishRelay.create<Unit>()

    private val adapter = AppsAdapter(itemClickRelay::accept, itemLongClickRelay::accept)
    private val searchAdapter = AppsAdapter(
        onClick = { item ->
            binding.searchView.hide()
            itemClickRelay.accept(item)
        },
        onLongClick = {},
    )

    private val selectionBack = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() = cancelSelectionRelay.accept(Unit)
    }
    private val searchBack = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() = binding.searchView.hide()
    }

    private var bindingFilters = false
    private var searching = false
    private var selecting = false

    init {
        backDispatcher.addCallback(lifecycleOwner, selectionBack)
        backDispatcher.addCallback(lifecycleOwner, searchBack)
        binding.appBar.applySystemBarInsets(top = true)
        binding.searchView.applySystemBarInsets(bottom = true)
        binding.list.layoutManager = LinearLayoutManager(context)
        binding.list.adapter = adapter
        binding.searchResults.layoutManager = LinearLayoutManager(context)
        binding.searchResults.adapter = searchAdapter
        binding.refresh.setOnRefreshListener { refreshRelay.accept(Unit) }

        binding.searchView.setupWithSearchBar(binding.searchBar)
        binding.searchView.editText.doAfterTextChanged { queryRelay.accept(it?.toString().orEmpty()) }
        binding.searchView.addTransitionListener { _, _, state ->
            searchBack.isEnabled = state == SearchView.TransitionState.SHOWN
            searching = state != SearchView.TransitionState.HIDDEN
            updateBottomBar()
            if (state == SearchView.TransitionState.HIDDEN) binding.searchView.editText.text = null
        }
        binding.searchBar.inflateMenu(R.menu.apps_search)
        binding.searchBar.setOnMenuItemClickListener {
            selectRelay.accept(Unit)
            true
        }

        binding.sortChip.setOnClickListener { showSortMenu() }
        binding.sortChip.setOnCloseIconClickListener { showSortMenu() }
        binding.systemChip.setOnCheckedChangeListener { _, checked ->
            if (!bindingFilters) systemFilterRelay.accept(checked)
        }
        binding.splitChip.setOnCheckedChangeListener { _, checked ->
            if (!bindingFilters) splitFilterRelay.accept(checked)
        }
        binding.runnableChip.setOnCheckedChangeListener { _, checked ->
            if (!bindingFilters) runnableFilterRelay.accept(checked)
        }

        binding.selectionBar.setNavigationOnClickListener { cancelSelectionRelay.accept(Unit) }
        binding.selectionBar.setOnMenuItemClickListener {
            selectAllRelay.accept(Unit)
            true
        }
        binding.shareButton.setOnClickListener { shareRelay.accept(Unit) }
        binding.removeButton.setOnClickListener { removeRelay.accept(Unit) }
        binding.extractButton.setOnClickListener { extractRelay.accept(Unit) }
    }

    override fun render(model: AppsViewModel) {
        adapter.submitList(model.items)
        searchAdapter.submitList(model.searchResults)

        binding.loading.isVisible = model.loading
        binding.message.isVisible = model.failed || model.empty
        binding.message.setText(if (model.failed) R.string.apps_loading_failed else R.string.apps_empty)
        binding.refresh.isRefreshing = model.refreshing

        binding.searchBar.hint = if (model.total > 0) {
            context.getString(R.string.search_apps_hint, model.total)
        } else {
            context.getString(R.string.menu_search)
        }
        bindingFilters = true
        binding.sortChip.text = context.getString(sortLabel(model.filter.sort))
        binding.systemChip.isChecked = model.filter.showSystem
        binding.splitChip.isChecked = model.filter.splitOnly
        binding.runnableChip.isChecked = model.filter.runnableOnly
        bindingFilters = false

        selecting = model.selectionMode
        updateBottomBar()
        selectionBack.isEnabled = selecting
        binding.searchContainer.isVisible = !selecting
        binding.chipsScroll.isVisible = !selecting
        binding.selectionBar.isVisible = selecting
        binding.floatingToolbar.isVisible = selecting
        binding.refresh.isEnabled = !selecting
        binding.selectionBar.title = context.getString(R.string.selected_apps_count, model.selectedCount)
        binding.selectionBar.subtitle = model.selectedSize
    }

    private fun updateBottomBar() {
        bottomBar?.setBottomBarHidden(BAR_OWNER, selecting || searching)
    }

    private fun showSortMenu() {
        val menu = PopupMenu(context, binding.sortChip, Gravity.START)
        SortOrder.entries.forEach { order ->
            menu.menu.add(sortLabel(order)).setOnMenuItemClickListener {
                sortRelay.accept(order)
                true
            }
        }
        menu.show()
    }

    private fun sortLabel(order: SortOrder): Int = when (order) {
        SortOrder.NAME -> R.string.sort_name
        SortOrder.SIZE -> R.string.sort_size
        SortOrder.UPDATED -> R.string.sort_updated
        SortOrder.INSTALLED -> R.string.sort_installed
    }

    override fun closeSearch() {
        binding.searchView.hide()
    }

    override fun showNothingSelected() {
        Snackbar.make(binding.root, R.string.no_apps_selected, Snackbar.LENGTH_SHORT)
            .setAnchorView(binding.floatingToolbar)
            .show()
    }

    override fun showBackupWarning() {
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.backup_limitations_warning_title)
            .setMessage(R.string.backup_limitations_warning_message)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.continue_anyway) { _, _ -> backupWarningRelay.accept(Unit) }
            .show()
    }

    override fun refreshRequests(): Observable<Unit> = refreshRelay

    override fun itemClicks(): Observable<AppItem> = itemClickRelay

    override fun itemLongClicks(): Observable<AppItem> = itemLongClickRelay

    override fun sortChanges(): Observable<SortOrder> = sortRelay

    override fun systemFilterChanges(): Observable<Boolean> = systemFilterRelay

    override fun splitFilterChanges(): Observable<Boolean> = splitFilterRelay

    override fun runnableFilterChanges(): Observable<Boolean> = runnableFilterRelay

    override fun queryChanges(): Observable<String> = queryRelay
    override fun selectClicks(): Observable<Unit> = selectRelay

    override fun cancelSelectionClicks(): Observable<Unit> = cancelSelectionRelay

    override fun selectAllClicks(): Observable<Unit> = selectAllRelay

    override fun shareClicks(): Observable<Unit> = shareRelay

    override fun extractClicks(): Observable<Unit> = extractRelay

    override fun removeClicks(): Observable<Unit> = removeRelay

    override fun backupWarningConfirmations(): Observable<Unit> = backupWarningRelay
}

private const val BAR_OWNER = "apps"
