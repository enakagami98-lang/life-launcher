package com.enakagami.lifelauncher

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.security.MessageDigest

/** 改造対策：このアプリが本物の鍵で署名されているかを調べる */
object Integrity {
    fun genuine(context: Context): Boolean {
        val allowed = BuildConfig.CERTS.split(",").filter { it.isNotEmpty() }
        // 鍵の指紋が登録されていない（Play の鍵をまだ登録していない）ときは確認しない
        if (allowed.isEmpty()) return true
        return runCatching { signatures(context).any { it in allowed } }.getOrDefault(false)
    }

    @Suppress("DEPRECATION")
    private fun signatures(context: Context): List<String> {
        val pm = context.packageManager
        val sigs = if (Build.VERSION.SDK_INT >= 28) {
            val info = pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES).signingInfo
                ?: return emptyList()
            if (info.hasMultipleSigners()) info.apkContentsSigners else info.signingCertificateHistory
        } else {
            pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES).signatures
        } ?: return emptyList()
        return sigs.map { sig ->
            MessageDigest.getInstance("SHA-256").digest(sig.toByteArray()).joinToString("") { "%02x".format(it) }
        }
    }
}
