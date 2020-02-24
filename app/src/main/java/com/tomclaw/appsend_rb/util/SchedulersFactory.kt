package com.tomclaw.appsend_rb.util

import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

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
