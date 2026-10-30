package com.tomclaw.appsend_rb.domain

import io.reactivex.rxjava3.core.Scheduler

interface Analytics {

    fun register()

    fun trackEvent(name: String)

    fun trackEvent(name: String, tags: Map<String, String>)

    fun trackEvent(name: String, tags: Map<String, String>, fields: Map<String, Double>)

    fun trackException(throwable: Throwable, context: Map<String, String>)

}

interface SchedulersFactory {

    fun io(): Scheduler

    fun mainThread(): Scheduler

}

interface Clipboard {

    fun copy(label: String, text: String)

}

/** Turns domain values into text. Presenters render through this port. */
interface ValueFormatter {

    fun bytes(value: Long): String

    fun relativeTime(time: Long): String

    fun date(time: Long): String

}
