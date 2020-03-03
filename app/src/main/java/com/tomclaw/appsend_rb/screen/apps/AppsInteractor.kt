package com.tomclaw.appsend_rb.screen.apps

import com.tomclaw.appsend_rb.dto.AppEntity
import com.tomclaw.appsend_rb.util.SchedulersFactory
import io.reactivex.Observable
import io.reactivex.Single

interface AppsInteractor {
    fun loadApps(): Observable<List<AppEntity>>

}

class AppsInteractorImpl(
        private val schedulers: SchedulersFactory
) : AppsInteractor {

    override fun loadApps(): Observable<List<AppEntity>> {
        return Single.just(listOf(AppEntity(
                label = "AppSend",
                packageName = "com.tomclaw.appsend_rb",
                versionName = "1.0",
                versionCode = 15,
                path = "",
                size = 1024,
                firstInstallTime = 1024,
                lastUpdateTime = 1024
        ))).toObservable().subscribeOn(schedulers.io())
    }

}