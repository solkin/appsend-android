package com.tomclaw.appsend_rb.screen.apps

import android.view.View

interface AppsView {

    fun showProgress()

    fun showContent()

}

class AppsViewImpl(
        private val view: View
) : AppsView {

    init {

    }

    override fun showProgress() {

    }

    override fun showContent() {

    }

}
