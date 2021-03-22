package com.tomclaw.appsend_rb.screen.apps.adapter.app

import com.avito.konveyor.blueprint.ItemPresenter
import com.tomclaw.appsend_rb.screen.apps.adapter.ItemClickListener

class AppItemPresenter(
    private val listener: ItemClickListener
) : ItemPresenter<AppItemView, AppItem> {

    override fun bindView(view: AppItemView, item: AppItem, position: Int) {
        view.setIcon(item.icon)
        view.setName(item.name)
        view.setVersion(item.versionName)
        view.setSize(item.size)
        view.setTime(item.lastUpdateTime)
        view.setOnClickListener { listener.onItemClick(item) }
    }

}