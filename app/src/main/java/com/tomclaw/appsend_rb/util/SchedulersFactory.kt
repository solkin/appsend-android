package com.tomclaw.appsend_rb.util

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.schedulers.Schedulers


interface SchedulersFactory {

    fun io(): Scheduler

    fun single(): Scheduler

    fun mainThread(): Scheduler

}

class SchedulersFactoryImpl : SchedulersFactory {

    override fun io(): Scheduler {
        return Schedulers.io()
    }

    override fun single(): Scheduler {
        return Schedulers.single()
    }

    override fun mainThread(): Scheduler {
        return AndroidSchedulers.mainThread()
    }
}
