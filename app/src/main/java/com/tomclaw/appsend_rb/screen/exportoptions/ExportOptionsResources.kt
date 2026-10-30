package com.tomclaw.appsend_rb.screen.exportoptions

import android.content.Context
import com.tomclaw.appsend_rb.R

class ExportOptionsResourcesImpl(private val context: Context) : ExportOptionsResources {

    override fun appsCount(count: Int): String =
        context.resources.getQuantityString(R.plurals.export_title_apps, count, count)

    override fun filesCount(count: Int): String =
        context.resources.getQuantityString(R.plurals.export_files_count, count, count)

}
