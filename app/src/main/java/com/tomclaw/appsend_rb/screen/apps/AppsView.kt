package com.tomclaw.appsend_rb.screen.apps

import android.os.Build
import android.text.Html
import android.text.Html.FROM_HTML_MODE_COMPACT
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.avito.konveyor.adapter.SimpleRecyclerAdapter
import com.github.rubensousa.bottomsheetbuilder.BottomSheetBuilder
import com.google.android.material.snackbar.Snackbar
import com.jakewharton.rxrelay2.PublishRelay
import com.tomclaw.appsend_rb.R
import com.tomclaw.appsend_rb.screen.apps.adapter.app.AppItem
import com.tomclaw.appsend_rb.util.ColorHelper.getAttributedColor
import com.tomclaw.appsend_rb.util.hideWithAlphaAnimation
import com.tomclaw.appsend_rb.util.showWithAlphaAnimation
import io.reactivex.Observable

interface AppsView {

    fun showProgress()

    fun showContent()

    fun contentUpdated()

    fun refreshClicks(): Observable<Unit>

    fun prefsClicks(): Observable<Unit>

    fun infoClicks(): Observable<Unit>

    fun appMenuClicks(): Observable<Pair<Int, AppItem>>

    fun searchTextChanged(): Observable<String>

    fun searchCloseChanged(): Observable<Unit>

    fun showAppMenu(item: AppItem)

    fun showExtractSuccess(path: String)

    fun showAppLaunchError()

    fun showAppExportError(msg: String)

    fun showNoRequestedPermissionsMessage()

    fun showUnableToGetPermissionsError()

    fun showWritePermissionsRequiredError()

}

class AppsViewImpl(
    private val view: View,
    private val adapter: SimpleRecyclerAdapter,
    private val preferences: PreferencesProvider
) : AppsView {

    private val context = view.context
    private val toolbar: Toolbar = view.findViewById(R.id.toolbar)
    private val recycler: RecyclerView = view.findViewById(R.id.recycler)
    private val overlayProgress: View = view.findViewById(R.id.overlay_progress)

    private val refreshRelay = PublishRelay.create<Unit>()
    private val prefsRelay = PublishRelay.create<Unit>()
    private val infoRelay = PublishRelay.create<Unit>()
    private val appMenuRelay = PublishRelay.create<Pair<Int, AppItem>>()
    private val searchTextRelay = PublishRelay.create<String>()
    private val searchCloseRelay = PublishRelay.create<Unit>()

    init {
        toolbar.setTitle(R.string.app_name)
        toolbar.inflateMenu(R.menu.main_menu)
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.refresh -> refreshRelay.accept(Unit)
                R.id.settings -> prefsRelay.accept(Unit)
                R.id.info -> infoRelay.accept(Unit)
            }
            true
        }
        val searchMenu: MenuItem = toolbar.menu.findItem(R.id.menu_search)
        val searchView = searchMenu.actionView as SearchView
        searchView.queryHint = searchMenu.title
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                return true
            }
            override fun onQueryTextChange(newText: String): Boolean {
                searchTextRelay.accept(newText)
                return true
            }
        })
        searchView.setOnCloseListener { searchCloseRelay.accept(Unit); false }
        searchView.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {}
            override fun onViewDetachedFromWindow(v: View) {
                searchCloseRelay.accept(Unit)
            }
        })
        adapter.setHasStableIds(true)
        recycler.adapter = adapter
        recycler.layoutManager = LinearLayoutManager(
            context,
            RecyclerView.VERTICAL,
            false
        )
        recycler.itemAnimator = DefaultItemAnimator()
    }

    override fun showProgress() {
        overlayProgress.showWithAlphaAnimation(animateFully = true)
    }

    override fun showContent() {
        overlayProgress.hideWithAlphaAnimation(animateFully = false)
    }

    override fun contentUpdated() {
        adapter.notifyDataSetChanged()
    }

    override fun refreshClicks(): Observable<Unit> = refreshRelay

    override fun prefsClicks(): Observable<Unit> = prefsRelay

    override fun infoClicks(): Observable<Unit> = infoRelay

    override fun appMenuClicks(): Observable<Pair<Int, AppItem>> = appMenuRelay

    override fun searchTextChanged(): Observable<String> = searchTextRelay

    override fun searchCloseChanged(): Observable<Unit> = searchCloseRelay

    override fun showAppMenu(item: AppItem) {
        val theme = R.style.AppTheme_BottomSheetDialog_Dark.takeIf { preferences.isDarkTheme() }
            ?: R.style.AppTheme_BottomSheetDialog_Light
        BottomSheetBuilder(view.context, theme)
            .setMode(BottomSheetBuilder.MODE_LIST)
            .setIconTintColor(getAttributedColor(context, R.attr.menu_icons_tint))
            .setItemTextColor(getAttributedColor(context, R.attr.text_primary_color))
            .apply {
                addItem(ACTION_RUN_APP, R.string.run_app, R.drawable.run)
                addItem(ACTION_FIND_IN_GP, R.string.find_on_gp, R.drawable.google_play)
                addItem(ACTION_SHARE_APP, R.string.share_apk, R.drawable.share)
                addItem(ACTION_EXTRACT_APP, R.string.extract_apk, R.drawable.floppy)
                addItem(
                    ACTION_SHOW_PERMISSIONS,
                    R.string.required_permissions,
                    R.drawable.lock_open
                )
                addItem(ACTION_SHOW_DETAILS, R.string.app_details, R.drawable.settings_box)
                addItem(ACTION_REMOVE_APP, R.string.remove, R.drawable.delete)
            }
            .setItemClickListener {
                appMenuRelay.accept(Pair(it.itemId, item))
            }
            .createDialog()
            .show()
    }

    override fun showExtractSuccess(path: String) {
        val text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(
                context.getString(R.string.app_extract_success, path),
                FROM_HTML_MODE_COMPACT
            )
        } else {
            @Suppress("DEPRECATION")
            Html.fromHtml(context.getString(R.string.app_extract_success, path))
        }
        Snackbar.make(recycler, text, Snackbar.LENGTH_LONG).show()
    }

    override fun showAppLaunchError() {
        Snackbar.make(recycler, R.string.non_launchable_package, Snackbar.LENGTH_LONG).show()
    }

    override fun showAppExportError(msg: String) {
        Snackbar.make(recycler, msg, Snackbar.LENGTH_LONG).show()
    }

    override fun showNoRequestedPermissionsMessage() {
        Snackbar.make(recycler, R.string.no_requested_permissions, Snackbar.LENGTH_LONG).show()
    }

    override fun showUnableToGetPermissionsError() {
        Snackbar.make(recycler, R.string.unable_to_get_permissions, Snackbar.LENGTH_LONG).show()
    }

    override fun showWritePermissionsRequiredError() {
        Snackbar.make(recycler, R.string.write_permission_extract, Snackbar.LENGTH_LONG).show()
    }

}

const val ACTION_RUN_APP = 0
const val ACTION_FIND_IN_GP = 1
const val ACTION_SHARE_APP = 2
const val ACTION_EXTRACT_APP = 3
const val ACTION_SHOW_PERMISSIONS = 4
const val ACTION_SHOW_DETAILS = 5
const val ACTION_REMOVE_APP = 6
