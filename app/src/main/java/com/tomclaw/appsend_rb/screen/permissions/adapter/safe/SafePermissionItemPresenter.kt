package com.tomclaw.appsend_rb.screen.permissions.adapter.safe

import com.avito.konveyor.blueprint.ItemPresenter
import com.tomclaw.appsend_rb.screen.permissions.PermissionsResourceProvider

class SafePermissionItemPresenter(
    private val resourceProvider: PermissionsResourceProvider
) : ItemPresenter<SafePermissionItemView, SafePermissionItem> {

    override fun bindView(view: SafePermissionItemView, item: SafePermissionItem, position: Int) {
        val description = item.description ?: resourceProvider.getUnknownPermissionString()
        view.setDescription(description)
        view.setPermission(item.permission)
    }

}
