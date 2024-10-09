package com.tomclaw.appsend_rb.screen.permissions.adapter.unsafe

import android.os.Parcelable
import com.avito.konveyor.blueprint.Item
import kotlinx.parcelize.Parcelize

@Parcelize
data class UnsafePermissionItem(
    override val id: Long,
    val description: String?,
    val permission: String,
) : Item, Parcelable
