package com.tomclaw.appsend_rb.screen.exportoptions

import android.os.Parcelable
import com.tomclaw.appsend_rb.domain.ExportAction
import com.tomclaw.appsend_rb.domain.ExportFormat
import kotlinx.parcelize.Parcelize

@Parcelize
data class ExportOptionsState(
    val format: ExportFormat,
    val pendingAction: ExportAction?,
) : Parcelable
