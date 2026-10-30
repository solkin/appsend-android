package com.tomclaw.appsend_rb.data

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.tomclaw.appsend_rb.domain.Clipboard
import com.tomclaw.appsend_rb.domain.SchedulersFactory
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.schedulers.Schedulers

class SchedulersFactoryImpl : SchedulersFactory {

    override fun io(): Scheduler = Schedulers.io()

    override fun mainThread(): Scheduler = AndroidSchedulers.mainThread()

}

class SystemClipboard(private val context: Context) : Clipboard {

    override fun copy(label: String, text: String) {
        context.getSystemService(ClipboardManager::class.java)
            .setPrimaryClip(ClipData.newPlainText(label, text))
    }

}
