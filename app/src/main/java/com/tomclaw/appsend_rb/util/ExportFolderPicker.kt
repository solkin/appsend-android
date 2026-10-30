package com.tomclaw.appsend_rb.util

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.contract.ActivityResultContracts

/**
 * Lets the user choose an export folder and keeps access to it across restarts. The
 * result goes to [onPicked], fixed at construction, so it survives the host's re-creation.
 */
class ExportFolderPicker(
    caller: ActivityResultCaller,
    context: Context,
    private val onPicked: (String) -> Unit,
) {

    private val appContext = context.applicationContext

    private val launcher = caller.registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            appContext.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
            onPicked(uri.toString())
        }
    }

    fun pick() = launcher.launch(null)

}
