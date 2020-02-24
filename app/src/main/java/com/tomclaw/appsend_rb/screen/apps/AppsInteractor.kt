package com.tomclaw.appsend_rb.screen.apps

import com.tomclaw.appsend_rb.util.SchedulersFactory

interface AppsInteractor {

}

class AppsInteractorImpl(
        private val schedulers: SchedulersFactory
) : AppsInteractor {

}