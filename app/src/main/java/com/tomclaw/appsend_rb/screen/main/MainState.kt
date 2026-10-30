package com.tomclaw.appsend_rb.screen.main

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class MainState(val tab: MainTab) : Parcelable
