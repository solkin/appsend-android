package com.tomclaw.appsend_rb.screen.permissions

import android.content.res.Resources
import com.tomclaw.appsend_rb.R

interface PermissionsResourceProvider {

    fun getUnknownPermissionString(): String
}

class PermissionsResourceProviderImpl(
    private val resources: Resources
) : PermissionsResourceProvider {

    override fun getUnknownPermissionString(): String {
        return resources.getString(R.string.unknown_permission_description)
    }

}
