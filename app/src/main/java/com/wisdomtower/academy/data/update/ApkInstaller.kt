package com.wisdomtower.academy.data.update

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

object ApkInstaller {
    fun install(context: Context, apk: File) {
        require(apk.isFile && apk.length() > 0L) { "Downloaded APK is missing or empty." }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.updatefiles",
            apk
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    }
}
