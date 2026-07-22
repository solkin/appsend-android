package com.tomclaw.appsend_rb.screen.permissions

import com.jakewharton.rxrelay3.PublishRelay
import com.tomclaw.appsend_rb.R
import com.tomclaw.knopochki.core.Column
import com.tomclaw.knopochki.core.Insets
import com.tomclaw.knopochki.core.KnopochkiView
import com.tomclaw.knopochki.core.LazyColumn
import com.tomclaw.knopochki.core.LazyList
import com.tomclaw.knopochki.core.ListAdapter
import com.tomclaw.knopochki.core.Node
import com.tomclaw.knopochki.draw.Icons
import com.tomclaw.knopochki.theme.Role
import com.tomclaw.knopochki.widget.Text
import com.tomclaw.knopochki.widget.TextStyle
import com.tomclaw.knopochki.widget.TopAppBar
import io.reactivex.rxjava3.core.Observable
import java.util.IdentityHashMap

interface PermissionsView {

    fun setPermissions(items: List<PermissionModel>)

    fun navigationClicks(): Observable<Unit>

}

class PermissionsViewImpl(knopochkiView: KnopochkiView) : PermissionsView {

    private val resources = knopochkiView.context.resources
    private val unknown = resources.getString(R.string.unknown_permission_description)

    private val navigationRelay = PublishRelay.create<Unit>()

    private val items = ArrayList<PermissionModel>()
    private val holders = IdentityHashMap<Node, PermissionHolder>()

    private val adapter = object : ListAdapter {
        override val count get() = items.size
        override fun rowType(index: Int): Int =
            if (items[index].dangerous) TYPE_DANGEROUS else TYPE_SAFE

        override fun createRow(type: Int): Node = createPermissionRow(type)
        override fun bindRow(row: Node, index: Int) = bindPermissionRow(row, index)
    }
    private val list: LazyList = LazyColumn(adapter)

    init {
        val bar = TopAppBar(
            title = resources.getString(R.string.required_permissions),
            navigationIcon = Icons.ARROW_BACK,
            onNavigationClick = { navigationRelay.accept(Unit) },
        )
        list.onScroll = { bar.onContentScrolled(it) }
        knopochkiView.root = Column()
            .add(bar)
            .addFilling(Insets(list, applyBottom = true, applySides = true))
    }

    private class PermissionHolder(val description: Text, val permission: Text)

    private fun createPermissionRow(type: Int): Node {
        val description = Text("", style = TextStyle.BODY_LARGE, role = Role.ON_SURFACE)
        val permission = Text(
            "",
            style = TextStyle.BODY_SMALL,
            role = if (type == TYPE_DANGEROUS) Role.ERROR else Role.ON_SURFACE_VARIANT,
        )
        val row = Column(spacingDp = 2f, paddingDp = 16f)
            .add(description)
            .add(permission)
        holders[row] = PermissionHolder(description, permission)
        return row
    }

    private fun bindPermissionRow(row: Node, index: Int) {
        val item = items[index]
        val holder = holders[row] ?: return
        holder.description.text = item.description ?: unknown
        holder.permission.text = item.permission
    }

    override fun setPermissions(items: List<PermissionModel>) {
        this.items.clear()
        this.items.addAll(items)
        list.notifyDataChanged()
    }

    override fun navigationClicks(): Observable<Unit> = navigationRelay

    private companion object {
        const val TYPE_SAFE = 0
        const val TYPE_DANGEROUS = 1
    }
}
