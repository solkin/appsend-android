package com.tomclaw.appsend_rb.screen.apps

import androidx.core.text.HtmlCompat
import com.jakewharton.rxrelay3.PublishRelay
import com.tomclaw.appsend_rb.R
import com.tomclaw.appsend_rb.screen.apps.adapter.app.AppItem
import com.tomclaw.appsend_rb.util.AppIcons
import com.tomclaw.appsend_rb.util.Glyphs
import com.tomclaw.knopochki.core.Alignment
import com.tomclaw.knopochki.core.Box
import com.tomclaw.knopochki.core.Column
import com.tomclaw.knopochki.core.Frame
import com.tomclaw.knopochki.core.Insets
import com.tomclaw.knopochki.core.KnopochkiView
import com.tomclaw.knopochki.core.LazyColumn
import com.tomclaw.knopochki.core.LazyList
import com.tomclaw.knopochki.core.ListAdapter
import com.tomclaw.knopochki.core.Node
import com.tomclaw.knopochki.core.Orientation
import com.tomclaw.knopochki.core.Popup
import com.tomclaw.knopochki.core.Row
import com.tomclaw.knopochki.core.Scroll
import com.tomclaw.knopochki.draw.Icons
import com.tomclaw.knopochki.draw.Shape
import com.tomclaw.knopochki.theme.Role
import com.tomclaw.knopochki.widget.BottomSheet
import com.tomclaw.knopochki.widget.Chip
import com.tomclaw.knopochki.widget.Dialog
import com.tomclaw.knopochki.widget.Divider
import com.tomclaw.knopochki.widget.Icon
import com.tomclaw.knopochki.widget.IconButton
import com.tomclaw.knopochki.widget.Image
import com.tomclaw.knopochki.widget.ImageScale
import com.tomclaw.knopochki.widget.ListItem
import com.tomclaw.knopochki.widget.Menu
import com.tomclaw.knopochki.widget.Progress
import com.tomclaw.knopochki.widget.PullRefresh
import com.tomclaw.knopochki.widget.ProgressStyle
import com.tomclaw.knopochki.widget.SearchBar
import com.tomclaw.knopochki.widget.Snackbar
import com.tomclaw.knopochki.widget.Text
import com.tomclaw.knopochki.widget.TextStyle
import com.tomclaw.knopochki.widget.TopAppBar
import io.reactivex.rxjava3.core.Observable
import java.util.IdentityHashMap

interface AppsView {

    fun showProgress()

    fun showContent()

    fun setApps(items: List<AppItem>)

    fun refreshClicks(): Observable<Unit>

    fun prefsClicks(): Observable<Unit>

    fun infoClicks(): Observable<Unit>

    fun appMenuClicks(): Observable<Pair<Int, AppItem>>

    fun appItemClicks(): Observable<AppItem>

    fun appItemLongClicks(): Observable<AppItem>

    fun selectionClicks(): Observable<Unit>

    fun batchShareClicks(): Observable<Unit>

    fun batchExtractClicks(): Observable<Unit>

    fun cancelSelectionClicks(): Observable<Unit>

    fun searchTextChanged(): Observable<String>

    fun searchCloseChanged(): Observable<Unit>

    fun sortSelectedClicks(): Observable<Int>

    fun systemToggleClicks(): Observable<Boolean>

    fun setFilters(sortOrder: Int, showSystem: Boolean)

    fun showAppMenu(item: AppItem)

    fun showExtractSuccess()

    fun showAppLaunchError()

    fun showAppExportError()

    fun showAppsLoadingError()

    fun showNoRequestedPermissionsMessage()

    fun showUnableToGetPermissionsError()

    /**
     * [canOpenSettings] is true when the permission was refused for good ("don't ask
     * again"), so the only way forward is the system settings screen.
     */
    fun showWritePermissionsRequiredError(canOpenSettings: Boolean)

    fun appSettingsClicks(): Observable<Unit>

    fun showSelectionMode(enabled: Boolean, selectedCount: Int)

    fun showNoAppsSelectedMessage()

    fun showBatchExtractSuccess(count: Int)

    fun showBackupLimitationsWarning(onConfirmed: () -> Unit)

    fun onBackPressed(): Boolean

}

class AppsViewImpl(private val knopochkiView: KnopochkiView) : AppsView {

    private val context = knopochkiView.context
    private val resources = context.resources
    private val iconSizePx = (ICON_DP * resources.displayMetrics.density).toInt()
    private val badgeNew = resources.getString(R.string.badge_new).uppercase()

    private val refreshRelay = PublishRelay.create<Unit>()
    private val prefsRelay = PublishRelay.create<Unit>()
    private val infoRelay = PublishRelay.create<Unit>()
    private val appMenuRelay = PublishRelay.create<Pair<Int, AppItem>>()
    private val appItemRelay = PublishRelay.create<AppItem>()
    private val appItemLongRelay = PublishRelay.create<AppItem>()
    private val selectionRelay = PublishRelay.create<Unit>()
    private val batchShareRelay = PublishRelay.create<Unit>()
    private val batchExtractRelay = PublishRelay.create<Unit>()
    private val cancelSelectionRelay = PublishRelay.create<Unit>()
    private val searchTextRelay = PublishRelay.create<String>()
    private val searchCloseRelay = PublishRelay.create<Unit>()
    private val sortRelay = PublishRelay.create<Int>()
    private val systemRelay = PublishRelay.create<Boolean>()
    private val appSettingsRelay = PublishRelay.create<Unit>()

    private val items = ArrayList<AppItem>()
    private val holders = IdentityHashMap<Node, RowHolder>()

    private var selectionMode = false
    private var selectedCount = 0
    private var searchActive = false
    private var loading = false
    private var bindingFilters = false

    // --- persistent nodes ---

    private val adapter = object : ListAdapter {
        override val count get() = items.size
        override fun rowType(index: Int): Int =
            if (items[index].selectable) TYPE_SELECTABLE else TYPE_NORMAL

        override fun itemId(index: Int): Long = items[index].id
        override fun createRow(type: Int): Node = createAppRow(type)
        override fun bindRow(row: Node, index: Int) = bindAppRow(row, index)
    }
    private val list: LazyList = LazyColumn(adapter)

    /**
     * Wraps the list so a pull at the top starts a refresh. The toolbar action
     * stays as well — the gesture is a shortcut, not the only way.
     */
    private val pullRefresh = PullRefresh(list) { refreshRelay.accept(Unit) }

    private val normalBar = TopAppBar(title = resources.getString(R.string.app_name)).apply {
        action(Icons.SEARCH, resources.getString(R.string.menu_search)) { openSearch() }
        action(Icons.REFRESH, resources.getString(R.string.refresh)) { refreshRelay.accept(Unit) }
        action(Icons.MORE_VERT, resources.getString(R.string.info)) { showOverflow(this) }
    }
    private val selectionBar = TopAppBar(
        title = "",
        navigationIcon = Icons.CLOSE,
        navigationLabel = resources.getString(R.string.cancel_selection),
        onNavigationClick = { cancelSelectionRelay.accept(Unit) },
    ).apply {
        action(Glyphs.SHARE, resources.getString(R.string.share_selected_apks)) {
            batchShareRelay.accept(Unit)
        }
        action(Glyphs.EXTRACT, resources.getString(R.string.extract_selected_apks)) {
            batchExtractRelay.accept(Unit)
        }
    }

    private val searchBar = SearchBar(
        hint = resources.getString(R.string.menu_search),
        onQueryChange = { searchTextRelay.accept(it) },
    )
    // TopAppBar pads itself below the status bar, a bare row does not — without these
    // insets the search field slides up under the status bar when search opens.
    private val searchRow: Node = Insets(
        Row(spacingDp = 4f, paddingDp = 4f)
            .add(
                IconButton(Icons.ARROW_BACK) { closeSearch() }
                    .also { it.accessibilityLabel = resources.getString(R.string.cancel) },
                align = Alignment.CENTER,
            )
            .addFilling(searchBar),
        applyTop = true,
        applyBottom = false,
        applySides = true,
    )

    private val systemChip = Chip(resources.getString(R.string.filter_system)) { checked ->
        if (!bindingFilters) systemRelay.accept(checked)
    }
    private val sortChips = linkedMapOf(
        NAME_ASCENDING to Chip(resources.getString(R.string.sort_name)) { c -> onSortChip(NAME_ASCENDING, c) },
        APP_SIZE to Chip(resources.getString(R.string.sort_size)) { c -> onSortChip(APP_SIZE, c) },
        UPDATE_TIME to Chip(resources.getString(R.string.sort_updated)) { c -> onSortChip(UPDATE_TIME, c) },
        INSTALL_TIME to Chip(resources.getString(R.string.sort_installed)) { c -> onSortChip(INSTALL_TIME, c) },
    )
    // A horizontal Scroll fills its cross axis when measured with AT_MOST, so it
    // must be pinned to the chip-row height (Chip is a fixed 32dp) — otherwise the
    // header would eat the whole screen and squeeze the list to zero height.
    private val chipsScroll: Node = Frame(
        Scroll(
            Row(spacingDp = 8f, paddingDp = 10f).apply {
                add(systemChip)
                sortChips.values.forEach { add(it) }
            },
            orientation = Orientation.HORIZONTAL,
        ),
        heightDp = 52f,
    )

    private val progressLine = Progress(ProgressStyle.LINEAR, indeterminate = true)
        .also { it.accessibilityLabel = resources.getString(R.string.please_wait) }

    private val headerColumn = Column()

    private val sheetContent = Column()
    private val bottomSheet = BottomSheet(sheetContent)
    private val backupDialog = Dialog(
        title = resources.getString(R.string.backup_limitations_warning_title),
        message = resources.getString(R.string.backup_limitations_warning_message),
    )
        .addAction(resources.getString(R.string.cancel))
        .addAction(resources.getString(R.string.continue_anyway)) { pendingBackupConfirm?.invoke() }
    private var pendingBackupConfirm: (() -> Unit)? = null

    private val popup = Popup()
    private val snackbar = Snackbar()

    init {
        list.onScroll = { offset ->
            normalBar.onContentScrolled(offset)
            selectionBar.onContentScrolled(offset)
        }
        val content = Column()
            .add(headerColumn)
            .addFilling(Box().add(Insets(pullRefresh, applyBottom = true, applySides = true)))
        knopochkiView.root = Box()
            .add(content)
            .add(popup)
            .add(backupDialog)
            .add(bottomSheet)
            .add(Insets(snackbar, applyBottom = true, applySides = true))
        buildHeader()
    }

    // ------------------------------------------------------------------ header

    private fun buildHeader() {
        headerColumn.clear()
        when {
            selectionMode -> headerColumn.add(selectionBar)
            searchActive -> {
                headerColumn.add(searchRow)
                headerColumn.add(chipsScroll)
            }
            else -> {
                headerColumn.add(normalBar)
                headerColumn.add(chipsScroll)
            }
        }
        // While the pull indicator is on screen it already says "loading" —
        // a second bar under the chips would say it twice.
        if (loading && !pullRefresh.refreshing) headerColumn.add(progressLine)
        relayout()
    }

    private fun relayout() {
        knopochkiView.requestLayout()
        knopochkiView.invalidate()
    }

    private fun openSearch() {
        if (searchActive) return
        searchActive = true
        buildHeader()
        knopochkiView.post { knopochkiView.ui.requestEdit(searchBar) }
    }

    private fun closeSearch() {
        if (!searchActive) return
        searchActive = false
        searchBar.query = ""
        knopochkiView.ui.requestEdit(null)
        buildHeader()
        searchCloseRelay.accept(Unit)
    }

    private fun showOverflow(bar: TopAppBar) {
        val anchor = bar.lastAction ?: return
        Menu(popup)
            .item(resources.getString(R.string.select_apps), Glyphs.SELECT) { selectionRelay.accept(Unit) }
            .item(resources.getString(R.string.settings), Icons.SETTINGS) { prefsRelay.accept(Unit) }
            .item(resources.getString(R.string.info), Glyphs.INFO) { infoRelay.accept(Unit) }
            .showAt(anchor)
    }

    private fun onSortChip(order: Int, checked: Boolean) {
        if (bindingFilters) return
        if (!checked) {
            // Filter chips are single-select with one always active: re-check.
            bindingFilters = true
            sortChips[order]?.checked = true
            bindingFilters = false
            return
        }
        bindingFilters = true
        sortChips.forEach { (key, chip) -> if (key != order) chip.checked = false }
        bindingFilters = false
        sortRelay.accept(order)
    }

    // ---------------------------------------------------------------- list rows

    private class RowHolder(val image: Image, val badge: Text?, val selection: Icon?) {
        var packageName: String = ""
    }

    /**
     * Two row types, because `ListItem`'s trailing slot is fixed at construction:
     * a "NEW" badge normally, a selection mark in selection mode. Hiding one of them
     * by shrinking it is not an option — a zero-sized `Icon` collapses in layout but
     * still paints its previous path, which spills outside the row.
     */
    private fun createAppRow(type: Int): Node {
        val image = Image(scale = ImageScale.FIT)
        val leading = Frame(image, widthDp = ICON_DP, heightDp = ICON_DP)
        val badge: Text?
        val selection: Icon?
        val trailing: Node?
        if (type == TYPE_SELECTABLE) {
            selection = Icon(Glyphs.CIRCLE, sizeDp = SELECTION_DP, role = Role.ON_SURFACE_VARIANT)
            trailing = selection
            badge = null
        } else {
            badge = Text("", style = TextStyle.LABEL_SMALL, role = Role.PRIMARY)
            trailing = badge
            selection = null
        }
        val row = ListItem(headline = "", supporting = null, leading = leading, trailing = trailing)
        holders[row] = RowHolder(image, badge, selection)
        return row
    }

    private fun bindAppRow(row: Node, index: Int) {
        val item = items[index]
        val listItem = row as ListItem
        val holder = holders[row] ?: return
        holder.packageName = item.packageName
        listItem.headline = item.name
        listItem.supporting = metaLine(item)
        listItem.onClick = { appItemRelay.accept(item) }
        listItem.onLongClick = { appItemLongRelay.accept(item) }
        holder.image.bitmap = AppIcons.cached(item.packageName)
        AppIcons.load(context, item.packageName, iconSizePx) { pkg, bitmap ->
            if (holder.packageName == pkg) holder.image.bitmap = bitmap
        }
        holder.badge?.text = if (item.newApp) badgeNew else ""
        holder.selection?.let {
            it.icon = if (item.selected) Glyphs.CHECK_CIRCLE else Glyphs.CIRCLE
            it.role = if (item.selected) Role.PRIMARY else Role.ON_SURFACE_VARIANT
        }
    }

    private fun metaLine(item: AppItem): String =
        listOf(item.versionName, item.size, item.lastUpdateTime)
            .filter { it.isNotBlank() }
            .joinToString(SEPARATOR)

    // ------------------------------------------------------------- AppsView API

    override fun showProgress() {
        if (loading) return
        loading = true
        buildHeader()
    }

    override fun showContent() {
        pullRefresh.refreshing = false
        if (!loading) return
        loading = false
        buildHeader()
    }

    override fun setApps(items: List<AppItem>) {
        // Toggling one selection re-emits the whole list. A full notifyDataChanged
        // resets the scroll offset and snaps the anchor row to the top of the
        // viewport, so while the rows still line up by id we patch only the ones
        // that actually changed — that keeps the list exactly where the user left it.
        // `selectable` is part of the test because it decides the row type, and a
        // recycled row of the old type cannot be re-bound into the new one.
        val patchable = items.size == this.items.size &&
            items.indices.all {
                this.items[it].id == items[it].id &&
                    this.items[it].selectable == items[it].selectable
            }
        val changed = if (patchable) items.indices.filter { this.items[it] != items[it] } else null
        this.items.clear()
        this.items.addAll(items)
        if (changed != null) {
            changed.forEach { list.notifyItemChanged(it) }
        } else {
            list.notifyDataChanged()
        }
    }

    override fun refreshClicks(): Observable<Unit> = refreshRelay

    override fun prefsClicks(): Observable<Unit> = prefsRelay

    override fun infoClicks(): Observable<Unit> = infoRelay

    override fun appMenuClicks(): Observable<Pair<Int, AppItem>> = appMenuRelay

    override fun appItemClicks(): Observable<AppItem> = appItemRelay

    override fun appItemLongClicks(): Observable<AppItem> = appItemLongRelay

    override fun selectionClicks(): Observable<Unit> = selectionRelay

    override fun batchShareClicks(): Observable<Unit> = batchShareRelay

    override fun batchExtractClicks(): Observable<Unit> = batchExtractRelay

    override fun cancelSelectionClicks(): Observable<Unit> = cancelSelectionRelay

    override fun searchTextChanged(): Observable<String> = searchTextRelay

    override fun searchCloseChanged(): Observable<Unit> = searchCloseRelay

    override fun sortSelectedClicks(): Observable<Int> = sortRelay

    override fun systemToggleClicks(): Observable<Boolean> = systemRelay

    override fun setFilters(sortOrder: Int, showSystem: Boolean) {
        bindingFilters = true
        systemChip.checked = showSystem
        val active = when (sortOrder) {
            APP_SIZE -> APP_SIZE
            UPDATE_TIME -> UPDATE_TIME
            INSTALL_TIME -> INSTALL_TIME
            else -> NAME_ASCENDING
        }
        sortChips.forEach { (key, chip) -> chip.checked = key == active }
        bindingFilters = false
    }

    override fun showAppMenu(item: AppItem) {
        sheetContent.clear()
        sheetContent.add(
            Column(spacingDp = 2f, paddingDp = 16f)
                .add(Text(item.name, style = TextStyle.TITLE_MEDIUM, role = Role.ON_SURFACE))
                .add(
                    Text(
                        item.packageName,
                        style = TextStyle.BODY_SMALL,
                        role = Role.ON_SURFACE_VARIANT,
                    )
                )
        )
        sheetContent.add(Divider())
        sheetContent.add(actionRow(Glyphs.RUN, R.string.run_app, ACTION_RUN_APP, item))
        sheetContent.add(actionRow(Glyphs.GOOGLE_PLAY, R.string.find_on_gp, ACTION_FIND_IN_GP, item))
        sheetContent.add(actionRow(Glyphs.SHARE, R.string.share_apk, ACTION_SHARE_APP, item))
        sheetContent.add(actionRow(Glyphs.EXTRACT, R.string.extract_apk, ACTION_EXTRACT_APP, item))
        sheetContent.add(
            actionRow(Glyphs.PERMISSIONS, R.string.required_permissions, ACTION_SHOW_PERMISSIONS, item)
        )
        sheetContent.add(actionRow(Glyphs.DETAILS, R.string.app_details, ACTION_SHOW_DETAILS, item))
        sheetContent.add(
            actionRow(Glyphs.DELETE, R.string.remove, ACTION_REMOVE_APP, item, role = Role.ERROR)
        )
        bottomSheet.show()
        relayout()
    }

    private fun actionRow(
        icon: com.tomclaw.knopochki.draw.Glyph,
        labelRes: Int,
        actionId: Int,
        item: AppItem,
        role: Int = Role.ON_SURFACE_VARIANT,
    ): Node = ListItem(
        headline = resources.getString(labelRes),
        leading = Icon(icon, sizeDp = 24f, role = role),
        onClick = {
            appMenuRelay.accept(actionId to item)
            bottomSheet.dismiss()
        },
    )

    override fun showExtractSuccess() = snack(R.string.app_extract_success)

    override fun showAppLaunchError() = snack(R.string.non_launchable_package)

    override fun showAppExportError() = snack(R.string.app_extract_failed)

    override fun showAppsLoadingError() = snack(R.string.apps_loading_failed)

    override fun showNoRequestedPermissionsMessage() = snack(R.string.no_requested_permissions)

    override fun showUnableToGetPermissionsError() = snack(R.string.unable_to_get_permissions)

    override fun showWritePermissionsRequiredError(canOpenSettings: Boolean) {
        val message = text(R.string.write_permission_extract)
        if (canOpenSettings) {
            snackbar.show(message, resources.getString(R.string.settings)) {
                appSettingsRelay.accept(Unit)
            }
        } else {
            snackbar.show(message)
        }
    }

    override fun appSettingsClicks(): Observable<Unit> = appSettingsRelay

    override fun showNoAppsSelectedMessage() = snack(R.string.no_apps_selected)

    override fun showBatchExtractSuccess(count: Int) {
        snackbar.show(resources.getQuantityString(R.plurals.apps_extract_success, count, count))
    }

    override fun showSelectionMode(enabled: Boolean, selectedCount: Int) {
        selectionMode = enabled
        this.selectedCount = selectedCount
        selectionBar.title = resources.getString(R.string.selected_apps_count, selectedCount)
        buildHeader()
    }

    override fun onBackPressed(): Boolean = when {
        popup.isShowing -> {
            popup.dismiss(); true
        }
        backupDialog.visible -> {
            backupDialog.visible = false; true
        }
        bottomSheet.visible -> {
            bottomSheet.dismiss(); true
        }
        searchActive -> {
            closeSearch(); true
        }
        else -> false
    }

    override fun showBackupLimitationsWarning(onConfirmed: () -> Unit) {
        pendingBackupConfirm = onConfirmed
        backupDialog.visible = true
    }

    private fun snack(res: Int) {
        snackbar.show(text(res))
    }

    /**
     * A few strings still carry `<b>` markup from the old HTML-rendering snackbar;
     * Knopochki draws plain text, so flatten it instead of printing the tags.
     */
    private fun text(res: Int): String =
        HtmlCompat.fromHtml(resources.getString(res), HtmlCompat.FROM_HTML_MODE_LEGACY).toString()

    private companion object {
        const val TYPE_NORMAL = 0
        const val TYPE_SELECTABLE = 1
        const val ICON_DP = 40f
        const val SELECTION_DP = 24f
        const val SEPARATOR = "  ·  "
    }
}

const val ACTION_RUN_APP = 0
const val ACTION_FIND_IN_GP = 1
const val ACTION_SHARE_APP = 2
const val ACTION_EXTRACT_APP = 3
const val ACTION_SHOW_PERMISSIONS = 4
const val ACTION_SHOW_DETAILS = 5
const val ACTION_REMOVE_APP = 6
