package com.tomclaw.appsend_rb.screen.apps.adapter

import com.tomclaw.appsend_rb.screen.apps.adapter.app.AppItem

interface ItemClickListener {

    fun onItemClick(item: AppItem)

    fun onItemLongClick(item: AppItem)

}
