package com.tomclaw.appsend_rb.screen.permissions

/** Display model for one requested permission row. */
data class PermissionModel(
    val permission: String,
    val description: String?,
    val dangerous: Boolean
)

interface PermissionsConverter {

    fun convert(permission: String): PermissionModel

}

class PermissionsConverterImpl(
    private val permissionInfoProvider: PermissionInfoProvider
) : PermissionsConverter {

    override fun convert(permission: String): PermissionModel {
        val brief = permissionInfoProvider.getPermissionBrief(permission)
        return PermissionModel(
            permission = brief.permission,
            description = brief.description,
            dangerous = brief.dangerous
        )
    }

}
