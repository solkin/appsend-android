package com.tomclaw.appsend_rb.screen.apps.adapter.app

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.avito.konveyor.adapter.BaseViewHolder
import com.avito.konveyor.blueprint.ItemView
import com.tomclaw.appsend_rb.R
import com.tomclaw.appsend_rb.util.bind
import com.tomclaw.appsend_rb.util.hide
import com.tomclaw.appsend_rb.util.show
import com.tomclaw.imageloader.util.*

interface AppItemView : ItemView {

    fun setIcon(url: String?)

    fun setName(name: String)

    fun setVersion(version: String?)

    fun setSize(size: String?)

    fun setTime(time: String?)

    fun setBadgeVisible(visible: Boolean)

    fun setOnClickListener(listener: (() -> Unit)?)

}

class AppItemViewHolder(view: View) : BaseViewHolder(view), AppItemView {

    private val icon: ImageView = view.findViewById(R.id.app_icon)
    private val name: TextView = view.findViewById(R.id.app_name)
    private val version: TextView = view.findViewById(R.id.app_version)
    private val size: TextView = view.findViewById(R.id.app_size)
    private val time: TextView = view.findViewById(R.id.app_time)
    private val badge: View = view.findViewById(R.id.badge_new)

    private var listener: (() -> Unit)? = null

    init {
        view.setOnClickListener { listener?.invoke() }
    }

    override fun setIcon(url: String?) {
        icon.scaleType = ImageView.ScaleType.CENTER_CROP
        icon.fetch(url.orEmpty()) {
            centerCrop()
            placeholder(R.drawable.app_placeholder)
            error(R.drawable.app_placeholder)
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

    override fun setBadgeVisible(visible: Boolean) {
        if (visible) {
            badge.show()
        } else {
            badge.hide()
        }
    }

    override fun setOnClickListener(listener: (() -> Unit)?) {
        this.listener = listener
    }

    override fun onUnbind() {
        this.listener = null
    }

}