package com.tomclaw.appsend_rb.util

import android.Manifest
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.contract.ActivityResultContracts
import com.tomclaw.appsend_rb.domain.StorageAccess

/**
 * Asks for the storage permission that exports need on Android 9 and older. The result
 * goes to [onResult], fixed at construction, so it still arrives after the host was
 * re-created while the system dialog was up.
 */
class StorageAccessRequester(
    caller: ActivityResultCaller,
    private val shouldShowRationale: (String) -> Boolean,
    private val onResult: (StorageAccess) -> Unit,
) {

    private val launcher = caller.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        onResult(
            when {
                granted -> StorageAccess.GRANTED
                // Android has no "don't ask again" flag: the system stopping to suggest
                // a rationale is the only signal, and only app settings can undo it.
                !shouldShowRationale(PERMISSION) -> StorageAccess.PERMANENTLY_DENIED
                else -> StorageAccess.DENIED
            }
        )
    }

    fun request() = launcher.launch(PERMISSION)

}

private const val PERMISSION = Manifest.permission.WRITE_EXTERNAL_STORAGE
