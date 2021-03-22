package com.tomclaw.appsend_rb.screen.apps

import android.content.Context
import com.tomclaw.appsend_rb.util.FileHelper
import java.text.DateFormat

interface ResourceProvider {

    fun formatBytes(bytes: Long): String

    fun formatTime(time: Long): String

}

class ResourceProviderImpl(
    private val context: Context,
    private val dateFormat: DateFormat
) : ResourceProvider {

    override fun formatBytes(bytes: Long): String {
        return FileHelper.formatBytes(context.resources, bytes)
    }

    override fun formatTime(time: Long): String {
        return dateFormat.format(time)
    }

}
