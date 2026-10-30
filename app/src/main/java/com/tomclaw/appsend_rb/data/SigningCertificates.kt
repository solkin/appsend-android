package com.tomclaw.appsend_rb.data

import android.content.pm.PackageInfo
import android.content.pm.Signature
import android.os.Build
import java.security.MessageDigest

/**
 * SHA-256 fingerprints formatted as `AB:CD:…`. For a single signer this is the whole
 * rotation history with the current key last, for several signers just those signers.
 */
fun signingCertificates(packageInfo: PackageInfo): List<String> {
    val signatures: List<Signature> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        packageInfo.signingInfo?.let { info ->
            if (info.hasMultipleSigners()) info.apkContentsSigners else info.signingCertificateHistory
        }?.toList().orEmpty()
    } else {
        @Suppress("DEPRECATION")
        packageInfo.signatures?.toList().orEmpty()
    }
    return signatures.map { fingerprint(it.toByteArray()) }
}

private fun fingerprint(certificate: ByteArray): String =
    MessageDigest.getInstance("SHA-256")
        .digest(certificate)
        .joinToString(":") { "%02X".format(it) }
