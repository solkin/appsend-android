package com.tomclaw.appsend_rb.screen.install

import android.os.Parcelable
import com.tomclaw.appsend_rb.domain.InstallFailure
import kotlinx.parcelize.Parcelize

/** What must outlive the screen. A running install is kept by the gateway instead. */
@Parcelize
data class InstallState(
    val installed: Boolean,
    val failure: InstallFailure?,
) : Parcelable
