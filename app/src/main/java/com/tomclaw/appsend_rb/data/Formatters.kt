package com.tomclaw.appsend_rb.data

import android.content.Context
import android.text.format.DateUtils
import android.text.format.Formatter
import com.tomclaw.appsend_rb.domain.ValueFormatter
import java.text.DateFormat

class AndroidValueFormatter(private val context: Context) : ValueFormatter {

    override fun bytes(value: Long): String = Formatter.formatShortFileSize(context, value)

    override fun relativeTime(time: Long): String = DateUtils.getRelativeTimeSpanString(
        time,
        System.currentTimeMillis(),
        DateUtils.DAY_IN_MILLIS,
        DateUtils.FORMAT_ABBREV_RELATIVE,
    ).toString()

    override fun date(time: Long): String =
        DateFormat.getDateInstance(DateFormat.MEDIUM).format(time)

}
