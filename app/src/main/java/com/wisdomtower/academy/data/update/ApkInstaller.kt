package com.wisdomtower.academy.data.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.FileProvider
import java.io.File

object ApkInstaller {
    fun install(context: Context, apk: File) {
        validateUpdate(context, apk)

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

    /**
     * Rejects a downloaded archive unless it is a genuine update for this exact
     * installation: same application id, newer version, and a signing certificate
     * accepted by the installed app's signing identity/lineage.
     *
     * Android's package installer still performs its own signature and install checks.
     */
    @Suppress("DEPRECATION")
    private fun validateUpdate(context: Context, apk: File) {
        require(apk.isFile && apk.length() > 0L) {
            "Downloaded APK is missing or empty."
        }

        val packageManager = context.packageManager
        val archive = packageManager.getPackageArchiveInfo(
            apk.absolutePath,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageManager.GET_SIGNING_CERTIFICATES
            } else {
                PackageManager.GET_SIGNATURES
            }
        ) ?: error("The downloaded file is not a readable Android APK.")

        require(archive.packageName == context.packageName) {
            "This APK belongs to a different app and cannot update Wisdom Tower Academy."
        }

        val installed = packageManager.getPackageInfo(
            context.packageName,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageManager.GET_SIGNING_CERTIFICATES
            } else {
                PackageManager.GET_SIGNATURES
            }
        )

        require(archive.versionCode > installed.versionCode) {
            "The downloaded APK is not newer than the installed version."
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val archiveSigning = archive.signingInfo
                ?: error("The APK signing information could not be read.")
            val archiveSigners = archiveSigning.apkContentsSigners
            require(archiveSigners.isNotEmpty()) {
                "The downloaded APK has no signing certificate."
            }

            val accepted = if (archiveSigning.hasMultipleSigners()) {
                val installedSigning = installed.signingInfo
                    ?: error("The installed app signing information could not be read.")
                val installedSigners = installedSigning.apkContentsSigners
                installedSigners.isNotEmpty() &&
                    archiveSigners.size == installedSigners.size &&
                    archiveSigners.all { candidate ->
                        installedSigners.any { current -> candidate == current }
                    }
            } else {
                archiveSigners.all { candidate ->
                    packageManager.hasSigningCertificate(
                        context.packageName,
                        candidate.toByteArray(),
                        PackageManager.CERT_INPUT_RAW_X509
                    )
                }
            }

            require(accepted) {
                "This APK is not signed by a certificate trusted by the installed app."
            }
        } else {
            val archiveSigners = archive.signatures.orEmpty()
            val installedSigners = installed.signatures.orEmpty()
            require(
                archiveSigners.isNotEmpty() &&
                    installedSigners.isNotEmpty() &&
                    archiveSigners.size == installedSigners.size &&
                    archiveSigners.all { candidate ->
                        installedSigners.any { current -> candidate == current }
                    }
            ) {
                "This APK is not signed with the installed app's certificate."
            }
        }
    }
}
