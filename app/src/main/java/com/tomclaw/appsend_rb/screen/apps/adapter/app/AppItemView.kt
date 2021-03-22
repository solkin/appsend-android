package com.tomclaw.appsend_rb.screen.apps.adapter.app

import android.content.Context
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.avito.konveyor.adapter.BaseViewHolder
import com.avito.konveyor.blueprint.ItemView
import com.tomclaw.appsend_rb.R
import com.tomclaw.appsend_rb.core.GlideApp
import com.tomclaw.appsend_rb.util.AppIconData
import com.tomclaw.appsend_rb.util.bind

interface AppItemView : ItemView {

    fun setIcon(iconData: AppIconData)

    fun setName(name: String)

    fun setVersion(version: String?)

    fun setSize(size: String?)

    fun setTime(time: String?)

    fun setOnClickListener(listener: (() -> Unit)?)

}

class AppItemViewHolder(view: View) : BaseViewHolder(view), AppItemView {

    private val context: Context = view.context
    private val icon: ImageView = view.findViewById(R.id.app_icon)
    private val name: TextView = view.findViewById(R.id.app_name)
    private val version: TextView = view.findViewById(R.id.app_version)
    private val size: TextView = view.findViewById(R.id.app_size)
    private val time: TextView = view.findViewById(R.id.app_time)

    private var listener: (() -> Unit)? = null

    init {
        view.setOnClickListener { listener?.invoke() }
    }

    override fun setIcon(iconData: AppIconData) {
        try {
            GlideApp.with(context)
                .load(iconData)
                .into(icon)
        } catch (ignored: Throwable) {
        }
    }

    override fun setName(name: String) {
        this.name.bind(name)
    }

    override fun setVersion(version: String?) {
        this.version.bind(version)
    }

    override fun setSize(size: String?) {
        this.size.bind(size)
    }

    override fun setTime(time: String?) {
        this.time.bind(time)
    }

    override fun setOnClickListener(listener: (() -> Unit)?) {
        this.listener = listener
    }

    override fun onUnbind() {
        this.listener = null
    }

}