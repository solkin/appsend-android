package com.tomclaw.appsend_rb.screen.permissions.adapter.safe

import android.view.View
import android.widget.TextView
import com.avito.konveyor.adapter.BaseViewHolder
import com.avito.konveyor.blueprint.ItemView
import com.tomclaw.appsend_rb.R
import com.tomclaw.appsend_rb.util.bind

interface SafePermissionItemView : ItemView {

    fun setDescription(value: String?)

    fun setPermission(value: String)

}

class SafePermissionItemViewHolder(view: View) : BaseViewHolder(view), SafePermissionItemView {

    private val description: TextView = view.findViewById(R.id.description)
    private val permission: TextView = view.findViewById(R.id.permission)

    override fun setDescription(value: String?) {
        description.bind(value)
    }

    override fun setPermission(value: String) {
        permission.bind(value)
    }

}
