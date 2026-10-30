package com.tomclaw.appsend_rb.screen.main

import androidx.core.view.isVisible
import com.jakewharton.rxrelay3.PublishRelay
import com.tomclaw.appsend_rb.R
import com.tomclaw.appsend_rb.databinding.ActivityMainBinding
import io.reactivex.rxjava3.core.Observable

interface MainView {

    fun selectTab(tab: MainTab)

    fun setExportsBadge(count: Int)

    fun tabSelections(): Observable<MainTab>

}

class MainViewImpl(binding: ActivityMainBinding) : MainView, BottomBarHost {

    private val navigation = binding.navigation
    private val tabRelay = PublishRelay.create<MainTab>()
    private val barHiders = HashSet<String>()

    init {
        navigation.setOnItemSelectedListener { item ->
            tabRelay.accept(
                when (item.itemId) {
                    R.id.tab_exports -> MainTab.EXPORTS
                    R.id.tab_settings -> MainTab.SETTINGS
                    else -> MainTab.APPS
                }
            )
            true
        }
    }

    override fun selectTab(tab: MainTab) {
        navigation.selectedItemId = when (tab) {
            MainTab.APPS -> R.id.tab_apps
            MainTab.EXPORTS -> R.id.tab_exports
            MainTab.SETTINGS -> R.id.tab_settings
        }
    }

    override fun setExportsBadge(count: Int) {
        if (count > 0) {
            navigation.getOrCreateBadge(R.id.tab_exports).number = count
        } else {
            navigation.removeBadge(R.id.tab_exports)
        }
    }

    override fun setBottomBarHidden(owner: String, hidden: Boolean) {
        if (hidden) barHiders += owner else barHiders -= owner
        navigation.isVisible = barHiders.isEmpty()
    }

    override fun tabSelections(): Observable<MainTab> = tabRelay

}
