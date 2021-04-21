package com.tomclaw.appsend_rb.screen.about

import android.view.View

interface AboutView {
}

class AboutViewImpl(
    private val view: View,
) : AboutView {
}