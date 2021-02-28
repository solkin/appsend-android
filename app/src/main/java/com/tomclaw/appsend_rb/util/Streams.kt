package com.tomclaw.appsend_rb.util

import java.io.Closeable
import java.io.IOException

fun Closeable?.safeClose() {
    try {
        this?.close()
    } catch (ignored: IOException) {
    }
}
